/********************************************************************************/
/*										*/
/*		IQsignServer.java						*/
/*										*/
/*	Web server for iQsign server						*/
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Consumer;

import org.json.JSONArray;

import com.sun.net.httpserver.HttpExchange;

import edu.brown.cs.ivy.bower.BowerCORS;
import edu.brown.cs.ivy.bower.BowerRouter;
import edu.brown.cs.ivy.bower.BowerServer;
import edu.brown.cs.ivy.bower.BowerConstants.BowerSessionHandler;
import edu.brown.cs.ivy.bower.BowerConstants.BowerSessionStore;
import edu.brown.cs.ivy.file.IvyLog;


class IQsignServer implements IQsignConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/


private BowerServer<IQsignSession> http_server;
private IQsignMain	iqsign_main;
private IQsignAuth	iqsign_auth;
private IQsignDatabase	iqsign_database;
private SessionStore	session_store;
private Set<String>	users_updated;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

IQsignServer(IQsignMain main,String keystorepwd)
{
   session_store = new SessionStore(main);

   File f1 = main.getBaseDirectory();
   File f2 = new File(f1,"secret");
   File f3 = new File(f2,"catre.jks");

   iqsign_main = main;
   iqsign_auth = main.getAuthenticator();
   iqsign_database = main.getDatabaseManager();
   BowerRouter<IQsignSession> br = setupRouter();

   users_updated = new HashSet<>();

   http_server = new BowerServer<>(HTTPS_PORT,session_store);
   http_server.setRouter(br);

   if (keystorepwd != null) {
      http_server.setupHttps(f3,keystorepwd);
    }
}


/********************************************************************************/
/*										*/
/*	Start the servers							*/
/*										*/
/********************************************************************************/

void start()
{
   if (!http_server.start()) {
      IQsignMain.reportError("Can't start web service");
    }
   IvyLog.logD("IQSIGN","IQSIGN server set up on port " + HTTPS_PORT);
}



/********************************************************************************/
/*										*/
/*	Setup the router							*/
/*										*/
/********************************************************************************/

BowerRouter<IQsignSession> setupRouter()
{
   BowerRouter<IQsignSession> br = new BowerRouter<>(session_store);
   br.addRoute("ALL",BowerRouter::handleParameters);
   br.addRoute("ALL",br::handleSessions); 
   br.addRoute("ALL",BowerRouter::handleLogging);
   br.addRoute("ALL",new BowerCORS("*"));
   br.addRoute("ALL","/rest/ping",new PingAction()); 
   br.addRoute("ALL","/ping",new PingAction());

   br.addRoute("GET","/rest/signimage/:filename",new SignImageAction());
   br.addRoute("GET","/rest/svgimage/:topic/:name",new LocalSvgImageAction());
   br.addRoute("GET","/rest/userimage/:name",new LocalImageAction());
   
   br.addRoute("GET","/rest/login",iqsign_auth::handlePreLogin);
   br.addRoute("GET","/rest/register",iqsign_auth::handlePreLogin);
   br.addRoute("POST","/rest/login",new LoginAction());
   br.addRoute("POST","/rest/register",iqsign_auth::handleRegister);
   br.addRoute("ALL","/rest/logout",new LogoutAction());
   br.addRoute("ALL","/rest/authorize",new AuthorizeAction());
   br.addRoute("GET","/validate",iqsign_auth::handleValidationRequest);
   br.addRoute("GET","/rest/validate",iqsign_auth::handleValidationRequest);
   br.addRoute("ALL","/rest/forgotpassword",iqsign_auth::handleForgotPassword);
   br.addRoute("POST","/rest/newpassword",iqsign_auth::handleResetPassword);

   br.addRoute("USE",new Authenticator());

   br.addRoute("ALL","/rest/signs",new GetAllSignsAction());
   SetSignAction ssa = new SetSignAction();
   br.addRoute("PUT","/rest/sign/:signid/update",ssa);
   br.addRoute("POST","/rest/sign/setto",ssa);
   SignUpdateAction sua = new SignUpdateAction();
   br.addRoute("POST","/rest/sign/update",sua);
   br.addRoute("POST","/rest/sign/:signid/update",sua);
   br.addRoute("POST","/rest/loadsignimage",new LoadSignImageAction());
   br.addRoute("POST","/rest/savesignimage",new SaveSignImageAction());
   br.addRoute("POST","/rest/removesignimage",new RemvoeSavedSignImageAction());

   br.addRoute("POST","/rest/sign/preview",new PreviewAction());
   br.addRoute("POST","/rest/createcode",new CreateCodeAction());
   br.addRoute("ALL","/rest/namedsigns",new GetSavedSignsAction());
   br.addRoute("POST","/rest/addsign",new AddSignAction());
   br.addRoute("POST","/rest/removeuser",new RemoveUserAction());
   br.addRoute("POST","/rest/removesign",new RemoveSignAction());
   br.addRoute("POST","/rest/changepassword",iqsign_auth::handleChangePassword);
   br.addRoute("POST","/rest/defineimage",new DefineUserImageAction());
   br.addRoute("ALL","/rest/findimages",new FindImagesAction());

   br.addRoute("ALL","/rest/about",new AboutAction());
   br.addRoute("ALL","/rest/instructions",new InstructionsAction());
  
   br.addRoute("ALL",new Handle404Action());
   br.addErrorHandler(new HandleErrorAction());

   return br;
}




