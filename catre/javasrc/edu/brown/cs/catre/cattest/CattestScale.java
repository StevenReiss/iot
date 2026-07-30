/********************************************************************************/
/*                                                                              */
/*              CattestScale.java                                               */
/*                                                                              */
/*      Code for testing CATRE at scale                                         */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2023 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.catre.cattest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.bson.Document;
import org.json.JSONObject;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import edu.brown.cs.catre.catmain.CatmainMain;
import edu.brown.cs.catre.catre.CatreUtil;
import edu.brown.cs.ivy.exec.IvyExec;

public class CattestScale implements CattestConstants
{



/********************************************************************************/
/*                                                                              */
/*      Main program for testing at scale                                       */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   CattestScale cs = new CattestScale(args);
   cs.process();
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private int     user_count;
private File    iot_directory;
private File    base_directory;
private Properties catre_props;
private IvyExec catre_exec;
private Map<String,String> user_session;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private CattestScale(String [] args)
{
   user_count = 100;
   base_directory = null;
 
   scanArgs(args);
   
   if (base_directory == null) {
      File f1 = CatmainMain.getBaseDirectory();
      iot_directory = f1;
      File f2 = new File(f1,SCALE_DIRECTORY);
      if (f2.exists() && f2.isDirectory()) base_directory = f2;
      else base_directory = f1;
    }
   
   File f2 = new File(base_directory,"secret");
   File f4 = new File(f2,"catre.props");
   Properties p = new Properties();
   try (FileInputStream fis = new FileInputStream(f4)) {
      p.loadFromXML(fis);
    }
   catch (IOException e) { } 
   catre_props = p;
   
   user_session = new LinkedHashMap<>();
}



/********************************************************************************/
/*                                                                              */
/*      Argument scanning                                                       */
/*                                                                              */
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
         if (args[i].startsWith("-u") && i+1 < args.length) {           // -users #
            try {
               user_count = Integer.parseInt(args[++i]);
             }
            catch (NumberFormatException e) {
               badArgs();
             }
          }
         else if (args[i].startsWith("-d") && i+1 < args.length) {      // -dir <base dir<
            base_directory = new File(args[++i]);
          }
         else badArgs();
       }
      else {
         badArgs();
       }
    }
}


private void badArgs()
{
   System.err.println("CattestScale [-users <#users>] [-dir <basedir>]");
   System.exit(1);
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

private void process()
{
   // First provide a clean database
   cleanDatabase();
   
   // Next start Catre locally
   startCatre();
   
   // Next register all the users
   registerUsers();
   
   // Next setup each user's devices
   
   // Next define each user's programs
   
   // Next run the system for a long time
   
   // then remove users
   removeUsers();
   
   // Stop local catre at the end
   stopCatre();
}



/********************************************************************************/
/*                                                                              */
/*      Database methods                                                        */
/*                                                                              */
/********************************************************************************/

private void cleanDatabase()
{
   String con = "mongodb://USER:PASS@HOST:PORT/catre?maxPoolSize=20&w=majority";
   
   con = con.replace("USER",catre_props.getProperty("mongouser","sherpa"));
   con = con.replace("PASS",
         catre_props.getProperty("mongopass","XXX"));
   con = con.replace("HOST",
         catre_props.getProperty("mongohost","localhost"));
   con = con.replace("PORT",
         catre_props.getProperty("mongoport","27017"));
   
   String dbname = catre_props.getProperty("mongodatabase","catre");
   
   MongoClient client = MongoClients.create(con);
   MongoDatabase catredb = client.getDatabase(dbname);
   for (String collectionName : catredb.listCollectionNames()) {
      MongoCollection<Document> collection = catredb.getCollection(collectionName);
      collection.deleteMany(new Document());
    }
}



/********************************************************************************/
/*                                                                              */
/*      Catre control methods                                                   */
/*                                                                              */
/********************************************************************************/

private void startCatre()
{
   catre_exec = null;
   
   String cmd = null;
   File f1 = new File(base_directory,"bin");
   File f2 = new File(f1,"catserver.sh");
   File f3 = new File(iot_directory,"bin");
   File f4 = new File(f3,"catserver.sh");
   if (f2.exists() && f2.canExecute()) {
      cmd = f2.getAbsolutePath();
    }
   else if (f4.exists() && f4.canExecute()) {
      cmd = f4.getAbsolutePath();
    }
   else {
      System.err.println("CattestStream: Can't find CATRE");
      System.exit(1);
    }
   cmd = "/bin/bash " + cmd;
   
   try {
      catre_exec = new IvyExec(cmd);
    }
   catch (IOException e) {
      System.err.println("CattestStream: Problem running CATRE: " + e);
      System.exit(1);
    }
   
   String port = catre_props.getProperty("https_port","3334");
   String url = SCALE_HOST.replace("PORT",port);
   CattestUtil.setTestHost(url);
   
   for (int i = 0; i < 100; ++i) {
      if (!catre_exec.isRunning()) break;
      JSONObject r1 = CattestUtil.sendGetOptional("/ping",null,true);
      if (r1 != null && r1.optBoolean("pong")) {
         return;
       }
      try {
       Thread.sleep(1000);
       }
      catch (InterruptedException e) { }
    }
   
   System.err.println("CattestStream: CATRE not started successfully");
   System.exit(1);
}



private void stopCatre()
{
   if (catre_exec != null) {
      catre_exec.destroy();
      catre_exec = null;
    }
}


/********************************************************************************/
/*                                                                              */
/*      Handle user setup                                                       */
/*                                                                              */
/********************************************************************************/

private void registerUsers()
{
   for (int i = 0; i < user_count; ++i) {
      JSONObject rslt2 = CattestUtil.sendGet("/login");
      String sid = rslt2.getString("CATRESESSION");
      
      String user = USER_NAME.replace("#",String.valueOf(i));
      String email = USER_EMAIL.replace("#",String.valueOf(i));
      String pwd = USER_PWD.replace("#",String.valueOf(i));
      String univ = USER_UNIVERSE.replace("#",String.valueOf(i));
      String v1 = CatreUtil.secureHash(pwd);
      String v2 = v1 + user;
      String v3 = CatreUtil.secureHash(v2);
      
//    String salt = rslt2.getString("SALT");
//    String v4 = v3 + salt;
//    String v5 = CatreUtil.secureHash(v4);
      
      JSONObject rslt1 = CattestUtil.sendJson("POST","/register",
            "CATRESESSION",sid,
            "username",user,
            "email",email,
            "password",v3,
            "universe",univ);
      sid = rslt1.getString("CATRESESSION");
      user_session.put(user,sid);
    }
}


private void removeUsers()
{
   for (int i = 0; i < user_count; ++i) {
      String user = USER_NAME.replace("#",String.valueOf(i));
      String sid = user_session.get(user);
      CattestUtil.sendJson("POST","/removeuser",
         "CATRESESSION",sid);
    }
}



}       // end of class CattestScale




/* end of CattestScale.java */

