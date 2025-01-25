/********************************************************************************/
/*                                                                              */
/*              IQsignDatabase.java                                             */
/*                                                                              */
/*      Database operations for iQsign server                                   */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2025 Steven P. Reiss                                          */
/*********************************************************************************
 *  Copyright 2025, Steven P. Reiss, Rehoboth MA.                                *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of the holder or affiliations not be used   *
 *  in advertising or publicity pertaining to distribution of the software       *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  THE COPYRIGHT HOLDER DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS            *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL THE HOLDER            *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/


package edu.brown.cs.iqsign;

import java.io.File;
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
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IQsignMain iqsign_main;
private Connection sql_database;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

IQsignDatabase(IQsignMain main,String db)
{
   iqsign_main = main;
   sql_database = null;
   
   try {
      File f0 = iqsign_main.getBaseDirectory();
      File f1 = new File(f0,"secret");
      File dbf = new File(f1,"Database.props");
      IvyLog.logD("Using database file " + dbf);
      IvyDatabase.setProperties(dbf);
      sql_database = IvyDatabase.openDatabase(db);
    }
   catch (Throwable t) {
      IvyLog.logE("Database connection problem",t);
      IQsignMain.reportError("Can't connect to database " + db); 
    }
}



/********************************************************************************/
/*                                                                              */
/*      Session management methods                                              */
/*                                                                              */
/********************************************************************************/

void deleteOldRestSessions()
{
   String q = "DELETE FROM iQsignRestful WHERE last_used + interval '4 days' < CURRENT_TIMESTAMP";
   sqlUpdate(q);
}


void updateSession(String sid,String user)
{
   String q = "UPDATE iQsignRestful SET userid = $1, last_used = CURRENT_TIMESTAMP " +
      "WHERE session = $2";
   sqlUpdate(q,toId(user),sid);
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
      long lupt = json.getLong("last_time");
      if (now - lupt <= SESSION_TIMEOUT) { 
         IQsignSession bs = new IQsignSession(bss,json);   
         return bs;
       }
    }
      
   return null;
}




/********************************************************************************/
/*                                                                              */
/*      Get namekey for all signs                                               */
/*                                                                              */
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
/*                                                                              */
/*      Login management                                                        */
/*                                                                              */
/********************************************************************************/

