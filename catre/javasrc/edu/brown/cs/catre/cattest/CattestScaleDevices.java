/********************************************************************************/
/*                                                                              */
/*              CattestScaleDevices.java                                        */
/*                                                                              */
/*      Runnable set of devices for scale test                                  */
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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog.LogLevel;

public final class CattestScaleDevices implements CattestConstants
{



/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   CattestScaleDevices csd = new CattestScaleDevices(args);
   
   csd.start();
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private int     first_user;
private int     user_count;
private Timer   device_timer;
private String  cedes_url;
private Map<String,String> access_tokens;
private Map<String,List<ScaleDevice>> user_devices;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private CattestScaleDevices(String [] args)
{
   CatreLog.setupLogging("CATSCALEDEV",true);
   
   first_user = 0;
   user_count = 1;
   device_timer = new Timer("DeviceTimer",false);
   cedes_url = CEDES_URL;
   access_tokens = new HashMap<>();
   user_devices = new LinkedHashMap<>();
   
   scanArgs(args);
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
         if (args[i].startsWith("-u") && i+1 < args.length) {           // -u <user>
            try {
               first_user = Integer.parseInt(args[++i]);
             }
            catch (NumberFormatException e) {
               badArgs();
             }
          }
         else if (args[i].startsWith("-n") && i+1 < args.length) {      // -n <user_count>
            try {
               user_count = Integer.parseInt(args[++i]);
             }
            catch (NumberFormatException e) {
               badArgs();
             }
          }
         else if (args[i].startsWith("-c") && i+1 < args.length) {      // -c <cedes url>    
            cedes_url = args[++i];
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
	    CatreLog.setLogFile(args[++i]);
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
   CatreLog.logE("CATSCALEDEV","Bad arguments");
   System.err.println("CATTESTSCALEDEVICES -u <user> -n <count>");
   System.exit(1);
}



/********************************************************************************/
/*                                                                              */
/*      Running methods                                                         */
/*                                                                              */
/********************************************************************************/

private void start()
{
   for (int i = 0; i < user_count; ++i) {
      setupDevices(first_user + i);
    }
}


private void setupDevices(int user)
{
   String uid = String.valueOf(user);
   CatreLog.logD("CATSCALEDEV","Setup Devices for " + uid);

   List<ScaleDevice> devs = new ArrayList<>();
   for (DeviceInfo di : DEVICE_SET) {
      ScaleDevice sd = null;
      switch (di.getDeviceType()) {
         case ENUM :
            DeviceEnumInfo dei = (DeviceEnumInfo) di;
            sd = new ScaleDeviceEnum(uid,dei.getDeviceName(),
                  dei.getOffTime(),dei.getOnTime(),
                  dei.getStates());
            break;
         case RANGE :
            DeviceRangeInfo dri = (DeviceRangeInfo) di;
            sd = new  ScaleDeviceRange(uid,dri.getDeviceName(),
                  dri.getChangeTime(),
                  dri.getMinValue(),dri.getMaxValue());
            break;
       }
      if (sd != null) devs.add(sd);
    }
   user_devices.put(uid,devs);
   for (ScaleDevice sd : devs) {
      sd.start();
    }
}




/********************************************************************************/
/*                                                                              */
/*      Cedes communications methods                                            */
/*                                                                              */
/********************************************************************************/

private boolean authenticate(String userid) 
{
   String acctok = access_tokens.get(userid);
   if (acctok != null) return true;
   
   String uid = USER_GENERIC_UID.replace("#",userid);
   
   CatreLog.logD("CATSCALEDEV","Device start authentication " + userid);
   JSONObject rslt = sendToCedes(userid,"generic/attach","uid",uid);
   if (rslt == null) {
      CatreLog.logE("CATSCALEDEV","Failed to attach to cedes at " + new Date());
      return false;
    }
   
   CatreLog.logD("CATSCALEDEV","Attach result " + rslt.toString(2));
   
   String seed = rslt.optString("seed",null);
   if (seed == null) {
      CatreLog.logE("CATSCALEDEV","Did not receive seed from cedes: " + rslt + " " +
            new Date());
      return false;
    }
   CatreLog.logD("CATSCALEDEV","Received seed " + seed);
   
   String pat = USER_GENERIC_PAT.replace("#",userid);
   
   String p0 = CattestUtil.secureHash(pat);
   String p1 = CattestUtil.secureHash(p0 + uid);
   String p2 = CattestUtil.secureHash(p1 + seed);
   
   JSONObject rslt1 = sendToCedes(userid,"generic/authorize","uid",uid,
         "patencoded",p2);
   String tok = rslt1.optString("token",null);
   if (tok == null) {
      CatreLog.logE("CATSCALEDEV","Failed to get access token from cedes: " +
            rslt1 + " " + new Date());
      return false;
    }
   
   CatreLog.logD("CATSCALEDEV","Device received access token " + tok);
   
   access_tokens.put(userid,tok);
   
   CatreLog.logD("CATSCALEDEV","Cedes access token " + tok + " " +
         new Date());
   
   return true;
}


private JSONObject sendToCedes(String uid,String nm,Object... args)
{
   JSONObject obj = CattestUtil.buildJson(args);
   
   return sendToCedes(uid,nm,obj);
}


private JSONObject sendToCedes(String uid,String nm,JSONObject obj) 
{
   return sendToCedes(uid,nm,obj.toString(2));
}


private JSONObject sendToCedes(String uid,String nm,String cnts) 
{
   CatreLog.logD("CATSCALEDEV","SEND TO CEDES:" + 
         nm + " " + uid + " " + cnts);
   
   try {
      if (!cedes_url.endsWith("/")) cedes_url += "/";
      String url = cedes_url + nm;
      URL u = new URI(url).toURL(); 
      HttpURLConnection hc = (HttpURLConnection) u.openConnection();
      hc.setUseCaches(false);
      hc.addRequestProperty("content-type","application/json");
      hc.addRequestProperty("accept","application/json");
      hc.setRequestMethod("POST");
      String acctok = access_tokens.get(uid);
      if (acctok != null) {
         hc.addRequestProperty("Authorization","Bearer " + acctok);
       }
      hc.setDoOutput(true);
      hc.setDoInput(true);
      
      hc.connect();
      
      OutputStream ots = hc.getOutputStream();
      ots.write(cnts.getBytes());
      
      InputStream ins = hc.getInputStream();
      String rslts = IvyFile.loadFile(ins);
      CatreLog.logD("CATSCALEDEV","Cedes Response: " + nm + ": " + rslts);
      return new JSONObject(rslts);
    }
   catch (Throwable e) {
      CatreLog.logE("CATSCALEDEV","Error sending to Cedes",e);
      // report error?
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Scale Test Device                                                       */
/*                                                                              */
/********************************************************************************/

private abstract class ScaleDevice implements Runnable {
   
   private String device_name;
   private String user_id;
   private String device_uid;
   private int device_counter;
   
   protected ScaleDevice(String user,String name) {
      device_name = name + "_" + user;
      user_id = user;
      device_uid = CattestUtil.getDeviceUid(user,name); 
      device_counter = 0;
      authenticate(user);
    }
   
   String getDeviceUser()                       { return user_id; }
   String getDeviceUid()                        { return device_uid; }
   int getDeviceCounter()                       { return device_counter; }
   void setDeviceCounter(int v)                 { device_counter = v; }
   
   void sendDeviceInfo() { 
      JSONObject dev = getDeviceJson();
      dev.put("UID",device_uid);
      dev.put("NAME",device_name);
      dev.put("BRIDGE","generic");
      dev.put("LABEL",device_name + " for " + device_uid);
      dev.put("PINGTIME",PING_TIME);
      
      JSONArray jarr = new JSONArray();
      jarr.put(dev);
      
      sendToCedes(user_id,"generic/devices","devices",jarr);
    }
   
   abstract JSONObject getDeviceJson();
   
   void handleCommand(JSONObject cmd)  {
      CatreLog.logI("CATSCALEDEV","Handle Command " + getDeviceUid() + " " +
            getDeviceUser() + " " + cmd.toString(2));
    }
   
   abstract void start();
   public abstract void run(); 
   
   void changeValue() {
      JSONObject evt = CattestUtil.buildJson("DEVICE",device_uid,
            "TYPE","PARAMETER",
            "PARAMETER","Value",
            "VALUE",getCurrentValue());
      sendToCedes(user_id,"generic/event","event",evt);
    }
   
   abstract Object getCurrentValue();
   
   protected void schedule(long delta) {
      double v = CattestUtil.nextRandomExp(delta);
      if (v < 0) v = 0;
      v = v*TIME_COMPRESSION;
      v = Math.round(v);
      long delay = (long) v;
      device_timer.schedule(new DeviceTask(this),delay);
    }
   
}       // end of inner class CattestScaleDevice



private final class ScaleDeviceEnum extends ScaleDevice {
   
   private List<String> device_states;
   private long on_time;
   private long off_time;
   private String current_state;
   
   ScaleDeviceEnum(String user,String name,long off,long on,String... states) {
      super(user,name);
      on_time = on * 60*1000;
      off_time = off * 60*1000;
      device_states = new ArrayList<>();
      if (states.length == 0) {
         device_states.add("OFF");
         device_states.add("ON");
       }
      else if (states.length == 1) {
         device_states.add("OFF");
         device_states.add(states[0]);
       }
      else {
         for (String s : states) {
            device_states.add(s);
          }
       }
      current_state = device_states.get(0);
    }
   
   @Override void start() {
      device_timer.schedule(new PingTask(this),PING_TIME,PING_TIME);
      schedule(off_time);
    }
   
   @Override public void run() {
      if (current_state.equals(device_states.get(0))) {
         // currently off
         int v = CattestUtil.nextRandom(device_states.size()-1);
         current_state = device_states.get(v+1);
         schedule(on_time);
       }
      else {
         current_state = device_states.get(0);
         schedule(off_time);
       }
      changeValue();
    }
   
   @Override JSONObject getDeviceJson() {
      JSONObject p0 = CattestUtil.buildJson("NAME","Value",
            "TYPE","ENUM",
            "ISSENSOR",true,
            "ISTARGET",false,
            "NOPING","OFF",
            "VALUES",device_states);
      List<JSONObject> params = new ArrayList<>();
      params.add(p0);
      
      JSONObject t1 = CattestUtil.buildJson("NAME","Notify");
      List<JSONObject> trans = new ArrayList<>();
      trans.add(t1);
      
      JSONObject rslt = CattestUtil.buildJson("TRANSITIONS",trans,
            "PARAMETERS",params);   
      
      return rslt;
    }
   
   @Override Object getCurrentValue()          { return current_state; }
   

}       // end of inner class ScaleDeviceEnum


private final class ScaleDeviceRange extends ScaleDevice {
   
   private double min_value;
   private double max_value;
   private long change_time;
   private double current_value;
   private double show_value;
   
   ScaleDeviceRange(String user,String name,long delta,double min,double max) {
      super(user,name);
      change_time = delta * 60 *1000;
      min_value = Math.min(min,max);
      max_value = Math.max(min,max);
      setValue((min+max)/2.0);
    }
   
   @Override void start() {
      device_timer.schedule(new PingTask(this),PING_TIME,PING_TIME);
      schedule(change_time);
    }
   
   @Override public void run() {
      double d = (max_value - min_value)/100.0;
      d *= CattestUtil.nextRandom() - 0.5;
      double v = current_value + d;
      if (v < min_value) v = min_value;
      if (v > max_value) v = max_value;
      if (setValue(v)) {
         changeValue();
       }
      schedule(change_time);
    }
   
   private boolean setValue(double v) {
      current_value = v;
      double v1 = Math.round(v * 10) / 10.0;
      if (show_value == v1) return false;
      show_value = v1;
      return true;
    }
   
   
   @Override JSONObject getDeviceJson() {
      JSONObject p0 = CattestUtil.buildJson("NAME","Value",
            "TYPE","REAL",
            "ISSENSOR",true,
            "ISTARGET",false,
            "NOPING","OFF",
            "MIN",min_value,
            "MAX",max_value);
      List<JSONObject> params = new ArrayList<>();
      params.add(p0);
      
      List<JSONObject> trans = new ArrayList<>();
      
      JSONObject rslt = CattestUtil.buildJson("TRANSITIONS",trans,
            "PARAMETERS",params);   
      
      return rslt;
    }
   
   @Override Object getCurrentValue()          { return show_value; }
   
}       // end of inner class ScaleDeviceRange


private class DeviceTask extends TimerTask {
   
   private ScaleDevice for_device;
   
   DeviceTask(ScaleDevice sd) {
      for_device = sd;
    }
   
   @Override public void run() {
      for_device.run();
    }
   
}       // end of inner class DeviceTask



/********************************************************************************/
/*                                                                              */
/*      Ping task for a user                                                    */
/*                                                                              */
/********************************************************************************/

private class PingTask extends TimerTask {
   
   private long last_time;
   private ScaleDevice for_device;
   private String user_id;
   
   PingTask(ScaleDevice dev) {
      last_time = 0;
      user_id = dev.getDeviceUser();
      for_device = dev;;
    }
   
   @Override public void run() {
      String acctok = access_tokens.get(user_id);
      try {
         if (acctok == null) {
            CatreLog.logD("CATSCALEDEV",
                  "Device ping " + acctok + " " + new Date() + " " +
                  last_time + " " + (System.currentTimeMillis() - last_time));
            if (last_time > 0 && System.currentTimeMillis() - last_time > ACCESS_TIME) {
               authenticate(user_id);
               last_time = System.currentTimeMillis();
             }
            else if (last_time <= 0) {
               last_time = System.currentTimeMillis();
             }
          }
         else {
            String usernm = USER_GENERIC_UID.replace("#",user_id);
            JSONObject obj = sendToCedes(user_id,"generic/ping",
                  "user",usernm,
                  "device",for_device.getDeviceUid(),
                  "counter",for_device.getDeviceCounter());
            String sts = "FAIL";
            if (obj != null) sts = obj.optString("status","FAIL");
            CatreLog.logD("CATSCALEDEV","CEDESPING " + obj.toString(2));
            switch (sts) {
               case "DEVICES" :
                  int ctr = obj.optInt("counter",0);
                  CatreLog.logD("CATSCALEDEV","Device Ping DEVICES " + 
                        for_device.getDeviceCounter() + " " + ctr);
                  if (ctr > 0) {
                     for_device.setDeviceCounter(ctr);
                   }
                  for_device.sendDeviceInfo();
                  break;
               case "COMMAND" :
                  JSONObject cmd = obj.getJSONObject("command");
                  for_device.handleCommand(cmd);
                  break;
               case "OK" :
                  break;
               default :
                  CatreLog.logI("CATSCALEDEV","Device lost access token: " + sts);
                  access_tokens.remove(user_id);
                  break;
             }
            last_time = System.currentTimeMillis();
          }
       }
      catch (Throwable t) {
         CatreLog.logE("CATSCALEDEV","PROBLEM HANDLING PING",t);
       }
    }
   
}       // end of inner class PingTask


}       // end of class CattestScaleDevices




/* end of CattestScaleDevices.java */