/********************************************************************************/
/*										*/
/*	Session management							*/
/*										*/
/********************************************************************************/

private final class SessionStore implements BowerSessionStore<IQsignSession> {

   private IQsignDatabase store_db;

   SessionStore(IQsignMain main) {
      store_db = main.getDatabaseManager();
    }

   @Override public String getSessionCookie()	{ return SESSION_COOKIE; }
   @Override public String getSessionKey()	{ return SESSION_PARAMETER; }
   @Override public String getStatusKey()	{ return "status"; }
   @Override public String getErrorMessageKey() { return "message"; }

   @Override public IQsignSession createNewSession() {
      return new IQsignSession(this);
    }

   @Override public void saveSession(IQsignSession bs) {
      store_db.startSession(bs.getSessionId(),bs.getCode());
    }

   @Override public IQsignSession loadSession(String sid) {
      IQsignSession bs = store_db.checkSession(this,sid);
      return bs;
    }

   @Override public void removeSession(IQsignSession bs) {
      if (bs == null) return;
      store_db.removeSession(bs.getSessionId());
    }
   
   @Override public void updateSession(IQsignSession bs) {
      if (bs == null) return;
      store_db.updateSession(bs.getSessionId(),bs.getUserId());
    }

}	// end of inner class Session Store



/********************************************************************************/
/*										*/
/*	Basic actions								*/
/*										*/
/********************************************************************************/

private final class PingAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange e,IQsignSession session) {
      List<String> users = BowerRouter.getParameterList(e,"users");
   
      JSONArray update = new JSONArray();
      if (users != null) {
         for (String u : users) {
            IvyLog.logD("IQSIGN","Check on user " + u);
            if (users_updated.remove(u)) update.put(u);
          }
       }
   
      return BowerRouter.jsonOKResponse(session,"update",update);
    }

}	// end of inner class PingAction




/********************************************************************************/
/*										*/
/*	Login and register actions						*/
/*										*/
/********************************************************************************/

private final class LoginAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange e,IQsignSession session) {
      String rslt = iqsign_auth.handleLogin(e,session);
      Number userid = session.getUserId();
      if (userid != null) {
         session_store.updateSession(session);
       }
   
      return rslt;
    }

}	// end of inner class LoginAction






private final class LogoutAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange e,IQsignSession session) {
      session.setUser(null);
      session_store.updateSession(session);
   
      return BowerRouter.jsonOKResponse(session);
    }

}	// end of inner class LogoutAction



