/********************************************************************************/
/*                                                                              */
/*              DeviceComputerMonitor.java                                      */
/*                                                                              */
/*      Machine usage monitor sensor device                                     */
/*                                                                              */
/********************************************************************************/
/*	Copyright 2023 Brown University -- Steven P. Reiss			*/
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/


/* SVN: $Id$ */



package edu.brown.cs.iot.device;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public final class DeviceComputerMonitor extends DeviceBase
{ 


/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String... args)
{
   DeviceComputerMonitor mon = new DeviceComputerMonitor(args);
   mon.start();
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

enum ZoomOption { ON_ZOOM, NOT_ON_ZOOM };
enum WorkOption { WORKING, IDLE, AWAY, OFF };
enum PresenceOption { PRESENT, NOT_PRESENT };

private long    last_idle;
private WorkOption last_work;
private ZoomOption last_zoom;
private long    last_check;
private Map<String,PresenceOption> last_presence;
private Map<String,Integer> last_count;
private List<BluetoothDevice> active_devices;

private String  alert_command;
private String  zoom_command;
private File    zoom_docs;

private String  idle_command;
private long    idle_divider;

private String  bt_command;
private boolean need_python_setup;


private final String IDLE_COMMAND_1 = "xssstate -i";
private final String IDLE_COMMAND_2 = "ioreg -c IOHIDSystem | fgrep HIDIdleTime";
private final String IDLE_COMMAND_3 = "xprintidle";
      

// private final String ZOOM_COMMAND = "ps -ax | fgrep zoom | fgrep CptHost";
private final String ZOOM_COMMAND = "ps -ax -o lstart,command | fgrep zoom | fgrep CptHost";

private final String ALERT_COMMAND_1 = "notify-send $MSG";
private final String ALERT_COMMAND_2 = 
   "osascript -e 'display notification \"$MSG\" with title \"From Sherpa\" sound name \"Basso\" '";
private final String ALERT_COMMAND_3 = "zenity --notification '--title=From Sherpa' '--text=$MSG'";
   


private final DateFormat PS_DATE = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy");

private final String BT_COMMAND_1 = "sdptool browse $MAC";
private final String BT_COMMAND_2 = "python3 $BTDISCOVERY";
private final String BT_COMMAND_3 = "python $BTDISCOVERY";
private final String BT_FILE = "btDiscovery.py";

private final long POLL_TIME = 30000;

private static Map<String,String> carrier_table;

static {
   carrier_table = new HashMap<>();
   carrier_table.put("att","txt.att.net");
   carrier_table.put("boost","sms.myboostmobile.com");
   carrier_table.put("boostmobile","sms.myboostmobile.com");
   carrier_table.put("consumer","mailmymobile.net");
   carrier_table.put("consumercellular","mailmymobile.net");
   carrier_table.put("cricket","sms.cricketwireless.net");
   carrier_table.put("cspire","cspire1.com");
   carrier_table.put("fi","msg.fi.google.com");
   carrier_table.put("googlefi","msg.fi.google.com");
   carrier_table.put("h2o","txt.att.net");
   carrier_table.put("h2owireless","txt.att.net");
   carrier_table.put("metro","mymetropcs.com");
   carrier_table.put("paqeplus","vtext.com");
   carrier_table.put("replublic","text.republicwireless.com");
   carrier_table.put("replublicwireless","text.republicwireless.com");
   carrier_table.put("simple","smtext.com");
   carrier_table.put("simplemobile","smtext.com");
   carrier_table.put("sprint", "messaging.sprintpcs.com");
   carrier_table.put("straighttalk","vtext.com");
   carrier_table.put("tmobile","tmomail.net");
   carrier_table.put("ting","message.ting.com");
   carrier_table.put("tracfone","mmst5.tracfone.com");
   carrier_table.put("ultra","mailmymobile.net");
   carrier_table.put("ultramobile","mailmymobile.net");
   carrier_table.put("uscellular","email.uscc.net");
   carrier_table.put("usmobile","vtext.com");
   carrier_table.put("verizon","vtext.com");
   carrier_table.put("virgin","vmobl.com");
   carrier_table.put("virginmobile","vmobl.com"); 
   carrier_table.put("visible","vmobl.com");
   carrier_table.put("xfinity","vtext.com");
   carrier_table.put("xfinitymobile","vtext.com");
   carrier_table.put("bellmobility","txt.bellmobility.com");
   carrier_table.put("bell mobility","txt.bellmobility.com");
   carrier_table.put("rogers","pcs.rogers.com");
   carrier_table.put("fido","fido.ca");
   carrier_table.put("telus","msg.telus.com");
   carrier_table.put("koodo","msg.koodomobile.com");
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private DeviceComputerMonitor(String [] args)
{
   last_idle = -1;
   last_work = null;
   last_zoom = null;
   last_check = 0;
   last_presence = new HashMap<>();
   last_count = new HashMap<>();
   active_devices = new ArrayList<>();
   
   String os = System.getProperty("os.name").toLowerCase();
   
   File f1 = new File(System.getProperty("user.home"));
   File f2 = new File(f1,"Documents");
   zoom_docs = new File(f2,"Zoom");
   
   if (os.contains("mac")) {
      zoom_command = ZOOM_COMMAND;
    }
   else if (os.contains("win")) {
      zoom_command = null;
    }
   else {                               // linux
      zoom_command = ZOOM_COMMAND;
    }
   
   setupIdleCommands();
   setupAlertCommands();
   setupBtCommands();
}



/********************************************************************************/
/*                                                                              */
/*      Setup checking commands                                                 */
/*                                                                              */
/********************************************************************************/

private void setupIdleCommands()
{
   idle_command = null;
   idle_divider = 1;
   
   if (checkCommand(null,"xssstate","-i")) {
      idle_command = IDLE_COMMAND_1;
      idle_divider = 1000L;
    }
   if (checkCommand("HIDIdleTime","ioreg","-c","IOHIDSystem")) {
      idle_command = IDLE_COMMAND_2;
      idle_divider = 1000000000L;
    }
   if (checkCommand(null,"xprintidle")) {
      idle_command = IDLE_COMMAND_3;
      idle_divider = 1000L;
    }
   
   System.err.println("IDLE COMMAND: " + idle_command + " " + idle_divider);
}


private void setupAlertCommands()
{
   alert_command = null;
   
   if (checkCommand(null,"notify-send","-help")) {
      alert_command = ALERT_COMMAND_1;
    }
   else if (checkCommand(null,"osascript","-e","current date")) {
      alert_command = ALERT_COMMAND_2;
    }
   else if (checkCommand(null,"zenity","-help")) {
      alert_command = ALERT_COMMAND_3;
    }
}



private void setupBtCommands()
{
   bt_command = null;
   need_python_setup = false;

   if (checkCommand(null,"sdptool","-h")) {
      bt_command = BT_COMMAND_1;
    }
   else if (checkCommand("Python 3","python","--vesion")) {
      bt_command = BT_COMMAND_3;
      need_python_setup = true;
    } 
   else if (checkCommand("Python 3","python3","--version")) {
      bt_command = BT_COMMAND_2;
      need_python_setup = true;
    }
   
   System.err.println("Use BT command " + bt_command + " " + need_python_setup);
}


private void setupPython()
{
   if (bt_command == null) return; 
   
   boolean done = false;
   if (bt_command.equals(BT_COMMAND_2)) {
      if (checkCommand(null,"pip3","--help")) {
         try (BufferedReader br = runCommand("pip3 install bleak")) {
            done = true;
          }
         catch (IOException e) { }
       }
    }
   if (!done) {
      try (BufferedReader br = runCommand("pip install bleak")) {
         done = true;
       }
      catch (IOException e) { }
    }
   if (!done) bt_command = null;
   else if (bt_command.contains("$BTDISCOVERY")) {
      try (InputStream ins = this.getClass().getClassLoader().getResourceAsStream(BT_FILE)) {
         File f1 = File.createTempFile("btDiscovery",".py");
         try (FileOutputStream ots = new FileOutputStream(f1)) {
            byte [] buf = new byte[8192];
            for ( ; ; ) {
               int ln = ins.read(buf);
               if (ln <= 0) break;
               ots.write(buf,0,ln);
             }
          }
         catch (IOException e) { 
            f1 = null;
          }
         if (f1 != null) {
            f1.deleteOnExit();
            bt_command = bt_command.replace("$BTDISCOVERY",f1.getAbsolutePath());
          }
         else bt_command = null;
       }
      catch (IOException e) { }
    }
   
   System.err.println("Finished python setup " + bt_command);
   
   return;
}




private boolean checkCommand(String rslt,String... cmd)
{
   ProcessBuilder bp = new ProcessBuilder(cmd);
   bp.redirectError(ProcessBuilder.Redirect.DISCARD);
   String cnts = null;
   int sts = -1;
   try {
      Process p = bp.start();
      InputStream ins = p.getInputStream();
      InputStreamReader isr = new InputStreamReader(ins);
      StringBuffer buf = new StringBuffer();
      char [] cbuf = new char[16384];
      for ( ; ; ) {
         int ln = isr.read(cbuf);
         if (ln <= 0) break;
         buf.append(cbuf,0,ln);
       }
      cnts = buf.toString();
      ins.close();
      for ( ; ; ) {
         try {
            sts = p.waitFor();
            break;
          }
         catch (InterruptedException e) { }
       }
    }
   catch (IOException e) {
      return false;
    }
   
   if (sts != 0) return false;
   if (rslt != null) {
      if (cnts == null) return false;
      if (!cnts.contains(rslt)) return false;
    }
   
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override protected String getDeviceName()
{
   return "COMPUTER_MONITOR_" + getHostName();
}



@Override protected JSONObject getDeviceJson()
{
   JSONObject param0 = buildJson("NAME","WorkStatus",
         "TYPE","ENUM",
         "ISSENSOR",true,
         "ISTARGET",false,
         "NOPING",WorkOption.OFF,
         "VALUES",List.of(WorkOption.values()));
   
   JSONObject param1 = buildJson("NAME","ZoomStatus",
         "TYPE","ENUM",
         "ISSENSOR",true,
         "ISTARGET",false,
         "NOPING",ZoomOption.NOT_ON_ZOOM,
         "VALUES",List.of(ZoomOption.values()));
   
   
   JSONObject tparam2 = buildJson("NAME","Subject","TYPE","STRING");
   JSONObject tparam3 = buildJson("NAME","Body","TYPE","STRING");     
   JSONObject tparam4 = buildJson("NAME","Message","TYPE","STRING");
   JSONObject pset1 = buildJson("PARAMETERS",List.of(tparam2,tparam3));
   JSONObject pset2 = buildJson("PARAMETERS",List.of(tparam4));
   JSONObject pset3 = buildJson("PARAMETERS",List.of(tparam4));
         
   JSONObject trans1 = buildJson("NAME","SendEmail","DEFAULTS",pset1);
   JSONObject trans2 = buildJson("NAME","SendText","DEFAULTS",pset2);
   JSONObject trans3 = buildJson("NAME","SendAlert","DEFAULTS",pset3);
   
   List<JSONObject> translist = new ArrayList<>();
   if (getDeviceParameter("email") != null) translist.add(trans1);
   if (getDeviceParameter("textNumber") != null && getDeviceParameter("textProvider") != null) {
      translist.add(trans2);
    }
   if (alert_command != null) translist.add(trans3);
   
   List<JSONObject> paramlist = new ArrayList<>();
   if (idle_command != null) paramlist.add(param0);
   if (zoom_command != null) paramlist.add(param1);
   if (bt_command != null) {
      for (BluetoothDevice bd : active_devices) {
         JSONObject param2 = buildJson("NAME","Presence-" + bd.getName(),
               "TYPE","ENUM",
               "ISSENSOR",true,
               "ISTARGET",false,
               "NOPING",PresenceOption.NOT_PRESENT,
               "VALUES",List.of(PresenceOption.values()));
         paramlist.add(param2);
       }
    }
   
   String lbl = getDeviceParameter("label");
   if (lbl == null || lbl.isEmpty()) lbl= "Monitor status on " + getHostName();
     
   JSONObject obj = buildJson("LABEL",lbl,
         "PINGTIME",PING_TIME,
         "TRANSITIONS",translist,
         "PARAMETERS",paramlist);
   
   resetParameters();
   
   return obj;
}


@Override protected void resetDevice(boolean fg)
{
   System.err.println("RESET computer monitor DEVICE " + fg);
   if (fg) {
      resetParameters();
    }
   super.resetDevice(fg);
}


private void resetParameters()
{
   last_work = null;
   last_zoom = null;
   last_check = 0;
   last_presence.clear();
   last_count.clear();
}


/********************************************************************************/
/*                                                                              */
/*      Start method                                                            */
/*                                                                              */
/********************************************************************************/

@Override protected void start()
{
   super.start();
   
   setupBluetoothDevices();
   
   System.err.println("Start computer monitor");
}



/********************************************************************************/
/*                                                                              */
/*      Command processing                                                      */
/*                                                                              */
/********************************************************************************/

@Override protected void processDeviceCommand(String name,JSONObject values)
{
   System.err.println("Process computer monitor command " + name + " " + values);
   
   try {
      switch (name) {
         case "SendEmail" :
            sendEmail(values.optString("Subject"),values.optString("Body"));
            break;
         case "SendText" :
            sendText(values.optString("Message"));
            break;
         case "SendAlert" :
            sendAlert(values.optString("Message"));
       }
    }
   catch (Throwable t) {
      System.err.println("Computer monitor command problem");
      t.printStackTrace();
    }
}


private boolean sendEmail(String subj,String body)
{
   if (subj == null && body == null) return false;
   String sendto = getDeviceParameter("email");
   if (sendto == null) return false;
   
   try {
      if (subj != null) subj = URLEncoder.encode(subj,"UTF-8");
    }
   catch (UnsupportedEncodingException e) { }
   try {
      if (body != null) body = URLEncoder.encode(body,"UTF-8");
    }
   catch (UnsupportedEncodingException e) { }
   
   String full = "mailto:" + sendto;
   String pfx = "?";
   try {
      if (subj != null) {
         full += pfx + "subject=" + subj;
         pfx = "&";
       }
      if (body != null) {
         full +=  pfx + "body=" + body;
         pfx = "&";
       }
      URI u = new URI(full);
      Desktop.getDesktop().mail(u);  
    }
   catch (Throwable e) {
      return false;
    }
   
   return true;
}


private boolean sendText(String msg)
{
   if (msg == null) return false;
   String num = getDeviceParameter("textNumber");
   String prov = getDeviceParameter("textProvider");
   if (num == null || prov == null) return false;
   
   if (num.length() > 10 && num.startsWith("1")) num = num.substring(1);
   
   prov = prov.toLowerCase();
   prov = prov.replace(" ","");
   prov = prov.replace("&","");
   prov = prov.replace(".","");
   prov = prov.replace("-","");
   
   String sfx = carrier_table.get(prov);
   if (sfx == null) return false;
   
   try {
      msg = URLEncoder.encode(msg,"UTF-8");
    }
   catch (UnsupportedEncodingException e) { }
   
   String full = "mailto:" + num + "@" + sfx + "?body=" +  msg;
   
   try {
      URI u = new URI(full);
      Desktop.getDesktop().mail(u);
    }
   catch (Throwable t) {
      return false;
    }
   
   return true;
}


private boolean sendAlert(String msg)
{
   if (msg == null || alert_command == null) return false;
   
   msg = msg.replace("\"","#");
   String cmd = alert_command.replace("$MSG",msg);
   
   try {
      runCommand(cmd);
    }
   catch (IOException e) {
      return false;
    }
   
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Check for changes                                                       */
/*                                                                              */
/********************************************************************************/

@Override protected void handlePoll()
{
   long now = System.currentTimeMillis();
   if (now - last_check < POLL_TIME) return;
   last_check = now;
   checkStatus();
}


private void checkStatus()
{
   if (access_token == null) {
      last_idle = -1;
      last_zoom = null;
    }
   
   long idle = getIdleTime();
   
   System.err.println("CHECK STATUS at " + new Date());
   System.err.println("WORK STATUS: " + idle + " " + last_idle + " " + last_work);
   
   WorkOption workopt = WorkOption.WORKING;
   if (idle < last_idle || idle < 60) {
      // if idle went down, then we are working
      // otherwise give us 30 seconds grace to read the display
    }
   else if (idle >= 7200) {
      workopt = WorkOption.OFF;
    }
   else if (idle >= 300) {
      workopt = WorkOption.AWAY;
    }
   else workopt = WorkOption.IDLE;
   if (workopt == last_work) {
      workopt = null;
    }
   else {
      System.err.println("Work status change " + idle + " " + last_idle + " " + workopt);
      last_work = workopt;
    }
   last_idle = idle;
   
   ZoomOption zoomval = getZoomStatus();
   if (zoomval == last_zoom) zoomval = null;
   else {
      System.err.println("Note CHANGE Zoom status " + last_zoom + " -> " + zoomval);
      last_zoom = zoomval;
    }
   
   Map<String,PresenceOption> preopts = getDeviceStatus();
   for (BluetoothDevice bd : active_devices) {
      String nm = bd.getName();
      PresenceOption preopt = preopts.get(nm);
      if (preopt == null) preopt = PresenceOption.NOT_PRESENT;
      PresenceOption lastpre = last_presence.get(nm);
      if (preopt == lastpre) {
         preopt = null;
         last_count.remove(nm);
       }
      else if (preopt == PresenceOption.NOT_PRESENT && 
            lastpre == PresenceOption.PRESENT) {
         Integer ct0 = last_count.get(nm);
         int ct = 0;
         if (ct0 != null) ct = ct0;
         ++ct;
         if (ct <= 4) {
            preopt = null;
            last_count.put(nm,ct);
          }
         else {
            last_presence.put(nm,preopt);
            last_count.remove(nm);
          }
       }
      else {
         last_presence.put(nm,preopt);
         last_count.remove(nm);
       }
      if (preopt != null) {
         System.err.println("Note computer monitor " + nm + " status: " + preopt);
         sendParameterEvent("Presence-" + nm,preopt);
       }
    }
   
   if (workopt != null) {
      System.err.println("Note computer monitor work status: " + workopt);
      sendParameterEvent("WorkStatus",workopt);
    }
   
   if (zoomval != null) {
      System.err.println("Note computer monitor zoom: " + zoomval);
      sendParameterEvent("ZoomStatus",zoomval);
    }
}


private long getIdleTime()
{
   if (idle_command == null) return -1;
   
   try (BufferedReader br = runCommand(idle_command)) {
      for ( ; ; ) {
         String ln = br.readLine();
         if (ln == null) break;
         System.err.println("IDLE COMMAND YIELDS: " + ln);
         String num = ln;
         int idx = ln.indexOf("=");
         if (idx > 0) {
            num = ln.substring(idx+1).trim();
          } 
         long lv = Long.parseLong(num);
         lv /= idle_divider;
         return lv;
       }
    }
   catch (IOException e) { }

   return -1;
}


private ZoomOption getZoomStatus()
{
   if (zoom_command == null) return null;
      
   ZoomOption zoomval = ZoomOption.NOT_ON_ZOOM;
   
   // first find an active zoom process and get its start time
   String zoomstart = null;
   try (BufferedReader br = runCommand(zoom_command)) {
      for ( ; ; ) {
         String ln = br.readLine();
         if (ln == null) break;
         if (ln.contains("sh -c")) continue;
         if (ln.contains("zoom") && ln.contains("CptHost")) {
            int idx = ln.indexOf("/");
            zoomstart = ln.substring(0,idx).trim();
            break;
          }
       }
      if (zoomstart == null) {
         // not on zoom at all
         return zoomval;
       }
    }
   catch (IOException e) {
      return zoomval;
    }
   
   Date starttime = null;
   try {
      starttime = PS_DATE.parse(zoomstart);
    }
   catch (ParseException e) { }
   
      
   zoomval = ZoomOption.ON_ZOOM;                                // might guess inactive here
   // next look for directory for this zoom session
   if (!zoom_docs.exists()) return zoomval;
   
   // this may only work with a chat or after some period of time
   
   File last = null;
   long lastdlm = 0;
   for (File f4 : zoom_docs.listFiles()) {
      if (f4.getName().contains(".DS_Store")) continue;
      if (f4.lastModified() > lastdlm) {
         last = f4;
         lastdlm = f4.lastModified();
       }
    }
   if (starttime != null && lastdlm < starttime.getTime()) last = null;
   if (last == null) return zoomval;
   
   zoomval = ZoomOption.ON_ZOOM;
   
   // This doesn't work -- directory not created at startup, only sometimes
// if (last.getName().contains("Personal Meeting Room")) zoomval = ZoomOption.PERSONAL_ZOOM;
      
   return zoomval;
}


@SuppressWarnings("unused")
private boolean inPersonalZoom(boolean zoom)
{
   if (!zoom) return false;
   
   if (!zoom_docs.exists()) return false;
   
   File last = null;
   long lastdlm = 0;
   for (File f4 : zoom_docs.listFiles()) {
      if (f4.lastModified() > lastdlm) {
         last = f4;
         lastdlm = f4.lastModified();
       }
    }
   if (last == null) return false;
   
   if (last.getName().contains("Personal Meeting Room")) return true;
   
   return false;
}



private Map<String,PresenceOption> getDeviceStatus()
{
   Map<String,PresenceOption> rslt = new HashMap<>();
   
   if (need_python_setup) {
      setupPython();
      need_python_setup = false;
    }
   
   if (bt_command == null) return rslt;
   
   List<String> bttext = null;
   for (BluetoothDevice bd : active_devices) {
      String m = bd.getBtMac();
      String u = bd.getBtUUID();
      String nm = bd.getName();
      String cmd = bt_command;
      if (bt_command.contains("$MAC")) {
         if (m == null) cmd = null;
         else {
            cmd = cmd.replace("$MAC",m);
            bttext = null;
          }
       }
      if (bttext == null) {
         bttext = getCommandOutput(cmd);
         if (bttext == null) continue;
       }
      PresenceOption po = PresenceOption.NOT_PRESENT;
      for (String ln : bttext) {
         if (ln.contains("FAILED")) break;
         if (m != null && ln.contains(m)) {
            po = PresenceOption.PRESENT;
            break;
          }
         if (u != null && ln.contains(u)) {
            po = PresenceOption.PRESENT;
            break;
          }
       }
      rslt.put(nm,po);
    }
   
   return rslt;
}



private List<String> getCommandOutput(String cmd)
{
   List<String> rslt = new ArrayList<>();
   
   try (BufferedReader rdr = runCommand(cmd)) {
      for ( ; ; ) {
         String ln = rdr.readLine();
//       System.err.println("PHONE STATUS LINE: " + ln);
         if (ln == null) break;
         ln = ln.toUpperCase();
         rslt.add(ln);
       }
      return rslt;
    }
   catch (IOException e) { 
      System.err.println("Problem checking phone status " + e);
      rslt = null;
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Information for a bluetooth device                                      */
/*                                                                              */
/********************************************************************************/

private void setupBluetoothDevices()
{
   Object devs = getDeviceJson("presence"); 
   if (devs == null) {
      String mac = getDeviceParameter("phoneBt");
      String uuid = getDeviceParameter("phboneBtUUID");
      if (mac != null || uuid != null) {
         BluetoothDevice dev = new BluetoothDevice("Phone",mac,uuid);
         if (dev.isValid()) {
            active_devices.add(dev);
          }
       }
    }
   else if (devs instanceof JSONObject) {
      BluetoothDevice dev = new BluetoothDevice((JSONObject) devs);
      if (dev.isValid()) {
         active_devices.add(dev);
       }
    }
   else if (devs instanceof JSONArray) {
      JSONArray arr = (JSONArray) devs;
      for (int i = 0; i < arr.length(); ++i) {
         JSONObject dev = arr.getJSONObject(i);
         BluetoothDevice de = new BluetoothDevice(dev);
         if (de.isValid()) {
            active_devices.add(de);
          }
       }
    }
   else {
      System.err.println("Bad type in presence data " + devs);
    }
}



private final class BluetoothDevice {
   
   private String device_name;
   private String bt_mac;
   private String bt_uuid;
   
   BluetoothDevice(JSONObject jo) {
      device_name = jo.getString("name");
      bt_mac = jo.optString("Bt");
      bt_uuid = jo.optString("BtUUID");
    }
   
   BluetoothDevice(String nm,String mac,String uuid) {
      device_name = nm;
      bt_mac = mac;
      bt_uuid = uuid;
    }
   
   String getName()                             { return device_name; }
   String getBtMac()                            { return bt_mac; }
   String getBtUUID()                           { return bt_uuid; }
   
   boolean isValid() {
      if (device_name == null || device_name.isEmpty()) return false;
      if (bt_mac == null && bt_uuid == null) return false;
      return true;
    }
   
}       // end of inner class BluetoothDevice



}       // end of class DeviceComputerMonitor



/* end of DeviceComputerMonitor.java */

