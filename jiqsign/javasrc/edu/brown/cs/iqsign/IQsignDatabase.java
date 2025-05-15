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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import edu.brown.cs.ivy.bower.BowerDatabasePool;
import edu.brown.cs.ivy.bower.BowerConstants.BowerSessionStore;
import edu.brown.cs.ivy.file.IvyLog;


class IQsignDatabase implements IQsignConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IQsignMain iqsign_main;
private BowerDatabasePool sql_database;
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
      sql_database = new BowerDatabasePool(dbf,database_name); 
//    IvyDatabase.setProperties(dbf);
    }
   catch (SQLException t) {
      IvyLog.logE("IQSIGN","Database connection problem",t);
      IQsignMain.reportError("Can't connect to database " + db);
    }
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
   sql_database.sqlUpdate(q1);
   sql_database.sqlUpdate(q2);
}


void updateSession(String sid,Number user)
{
   String q = "UPDATE iQsignRestful SET userid = $1, last_used = CURRENT_TIMESTAMP " +
      "WHERE session = $2";
   sql_database.sqlUpdate(q,user,sid);
}



void removeSession(String sid)
{
   String q = "DELETE FROM iQsignRestful WHERE session = $1";
   sql_database.sqlUpdate(q,sid);
}




void startSession(String sid,String code)
{
   String q = "INSERT INTO iQsignRestful (session, code) VALUES ( $1, $2 )";
   sql_database.sqlUpdate(q,sid,code);
}