private final class AuthorizeAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      session.setUser(null);
      String token = BowerRouter.getParameter(he,"token");
      IQsignLoginCode tokinfo = iqsign_database.checkAccessToken(token,null,null);  
      if (tokinfo == null) {
         return BowerRouter.errorResponse(he,session,402,"Bad access token");
       }
      Number uid = tokinfo.getUserId();
      session.setUserId(uid);
      session_store.updateSession(session);
   
      return BowerRouter.jsonOKResponse(session,
            "userid",tokinfo.getUserId(),
            "signid",tokinfo.getSignId());
    }

}	// end of inner class AuthorizeAction



/********************************************************************************/
/*										*/
/*	Authentication								*/
/*										*/
/********************************************************************************/

private final class Authenticator implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      if (session == null) {
         return BowerRouter.errorResponse(he,session,402,"Bad session");
       }
     
      String tok = BowerRouter.getAccessToken(he);
      if (tok != null && !tok.equals(session.getCode())) {
         return BowerRouter.errorResponse(he,session,402,"Bad authorization code");
       }
      
      Number uid = session.getUserId();
      if (uid == null) {
         return BowerRouter.errorResponse(he,session,402,"Unauthorized");
       }
      IQsignUser user = iqsign_database.findUser(uid);
      session.setUser(user);
      if (user != null) {
         user.clearPasswords();
       }
      else {
         uid = null;
       }
      session_store.updateSession(session);
      
      if (user == null) {
         return BowerRouter.errorResponse(he,session,402,"Unauthorized");
       }
      
      IvyLog.logD("IQSIGN","REST DONE AUTHENTICATION");
      
      return null;
    }

}	// end of inner class Authenticator



/********************************************************************************/
/*										*/
/*	Sign methods								*/
/*										*/
/********************************************************************************/

private final class GetAllSignsAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      Number uid = session.getUserId();
      if (uid == null) {
         BowerRouter.errorResponse(he,session,402,"Bad user id");
       }
      List<IQsignSign> signs = iqsign_database.getAllSignsForUser(uid);
      JSONArray jarr = new JSONArray();
      for (IQsignSign sign : signs) {
	 jarr.put(sign.toJson());
       }
      return BowerRouter.jsonOKResponse(session,"data",jarr);
    }

}	// end of inner class GetAllSignsAction




private final class SetSignAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      Number sid = getIdParameter(he,"signid");
      if (sid == null) {
         return BowerRouter.errorResponse(he,session,400,"Bad sign id");
       }
      IQsignSign sign = iqsign_database.findSignById(sid);
      if (sign == null) return BowerRouter.errorResponse(he,session,400,"Bad sign id");
      
      String setval = BowerRouter.getParameter(he,"value");
      if (setval == null || setval.isEmpty()) {
         IvyLog.logE("IQSIGN","No value given for set sign");
         return BowerRouter.errorResponse(he,session,400,"No value given");
       }
   
      String cnts = "=" + setval + "\n";
      
      String sets = BowerRouter.getParameter(he,"sets");
      if (sets != null && !sets.isEmpty()) {
         for (StringTokenizer tok = new StringTokenizer(sets); tok.hasMoreTokens(); ) {
            String s = tok.nextToken();
            cnts += "= " + s + "\n";
          }
       }
      String other = BowerRouter.getParameter(he,"other");
      if (other != null && !other.isEmpty()) {
         cnts += "# " + other + "\n";
       }
   
      FinishedUpdate fu = new FinishedUpdate(he,session,sign.toJson());
      sign.changeSign(cnts,fu);
   
      return BowerRouter.deferredResponse();
    }

}	// end of inner class SetSignAction



