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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import edu.brown.cs.ivy.bower.BowerSession;
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

IQsignDatabase(IQsignMain main)
{
   iqsign_main = main;
   sql_database = null;
   
   try {
      File f0 = iqsign_main.getBaseDirectory();
      File f1 = new File(f0,"secret");
      File dbf = new File(f1,"Database.props");
      IvyLog.logD("Using database file " + dbf);
      sql_database = IvyDatabase.openDatabase("iqsign");
    }
   catch (Throwable t) {
      IQsignMain.reportError("Can't connect to database iqsign"); 
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
  
   try {
      Statement s = sql_database.createStatement();
      s.executeUpdate(q);
    }
   catch (SQLException e) {
      IvyLog.logE("Database problem on " + q,e);
    }
}


void updateSession(String sid,String user)
{
   String q = "UPDATE iQsignRestful SET userid = $1, last_used = CURRENT_TIMESTAMP " +
      "WHERE session = $2";
   try {
      PreparedStatement ps = sql_database.prepareStatement(q);
      ps.setString(1,user);
      ps.setString(2,sid);
      ps.executeUpdate();
    }
   catch (SQLException e) {
      IvyLog.logE("Database problem on udpate session",e);
    }
}



void removeSession(String sid)
{
   String q = "DELETE FROM iQsignRestful WHERE session = $1";
   try {
      PreparedStatement ps = sql_database.prepareStatement(q);
      ps.setString(1,sid);
      ps.executeUpdate();
    }
   catch (SQLException e) {
      IvyLog.logE("Database problem on delete session",e);
    }
}




void startSession(String sid,String code)
{
   String q = "INSERT INTO iQsignRestful (session, code) VALUES ( $1, $2 )";
   try {
      PreparedStatement ps = sql_database.prepareStatement(q);
      ps.setString(1,sid);
      ps.setString(2,code);
      ps.executeUpdate();
    }
   catch (SQLException e) {
      IvyLog.logE("Database problem on udpate session",e);
    }
}


BowerSession checkSession(String sid)
{
   String q = "SELECT * FROM iQsignRestful WHERE session = $1";
   try {
      PreparedStatement ps = sql_database.prepareStatement(q);
      ps.setString(1,sid);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
         long now = System.currentTimeMillis();
         Date lup = rs.getDate("last_time");
         long lupt = 0;
         if (lup != null) lupt = lup.getTime();
         if (now - lupt <= SESSION_TIMEOUT) { 
            BowerSession bs = new BowerSession(sid,lup);
            bs.setValue("code",rs.getString("code"));
            bs.setValue("creation_time",rs.getString("creation_time"));   
            return bs;
          }
       }
    }
   catch (SQLException e) {
      IvyLog.logE("Database problem on udpate session",e);
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
   try {
      Statement s = sql_database.createStatement();
      ResultSet rs = s.executeQuery(q); 
      while (rs.next()) {
         rslt.add(rs.getString("namekey"));
       }
    }
   catch (SQLException e) {
      IvyLog.logE("Database problem on " + q,e);
    }
   
   return rslt;
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
      PreparedStatement st1 = sql_database.prepareStatement(q1);
      st1.setString(1,name);
      ResultSet rs = st1.executeQuery();
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
         PreparedStatement st2 = sql_database.prepareStatement(q2);
         st2.setString(1,name);
         st2.executeUpdate();
       }
      PreparedStatement st3 = null;
      if (isurl) {
         st3 = sql_database.prepareStatement(q3);
       }
      else {
         st3 = sql_database.prepareStatement(q4);
       }
      st3.setString(1,name);
      st3.setString(2,file);
      st3.executeUpdate();
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

void saveOrUpdateSign(String name,String cnts,long dlm)
{
   String q1 = "SELECT * FROM iQsignDefines WHERE name = $1 AND userid IS NULL";
   String q2 = "UPDATE iQsignDefines SET contents = $1, lastupdate = $2 WHERE id = $3";
   String q3 = "INSERT INTO iQsignDefines (id, userid, name, contents,lastupdate ) " +
         "VALUES ( DEFAULT, NULL, $1, $2, $3 )";
   try {
      PreparedStatement st1 = sql_database.prepareStatement(q1);
      st1.setString(1,name);
      ResultSet rs = st1.executeQuery();
      PreparedStatement st2 = null;
      if (rs.next()) {
         IvyLog.logD("DATE COMPARE " + dlm + " " + rs.getLong("lastupdate"));
         if (rs.getString("contents").equals(cnts)) return;
         st2 = sql_database.prepareStatement(q2);
         st2.setString(1,cnts);
         st2.setLong(2,dlm);
         st2.setString(3,rs.getString("id"));
       }
      else {
         st2 = sql_database.prepareStatement(q3);
         st2.setString(1,name);
         st2.setString(2,cnts);
         st2.setLong(3,dlm);
       }
      if (st2 != null) {
         st2.executeUpdate();
       }
    }
   catch (SQLException e) {
      IvyLog.logE("Database problem on save/update sign ",e);
    }
}





}       // end of class IQsignDatabase




/* end of IQsignDatabase.java */

