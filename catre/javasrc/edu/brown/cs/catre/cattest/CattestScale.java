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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import edu.brown.cs.catre.catmain.CatmainMain;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreUtil;
import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.file.IvyLog.LogLevel;

public final class CattestScale implements CattestConstants
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
private File    base_directory;
private Properties catre_props;
private IvyExec catre_exec;
private Map<String,String> user_session;
private List<IvyExec> device_execs;
private boolean  run_local;
private String log_file;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private CattestScale(String [] args)
{
   CatreLog.setLogLevel(LogLevel.DEBUG);
   CatreLog.setupLogging("CATSCALE",true);
   
   user_count = 200;
   base_directory = null;
   run_local = false;
   log_file = null;
 
   scanArgs(args);
   
   if (base_directory == null) {
      base_directory = CatmainMain.getBaseDirectory();
    }
   
   File f2 = new File(base_directory,"secret");
   File f4 = new File(f2,PROPS_FILE);
   Properties p = new Properties();
   try (FileInputStream fis = new FileInputStream(f4)) {
      p.loadFromXML(fis);
    }
   catch (IOException e) { } 
   catre_props = p;
   
   user_session = new LinkedHashMap<>();
   device_execs = new ArrayList<>();
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
         else if (args[i].startsWith("-l")) {                           // -local   
            run_local = true;
          }
         else if (args[i].startsWith("-LD")) {                          // -LDebug
	    CatreLog.setLogLevel(CatreLog.LogLevel.DEBUG);
	  }
	 else if (args[i].startsWith("-LI")) {                          // -LInfo
	    CatreLog.setLogLevel(CatreLog.LogLevel.INFO);
	  }
	 else if (args[i].startsWith("-LW")) {                          // -LWarning
	    CatreLog.setLogLevel(CatreLog.LogLevel.WARNING);
	  }
         else if (args[i].startsWith("-LE")) {                          // -LError
	    CatreLog.setLogLevel(CatreLog.LogLevel.ERROR);
	  }
	 else if (args[i].startsWith("-L") && i+1 < args.length) {      // -Log <file>
            log_file = args[++i];
	    CatreLog.setLogFile(log_file);
	  }
	 else if (args[i].startsWith("-S")) {                           // -Stderr
	    CatreLog.useStdErr(true);
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
   System.err.println("CattestScale [-users <#users>]");
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
   
   try {
      // Next start cedes
      startCedes();
      
      // Next start Catre locally
      startCatre();
      
      // Next register all the users
      registerUsers();
      
      // Next setup each user's devices and programs
      for (int i = 0; i < user_count; i += DEVICE_USER_COUNT) {
         int ct = Math.min(DEVICE_USER_COUNT,user_count-i);
         startDevices(i,ct);
       }
      
      // might need a delay here to allow all devices to be defined externally
      try {
         Thread.sleep(90*1000);
       }
      catch (InterruptedException e) { }
      
      CatreLog.logD("CATSCALE","DEFINING RULES");
      // next define rules for each user
      for (int i = 0; i < user_count; ++i) {
         setupRules(i);
       }
      
      CatreLog.logD("CATSCALE","STARTING EXPERIMENT");
      // Next run the system for a long time
      try {
         Thread.sleep(5*60*1000);
       }
      catch (InterruptedException e) { }
    }
   catch (Throwable t) {
      CatreLog.logE("CATSCALE","Problem with scale test",t);
    }
   finally {
      // remove users
      removeUsers();
      
      // Stop all devices
      stopDevices();
      
      // Stop local catre at the end
      stopCatre();
      
      // Stop local cedes 
      stopCedes();
      
      // and exit
      System.exit(0);
    }
   
}



/********************************************************************************/
/*                                                                              */
/*      Database methods                                                        */
/*                                                                              */
/********************************************************************************/

private void cleanDatabase()
{
   String con = "mongodb://USER:PASS@HOST:PORT/DATABASE?maxPoolSize=20&w=majority";
   
   con = con.replace("USER",
         catre_props.getProperty("mongouser","sherpa"));
   con = con.replace("PASS",
         catre_props.getProperty("mongopass","XXX"));
   con = con.replace("HOST",
         catre_props.getProperty("mongohost","localhost"));
   con = con.replace("PORT",
         catre_props.getProperty("mongoport","27017"));
   
   String dbname = catre_props.getProperty("mongodatabase","catretest");
   con = con.replace("DATABASE",dbname);
   
   MongoClient client = MongoClients.create(con);
   MongoDatabase catredb = client.getDatabase(dbname);
  
   for (String cname : catredb.listCollectionNames()) {
      MongoCollection<Document> collection = catredb.getCollection(cname);
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
   
   String port = catre_props.getProperty("https_port","3334");
   String url = SCALE_HOST.replace("PORT",port);
   CattestUtil.setTestHost(url);
   
   if (run_local) {
      CattestUtil.startCatre(CATRE_ARGS);
      return;
    }
    
   String cp = System.getProperty("java.class.path");
   StringBuffer buf = new StringBuffer();
   buf.append("'" + IvyExecQuery.getJavaPath() + "'");
   buf.append(" -cp '" + cp + "'");
   buf.append(" edu.brown.cs.catre.catmain.CatmainMain");
   buf.append(" -server ");
   buf.append(CATRE_ARGS);
   String cmd = buf.toString();
   
   try {
      catre_exec = new IvyExec(cmd);
      CatreLog.logD("CATTEST","Catre started as " + catre_exec.getPid());
    }
   catch (IOException e) {
      System.err.println("CattestStream: Problem running CATRE: " + e);
      System.exit(1);
    }
   
   for (int i = 0; i < 20; ++i) {
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
      CatreLog.logD("CATTEST","Remove Catre process " + catre_exec.getPid());
      catre_exec.destroy();
      catre_exec = null;
    }
}


/********************************************************************************/
/*                                                                              */
/*      Cedes control methods                                                   */
/*                                                                              */
/********************************************************************************/

private void startCedes()
{
   File f1 = new File(base_directory,"cedes");
   File f2 = new File(f1,"start.csh");
   String cmd = "/bin/csh " + f2.getPath();
   try {
      IvyExec cedes = new IvyExec(cmd,f1);
      CatreLog.logD("CATTEST","Start Cedes " + cedes.getPid());
    }
   catch (IOException e) {
      System.err.println("CattestStream: Problem running CEDES: " + e);
      System.exit(1);
    }
}


private void stopCedes() 
{
   File f1 = new File(base_directory,"cedes");
   File f2 = new File(f1,"stop.csh");
   String cmd = "/bin/csh " + f2.getPath();
   try {
      IvyExec cedes = new IvyExec(cmd);
      CatreLog.logD("CATTEST","Stop Cedes " + cedes.getPid());
    }
   catch (IOException e) {
      System.err.println("CattestStream: Problem running CEDES: " + e);
      System.exit(1);
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
      
      String un = String.valueOf(i);
      
      String user = USER_NAME.replace("#",un);
      String email = USER_EMAIL.replace("#",un);
      String pwd = USER_PWD.replace("#",un);
      String univ = USER_UNIVERSE.replace("#",un);
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
      
      String genuid = USER_GENERIC_UID.replace("#",un);
      String genpat = USER_GENERIC_PAT.replace("#",un);
      JSONObject rslt3 = CattestUtil.sendJson("POST","/bridge/add",
            "CATRESESSION",sid,"BRIDGE","generic",
            "AUTH_UID",genuid,
            "AUTH_PAT",genpat);
      sid = rslt3.getString("CATRESESSION");
      
      user_session.put(user,sid);
      user_session.put(un,sid);
      CatreLog.logD("CATTEST","Register user " + user);
    }
}


private void removeUsers()
{
   for (int i = 0; i < user_count; ++i) {
      String user = USER_NAME.replace("#",String.valueOf(i));
      String sid = user_session.get(user);
      CattestUtil.sendJson("POST","/removeuser",
         "CATRESESSION",sid);
      CatreLog.logD("CATTEST","Remove user " + user);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Device methods                                                          */
/*                                                                              */
/********************************************************************************/

private void startDevices(int start,int ct)
{
   String curl = catre_props.getProperty("cedes_url","http://localhost:3333");
   
   if (run_local) {
      String args = "-u " + start + " -n " + ct + " -c " + curl;
      DeviceRunner dr = new DeviceRunner(args,start/ct);
      dr.start();
      return;
    }
   
   String cp = System.getProperty("java.class.path");
   StringBuffer buf = new StringBuffer();
   buf.append("'" + IvyExecQuery.getJavaPath() + "'");
   buf.append(" -cp '" + cp + "'");
   buf.append(" edu.brown.cs.catre.cattest.CattestScalDevices");
   buf.append(" -u ");
   buf.append(start);
   buf.append(" -n ");
   buf.append(ct);
   buf.append(" -c ");
   buf.append(curl);
   String cmd = buf.toString();
   
   if (log_file != null) {
      int idx = log_file.lastIndexOf(".");
      String nf = log_file.substring(0,idx) +
            "_" + start + log_file.substring(idx);
      buf.append(" -L " + nf);
      buf.append(" -LD");
    }
   
   try {
      IvyExec ex = new IvyExec(cmd);
      CatreLog.logD("CATTEST","Devices " + start + " started as " + ex.getPid());
      device_execs.add(ex);
    }
   catch (IOException e) {
      System.err.println("CattestStream: Problem running DEVICES: " + e);
      System.exit(1);
    }
}


private void stopDevices()
{
   for (IvyExec ex : device_execs) {
      ex.destroy();
    }
   device_execs.clear();
}


private static class DeviceRunner extends Thread 
      implements CatreLog.LoggerThread {
   
   private String [] device_args;
   private int log_id;
   
   DeviceRunner(String args,int id) {
      super("CatreDeviceThread");
      if (args == null) {
         device_args = new String [] {};
       }
      else {
         device_args = args.split("\\s+");
       }
      log_id = id;
    }
   
   @Override public void run() {
      CattestScaleDevices.main(device_args);
    }
   
   @Override public int getLogId() {
      return log_id;
    }

}       // end of inner class CatreRunner


/********************************************************************************/
/*                                                                              */
/*      Setup rules                                                             */
/*                                                                              */
/********************************************************************************/

private void setupRules(int user)
{
   String uid = String.valueOf(user);
   for (int j = 0; j < RULES_PER_USER; ++j) {
      List<DeviceInfo> dis = new ArrayList<>();
      List<String> sds = new ArrayList<>();
      for (int k = 0; k < 3; ++k) {
         int idx = CattestUtil.nextRandom(DEVICE_SET.length);
         DeviceInfo di = DEVICE_SET[idx];
         if (!dis.contains(di)) {
            dis.add(di);
            sds.add(CattestUtil.getDeviceUid(uid,di.getDeviceName()));
          }
       }
      JSONArray conds = new JSONArray();
      for (int i = 0; i < dis.size(); ++i) {
         DeviceInfo di = dis.get(i);
         String sduid = sds.get(i);
         JSONObject cond = di.getCondition(sduid,"Value");
         conds.put(cond);
       }
      String sduid0 = CattestUtil.getDeviceUid(uid,
            DEVICE_SET[0].getDeviceName());
      JSONArray acts = new JSONArray();
      JSONObject act = CattestUtil.buildJson("NEEDSNAME",false,
            "DESCRIPTION","Rule " + uid + "-" + j + " action",
            "USERDESC",false,
            "TRIGGER",false,
            "LABEL","Rule " + uid + "-" + j + " action",
            "NAME","Rule " + uid + "-" + j + " action",
            "PARAMETERS", new JSONObject(),
            "TRANSITION", CattestUtil.buildJson("TRANSITION","Notify",
                  "DEVICE",sduid0)
      );
      acts.put(act);
      
      JSONObject rule = CattestUtil.buildJson("DEVICEID",sduid0,
            "DESCRIPTION","Rule " + user + "-" + j,
            "USERDESC",false,
            "TRIGGER",false,
            "NAME","Rule " + j,
            "CONDITIONS", conds,
            "ACTIONS",acts,
            "PRIORITY",200 + 100* j,
            "LABEL","Rule " + user + "-" +  j
      );
      
      JSONObject rslt3 = CattestUtil.sendJson("POST","/rule/add",
            "CATRESESSION",user_session.get(uid),
            "RULE",rule);
      CatreLog.logD("CATTESTSCALE","Add rule " + rslt3.toString(2));
    }
}


}       // end of class CattestScale




/* end of CattestScale.java */