private final class SignUpdateAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      IQsignSign sign = iqsign_database.findSignById(getIdParameter(he,"signid"));
      if (sign == null) {
         return BowerRouter.errorResponse(he,session,402,"Invalid sign");
       }
      Number suser = getIdParameter(he,"signuser");
      if (suser != null && !suser.equals(session.getUserId())) {
         return BowerRouter.errorResponse(he,session,402,"Invalid user");
       }
      String cnts = BowerRouter.getParameter(he,"signdata");
      cnts = sign.setContents(cnts);
      sign.updateProperties(BowerRouter.getParameter(he,"signname"),
            BowerRouter.getParameter(he,"signdim"),
            BowerRouter.getIntParameter(he,"signwidth"),
            BowerRouter.getIntParameter(he,"signheight"));
   
      FinishedUpdate fu = new FinishedUpdate(he,session,sign.toJson());
      sign.changeSign(cnts,fu);
   
      return BowerRouter.deferredResponse();
    }

}	// end of inner class SignUpdateAction



private final class LoadSignImageAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      String name = BowerRouter.getParameter(he,"signname");
      String nameid = BowerRouter.getParameter(he,"nameid");
      if (nameid != null && nameid.equals("*Current*")) name = "*Current*";
      if (nameid != null && nameid.isEmpty()) nameid = null;
      if (name != null && name.isEmpty()) name = null;
      if (name == null && nameid == null) {
         return BowerRouter.errorResponse(he,session,400,"No name given");
       }
   
      String cnts = null;
      if (name != null && name.equals("*Current*")) {
         IQsignSign sign = iqsign_database.findSignById(getIdParameter(he,"signid"));
         if (sign == null) {
            return BowerRouter.errorResponse(he,session,400,"Invalid sign");
          }
         if (!sign.getUserId().equals(session.getUserId())) {
            return BowerRouter.errorResponse(he,session,400,"Invalid user");
          }
         if (!sign.getNameKey().equals(BowerRouter.getParameter(he,"signnamekey"))) {
            return BowerRouter.errorResponse(he,session,400,"Invalid name key");
          }
         cnts = sign.getContents();
       }
      else {
         Number nid = getIdParameter(he,"nameid");
         IQsignDefinedImage di = iqsign_database.getDefineData(nid,name,session.getUserId());
         if (di == null) {
            return BowerRouter.errorResponse(he,session,400,"Bad define id");
          }
         Number duid = di.getUserId();
         if (duid != null && !duid.equals(session.getUserId())) {
            return BowerRouter.errorResponse(he,session,400,"Bad user define id");
          }
         cnts = di.getContents();
         name = di.getName();
       }
   
      return BowerRouter.jsonOKResponse(session,"name",name,"contents",cnts);
    }

}	// end of inner class LoadSignImageAction



private final class SaveSignImageAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      String uname = session.getUser().getUserName();
      users_updated.add(uname);
      IQsignSign sign = iqsign_database.findSignById(getIdParameter(he,"signid"));
      if (sign == null) {
         return BowerRouter.errorResponse(he,session,400,"Invalid sign");
       }
      String name = BowerRouter.getParameter(he,"name");
      if (name == null) {
         return BowerRouter.errorResponse(he,session,400,"No name given");
       }
      Number uid = session.getUserId();
      if (session.getUser().isAdmin()) uid = null;
      if (!sign.getUserId().equals(getIdParameter(he,"signuser"))) {
         return BowerRouter.errorResponse(he,session,400,"Invalid user");
       }
      if (!sign.getNameKey().equals(BowerRouter.getParameter(he,"signnamekey"))) {
         return BowerRouter.errorResponse(he,session,400,"Invalid name key");
       }
      String cnts = BowerRouter.getParameter(he,"signbody");
      if (cnts == null) cnts = sign.getContents();
      iqsign_database.addDefineName(uid,name,cnts,true);
        	
      return BowerRouter.jsonOKResponse(session);	
    }

}	// end of inner class SaveSignImageAction



private final class RemvoeSavedSignImageAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      String uname = session.getUser().getUserName();
      users_updated.add(uname);
      iqsign_database.removeDefineData(BowerRouter.getParameter(he,"name"),
            session.getUserId());
   
      return BowerRouter.jsonOKResponse(session);	
    }

}	// end of inner class RemoveSavedSignImageAction



