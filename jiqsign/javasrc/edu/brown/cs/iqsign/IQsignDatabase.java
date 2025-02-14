/********************************************************************************/
/*										*/
/*		IQsignDatabase.java						*/
/*										*/
/*	Database operations for iQsign server					*/
/*										*/
/********************************************************************************/
/*	Copyright 2025 Steven P. Reiss						*/
/*********************************************************************************
 *  Copyright 2025, Steven P. Reiss, Rehoboth MA.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of the holder or affiliations not be used	 *
 *  in advertising or publicity pertaining to distribution of the software	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  THE COPYRIGHT HOLDER DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL THE HOLDER		 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/


package edu.brown.cs.iqsign;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import edu.brown.cs.ivy.bower.BowerConstants.BowerSessionStore;
import edu.brown.cs.ivy.file.IvyDatabase;
import edu.brown.cs.ivy.file.IvyLog;


class IQsignDatabase implements IQsignConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IQsignMain iqsign_main;
private Connection sql_database;
private String     database_name;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

IQsignDatabase(IQsignMain main,String db)
{
   iqsign_main = main;
   sql_database = null;
   database_name = db;

   try {
      File f0 = iqsign_main.getBaseDirectory();
      File f1 = new File(f0,"secret");
      File dbf = new File(f1,"Database.props");
      IvyLog.logD("IQSIGN","Using database file " + dbf);
      IvyDatabase.setProperties(dbf);
    }
   catch (SQLException t) {
      IvyLog.logE("IQSIGN","Database connection problem",t);
      IQsignMain.reportError("Can't connect to database " + db);
    }
   
   checkDatabase();
}



private boolean checkDatabase()
{
   if (sql_database == null) {
      try {
         sql_database = IvyDatabase.openDatabase(database_name);
       }
      catch (Throwable t) {
         IvyLog.logE("IQSIGN","Database connection problem",t);
       }
    }
   
   return sql_database != null;
}



/********************************************************************************/
/*										*/
/*	Session management methods						*/
/*										*/
/********************************************************************************/

void deleteOutOfDateData()
{
   String q1 = "DELETE FROM iQsignRestful WHERE last_used + interval '4 days' < CURRENT_TIMESTAMP";
   String q2 = "DELETE FROM iQsignValidator WHERE timeout + interval '4 days' < CURRENT_TIMESTAMP";
   sqlUpdate(q1);
   sqlUpdate(q2);
   
}


void updateSession(String sid,Number user)
{
   String q = "UPDATE iQsignRestful SET userid = $1, last_used = CURRENT_TIMESTAMP " +
      "WHERE session = $2";
   sqlUpdate(q,user,sid);
}



void removeSession(String sid)
{
   String q = "DELETE FROM iQsignRestful WHERE session = $1";
   sqlUpdate(q,sid);
}




void startSession(String sid,String code)
{
   String q = "INSERT INTO iQsignRestful (session, code) VALUES ( $1, $2 )";
   sqlUpdate(q,sid,code);
}


