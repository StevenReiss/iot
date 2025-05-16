/********************************************************************************/
/*                                                                              */
/*              CatserveBowerServer.java                                        */
/*                                                                              */
/*      Web server for CATRE using Bower                                        */
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



package edu.brown.cs.catre.catserve;


import edu.brown.cs.ivy.bower.BowerCORS;
import edu.brown.cs.ivy.bower.BowerRouter;
import edu.brown.cs.ivy.bower.BowerServer;
import edu.brown.cs.ivy.bower.BowerConstants.BowerSessionStore;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.karma.KarmaUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.annotation.Tainted;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;

import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreJson;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatreRule;
import edu.brown.cs.catre.catre.CatreSavable;
import edu.brown.cs.catre.catre.CatreServer;
import edu.brown.cs.catre.catre.CatreSession;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTable;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUser;
import edu.brown.cs.catre.catre.CatreUtil;

public class CatserveBowerServer implements CatserveConstants, CatreJson, CatreServer
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BowerServer<CatserveSessionImpl> http_server;
private CatreController catre_control;
private CatserveAuth auth_manager;
private SessionStore session_store;
private BowerRouter<CatserveSessionImpl> bower_router;
private String url_prefix;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public CatserveBowerServer(CatreController cc)
{
   catre_control = cc;
   session_store = new SessionStore(catre_control);
   auth_manager = new CatserveAuth(cc);
   
   File f1 = cc.findBaseDirectory();
   File f2 = new File(f1,"secret");
   File f3 = new File(f2,"catre.jks");
   File f4 = new File(f2,"catre.props");
   Properties p = new Properties();
   p.put("jkspwd","XXX");
   try (FileInputStream fis = new FileInputStream(f4)) {
      p.loadFromXML(fis);
    }
   catch (IOException e) { }
   String keystorepwd = p.getProperty("jkspwd");
   if (keystorepwd != null && keystorepwd.equals("XXX")) keystorepwd = null;
   
   bower_router = setupRouter();
   http_server = new BowerServer<>(HTTPS_PORT,session_store);
   http_server.setRouter(bower_router);
   if (keystorepwd != null) {
      http_server.setupHttps(f3,keystorepwd);
    }
   http_server.setExecutor(new ServerExecutor());
   
   catre_control.register(new SessionTable());
   
   if (!http_server.setup()) {
      CatreLog.logE("CATSERVE","Problem starting https server");
      System.exit(1);
    }
   
   String pfx = p.getProperty("hostpfx");
   url_prefix = fixUrlPrefix(pfx);
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getUrlPrefix()
{
   return url_prefix;
}


private String fixUrlPrefix(String pfx)
{
   if (pfx == null || pfx.isEmpty()) {
      pfx = http_server.getUrlPrefix();
    }
   
   // add https: if needed
   int idx = pfx.indexOf(":");
   if (idx < 0 || idx > 6) {
      pfx = "https://" + pfx;
      idx = pfx.indexOf(":");
    }
   
   // add port number if needed
   if (idx < 6) {
      int idx1 = pfx.indexOf(":",idx+1);
      if (idx1 < 0) {
         pfx += ":" + HTTPS_PORT;
       }
    }
   
   return pfx;
}



/********************************************************************************/
/*                                                                              */
/*      Run methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override 
public void start() throws IOException
{
   http_server.start();
   
   CatreLog.logI("CATSERVE","CATRE SERVER STARTED ON " + HTTPS_PORT);
}




/********************************************************************************/
/*                                                                              */
/*      Router setup                                                            */
/*                                                                              */
/********************************************************************************/

private BowerRouter<CatserveSessionImpl> setupRouter()
{
   BowerRouter<CatserveSessionImpl> br = new BowerRouter<>(session_store);
   br.addRoute("ALL","/ping",this::handlePing);
   br.addRoute("ALL",BowerRouter::handleParameters);
   br.addRoute("ALL",br::handleSessions);
   br.addRoute("ALL",BowerRouter::handleLogging);
   br.addRoute("ALL",new BowerCORS("*"));
   
   br.addRoute("ALL","/static",this::handleStatic);
   
   br.addRoute("GET","/login",this::handlePrelogin);
   br.addRoute("POST","/login",auth_manager::handleLogin); 
   br.addRoute("POST","/register",auth_manager::handleRegister);
   br.addRoute("GET","/logout",this::handleLogout);
   br.addRoute("POST","/forgotpassword",auth_manager::handleForgotPassword);
   br.addRoute("GET","/validate",auth_manager::handleValidateUser);
   br.addRoute("ALL","/logmessage",this::handleLogMessage);
   
   // might want to handle favicon
   
   br.addRoute("ALL",this::handleAuthorize);
   br.addRoute("POST","/changepassword",auth_manager::handleChangePassword);
   
   br.addRoute("ALL",this::handleUserAuthorize);
   
   br.addRoute("ALL","/keypair",this::handleKeyPair);
   br.addRoute("POST","/removeuser",this::handleRemoveUser);
   
   br.addRoute("POST","/bridge/add",this::handleAddBridge);
   br.addRoute("GET","/bridge/list",this::handleListBridges);
   br.addRoute("GET","/universe",this::handleGetUniverse);
   br.addRoute("POST","/universe/discover",this::handleDiscover);
   br.addRoute("POST","/universe/addvirtual",this::handleAddVirtualDevice);
   br.addRoute("POST","/universe/addweb",this::handleAddWebDevice);
   br.addRoute("POST","/universe/removedevice",this::handleRemoveDevice);
   br.addRoute("POST","/universe/enabledevice",this::handleEnableDevice);
   br.addRoute("POST","/universe/deviceStates",this::handleDeviceStates);
   br.addRoute("POST","/universe/shareCondition",this::handleShareCondition);
   br.addRoute("POST","/universe/unshareCondition",this::handleUnshareCondition);
   br.addRoute("POST","/universe/getValue",this::handleGetValue);
   br.addRoute("GET","/rules",this::handleListRules);
   br.addRoute("POST","/rule/add",this::handleAddRule);
   br.addRoute("POST","/rule/edit",this::handleEditRule);
   br.addRoute("POST","/rule/validate",this::handleValidateRule);
   br.addRoute("POST","/rule/remove",this::handleRemoveRule);
   
   br.addRoute("POST","/rule/:ruleid/edit",this::handleEditRule);
   br.addRoute("POST","/rule/:ruleid/remove",this::handleRemoveRule);
   br.addRoute("POST","/rule/:ruleid/priority",this::handleSetRulePriority);
   
   return br;
}


/********************************************************************************/
/*                                                                              */
/*      Basic routes                                                            */
/*                                                                              */
/********************************************************************************/

private String handlePing(HttpExchange e)
{
   return "{ 'pong' : true }";
}


private String handleStatic(HttpExchange ex)
{
   URI uri = ex.getRequestURI();
   String path = uri.getPath();
   if (path.startsWith("/static/")) {
      path = path.substring(8);
    }
   if (path.isEmpty()) {
      path = "home.html";
    }
   
   File f1 = catre_control.findBaseDirectory();
   File f2 = new File(f1,"catre");
   File f3 = new File(f2,"web");
   File f4 = new File(f3,path);
   if (f4.exists()) {
      try {
	 return IvyFile.loadFile(f4);
       }
      catch (IOException e) {
	 // let system return 404
       }
    }
   
   return null;
}


private String handleLogMessage(HttpExchange he)
{
   String msg = BowerRouter.getParameter(he,"message");
   CatreLog.logD("SHERPA",msg);
   return "{ 'STATUS' : 'OK' }";
}


/********************************************************************************/
/*										*/
/*	Authorization functions 						*/
/*										*/
/********************************************************************************/

private String handlePrelogin(HttpExchange exchange,CatserveSessionImpl cs)
{
   if (cs == null) {
      return BowerRouter.errorResponse(exchange, cs,400,"Bad session");    
    }
   String salt = CatreUtil.randomString(32);
   cs.setValue("SALT",salt);
   return BowerRouter.jsonOKResponse(cs,"SALT",salt);
}


private String handleAuthorize(HttpExchange he,CatserveSessionImpl cs)
{
   CatreLog.logD("CATSERVE","AUTHORIZE " +
         BowerRouter.getParameter(he,SESSION_PARAMETER));
   if (cs == null || 
         cs.getUser(catre_control) == null ||
	 cs.getUniverse(catre_control) == null) {
      if (cs == null) {
         CatreLog.logI("CATSERVE","Unauthorized access -- no session");
       }
      else if (cs.getUser(catre_control) == null) {
         CatreLog.logI("CATSERVE","Unauthorized access -- no user");
       }
      else {
         CatreLog.logI("CATSERVE","Unauthorized access -- no universe");
       }
      
      return BowerRouter.errorResponse(he,cs,402,"Unauthorized access");
    }
   
   KarmaUtils.event("PREAUTHORIZED");
   
   return null;
}


private String handleUserAuthorize(HttpExchange he,CatserveSessionImpl cs)
{
   CatreLog.logD("CATSERVE","AUTHORIZE " +
         BowerRouter.getParameter(he,SESSION_PARAMETER));
   if (cs == null || cs.getUser(catre_control) == null ||
	 cs.getUniverse(catre_control) == null || cs.getUser(catre_control).isTemporary()) {
      return BowerRouter.errorResponse(he,cs,402,"Unauthorized access");
    }
   
   KarmaUtils.event("AUTHORIZED");
   
   return null;
}


private String handleLogout(HttpExchange e,CatserveSessionImpl cs)
{
   if (cs != null) {
      bower_router.endSession(cs.getSessionId()); 
      cs.removeSession(catre_control);
    }
   
   cs = null;
   
   return BowerRouter.jsonOKResponse(cs);
}


private String handleRemoveUser(HttpExchange e,CatserveSessionImpl cs)
{
   CatreUser cu = cs.getUser(catre_control);
   
   if (cu == null) {
      return BowerRouter.errorResponse(e,cs,400,"User doesn't exist");
    }
   
   CatreUniverse cuv = cs.getUniverse(catre_control);
   if (cuv != null) {
      catre_control.getDatabase().removeObject(cuv.getDataUID());
    }
   if (cu != null) {
      catre_control.getDatabase().removeObject(cu.getDataUID());
    }
   
   return handleLogout(e,cs);
}



/********************************************************************************/
/*										*/
/*	Handle model setup requests						*/
/*										*/
/********************************************************************************/

@SuppressWarnings("unchecked")
private String handleAddBridge(HttpExchange e,CatserveSessionImpl cs)
{
   Map<String,String> keys = new HashMap<>();
   String bridge = null;
   
   Map<String,List<String>> params = (Map<String,List<String>>) e.getAttribute("paramMap");
   for (Map.Entry<String,List<String>> ent : params.entrySet()) {
      if (ent.getValue() == null || ent.getValue().size() != 1) continue;
      String val = ent.getValue().get(0);
      if (ent.getKey().equalsIgnoreCase("BRIDGE")) {
	 bridge = val;
       }
      else if (ent.getKey().startsWith("AUTH")) {
	 keys.put(ent.getKey(),val);
       }
    }
   
   boolean fg = cs.getUser(catre_control).addAuthorization(bridge,keys);
   
   if (!fg) {
      return BowerRouter.errorResponse(e,cs,400,"No bridge given");
    }
   
   return BowerRouter.jsonOKResponse(cs);
}



private String handleListBridges(HttpExchange e,CatserveSessionImpl cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   
   Collection<CatreBridge> basebrs = catre_control.getAllBridges(null);
   Collection<CatreBridge> userbrs = catre_control.getAllBridges(cu);
   
   JSONArray rslt = new JSONArray();
   for (CatreBridge cb : basebrs) {
      for (CatreBridge ub1 : userbrs) {
	 if (ub1.getName().equals(cb.getName())) {
	    cb = ub1;
	    break;
	  }
       }
      Object obj = cb.getBridgeInfo();
      rslt.put(obj);
    }
   
   return BowerRouter.jsonOKResponse(cs,"BRIDGES",rslt);
}

private String handleKeyPair(HttpExchange e,CatserveSessionImpl cs)
{
   String uid = CatreUtil.randomString(16);
   String pat = CatreUtil.randomString(24);
   
   return BowerRouter.jsonOKResponse(cs,"UID",uid,"PAT",pat);
}


private String handleDiscover(HttpExchange e,CatserveSessionImpl cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   
   cu.updateDevices(false);
   
   return BowerRouter.jsonOKResponse(cs);
}


private String handleAddVirtualDevice(HttpExchange e,CatserveSessionImpl cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   
   JSONObject dev = getJson(e,"DEVICE");
   CatreDevice cd = null;
   
   if (dev != null) {
      cd = cu.createVirtualDevice(cu.getCatre().getDatabase(),dev.toMap());
    }
   
   if (cd == null) {
      return BowerRouter.errorResponse(e,cs,400,"Bad device definition");
    }
   else {
      return BowerRouter.jsonOKResponse(cs,"DEVICE",cd.toJson(),
	    "DEVICEID",cd.getDeviceId());
    }
}



private String handleAddWebDevice(HttpExchange e,CatserveSessionImpl cs)
{
   // TODO : implement new web device
   
   return BowerRouter.errorResponse(e,cs,500,"unimplemented");
}


private String handleRemoveDevice(HttpExchange e,CatserveSessionImpl cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   
   String devid = BowerRouter.getParameter(e, "DEVICEID"); 
   CatreDevice cd = cu.findDevice(devid);
   if (cd == null) {
      return BowerRouter.errorResponse(e,cs,400,"Device not found");
    }
   
   if (cd.getBridge() != null && cd.isEnabled()) {
      return BowerRouter.errorResponse(e,cs,400,"Can't remove active device");
    }
   
   cu.removeDevice(cd);
   return BowerRouter.jsonOKResponse(cs);
}


private String handleEnableDevice(HttpExchange e,CatserveSessionImpl cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   
   String devid = BowerRouter.getParameter(e, "DEVICEID");
   CatreDevice cd = cu.findDevice(devid);
   if (cd == null) {
      return BowerRouter.errorResponse(e,cs,400,"Device not found");
    }
   
   Boolean fg = BowerRouter.getBooleanParameter(e,"ENABLE",null);
   if (fg == null) {
      return BowerRouter.errorResponse(e,cs,400,"Bad enable value");
    }
   
   cd.setEnabled(fg);
   
   return BowerRouter.jsonOKResponse(cs);
}


private String handleDeviceStates(HttpExchange e,CatserveSessionImpl cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   String devid = BowerRouter.getParameter(e,"DEVICEID");
   CatreDevice cd = cu.findDevice(devid);
   if (cd == null) {
      return BowerRouter.errorResponse(e,cs,400,"Device not found");
    }
   
   // probably want to get the latest device state here
   
   cd.updateParameterValues();
   
   JSONObject rslt = buildJson("NAME",cd.getName(),
         "DESCRIPTION",cd.getDescription(),
         "LABEL",cd.getLabel(),
         "ENABLED",cd.isEnabled());
         
   for (CatreParameter cp : cd.getParameters()) {
      if (cp.isSensor()) {
	 rslt.put(cp.getName(),cd.getParameterValue(cp));
       }
    }
   
  return BowerRouter.jsonOKResponse(cs,rslt);
}




private String handleGetUniverse(HttpExchange e,CatserveSessionImpl cs)
{
   Map<String,Object> unimap = cs.getUniverse(catre_control).toJson();
   //TODO - remove any private information from unimap
   
   CatreLog.logD("CATSERVE","Return universe map " + unimap);
   
   JSONObject obj = new JSONObject(unimap);
   
   CatreLog.logD("CATSERVE","Return universe " + obj.toString(2));
   
  return BowerRouter.jsonOKResponse(cs,obj);
}


private String handleShareCondition(HttpExchange e,CatserveSessionImpl cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();
   
   String condtest = BowerRouter.getParameter(e,"CONDITION");
   JSONObject jobj = new JSONObject(condtest);
   Map<String,Object> condmap = jobj.toMap();
   
   CatreLog.logI("CATSERVE","Share condition: " + jobj.toString(2));
   
   CatreCondition cc = cp.createCondition(cu.getCatre().getDatabase(),condmap);
   
   if (cc == null) {
      return BowerRouter.errorResponse(e,cs,400,"Bad condition definition");
    }
   
   cp.addSharedCondition(cc);
   
  return BowerRouter.jsonOKResponse(cs);
}


private String handleUnshareCondition(HttpExchange e,CatserveSessionImpl cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();
   String condname = BowerRouter.getParameter(e,"CONDNAME");
   
   CatreLog.logI("CATSERVE","Unshare condition " + condname);
   cp.removeSharedCondition(condname);
   
  return BowerRouter.jsonOKResponse(cs);
}


private String handleGetValue(HttpExchange e,CatserveSessionImpl cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   String dnm = BowerRouter.getParameter(e,"DEVICE");
   String pnm = BowerRouter.getParameter(e,"PARAMETER");
   CatreLog.logD("CATSERVE","GET VALUE " + dnm + " " + pnm);
   
   CatreDevice cd = cu.findDevice(dnm);
   if (cd == null) {
      return BowerRouter.errorResponse(e,cs,400,"Bad device");
    }
   CatreParameter cp = cd.findParameter(pnm);
   if (cp == null) {
      return BowerRouter.errorResponse(e,cs,400,"Bad parameter");
    }
   Object v = cu.getValue(cp);
   v = cp.unnormalize(v);
   
   JSONObject obj = buildJson("STATUS","OK",
	 "DEVICE",dnm,
	 "PARAMETER",pnm,
	 "VALUE",v);
   
  return BowerRouter.jsonOKResponse(cs,obj);
}



private String handleListRules(HttpExchange e,CatserveSessionImpl cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();
   List<CatreRule> rules = cp.getRules();
   List<Map<String,Object>> ruleout = new ArrayList<>();
   for (CatreRule cr : rules) {
      ruleout.add(cr.toJson());
    }
   
   return BowerRouter.jsonOKResponse(cs,"RULES",ruleout);
}


private String handleAddRule(HttpExchange e,CatserveSessionImpl cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();
   
   String ruletext = BowerRouter.getParameter(e, "RULE");
   JSONObject jobj = new JSONObject(ruletext);
   Map<String,Object> rulemap = jobj.toMap();
   
   CatreLog.logI("CATSERVE","Create rule: " + jobj.toString(2));
   
   CatreRule cr = cp.createRule(cu.getCatre().getDatabase(),rulemap);
   
   if (cr == null) {
      return BowerRouter.errorResponse(e,cs,400,"Bad rule definition");
    }
   
   cp.addRule(cr);
   
   return BowerRouter.jsonOKResponse(cs,"RULE",cr.toJson());
   
}


private String handleValidateRule(HttpExchange e,CatserveSessionImpl cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();
   String ruletext = BowerRouter.getParameter(e,"RULE");
   JSONObject jobj = new JSONObject(ruletext);
   Map<String,Object> rulemap = jobj.toMap();
   CatreLog.logI("CATSERVER","Validate rule: " + jobj.toString(2));
   CatreRule cr = cp.createRule(cu.getCatre().getDatabase(),rulemap);
   
   if (cr == null) {
      return BowerRouter.errorResponse(e,cs,400,"Bad rule definition");
    }
   
   JSONObject rslt = cp.errorCheckRule(cr);
   
  return BowerRouter.jsonOKResponse(cs,"VALIDATION",rslt);
}

private String handleEditRule(HttpExchange e,CatserveSessionImpl cs)
{
   return handleAddRule(e,cs);
}


private String handleSetRulePriority(HttpExchange e,CatserveSessionImpl cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();
   String rid = BowerRouter.getParameter(e, "RULEID");
   CatreRule cr = cp.findRule(rid);
   if (cr == null) {
      return BowerRouter.errorResponse(e,cs,400,"No such rule");
    }
   
   String pstr = BowerRouter.getParameter(e, "PRIORITY");
   if (pstr == null) {
      return BowerRouter.errorResponse(e,cs,400,"No priority given");
    }
   
   try {
      double p = Double.valueOf(pstr);
      if (p > 0) {
	 cr.setPriority(p);
	return BowerRouter.jsonOKResponse(cs);
       }
    }
   catch (NumberFormatException err) { }
   
   return BowerRouter.errorResponse(e,cs,400,"Bad priority value");
}



private String handleRemoveRule(HttpExchange e,CatserveSessionImpl cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();
   String rid = BowerRouter.getParameter(e, "RULEID");
   if (rid == null) {
      String ruletext = BowerRouter.getParameter(e, "RULE");
      JSONObject jobj = new JSONObject(ruletext);
      rid = jobj.getString("_id");
    }
   CatreRule cr = cp.findRule(rid);
   if (cr == null) {
      return BowerRouter.errorResponse(e,cs,400,"No such rule");
    }
   
   CatreLog.logD("Remove Rule " + cr.getName());
   
   cp.removeRule(cr);
   
  return BowerRouter.jsonOKResponse(cs);
}


/********************************************************************************/
/*										*/
/*	Threading methods							*/
/*										*/
/********************************************************************************/

private final class ServerExecutor implements Executor {
   
   @Override public void execute(Runnable r) {
      catre_control.submit(r);
    }
   
}



/********************************************************************************/
/*                                                                              */
/*      SessionStore for Catre                                                  */
/*                                                                              */
/********************************************************************************/

private static final class SessionStore implements BowerSessionStore<CatserveSessionImpl> {
   
   private CatreController catre_control;
   private CatreStore store_db;
   
   SessionStore(CatreController main) {
      catre_control = main;
      store_db = main.getDatabase();
    }
   
   @Override public String getSessionCookie()	{ return SESSION_COOKIE; }
   @Override public String getSessionKey()	{ return SESSION_PARAMETER; }
   @Override public String getStatusKey()	{ return "STATUS"; }
   @Override public String getErrorMessageKey() { return "MESSAGE"; }
   
   @Override public CatserveSessionImpl createNewSession() {
      return new CatserveSessionImpl(this);
    }
   
   @Override public void saveSession(CatserveSessionImpl bs) {
      bs.saveSession(catre_control);
    }
   
   @Override public CatserveSessionImpl loadSession(String sid) {
      CatserveSessionImpl bs = (CatserveSessionImpl) store_db.loadObject(sid);
      return bs;
    }
   
   @Override public void removeSession(CatserveSessionImpl csi) { 
      if (csi != null) {
         csi.removeSession(catre_control);
       }
    }
   
}	// end of inner class Session Store



/********************************************************************************/
/*                                                                              */
/*      Parameter access methods                                                */
/*                                                                              */
/********************************************************************************/

static @Tainted JSONObject getJson(HttpExchange exchange,String fld)
{
   String jsonstr = BowerRouter.getParameter(exchange,fld);
   if (jsonstr == null) return null;
   return new JSONObject(jsonstr);
}




/********************************************************************************/
/*										*/
/*	Table for storing sessions						*/
/*										*/
/********************************************************************************/

private  final class SessionTable implements CatreTable {

   @Override public String getTableName()    { return "CatreSessions"; }
   
   @Override public String getTablePrefix()	{ return SESSION_PREFIX; }
   
   @Override public boolean useFor(CatreSavable cs) {
      return cs instanceof CatreSession;
    }
   
   @Override public CatserveSessionImpl create(CatreStore store,Map<String,Object> data) {
      return new CatserveSessionImpl(session_store,store,data);
    }

}  // end of inner class SessionTable

}       // end of class CatserveBowerServer




/* end of CatserveBowerServer.java */