private final class PreviewAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      String cnts = BowerRouter.getParameter(he,"signdata");
      if (cnts == null) {
         return BowerRouter.errorResponse(he,session,400,"No sign given");
       }
      cnts = cnts.trim();
      Number uid = getIdParameter(he,"signuser");
      Number sid = getIdParameter(he,"signid");
      if (!uid.equals(session.getUserId())) {
         return BowerRouter.errorResponse(he,session,400,"Invalid user");
       }
      IQsignSign sign = iqsign_database.findSign(sid,uid,
            BowerRouter.getParameter(he,"signkey"));
      if (sign == null) {
         return BowerRouter.errorResponse(he,session,400,"Invalid sign");
       }
   
      IvyLog.logD("IQSIGN","PREVIEW DATA " + cnts);
   
      cnts = sign.setContents(cnts);
      sign.updateSign(new FinishedUpdate(he,session),false,true);
   
      return BowerRouter.deferredResponse();	
    }


}	// end of inner class PreviewAction




private final class CreateCodeAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      Number sid = getIdParameter(he,"signid");
      Number uid = getIdParameter(he,"signuser");
      String nkey = BowerRouter.getParameter(he,"signkey");
      if (!uid.equals(session.getUserId())) {
         return BowerRouter.errorResponse(he,session,400,"Invalid user");
       }
      IQsignSign sign = iqsign_database.findSign(sid,uid,nkey);
      if (sign == null) {
         return BowerRouter.errorResponse(he,session,400,"Invalid sign");
       }
      String code = BowerRouter.getParameter(he,"code");
      if (code == null || code.length() < 12) {
         return BowerRouter.errorResponse(he,session,400,"Invalid user");
       }
      iqsign_database.addLoginCode(uid,sid,code);
        
      return BowerRouter.jsonOKResponse(session,"code",code);
    }

}	// end of inner class CreateCodeAction




private final class GetSavedSignsAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      List<IQsignDefinedImage> defs = iqsign_database.getSavedSigns(session.getUserId());
      if (defs == null) {
	 return BowerRouter.errorResponse(he,session,400,"No images");
       }
      JSONArray arr = new JSONArray();
      for (IQsignDefinedImage di : defs) {
	 arr.put(di.toJson());
       }
      return BowerRouter.jsonOKResponse(session,"data",arr);
    }

}	// end of inner class GetSavedSignsAction




private final class AddSignAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      String name = BowerRouter.getParameter(he,"name");
      String email = session.getUser().getEmail();
      String signname = BowerRouter.getParameter(he,"signname");
      String cnts = null;
      if (signname != null && !signname.isEmpty()) {
         cnts = "=" + signname;
       }
      FinishAddSign fas = new FinishAddSign(he,session);
      IQsignSign sign = IQsignSign.setupSign(iqsign_main,name,email,cnts,fas);
      if (sign == null) {
         return BowerRouter.errorResponse(he,session,400,"Can't create sign");
       }
      
      IvyLog.logD("IQSIGN","Add sign " + sign.getId() + " " + sign.getNameKey());
      
      IQsignUser user = session.getUser();
      users_updated.add(user.getUserName());
      
      return BowerRouter.deferredResponse();
    }

}	// end of inner class AddSignAction


private final class FinishAddSign implements Consumer<Boolean> {

   private HttpExchange http_exchange;
   private IQsignSession iqsign_session;


   FinishAddSign(HttpExchange he,IQsignSession session) {
      http_exchange = he;
      iqsign_session = session;
    }

   @Override public void accept(Boolean fg) {
      Number uid = iqsign_session.getUserId();
      List<IQsignSign> signs = iqsign_database.getAllSignsForUser(uid);
      JSONArray jarr = new JSONArray();
      for (IQsignSign sign : signs) {
	 jarr.put(sign.toJson());
       }
      BowerRouter.finishResponse(http_exchange,iqsign_session,"OK","data",jarr);
    }
}