IQsignSession checkSession(BowerSessionStore<IQsignSession> bss,String sid)
{
   String q = "SELECT * FROM iQsignRestful WHERE session = $1";

   JSONObject json = sqlQuery1(q,sid);
   if (json != null) {
      long now = System.currentTimeMillis();
      long lupt = json.getLong("last_used");
      if (now - lupt <= SESSION_TIMEOUT) {
	 IQsignSession bs = new IQsignSession(bss,json);
	 return bs;
       }
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Get namekey for all signs						*/
/*										*/
/********************************************************************************/

Set<String> getAllSignNameKeys()
{
   String q = "SELECT namekey FROM iQsignSigns";
   Set<String> rslt = new HashSet<>();

   List<JSONObject> qrslt = sqlQueryN(q);
   for (JSONObject jo : qrslt) {
      rslt.add(jo.getString("namekey"));
    }

   return rslt;
}


/********************************************************************************/
/*										*/
/*	Login management							*/
/*										*/
/********************************************************************************/

IQsignUser findUser(String name)
{
   if (name == null) return null;

   String q1 = "SELECT * FROM iQsignUsers WHERE email = $1 OR username = $2 AND valid";
   JSONObject json = sqlQuery1(q1,name,name);
   if (json != null) {
      return new IQsignUser(json);
    }
   return null;
}



IQsignUser findUser(Number uid)
{
   if (uid == null) return null;

   String q1 = "SELECT * FROM iQsignUsers WHERE id = $1 AND valid";
   JSONObject json = sqlQuery1(q1,uid);
   if (json != null) {
      return new IQsignUser(json);
    }
   return null;
}


String checkNoUser(String uid,String email)
{
   String q1 = "SELECT * FROM iQsignUsers WHERE username = $1 OR email = $2";

   List<JSONObject> qr = sqlQueryN(q1,uid,email);
   String rslt = null;
   if (!qr.isEmpty()) {
      JSONObject jo = qr.get(0);
      rslt = "Email or user name already in use";
      if (jo.getString("email").equals(email)) {
	 rslt = "Email already registered";
       }
      else if (jo.getString("username").equals(uid)) {
	 rslt = "User name already in use";
       }
    }

   return rslt;
}


boolean registerUser(String email,String user,String pwd,String apwd,boolean valid)
{
   String q1 = "INSERT INTO iQsignUsers " +
      "( id, email, username, password, altpassword, valid )" +
      "VALUES ( DEFAULT, $1, $2, $3, $4, $5 )";
   int ct = sqlUpdate(q1,email,user,pwd,apwd,valid);
   return ct > 0;
}


boolean registerValidator(String email,String code)
{
   String q1 = "INSERT INTO iQsignValidator ( userid, validator, timeout ) " +
      "VALUES ( ( SELECT id FROM iQsignUsers WHERE email = $1 ), $2, " +
      "( CURRENT_TIMESTAMP + INTERVAL '2 DAYS' ) )";
   int ct = sqlUpdate(q1,email,code);
   return ct > 0;
}


boolean validateUser(String email,String code)
{
   String q1 = "SELECT U.id as userid " +
      "FROM iQsignValidator V, IQsignUsers U " +
      "WHERE V.userid = U.id AND U.email = $1 AND V.validator = $2 AND " +
      "V.timeout > CURRENT_TIMESTAMP";
   String q2 = "DELETE FROM iQsignValidator WHERE userid = $1 AND validator = $2";
   String q3 = "UPDATE iQsignUsers SET valid = TRUE WHERE id = $1";
   
   JSONObject rslt = sqlQuery1(q1,email,code);
   if (rslt == null) return false;
   
   Number uid = rslt.getNumber("userid");
   sqlUpdate(q2,uid,code);
   sqlUpdate(q3,uid);
   
   return true;
}


void updatePassword(Number uid,String pwd,String apwd)
{
   String q1 = "UPDATE iQsignUsers SET password = $1, altpassword = $2, " +
      " temppassword = NULL " +
      "WHERE id = $3";
   sqlUpdate(q1,pwd,apwd,uid);
}


void setTemporaryPassword(Number uid,String tpwd)
{
   String q1 = "UPDATE iQsignUsers SET temppassword = $1 " +
      "WHERE id = $3";
   sqlUpdate(q1,tpwd,uid);
}




IQsignLoginCode checkAccessToken(String token,Number uid,String code)
{
   String q1 = "SELECT * FROM iQsignLoginCodes WHERE code = $1";
   String q2 = "UPDATE iQsignLoginCodes SET last_used = CURRENT_TIMETTAMP WHERE code = $1";
   String q3 = "SELECT * FROM iQsignLoginCodes WHERE userid = $1";

   JSONObject rslt = null;
   if (code == null) {
      rslt = sqlQuery1(q1,token);
    }
   else if (uid != null) {
      List<JSONObject> rslts = sqlQueryN(q3,uid);
      for (JSONObject r : rslts) {
         IQsignLoginCode lc = new IQsignLoginCode(r);
         String c1 = IQsignMain.secureHash(lc.getCode());
         String c2 = IQsignMain.secureHash(c1 + code);
         if (c2.equals(token)) {
            rslt = r;
            break;
          }
       }
    }
   
   if (rslt !=	null) {
      sqlUpdate(q2,token);
    }

   IQsignLoginCode lcrslt = null;
   if (rslt != null) lcrslt = new IQsignLoginCode(rslt);

   return lcrslt;
}


String addLoginCode(Number uid,Number sid)
{
   String code = IQsignMain.randomString(24);
   String q1 = "INSERT INTO iQsignLoginCodes ( code, userid, signid ) " +
      "VALUES ( $1, $2, $3 )";
   sqlUpdate(q1,code,uid,sid);
   return code;
}


void unregisterUser(String email)
{
   String q2 = "DELETE FROM iQsignValidator WHERE userid = $1";
   String q3 = "DELETE FROM iQsignSigns WHERE userid = $1";
   String q4 = "DELETE FROM iQsignUsers WHERE id = $1";

   IQsignUser user = findUser(email);
   if (user == null) return;
   Number uid = user.getUserId();
   sqlUpdate(q2,uid);
   sqlUpdate(q3,uid);
   sqlUpdate(q4,uid);
}


void removeUser(Number uid)
{
   String q1 = "DELETE FROM OauthTokens WHERE userid = $1";
   String q2 = "DELETE FROM OauthCodes WHERE userid = $1";
   String q3 = "DELETE FROM iQsignRestful WHERE userid = $1";
   String q4 = "DELETE FROM iQsignUseCounts WHERE userid = $1";
   String q5 = "DELETE FROM iQsignSignCodes WHERE userid = $1";
   String q6 = "DELETE FROM iQsignDefines WHERE userid = $1";
   String q7 = "DELETE FROM iQsignImages WHERE userid = $1";
   String q8 = "DELETE FROM iQsignSigns WHERE userid = $1";
   String q9 = "DELETE FROM iQsignValidator WHERE userid = $1";
   String q10 = "DELETE FROM iQsignUsers WHERE id = $1";
   sqlUpdate(q1,uid);
   sqlUpdate(q2,uid);
   sqlUpdate(q3,uid);
   sqlUpdate(q4,uid);
   sqlUpdate(q5,uid);
   sqlUpdate(q6,uid);
   sqlUpdate(q7,uid);
   sqlUpdate(q8,uid);
   sqlUpdate(q9,uid);
   sqlUpdate(q10,uid);
}


Number removeSign(Number uid,Number sid)
{
   String q2 = "DELETE FROM iQsignSignCodes WEHRE signid = $1";
   String q3 = "DELETE FROM iQsignLoginCodes WEHRE signid = $1";
   String q4 = "DEELTE FROM OauthTokens WHERE signid = $1";
   String q5 = "DEELTE FROM OauthCodes WHERE signid = $1";
   String q6 = "DELETE FROM iQsignSigns WHERE id = $1";
   
   IQsignSign sign = findSignById(sid);
   if (sign == null) return null;
   if (!sign.getUserId().equals(uid)) return null;
   
   sqlUpdate(q2,sid);
   sqlUpdate(q3,sid);
   sqlUpdate(q4,sid);
   sqlUpdate(q5,sid);
   sqlUpdate(q6,sid);
   
   return sid;
}


/********************************************************************************/
/*										*/
/*	Sign Management 							*/
/*										*/
/********************************************************************************/

IQsignSign createSign(Number uid,String name,String key,String cnts)
{
   String q1 = "INSERT INTO iQsignSigns ( id, userid, name, namekey, lastsign ) " +
      "VALUES ( DEFAULT, $1, $2, $3, $4 )";
   sqlUpdate(q1,uid,name,key,cnts);
   return findSignByKey(key);
}


IQsignSign findSignByKey(String key)
{
   String q1 = "SELECT * FROM iQsignSigns WHERE namekey = $1";
   JSONObject rslt = sqlQuery1(q1,key);
   if (rslt == null) return null;
   return new IQsignSign(iqsign_main,rslt);
}

IQsignSign findSignById(Number sid)
{
   if (sid == null) return null;
   String q1 = "SELECT * FROM iQsignSigns WHERE id = $1";
   JSONObject rslt = sqlQuery1(q1,sid);
   if (rslt == null) return null;
   return new IQsignSign(iqsign_main,rslt);
}


IQsignSign findSign(Number id,Number uid,String namekey)
{
   String q1 = "SELECT * FROM iQsignSigns WHERE id = $1 AND userid = $2 AND namekey = $3";
   JSONObject rslt = sqlQuery1(q1,id,uid,namekey);
   if (rslt == null) return null;
   return new IQsignSign(iqsign_main,rslt);
}


String getDefineName(String contents,Number uid)
{
   String q1 = "SELECT * FROM iQsignDefines WHERE contents = $1 AND userid = $2";
   String q2 = "SELECT * FROM iQsignDefines WHERE contents = $1 AND userid IS NULL";
   
   List<JSONObject> dnms = sqlQueryN(q1,contents,uid);
   if (dnms.isEmpty()) {
      dnms = sqlQueryN(q2,contents);
    }
   if (!dnms.isEmpty()) {
      IQsignDefinedImage di = new IQsignDefinedImage(dnms.get(0));
      return di.getName();
    }

   return null;
}


boolean updateDisplayName(Number id,String name)
{
   String q1 = "UPDATE iQsignSigns SET displayname = $1 WHERE id = $2";
   int ct = sqlUpdate(q1,name,id);
   return ct > 0;
}


void addDefineName(Number uid,String dname,String contents,boolean useronly)
{
   String q1 = "SELECT * FROM iQsignDefines WHERE userid = $1 AND name = $2";
   String q2 = "SELECT * FROM iQsignDefines WHERE userid IS NULL AND name = $1";
   String q3 = "INSERT INTO iQsignDefines(id,userid,name,contents) " +
	 "VALUES ( DEFAULT, $1, $2, $3 )";
   String q4 = "UPDATE iQsignDefines SET contents = $1 WHERE id = $2";
   String q5 = "INSERT INTO iqSignUseCounts (defineid,userid) VALUES ($1,$2)";

   boolean user = true;
   JSONObject json = sqlQuery1(q1,uid,dname);
   if (json == null && !useronly) {
      user = false;
      json = sqlQuery1(q2,dname);
    }
   if (json == null) {
      // no previous definition
      sqlUpdate(q3,uid,dname,contents);
      IQsignDefinedImage di = getDefineData(null,dname,uid);
      sqlUpdate(q5,di.getId(),uid);
    }
   else {
      IQsignDefinedImage di = new IQsignDefinedImage(json);
      if (!di.getContents().equals(contents) && user) {
	 sqlUpdate(q4,contents,di.getId());
       }
    }
}


IQsignDefinedImage getDefineData(Number id,String name,Number userid)
{
   String q1 = "SELECT * FROM iQsignDefines WHERE id = $1";
   String q2 = "SELECT * FROM iQsignDefines WHERE userid = $1 AND name = $2";
   String q3 = "SELECT * FROM iQsignDefines WHERE userid IS NULL AND name = $1";

   List<JSONObject> rslt;
   if (id != null) {
      rslt = sqlQueryN(q1,id);
    }
   else if (userid == null) {
      rslt = sqlQueryN(q3,name);
    }
   else {
      rslt = sqlQueryN(q2,userid,name);
      if (rslt.isEmpty()) {
	 rslt = sqlQueryN(q3,name);
       }
    }
   if (rslt.isEmpty()) return null;

   return new IQsignDefinedImage(rslt.get(0));
}


void removeDefineData(String name,Number userid)
{
   String q1 = "DELETE FROM iQsignUsageCounts WHERE defineid = $1";
   String q2 = "DELETE FROM iQsignDefines WHERE name = $1 AND userid = $2";
   IQsignDefinedImage defdata = getDefineData(null,name,userid);
   if (defdata == null) return;
   if (userid.equals(defdata.getUserId())) {
      sqlUpdate(q1,defdata.getId());
    }
   sqlUpdate(q2,name,userid);
}

List<IQsignSign> getAllSignsForUser(Number uid)
{
   String q1 = "SELECT * FROM iQsignSigns WHERE userid = $1";
   List<JSONObject> qr = sqlQueryN(q1,uid);
   List<IQsignSign> rslt = new ArrayList<>();
   for (JSONObject jo : qr) {
      IQsignSign sign = new IQsignSign(iqsign_main,jo);
      rslt.add(sign);
    }
   return rslt;
}


void changeSign(Number sid,String cnts)
{
   IvyLog.logD("IQSIGN","SIGN UPDATE " + sid + " " + cnts);

   String q1 = "UPDATE iQsignSigns SET lastsign = $1, displayname = NULL " +
	 "WHERE id = $2";

   sqlUpdate(q1,cnts,sid);
}



/********************************************************************************/
/*										*/
/*	Save or update image							*/
/*										*/
/********************************************************************************/

void saveOrUpdateImage(String name,String file,String url,String desc,boolean border)
{
   String q1 = "SELECT * FROM iQsignImages WHERE userid IS NULL AND name = $1";
   String q2 = "DELETE FROM iQsignImages WHERE userid IS NULL AND name = $1";
   String q3 = "INSERT INTO iQsignImages ( userid, name, url, file, is_border, description ) " + 
          "VALUES ( NULL, $1, $2, $3, $4, $5 )";
   
   JSONObject imgjs = sqlQuery1(q1,name);
   if (imgjs != null) {
      IQsignImage img = new IQsignImage(imgjs); 
      String rf = img.getFile();
      String ru = img.getUrl();
      if (file.equals(rf) || file.equals(ru)) return;
      sqlUpdate(q2,name);
    }
   sqlUpdate(q3,name,url,file,border,desc);
}


void saveOrUpdateUserImage(Number uid,String name,String file,String url,String desc,boolean border)
{
   String q1 = "SELECT * FROM iQsignImages WHERE userid = $1 AND name = $2";
   String q2 = "DELETE FROM iQsignImages WHERE userid = $1 AND name = $2";
   String q3 = "INSERT INTO iQsignImages ( userid, name, url, file, is_border, description ) " + 
         "VALUES ( $1, $2, $3, $4, $5, $6 )";
   
   JSONObject imgjs = sqlQuery1(q1,uid,name);
   if (imgjs != null) {
      IQsignImage img = new IQsignImage(imgjs);
      sqlUpdate(q2,uid,name);
      String rf = img.getFile();
      if (rf != null && !rf.isEmpty()) {
         File rff = new File(rf);
         if (!rff.isAbsolute()) {
            File f1 = new File(iqsign_main.getBaseDirectory(),IMAGE_DIRECTORY); 
            rff = new File(f1,rff.getPath());
          }
         rff.delete();
       }
    }
   sqlUpdate(q3,uid,name,url,file,border,desc);
}



List<IQsignImage> findImages(Number uid,boolean border)
{
   String q1 = "SELECT * FROM iQsignImages WHERE (userid IS NULL OR userid = $1) AND " +
      "is_border = $2";
   List<JSONObject> imgs = sqlQueryN(q1,uid,border);
   
   List<IQsignImage> rslt = new ArrayList<>();
   for (JSONObject jo : imgs) {
      rslt.add(new IQsignImage(jo));
    }
   
   return rslt;
}


Set<String> getAllImageFiles()
{
   String q1 = "SELECT file FROM iQsignImages WHERE file IS NOT NULL";
   
   Set<String> rslt = new HashSet<>();
   List<JSONObject> fils = sqlQueryN(q1);
   for (JSONObject js : fils) {
      String fn = js.optString("file",null);
      if (fn != null) rslt.add(fn);
    }
   
   return rslt;
}


/********************************************************************************/
/*										*/
/*	Save or update a default sign						*/
/*										*/
/********************************************************************************/

void saveOrUpdateSign(String name,String cnts,long dlmtime)
{
   String q1 = "SELECT * FROM iQsignDefines WHERE name = $1 AND userid IS NULL";
   String q2 = "UPDATE iQsignDefines SET contents = $1, lastupdate = $2 WHERE id = $3";
   String q3 = "INSERT INTO iQsignDefines (id, userid, name, contents,lastupdate ) " +
	 "VALUES ( DEFAULT, NULL, $1, $2, $3 )";

   Timestamp dlm = new Timestamp(dlmtime);

   JSONObject jo = sqlQuery1(q1,name);
   if (jo != null) {
      IQsignDefinedImage di = new IQsignDefinedImage(jo);
      if (di.getLastUpdate() < dlmtime) return;
      if (di.getContents().equals(cnts)) return;
      sqlUpdate(q2,cnts,dlm,di.getId());
    }
   else {
      sqlUpdate(q3,name,cnts,dlm);
    }
}


void updateSignProperties(IQsignSign sign)
{
   String q1 = "UPDATE iQsignSigns SET name = $1, " +
      "lastupdate = CURRENT_TIMESTAMP, dimension = $2, " +
      "width = $3, height = $4, displayname = NULL " +
      "WHERE id = $5";
   sqlUpdate(q1,sign.getSignName(),
	 sign.getDimension(),sign.getWidth(),sign.getHeight(),
	 sign.getId());
}



List<IQsignDefinedImage> getSavedSigns(Number uid)
{
   String q1 = "SELECT * FROM iQsignDefines D " +
      "LEFT OUTER JOIN iQsignUseCounts C ON D.id = C.defineid " +
      "WHERE D.userid = $1 OR D.userid IS NULL " +
      "ORDER BY C.count DESC, C.last_used DESC, D.id";
   List<JSONObject> qrslt = sqlQueryN(q1,uid);
   List<IQsignDefinedImage> rslt = new ArrayList<>();
   for (JSONObject jo : qrslt) {
      rslt.add(new IQsignDefinedImage(jo));
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

private int sqlUpdate(String query,Object... data)
{
   IvyLog.logD("IQSIGN","SQL: " + query + " " + getDataString(data));

   try {
      return executeUpdateStatement(query,data);
    }
   catch (SQLException e) {
      IvyLog.logE("IQSIGN","SQL problem",e);
    }

   return 0;
}


private JSONObject sqlQuery1(String query,Object... data)
{
   IvyLog.logD("IQSIGN","SQL: " + query + " " + getDataString(data));

   JSONObject rslt = null;

   try {
      ResultSet rs = executeQueryStatement(query,data);
      if (rs.next()) {
	 rslt = getJsonFromResultSet(rs);
       }
      if (rs.next()) rslt = null;
    }
   catch (SQLException e) {
      IvyLog.logE("IQSIGN","SQL problem",e);
    }

   return rslt;
}



private List<JSONObject> sqlQueryN(String query,Object... data)
{
   IvyLog.logD("IQSIGN","SQL: " + query + " " + getDataString(data));

   List<JSONObject> rslt = new ArrayList<>();;

   try {
      ResultSet rs = executeQueryStatement(query,data);
      while (rs.next()) {
	 JSONObject json = getJsonFromResultSet(rs);
	 rslt.add(json);
       }
    }
   catch (SQLException e) {
      IvyLog.logE("IQSIGN","SQL problem",e);
    }

   return rslt;
}


private ResultSet executeQueryStatement(String q,Object... data) throws SQLException
{
   for ( ; ; ) {
      waitForDatabase();
      
      PreparedStatement pst = setupStatement(q,data);
     
      try {
         ResultSet rslt = pst.executeQuery();  
         return rslt;
       }
      catch (SQLException e) {
         if (checkDatabaseError(e)) throw e;
       }
    }
}


private int executeUpdateStatement(String q,Object... data) throws SQLException
{
   for ( ; ; ) {
      waitForDatabase();
      
      PreparedStatement pst = setupStatement(q,data);
      
      try {
         int rslt = pst.executeUpdate();  
         return rslt;
       }
      catch (SQLException e) {
         if (checkDatabaseError(e)) throw e;
       }
    }
}


private void waitForDatabase() 
{
   while (sql_database == null) {
      try {
         Thread.sleep(1000);
       }
      catch (InterruptedException e) { }
      checkDatabase();
    }
}


private boolean checkDatabaseError(SQLException e) 
{
   String msg = e.getMessage();
   if (msg.contains("FATAL")) sql_database = null;
   Throwable ex = e.getCause();
   if (ex instanceof IOException) sql_database = null;
   if (sql_database == null) {
      IvyLog.logE("IQSIGN","Database lost connection",e);
    }
   return sql_database != null;
}



private PreparedStatement setupStatement(String query,Object... data) throws SQLException
{
   query = query.replaceAll("\\$[0-9]+","?");
   PreparedStatement pst = sql_database.prepareStatement(query);
   for (int i = 0; i < data.length; ++i) {
      Object v = data[i];
      if (v instanceof String) {
	 pst.setString(i+1,(String) v);
       }
      else if (v instanceof Integer) {
	 pst.setInt(i+1,(Integer) v);
       }
      else if (v instanceof Long) {
	 pst.setLong(i+1,(Long) v);
       }
      else if (v instanceof Date) {
	 pst.setDate(i+1,(Date) v);
       }
      else if (v instanceof Timestamp) {
	 pst.setTimestamp(i+1,(Timestamp) v);
       }
      else if (v instanceof Boolean) {
	 pst.setBoolean(i+1,(Boolean) v);
       }
      else {
	 pst.setObject(i+1,v);
       }
    }
   return pst;
}


private String getDataString(Object... data)
{
   if (data.length == 0) return "";

   StringBuffer buf = new StringBuffer();
   for (int i = 0; i < data.length; ++i) {
      if (i == 0) buf.append("[");
      else buf.append(",");
      buf.append(String.valueOf(data[i]));
    }
   buf.append("]");

   return buf.toString();
}


private JSONObject getJsonFromResultSet(ResultSet rs)
{
   JSONObject rslt = new JSONObject();
   try {
      ResultSetMetaData meta = rs.getMetaData();
      for (int i = 1; i <= meta.getColumnCount(); ++i) {
	 String nm = meta.getColumnName(i);
	 Object v = rs.getObject(i);
	 if (v instanceof Date) {
	    Date d = (Date) v;
	    v = d.getTime();
	  }
	 else if (v instanceof Timestamp) {
	    Timestamp ts = (Timestamp) v;
	    v = ts.getTime();
	  }
	 if (v != null) rslt.put(nm,v);
       }
    }
   catch (SQLException e) {
      IvyLog.logE("IQSIGN","Database problem decoding result set ",e);
    }

   return rslt;
}







}	// end of class IQsignDatabase




/* end of IQsignDatabase.java */