IQsignSession checkSession(BowerSessionStore<IQsignSession> bss,String sid)
{
   String q = "SELECT * FROM iQsignRestful WHERE session = $1";

   JSONObject json = sql_database.sqlQuery1(q,sid);
   if (json != null) {
      long now = System.currentTimeMillis();
      long lupt = json.getLong("last_used");
      if (now - lupt <= SESSION_TIMEOUT) {
	 IQsignSession bs = new IQsignSession(bss,sid,json);
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

   List<JSONObject> qrslt = sql_database.sqlQueryN(q);
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
   JSONObject json = sql_database.sqlQuery1(q1,name,name);
   if (json != null) {
      return new IQsignUser(json);
    }
   return null;
}



IQsignUser findUser(Number uid)
{
   if (uid == null) return null;

   String q1 = "SELECT * FROM iQsignUsers WHERE id = $1 AND valid";
   JSONObject json = sql_database.sqlQuery1(q1,uid);
   if (json != null) {
      return new IQsignUser(json);
    }
   return null;
}


String checkNoUser(String uid,String email)
{
   String q1 = "SELECT * FROM iQsignUsers WHERE username = $1 OR email = $2";

   List<JSONObject> qr = sql_database.sqlQueryN(q1,uid,email);
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
   int ct = sql_database.sqlUpdate(q1,email,user,pwd,apwd,valid);
   return ct > 0;
}


boolean registerValidator(String email,String code)
{
   String q1 = "INSERT INTO iQsignValidator ( userid, validator, timeout ) " +
      "VALUES ( ( SELECT id FROM iQsignUsers WHERE email = $1 ), $2, " +
      "( CURRENT_TIMESTAMP + INTERVAL '2 DAYS' ) )";
   int ct = sql_database.sqlUpdate(q1,email,code);
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
   
   JSONObject rslt = sql_database.sqlQuery1(q1,email,code);
   if (rslt == null) return false;
   
   Number uid = rslt.getNumber("userid");
   sql_database.sqlUpdate(q2,uid,code);
   sql_database.sqlUpdate(q3,uid);
   
   return true;
}


void updatePassword(Number uid,String pwd,String apwd)
{
   String q1 = "UPDATE iQsignUsers SET password = $1, altpassword = $2, " +
      " temppassword = NULL " +
      "WHERE id = $3";
   sql_database.sqlUpdate(q1,pwd,apwd,uid);
}


void setTemporaryPassword(Number uid,String tpwd)
{
   String q1 = "UPDATE iQsignUsers SET temppassword = $1 " +
      "WHERE id = $2";
   sql_database.sqlUpdate(q1,tpwd,uid);
}




IQsignLoginCode checkAccessToken(String token,Number uid,String code)
{
   String q1 = "SELECT * FROM iQsignLoginCodes WHERE code = $1";
   String q2 = "UPDATE iQsignLoginCodes SET last_used = CURRENT_TIMETTAMP WHERE code = $1";
   String q3 = "SELECT * FROM iQsignLoginCodes WHERE userid = $1";

   JSONObject rslt = null;
   if (code == null) {
      rslt = sql_database.sqlQuery1(q1,token);
    }
   else if (uid != null) {
      List<JSONObject> rslts = sql_database.sqlQueryN(q3,uid);
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
      sql_database.sqlUpdate(q2,token);
    }

   IQsignLoginCode lcrslt = null;
   if (rslt != null) lcrslt = new IQsignLoginCode(rslt);

   return lcrslt;
}


void addLoginCode(Number uid,Number sid,String code)
{
   String q1 = "DELETE FROM iQsignLoginCodes WHERE userid = $1 AND signid = $2";
   String q2 = "INSERT INTO iQsignLoginCodes ( code, userid, signid ) " +
      "VALUES ( $1, $2, $3 )";
   
   sql_database.sqlUpdate(q1,uid,sid);
   sql_database.sqlUpdate(q2,code,uid,sid);
}


void unregisterUser(String email)
{
   String q2 = "DELETE FROM iQsignValidator WHERE userid = $1";
   String q3 = "DELETE FROM iQsignSigns WHERE userid = $1";
   String q4 = "DELETE FROM iQsignUsers WHERE id = $1";

   IQsignUser user = findUser(email);
   if (user == null) return;
   Number uid = user.getUserId();
   sql_database.sqlUpdate(q2,uid);
   sql_database.sqlUpdate(q3,uid);
   sql_database.sqlUpdate(q4,uid);
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
   sql_database.sqlUpdate(q1,uid);
   sql_database.sqlUpdate(q2,uid);
   sql_database.sqlUpdate(q3,uid);
   sql_database.sqlUpdate(q4,uid);
   sql_database.sqlUpdate(q5,uid);
   sql_database.sqlUpdate(q6,uid);
   sql_database.sqlUpdate(q7,uid);
   sql_database.sqlUpdate(q8,uid);
   sql_database.sqlUpdate(q9,uid);
   sql_database.sqlUpdate(q10,uid);
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
   
   sql_database.sqlUpdate(q2,sid);
   sql_database.sqlUpdate(q3,sid);
   sql_database.sqlUpdate(q4,sid);
   sql_database.sqlUpdate(q5,sid);
   sql_database.sqlUpdate(q6,sid);
   
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
   sql_database.sqlUpdate(q1,uid,name,key,cnts);
   return findSignByKey(key);
}


IQsignSign findSignByKey(String key)
{
   String q1 = "SELECT * FROM iQsignSigns WHERE namekey = $1";
   JSONObject rslt = sql_database.sqlQuery1(q1,key);
   if (rslt == null) return null;
   return new IQsignSign(iqsign_main,rslt);
}

IQsignSign findSignById(Number sid)
{
   if (sid == null) return null;
   String q1 = "SELECT * FROM iQsignSigns WHERE id = $1";
   JSONObject rslt = sql_database.sqlQuery1(q1,sid);
   if (rslt == null) return null;
   return new IQsignSign(iqsign_main,rslt);
}


IQsignSign findSign(Number id,Number uid,String namekey)
{
   String q1 = "SELECT * FROM iQsignSigns WHERE id = $1 AND userid = $2 AND namekey = $3";
   JSONObject rslt = sql_database.sqlQuery1(q1,id,uid,namekey);
   if (rslt == null) return null;
   return new IQsignSign(iqsign_main,rslt);
}


String getDefineName(String contents,Number uid)
{
   String q1 = "SELECT * FROM iQsignDefines WHERE contents = $1 AND userid = $2";
   String q2 = "SELECT * FROM iQsignDefines WHERE contents = $1 AND userid IS NULL";
   
   List<JSONObject> dnms = sql_database.sqlQueryN(q1,contents,uid);
   if (dnms.isEmpty()) {
      dnms = sql_database.sqlQueryN(q2,contents);
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
   int ct = sql_database.sqlUpdate(q1,name,id);
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
   JSONObject json = sql_database.sqlQuery1(q1,uid,dname);
   if (json == null && !useronly) {
      user = false;
      json = sql_database.sqlQuery1(q2,dname);
    }
   if (json == null) {
      // no previous definition
      sql_database.sqlUpdate(q3,uid,dname,contents);
      IQsignDefinedImage di = getDefineData(null,dname,uid);
      sql_database.sqlUpdate(q5,di.getId(),uid);
    }
   else {
      if (user) {
         // check for recursive reference
         String pat = "=\\s*\\Q" + dname + "\\E";
         if (contents.matches(pat)) {
            return; 
          }
       }
      IQsignDefinedImage di = new IQsignDefinedImage(json);
      if (!di.getContents().equals(contents) && user) {
         // update if this is a change and a user sign
	 sql_database.sqlUpdate(q4,contents,di.getId());
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
      rslt = sql_database.sqlQueryN(q1,id);
    }
   else if (userid == null) {
      rslt = sql_database.sqlQueryN(q3,name);
    }
   else {
      rslt = sql_database.sqlQueryN(q2,userid,name);
      if (rslt.isEmpty()) {
	 rslt = sql_database.sqlQueryN(q3,name);
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
      sql_database.sqlUpdate(q1,defdata.getId());
    }
   sql_database.sqlUpdate(q2,name,userid);
}

List<IQsignSign> getAllSignsForUser(Number uid)
{
   String q1 = "SELECT * FROM iQsignSigns WHERE userid = $1";
   List<JSONObject> qr = sql_database.sqlQueryN(q1,uid);
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

   sql_database.sqlUpdate(q1,cnts,sid);
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
   
   JSONObject imgjs = sql_database.sqlQuery1(q1,name);
   if (imgjs != null) {
      IQsignImage img = new IQsignImage(imgjs); 
      String rf = img.getFile();
      String ru = img.getUrl();
      if (file.equals(rf) || file.equals(ru)) return;
      sql_database.sqlUpdate(q2,name);
    }
   sql_database.sqlUpdate(q3,name,url,file,border,desc);
}


void saveOrUpdateUserImage(Number uid,String name,String file,String url,String desc,boolean border)
{
   String q1 = "SELECT * FROM iQsignImages WHERE userid = $1 AND name = $2";
   String q2 = "DELETE FROM iQsignImages WHERE userid = $1 AND name = $2";
   String q3 = "INSERT INTO iQsignImages ( userid, name, url, file, is_border, description ) " + 
         "VALUES ( $1, $2, $3, $4, $5, $6 )";
   
   JSONObject imgjs = sql_database.sqlQuery1(q1,uid,name);
   if (imgjs != null) {
      IQsignImage img = new IQsignImage(imgjs);
      sql_database.sqlUpdate(q2,uid,name);
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
   sql_database.sqlUpdate(q3,uid,name,url,file,border,desc);
}



List<IQsignImage> findImages(Number uid,boolean border)
{
   String q1 = "SELECT * FROM iQsignImages WHERE (userid IS NULL OR userid = $1) AND " +
      "is_border = $2";
   List<JSONObject> imgs = sql_database.sqlQueryN(q1,uid,border);
   
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
   List<JSONObject> fils = sql_database.sqlQueryN(q1);
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

   JSONObject jo = sql_database.sqlQuery1(q1,name);
   if (jo != null) {
      IQsignDefinedImage di = new IQsignDefinedImage(jo);
      if (di.getLastUpdate() < dlmtime) return;
      if (di.getContents().equals(cnts)) return;
      sql_database.sqlUpdate(q2,cnts,dlm,di.getId());
    }
   else {
      sql_database.sqlUpdate(q3,name,cnts,dlm);
    }
}


void updateSignProperties(IQsignSign sign)
{
   String q1 = "UPDATE iQsignSigns SET name = $1, " +
      "lastupdate = CURRENT_TIMESTAMP, dimension = $2, " +
      "width = $3, height = $4, displayname = NULL " +
      "WHERE id = $5";
   sql_database.sqlUpdate(q1,sign.getSignName(),
	 sign.getDimension(),sign.getWidth(),sign.getHeight(),
	 sign.getId());
}



List<IQsignDefinedImage> getSavedSigns(Number uid)
{
   String q1 = "SELECT * FROM iQsignDefines D " +
      "LEFT OUTER JOIN iQsignUseCounts C ON D.id = C.defineid " +
      "WHERE D.userid = $1 OR D.userid IS NULL " +
      "ORDER BY C.count DESC, C.last_used DESC, D.id";
   List<JSONObject> qrslt = sql_database.sqlQueryN(q1,uid);
   List<IQsignDefinedImage> rslt = new ArrayList<>();
   for (JSONObject jo : qrslt) {
      rslt.add(new IQsignDefinedImage(jo));
    }

   return rslt;
}



}	// end of class IQsignDatabase




/* end of IQsignDatabase.java */