private final class RemoveUserAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      Number uid = session.getUserId();
      IQsignUser user = session.getUser();
      if (user.isAdmin()) {
	 String what = BowerRouter.getParameter(he,"username");
	 IQsignUser user2 = iqsign_database.findUser(what);
	 if (user2 != null && !user2.getUserId().equals(uid)) {
	    uid = user2.getUserId();
	  }
	 else {
	    session.setUser(null);
	  }
       }
      else {
	 session.setUser(null);
       }
      
      iqsign_database.removeUser(uid);
      
      users_updated.add(user.getUserName());

      return BowerRouter.jsonOKResponse(session);
    }

}	// end of inner class RemoveUser



private final class RemoveSignAction implements BowerSessionHandler<IQsignSession> {
   
   @Override public String handle(HttpExchange he,IQsignSession session) {
      Number uid = session.getUserId();  
      Number sid = getIdParameter(he,"signid");
      
      Number newsid = iqsign_database.removeSign(uid,sid); 
      
      if (newsid == null) {
         return BowerRouter.errorResponse(he,session,400,
               "Bad sign specified");
       }
      
      IQsignUser user = session.getUser();
      users_updated.add(user.getUserName());
      
      return BowerRouter.jsonOKResponse(session);
    }

}	// end of inner class RemoveSign




private final class SignImageAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      String filename = BowerRouter.getParameter(he,"filename");
      int idx = filename.indexOf("?");
      if (idx > 0) filename = filename.substring(0,idx);
      File f = iqsign_main.getWebDirectory();
      File f2 = new File(f,filename);
      if (!f2.exists()) {
         return BowerRouter.errorResponse(he,session,400,
               "sign " + f2 + " doesn't exist");
       }
      return BowerRouter.sendFileResponse(he,f2);
    }

}       // end of inner class SignImageAction



private final class LocalSvgImageAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      String topic = BowerRouter.getParameter(he,"topic");
      String name = BowerRouter.getParameter(he,"name");
      int idx = name.indexOf("?");
      if (idx > 0) {
         name = name.substring(0,idx);
       }
      File f1 = iqsign_main.getImageManager().getSvgImage(topic,name);
      return BowerRouter.sendFileResponse(he,f1);
    }

}       // end of inner class LocalSvgImageAction



private final class LocalImageAction implements BowerSessionHandler<IQsignSession> {
   
   @Override public String handle(HttpExchange he,IQsignSession session) {
      String name = BowerRouter.getParameter(he,"name");
      File f1 = iqsign_main.getImageManager().getLocalImage(name);
      return BowerRouter.sendFileResponse(he,f1);
    }

}       // end of inner class LocalImageAction




private final class AboutAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      IQsignDatabase db = iqsign_main.getDatabaseManager();
      Number signid = getIdParameter(he,"signid");
      IQsignSign sign = null;
      if (signid != null && signid.intValue() != 0) {
         sign = db.findSignById(signid);
       }
      String rslt = iqsign_main.loadResource("iqsignabout.html",sign);
      if (rslt == null) rslt = "About Page";
      return BowerRouter.jsonOKResponse(session,"html",rslt);
    }
   
}       // end of inner class AboutAction



private final class InstructionsAction implements BowerSessionHandler<IQsignSession> {

   @Override public String handle(HttpExchange he,IQsignSession session) {
      IQsignDatabase db = iqsign_main.getDatabaseManager();
      Number signid = getIdParameter(he,"signid");
      IQsignSign sign = null;
      if (signid != null && signid.intValue() != 0) {
         sign = db.findSignById(signid);
       }
      String rslt = iqsign_main.loadResource("iqsigninstructions.html",sign);
      if (rslt == null) rslt = "Instruction Page";
      return BowerRouter.jsonOKResponse(session,"html",rslt);
    }

}       // end of inner class AboutAction


