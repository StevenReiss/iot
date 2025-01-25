/********************************************************************************/
/*                                                                              */
/*              IQsignServer.java                                               */
/*                                                                              */
/*      Web server for iQsign server                                            */
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.json.JSONArray;

import com.sun.net.httpserver.HttpExchange;

import edu.brown.cs.ivy.bower.BowerRouter;
import edu.brown.cs.ivy.bower.BowerServer;
import edu.brown.cs.ivy.bower.BowerConstants.BowerSessionHandler;
import edu.brown.cs.ivy.bower.BowerConstants.BowerSessionStore;
import edu.brown.cs.ivy.file.IvyLog;


class IQsignServer implements IQsignConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/


private BowerServer<IQsignSession> http_server;
private IQsignMain      iqsign_main;
private IQsignAuth      iqsign_auth;
private IQsignDatabase  iqsign_database;
private SessionStore    session_store;
private Set<String>     users_updated;
private Set<String>     users_active;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
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
   users_active = new HashSet<>();
   
   http_server = new BowerServer<>(HTTPS_PORT,session_store); 
   http_server.setRouter(br); 
   
   if (keystorepwd != null) {
      http_server.setupHttps(f3,keystorepwd);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Start the servers                                                       */
/*                                                                              */
/********************************************************************************/

void start()
{
   if (!http_server.start()) {
      IQsignMain.reportError("Can't start web service");
    }
   IvyLog.logD("IQSIGN server set up on port " + HTTPS_PORT);
}



/********************************************************************************/
/*                                                                              */
/*      Setup the router                                                        */
/*                                                                              */
/********************************************************************************/

BowerRouter<IQsignSession> setupRouter()
{
   BowerRouter<IQsignSession> br = new BowerRouter<>(session_store);
   br.addRoute("ALL",BowerRouter::handleParameters); 
   br.addRoute("ALL",br::handleSessions);
   br.addRoute("ALL",BowerRouter::handleLogging);
   br.addRoute("ALL","/rest/ping",new PingAction()); 
   br.addRoute("ALL","/ping",new PingAction());
   
   br.addRoute("GET","/rest/login",iqsign_auth::handlePreLogin); 
   br.addRoute("GET","/rest/register",iqsign_auth::handlePreLogin);
   br.addRoute("POST","/rest/login",new LoginAction());
   br.addRoute("POST","/rest/register",new RegisterAction());
   br.addRoute("ALL","/rest/logout",new LogoutAction());
   br.addRoute("ALL","/rest/authorize",new AuthorizeAction());
   
// br.addRoute("POST","/rest/forgotpassword",null);
// br.addRoute("POST","/rest/newpassword",null);
// br.addRoute("GET","/validate",null);   
   
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
// 
   br.addRoute("POST","/rest/sign/preview",new PreviewAction());
   br.addRoute("POST","/rest/createcode",new CreateCodeAction());
   br.addRoute("ALL","/rest/namedsigns",new GetSavedSignsAction());
   br.addRoute("POST","/rest/addsign",new AddSignAction());
   br.addRoute("POST","/rest/removeuser",new RemoveUserAction());
   
   br.addRoute("GET","/rest/signimage/:filename",new LocalImageAction());   
   
   
// br.addRoute("ALL","/rest/svgimages",null);
// br.addRoute("ALL","/rest/savedimages",null);

// br.addRoute("ALL","*",null);
// br.addRoute("USE",new ErrorHandler());
   
// br.addRoute("GET","/rest/svg/:svgttopic/:svgname",null);
// br.addRoute("GET","/rest/image/:imagename",null); 
// br.addRoute("ALL","/rest/about",null);
// br.addRoute("GET","/rest/instructions",null);
   
   return br;
}




/********************************************************************************/
/*                                                                              */
/*      Session management                                                      */
/*                                                                              */
/********************************************************************************/

private final class SessionStore implements BowerSessionStore<IQsignSession> {
   
   private IQsignDatabase store_db;
    
   SessionStore(IQsignMain main) {
      store_db = main.getDatabaseManager();
    }
   
   @Override public String getSessionCookie()   { return SESSION_COOKIE; }
   @Override public String getSessionKey()      { return SESSION_PARAMETER; }
   @Override public String getStatusKey()       { return "status"; }
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
  
   @Override public void removeSession(String sid) {
      store_db.removeSession(sid);
    }
   
   void updateSession(String sid,String user) {
      store_db.updateSession(sid,user);
    }
   
}       // end of inner class Session Store



/********************************************************************************/
/*                                                                              */
/*      Basic actions                                                           */
/*                                                                              */
/********************************************************************************/

private final class PingAction implements BowerSessionHandler<IQsignSession> { 
   
   @Override public String apply(HttpExchange e,IQsignSession session) {  
      List<String> users = BowerRouter.getParameterList(e,"users"); 
      
      JSONArray auth = new JSONArray();
      JSONArray update = new JSONArray();
      if (users != null) {
         for (String u : users) {
            IvyLog.logD("Check on user " + u);
            if (users_active.contains(u)) auth.put(u);
            if (users_updated.remove(u)) update.put(u);
          }
       }
      
      return BowerRouter.jsonOKResponse(session,"authorize",auth,"update",update); 
    }
   
}       // end of inner class PingAction




/********************************************************************************/
/*                                                                              */
/*      Login and register actions                                              */
/*                                                                              */
/********************************************************************************/

private final class LoginAction implements BowerSessionHandler<IQsignSession> {
   
   @Override public String apply(HttpExchange e,IQsignSession session) {
      String rslt = iqsign_auth.handleLogin(e,session); 
      String userid = session.getUserId();
      if (userid != null) {
         IQsignUser user = session.getUser();
         String name = user.getUserName();
         users_active.add(name);
         session_store.updateSession(session.getSessionId(),userid);
       }
      
      return rslt;
    }
   
}       // end of inner class LoginAction



private final class RegisterAction implements BowerSessionHandler<IQsignSession> {

   @Override public String apply(HttpExchange e,IQsignSession session) {
      String rslt = iqsign_auth.handleRegister(e,session); 
      return rslt;
    }

}       // end of inner class LoginAction


private final class LogoutAction implements BowerSessionHandler<IQsignSession> {

   @Override public String apply(HttpExchange e,IQsignSession session) {
      IQsignUser user = session.getUser();
      if (user != null) {
         String name = user.getUserName();
         users_active.remove(name);
       }
      session.setUser(null);
      session_store.updateSession(session.getSessionId(),null);
      
      return BowerRouter.jsonOKResponse(session);
    }
   
}       // end of inner class LogoutAction



private final class AuthorizeAction implements BowerSessionHandler<IQsignSession> {
   
   @Override public String apply(HttpExchange he,IQsignSession session) {
      session.setUser(null);
      String token = BowerRouter.getParameter(he,"token");
      IQsignLoginCode tokinfo = iqsign_database.checkAccessToken(token);
      if (tokinfo == null) {
         return BowerRouter.errorResponse(he,session,402,"Bad access token");
       }
      String uid = tokinfo.getUserId();
      session.setUserId(uid);
      session_store.updateSession(session.getSessionId(),uid);
      
      return BowerRouter.jsonOKResponse(session,
            "userid",tokinfo.getUserId(),
            "signid",tokinfo.getSignId());
    }
   
}       // end of inner class AuthorizeAction



/********************************************************************************/
/*                                                                              */
/*      Authentication                                                          */
/*                                                                              */
/********************************************************************************/

private final class Authenticator implements BowerSessionHandler<IQsignSession> {
   
   @Override public String apply(HttpExchange he,IQsignSession session) {
      String tok = BowerRouter.getAccessToken(he); 
      if (tok != null && !tok.equals(session.getCode())) {
         return BowerRouter.errorResponse(he,session,402,"Bad authorization code");
       }
      
      String uid = session.getUserId();
      if (uid == null) {
         return BowerRouter.errorResponse(he,session,402,"Unauthorized");
       }
      session_store.updateSession(session.getSessionId(),uid);
      IQsignUser user = iqsign_database.findUser(uid);
      if (user == null) {
         session_store.updateSession(session.getSessionId(),null);      
       }
      else {
         user.clearPasswords();
         session.setUser(user);
         users_active.add(user.getUserName());
       }
      
      IvyLog.logD("REST DONE AUTHENTICATION");
      
      return null;
    }
   
}       // end of inner class Authenticator



/********************************************************************************/
/*                                                                              */
/*      Sign methods                                                            */
/*                                                                              */
/********************************************************************************/

private final class GetAllSignsAction implements BowerSessionHandler<IQsignSession> {
   
   @Override public String apply(HttpExchange he,IQsignSession session) {
      String uid = session.getUserId();
      List<IQsignSign> signs = iqsign_database.getAllSignsForUser(uid);
      JSONArray jarr = new JSONArray();
      for (IQsignSign sign : signs) {
         jarr.put(sign.toJson()); 
       }
      return BowerRouter.jsonOKResponse(session,"data",jarr);
    }

}       // end of inner class GetAllSignsAction
      



private final class SetSignAction implements BowerSessionHandler<IQsignSession> {
   
   @Override public String apply(HttpExchange he,IQsignSession session) {
      String sid = BowerRouter.getParameter(he,"signid");
      IQsignSign sign = iqsign_database.findSignById(sid); 
      if (sign == null) return BowerRouter.errorResponse(he,session,400,"Bad sign id");
      
      String cnts = "=" + BowerRouter.getParameter(he,"value") + "\n";
      List<String> sets = BowerRouter.getParameterList(he,"sets");
      if (sets != null) {
         for (String s : sets) {
            cnts += "=" + s + "\n";
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

}       // end of inner class SetSignAction



private final class SignUpdateAction implements BowerSessionHandler<IQsignSession> {

   @Override public String apply(HttpExchange he,IQsignSession session) {
      IQsignSign sign = iqsign_database.findSignById(BowerRouter.getParameter(he,"signid"));
      if (sign == null) {
         return BowerRouter.errorResponse(he,session,402,"Invalid sign");
       }
      String suser = BowerRouter.getParameter(he,"signuser");
      if (suser != null && suser != session.getUserId()) {
         return BowerRouter.errorResponse(he,session,402,"Invalid user");
       }
      String cnts = BowerRouter.getParameter(he,"signdata");
      cnts = sign.setContents(cnts); 
      sign.updateProperties(BowerRouter.getParameter(he,"signname"),
            BowerRouter.getParameter(he,"signdim"),
            BowerRouter.getIntParameter(he,"signwidth"),
            BowerRouter.getIntParameter(he,"signheight"));
      iqsign_database.updateSignProperties(sign);
      
      FinishedUpdate fu = new FinishedUpdate(he,session,sign.toJson());
      sign.changeSign(cnts,fu); 
      
      return BowerRouter.deferredResponse();
    } 
   
}       // end of inner class SignUpdateAction



private final class LoadSignImageAction implements BowerSessionHandler<IQsignSession> {

   @Override public String apply(HttpExchange he,IQsignSession session) {
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
         IQsignSign sign = iqsign_database.findSignById(BowerRouter.getParameter(he,"signid"));
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
         IQsignDefinedImage di = iqsign_database.getDefineData(nameid,name,session.getUserId());
         if (di == null) {
            return BowerRouter.errorResponse(he,session,400,"Bad define id");
          }
         String duid = di.getUserId();
         if (duid != null && !duid.equals(session.getUserId())) {
            return BowerRouter.errorResponse(he,session,400,"Bad user define id");
          }
         cnts = di.getContents();
         name = di.getName();
       }
      
      return BowerRouter.jsonOKResponse(session,"name",name,"contents",cnts);
    }
   
}       // end of inner class LoadSignImageAction



private final class SaveSignImageAction implements BowerSessionHandler<IQsignSession> {
   
   @Override public String apply(HttpExchange he,IQsignSession session) {
      String uname = session.getUser().getUserName();
      users_updated.add(uname);
      IQsignSign sign = iqsign_database.findSignById(BowerRouter.getParameter(he,"signid"));
      if (sign == null) {
         return BowerRouter.errorResponse(he,session,400,"Invalid sign");
       }
      String name = BowerRouter.getParameter(he,"name");
      if (name == null) {
         return BowerRouter.errorResponse(he,session,400,"No name given");
       }
      String uid = session.getUserId();
      if (session.getUser().isAdmin()) uid = null;
      if (!sign.getUserId().equals(BowerRouter.getParameter(he,"signuser"))) {
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

}       // end of inner class SaveSignImageAction



private final class RemvoeSavedSignImageAction implements BowerSessionHandler<IQsignSession> {

   @Override public String apply(HttpExchange he,IQsignSession session) {
      String uname = session.getUser().getUserName();
      users_updated.add(uname);
      iqsign_database.removeDefineData(BowerRouter.getParameter(he,"name"),
            session.getUserId());
   
      return BowerRouter.jsonOKResponse(session);            
    }

}       // end of inner class RemoveSavedSignImageAction



private final class PreviewAction implements BowerSessionHandler<IQsignSession> {
   
   @Override public String apply(HttpExchange he,IQsignSession session) {
      String cnts = BowerRouter.getParameter(he,"signdata");
      cnts = cnts.trim();
      String uid = BowerRouter.getParameter(he,"signuser");
      String sid = BowerRouter.getParameter(he,"signid");
      if (!uid.equals(session.getUserId())) {
         return BowerRouter.errorResponse(he,session,400,"Invalid user");
       }
      IQsignSign sign = iqsign_database.findSign(sid,uid,
            BowerRouter.getParameter(he,"signkey")); 
      if (sign == null) {
         return BowerRouter.errorResponse(he,session,400,"Invalid sign");
       }
      
      IvyLog.logD("PREVIEW DATA " + cnts);
      
      cnts = sign.setContents(cnts);
      sign.updateSign(new FinishedUpdate(he,session),false,true);
      
      return BowerRouter.deferredResponse();            
    }
   
   
}       // end of inner class PreviewAction




private final class CreateCodeAction implements BowerSessionHandler<IQsignSession> {
   
   @Override public String apply(HttpExchange he,IQsignSession session) {
      String sid = BowerRouter.getParameter(he,"signid");
      String uid = BowerRouter.getParameter(he,"signuser");
      String nkey = BowerRouter.getParameter(he,"signkey");
      if (!uid.equals(session.getUserId())) {
         return BowerRouter.errorResponse(he,session,400,"Invalid user");
       }
      IQsignSign sign = iqsign_database.findSign(sid,uid,nkey);
      if (sign == null) {
         return BowerRouter.errorResponse(he,session,400,"Invalid sign");
       }
      String code = iqsign_database.addLoginCode(uid,sid);
         
      return BowerRouter.jsonOKResponse(session,"code",code);            
   
    }
   
}       // end of inner class CreateCodeAction




private final class GetSavedSignsAction implements BowerSessionHandler<IQsignSession> {

   @Override public String apply(HttpExchange he,IQsignSession session) {
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

}       // end of inner class GetSavedSignsAction




private final class AddSignAction implements BowerSessionHandler<IQsignSession> {

   @Override public String apply(HttpExchange he,IQsignSession session) {
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
      return BowerRouter.deferredResponse();
    }
   
}       // end of inner class AddSignAction


private final class FinishAddSign implements Consumer<Boolean> {
  
   private HttpExchange http_exchange;
   private IQsignSession iqsign_session;
   
   
   FinishAddSign(HttpExchange he,IQsignSession session) {
      http_exchange = he;
      iqsign_session = session;
    }
   
   @Override public void accept(Boolean fg) {
      String uid = iqsign_session.getUserId();
      List<IQsignSign> signs = iqsign_database.getAllSignsForUser(uid);
      JSONArray jarr = new JSONArray();
      for (IQsignSign sign : signs) {
         jarr.put(sign.toJson()); 
       }
      BowerRouter.finishResponse(http_exchange,iqsign_session,"OK","data",jarr);
    }
}



private final class RemoveUserAction implements BowerSessionHandler<IQsignSession> {
   
   @Override public String apply(HttpExchange he,IQsignSession session) {
      String uid = session.getUserId();
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
      
      return BowerRouter.jsonOKResponse(session);
    }
   
}       // end of inner class RemoveUser



private final class LocalImageAction implements BowerSessionHandler<IQsignSession> {
   
   @Override public String apply(HttpExchange he,IQsignSession session) {
      String filename = BowerRouter.getParameter(he,"filename");
      File f = iqsign_main.getWebDirectory();
      File f1 = new File(f,"signs");
      File f2 = new File(f1,filename);
      return BowerRouter.sendFileResponse(he,f2); 
    }
}

/********************************************************************************/
/*                                                                              */
/*      Handle delayed responses                                                */
/*                                                                              */
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
   
}       // end of inner class FinishedUpdate


}       // end of class IQsignServer




/* end of IQsignServer.java */