IQsignUser findUser(String uid) 
{
   if (uid == null) return null;
   
   String q1 = "SELECT * FROM iQsignUsers WHERE email = $1 OR username = $2 AND valid";
   JSONObject json = sqlQuery1(q1,uid,uid);
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


boolean registerValidator(String email,String valid)
{
   String q1 = "INSERT INTO iQsignValidator ( userid, validator, timeout ) " +
      "VALUES ( ( SELECT id FROM iQsignUsers WHERE email = $1 ), $2, " +
      "( CURRENT_TIMESTAMP + INTERVAL '1' DAY ) )";
   int ct = sqlUpdate(q1,email,valid);
   return ct > 0;
}


IQsignLoginCode checkAccessToken(String token)
{
   String q1 = "SELECT * FROM iQsignLoginCodes WHERE code = $1";
   String q2 = "UPDATE iQsignLoginCodes SET last_used = CURRENT_TIMETTAMP WHERE code = $1";
   
   JSONObject rslt = sqlQuery1(q1,token);
   if (rslt !=  null) {
      sqlUpdate(q2,token);
    }
   
   IQsignLoginCode lcrslt = null;
   if (rslt != null) lcrslt = new IQsignLoginCode(rslt);
   
   return lcrslt;
}


String addLoginCode(String uid,String sid)
{
   String code = IQsignMain.randomString(24);
   String q1 = "INSERT INTO iQsignLoginCodes ( code, userid, signid ) " +
      "VALUES ( $1, $2, $3 )";
   sqlUpdate(q1,code,toId(uid),toId(sid));
   return code;
}


void unregisterUser(String email) 
{
   String q2 = "DELETE FROM iQsignValidator WHERE userid = $1";
   String q3 = "DELETE FROM iQsignSigns WHERE userid = $1";
   String q4 = "DELETE FROM iQsignUsers WHERE id = $1";
   
   IQsignUser user = findUser(email);
   if (user == null) return;
   String uid = user.getUserId();
   sqlUpdate(q2,toId(uid));
   sqlUpdate(q3,toId(uid));
   sqlUpdate(q4,toId(uid));
}


void removeUser(String uidstr)
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
   Object uid = toId(uidstr);
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


/********************************************************************************/
/*                                                                              */
/*      Sign Management                                                         */
/*                                                                              */
/********************************************************************************/

IQsignSign createSign(String uid,String name,String key,String cnts)
{
   String q1 = "INSERT INTO iQsignSigns ( id, userid, name, namekey, lastsign ) " +
      "VALUES ( DEFAULT, $1, $2, $3, $4 )";
   sqlUpdate(q1,toId(uid),name,key,cnts);
   return findSignByKey(key);
}


IQsignSign findSignByKey(String key)
{
   String q1 = "SELECT * FROM iQsignSigns WHERE namekey = $1";
   JSONObject rslt = sqlQuery1(q1,key);
   if (rslt == null) return null;
   return new IQsignSign(iqsign_main,rslt);  
}

IQsignSign findSignById(String sid)
{
   if (sid == null) return null;
   String q1 = "SELECT * FROM iQsignSigns WHERE id = $1";
   JSONObject rslt = sqlQuery1(q1,toId(sid));
   if (rslt == null) return null;
   return new IQsignSign(iqsign_main,rslt);  
}


IQsignSign findSign(String id,String uid,String namekey)
{
   String q1 = "SELECT * FROM iQsignSigns WHERE id = $1 AND userid = $2 AND namekey = $3";
   JSONObject rslt = sqlQuery1(q1,toId(id),toId(uid),namekey);
   if (rslt == null) return null;
   return new IQsignSign(iqsign_main,rslt);  
}


String getDefineName(String contents,String uid)
{
   String q1 = "SELECT * FROM iQsignDefines WHERE contents = $1 AND userid = $2";
   String q2 = "SELECT * FROM iQsignDefines WHERE contents = $1 AND userid IS NULL";
   try {
      ResultSet rs = sqlQuery(q1,contents,toId(uid));
      if (!rs.next()) {
         rs = sqlQuery(q2,contents);
         if (!rs.next()) return null;
       }
      return rs.getString("name");
    }
   catch (SQLException e) {
      IvyLog.logD("SQL problem",e);
    }
   
   return null;
}


boolean updateDisplayName(String id,String name)
{
   String q1 = "UPDATE iQsignSigns SET displayname = $1 WHERE id = $2";
   int ct = sqlUpdate(q1,name,toId(id));
   return ct > 0;
}


void addDefineName(String uid,String dname,String contents,boolean useronly)
{
   String q1 = "SELECT * FROM iQsignDefines WHERE userid = $1 AND name = $2";
   String q2 = "SELECT * FROM iQsignDefines WHERE userid IS NULL AND name = $1"; 
   String q3 = "INSERT INTO iQsignDefines(id,userid,name,contents) " +
         "VALUES ( DEFAULT, $1, $2, $3 )";
   String q4 = "UPDATE iQsignDefines SET contents = $1 WHERE id = $2";
   String q5 = "INSERT INTO iqSignUseCounts (defineid,userid) VALUES ($1,$2)";
   
   boolean user = true;
   JSONObject json = sqlQuery1(q1,toId(uid),dname);
   if (json == null && !useronly) {
      user = false;
      json = sqlQuery1(q2,dname);
    }
   if (json == null) {
      // no previous definition
      sqlUpdate(q3,toId(uid),dname,contents);
      IQsignDefinedImage di = getDefineData(null,dname,uid);
      sqlUpdate(q5,toId(di.getId()),toId(uid));
    }
   else if (!json.getString("contents").equals(contents) && user) {
      sqlUpdate(q4,contents,IQsignMain.getId(json,"id"));
    }
}


IQsignDefinedImage getDefineData(String id,String name,String userid)
{
   String q1 = "SELECT * FROM iQsignDefines WHERE id = $1";
   String q2 = "SELECT * FROM iQsignDefines WHERE userid = $1 AND name = $2";
   String q3 = "SELECT * FROM iQsignDefines WHERE userid IS NULL AND name = $1"; 
   
   List<JSONObject> rslt;
   if (id != null) {
      rslt = sqlQueryN(q1,toId(id));
    }
   else {
      rslt = sqlQueryN(q2,toId(userid),name);
      if (rslt.isEmpty()) {
         rslt = sqlQueryN(q3,name);
       }
    }
   if (rslt.isEmpty()) return null;
   
   return new IQsignDefinedImage(rslt.get(0));
}


void removeDefineData(String name,String userid)
{
   String q1 = "DELETE FROM iQsignUsageCounts WHERE defineid = $1";
   String q2 = "DELETE FROM iQsignDefines WHERE name = $1 AND userid = $2";
   IQsignDefinedImage defdata = getDefineData(null,name,userid);
   if (defdata == null) return;
   if (userid.equals(defdata.getUserId())) {
      sqlUpdate(q1,toId(defdata.getId()));
    }
   sqlUpdate(q2,name,userid);
}

List<IQsignSign> getAllSignsForUser(String uid)
{
   String q1 = "SELECT * FROM iQsignSigns WHERE userid = $1";
   List<JSONObject> qr = sqlQueryN(q1,toId(uid));
   List<IQsignSign> rslt = new ArrayList<>();
   for (JSONObject jo : qr) {
      IQsignSign sign = new IQsignSign(iqsign_main,jo);
      rslt.add(sign);
    }
   return rslt;
}


void changeSign(String sid,String cnts)
{
   IvyLog.logD("SIGN UPDATE " + sid + " " + cnts);
   
   String q1 = "UPDATE iQsignSigns SET lastsign = $1, displayname = NULL " +
         "WHERE id = $2";
   
   sqlUpdate(q1,cnts,toId(sid));
}



/********************************************************************************/
/*                                                                              */
/*      Save or update image                                                    */
/*                                                                              */
/********************************************************************************/

void saveOrUpdateImage(String name,String file)
{
   String q1 = "SELECT * FROM iQsignImages WHERE userid IS NULL AND name = $1";
   String q2 = "DELETE FROM iQsignImages WHERE userid IS NULL AND name = $1";
   String q3 = "INSERT INTO iQsignImages ( userid, name, url ) VALUES ( NULL, $1, $2 )";
   String q4 = "INSERT INTO iQsignImages ( userid, name, file ) VALUES ( NULL, $1, $2 )";  
   
   boolean isurl = (file.startsWith("http:") || file.startsWith("https:"));
   
   try {
      ResultSet rs = sqlQuery(q1,name);
      boolean fnd = false;
      int ct = 0;
      while (rs.next()) {
         ++ct;
         String rf = rs.getString("file");
         String ru = rs.getString("url");
         if (file.equals(rf) || file.equals(ru)) continue;
         fnd = true;
       }
      if (fnd) return;
      if (ct > 0) {
         sqlUpdate(q2,name);
       }
      if (isurl) {
         sqlUpdate(q3,name,file);
       }
      else {
         sqlUpdate(q4,name,file);
       }
    }
   catch (SQLException e) {
      IvyLog.logE("Problem updating image " + name,e);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Save or update a default sign                                           */
/*                                                                              */
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
      sqlUpdate(q2,cnts,dlm,toId(di.getId()));
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
         toId(sign.getId())); 
}



List<IQsignDefinedImage> getSavedSigns(String uid)
{
   String q1 = "SELECT * FROM iQsignDefines D " +
      "LEFT OUTER JOIN iQsignUseCounts C ON D.id = C.defineid " +
      "WHERE D.userid = $1 OR D.userid IS NULL " +
      "ORDER BY C.count DESC, C.last_used DESC, D.id";
   List<JSONObject> qrslt = sqlQueryN(q1,toId(uid));
   List<IQsignDefinedImage> rslt = new ArrayList<>();
   for (JSONObject jo : qrslt) {
      rslt.add(new IQsignDefinedImage(jo));
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Utility methods                                                         */
/*                                                                              */
/********************************************************************************/

private int sqlUpdate(String query,Object... data) 
{
   IvyLog.logD("SQL: " + query + " " + getDataString(data));
   
   try {
      PreparedStatement pst = setupStatement(query,data);
      return pst.executeUpdate();
    }
   catch (SQLException e) {
      IvyLog.logE("SQL problem",e);
    }
   
   return 0;
}


private ResultSet sqlQuery(String query,Object... data)
{
   IvyLog.logD("SQL: " + query + " " + getDataString(data));
   
   try {
      PreparedStatement pst = setupStatement(query,data);
      return pst.executeQuery();
    }
   catch (SQLException e) {
      IvyLog.logE("SQL problem",e);
    }
   
   return null;
}


private JSONObject sqlQuery1(String query,Object... data)
{
   IvyLog.logD("SQL: " + query + " " + getDataString(data));
   
   JSONObject rslt = null;
   
   try {
      PreparedStatement pst = setupStatement(query,data);
      ResultSet rs = pst.executeQuery();
      if (rs.next()) {
         rslt = getJsonFromResultSet(rs);
       }
      if (rs.next()) rslt = null;
    }
   catch (SQLException e) {
      IvyLog.logE("SQL problem",e);
    }
   
   return rslt;
}



private List<JSONObject> sqlQueryN(String query,Object... data)
{
   IvyLog.logD("SQL: " + query + " " + getDataString(data));
   
   List<JSONObject> rslt = new ArrayList<>();;
   
   try {
      PreparedStatement pst = setupStatement(query,data);
      ResultSet rs = pst.executeQuery();
      while (rs.next()) {
         JSONObject json = getJsonFromResultSet(rs);
         rslt.add(json);
       }
    }
   catch (SQLException e) {
      IvyLog.logE("SQL problem",e);
    }
   
   return rslt;
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
      IvyLog.logE("Database problem decoding result set ",e);
    }
   
   return rslt;
}


private Object toId(Object s)
{
   return IQsignMain.toDatabaseId(s); 
}




}       // end of class IQsignDatabase




/* end of IQsignDatabase.java */