private final class DefineUserImageAction implements BowerSessionHandler<IQsignSession> {
   
   @Override public String handle(HttpExchange he,IQsignSession session) {
      Number uid = session.getUserId();
      String typ = BowerRouter.getParameter(he,"imagefile");
      if (typ == null || typ.isEmpty()) typ = BowerRouter.getParameter(he,"imageurl");
      int idx = typ.lastIndexOf(".");
      typ = typ.substring(idx+1);
      if (typ.isEmpty() || idx < 0) {
         return BowerRouter.errorResponse(he,session,400,"Can't determine type from name");
       }
      String url = BowerRouter.getParameter(he,"imageurl");
      if (url != null && url.isEmpty()) url = null;
      String data = BowerRouter.getParameter(he,"imagevalue");
      if (data != null && data.isEmpty()) data = null;
      if (url == null && data == null) {
         return BowerRouter.errorResponse(he,session,400,"No image present");
       }
      String name = BowerRouter.getParameter(he,"imagename");
      if (name == null || name.isEmpty()) {
         return BowerRouter.errorResponse(he,session,400,"No name for image");
       }
      String desc = BowerRouter.getParameter(he,"imagedescription");
      if (desc != null && desc.isEmpty()) {
         return BowerRouter.errorResponse(he,session,400,"No description for image");
       }
      boolean border = BowerRouter.getBooleanParameter(he,"imageborder",false);
      
      String err = iqsign_main.getImageManager().saveUserImage(uid,name,typ,url,
            data,desc,border);
      if (err != null) { 
         return BowerRouter.errorResponse(he,session,400,err);
       }
       
      return BowerRouter.jsonOKResponse(session);
    }
   
}       // end of inner class DefineUserImageAction


private final class FindImagesAction implements BowerSessionHandler<IQsignSession> {
   
   @Override public String handle(HttpExchange he,IQsignSession session) {
      Number uid = session.getUserId();
      boolean border = BowerRouter.getBooleanParameter(he,"border",false);
      boolean svg = BowerRouter.getBooleanParameter(he,"svg",false);
      
      JSONArray rslt = iqsign_main.getImageManager().getImageSet(uid,border,svg);
      
      return BowerRouter.jsonOKResponse(session,"data",rslt);
    }
}



private final class Handle404Action implements BowerSessionHandler<IQsignSession> {
   
   @Override public String handle(HttpExchange he,IQsignSession session) {
      return BowerRouter.errorResponse(he,session,404,"Invalid URL");
    }
   
}       // end of inner class Handle404Action


private final class HandleErrorAction implements BowerSessionHandler<IQsignSession> {
   
   @Override public String handle(HttpExchange he,IQsignSession session) {
      Object t = he.getAttribute(BowerRouter.BOWER_EXCEPTION);
      String msg = "Internal error";
      if (t != null) {
         msg += ": " + t;
       }
      
      return BowerRouter.errorResponse(he,session,500,msg);
    }
   
}       // end of inner class HandleErrorAction



/********************************************************************************/
/*										*/
/*	Handle delayed responses						*/
/*										*/
/********************************************************************************/

static final class FinishedUpdate implements Consumer<Boolean> {

   private HttpExchange http_exchange;
   private IQsignSession iqsign_session;
   private Object [] response_data;

   FinishedUpdate(HttpExchange he,IQsignSession session,Object... data) {
      http_exchange = he;
      iqsign_session = session;
      response_data = data;
    }

   @Override public void accept(Boolean fg) {
      String sts = "OK";
      if (!fg) sts = "ERROR";
      BowerRouter.finishResponse(http_exchange,iqsign_session,sts,response_data);
    }

}	// end of inner class FinishedUpdate



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

Number getIdParameter(HttpExchange he,String name)
{
   int v = BowerRouter.getIntParameter(he,name);
   if (v == 0) return null;
   return v;
}


}	// end of class IQsignServer




/* end of IQsignServer.java */

