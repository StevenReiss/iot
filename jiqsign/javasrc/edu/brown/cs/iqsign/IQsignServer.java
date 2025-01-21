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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;

import com.sun.net.httpserver.HttpExchange;

import edu.brown.cs.ivy.bower.BowerRouter;
import edu.brown.cs.ivy.bower.BowerServer;
import edu.brown.cs.ivy.bower.BowerSession;
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


private BowerServer     https_server;
private BowerServer     http_server;
private SessionStore    session_store;
private Set<String>     users_updated;
private Set<String>     users_active;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

IQsignServer(IQsignMain main)
{
   session_store = new SessionStore(main);
   
   BowerRouter br = setupRouter();
   
   users_updated = new HashSet<>();
   users_active = new HashSet<>();
   
   https_server = new BowerServer(HTTPS_PORT); 
   https_server.setRouter(br);
   // set HTTPs information
   // set Executor
   // set Session Parameter
   // set Session Cookie
   
   http_server = new BowerServer(HTTP_PORT);
   http_server.setRouter(br);
   // set executor
   // set session parameter
   // set session cookie
   
   
   https_server.start();
   http_server.start();
   
}


/********************************************************************************/
/*                                                                              */
/*      Start the servers                                                       */
/*                                                                              */
/********************************************************************************/

void start()
{
   boolean fg1 = https_server.start();
   boolean fg2 = http_server.start();
   
   if (!fg1 && !fg2) {
      IQsignMain.reportError("Can't start web service");
    }
}



/********************************************************************************/
/*                                                                              */
/*      Setup the router                                                        */
/*                                                                              */
/********************************************************************************/

BowerRouter setupRouter()
{
   BowerRouter br = new BowerRouter(session_store);
   br.addRoute("ALL",BowerRouter::handleParameters); 
   br.addRoute("ALL",br::handleSessions);
   br.addRoute("ALL",BowerRouter::handleLogging);
   br.addRoute("ALL",new PingAction());
   
// br.addRoute("GET","/rest/svg/:svgttopic/:svgname",null);
// br.addRoute("GET","/rest/image/:imagename",null);  
// br.addRoute("ALL","/rest/ping",null);
// br.addRoute("GET","/rest/login",null);
// br.addRoute("POST","/rest/login",null);
// br.addRoute("GET","/rest/register",null);
// br.addRoute("POST","/rest/register",null);
// br.addRoute("PORT","/rest/forgotpassword",null);
// br.addRoute("ALL","/rest/logout",null);
// br.addRoute("ALL","/rest/authorize",null);
// br.addRoute("ALL","/rest/about",null);
// br.addRoute("GET","/rest/instructions",null);
// br.addRoute("USE",new Authenticator());
// br.addRoute("ALL","/rest/signs",null);
// br.addRoute("PUT","/rest/sign/:signid/update",null);
// br.addRoute("POST","/rest/sign/setto",null);
// br.addRoute("POST","/rest/sign/update",null);
// br.addRoute("ALL","/rest/svgimages",null);
// br.addRoute("ALL","/rest/savedimages",null);
// br.addRoute("POST","/rest/loadsignimage",null);
// br.addRoute("POST","/rest/savesignimage",null);
// br.addRoute("POST","/rest/removesignimage",null);
// br.addRoute("POST","/rest/sign/preview",null);
// br.addRoute("POST","/rest/createcode",null);
// br.addRoute("ALL","/rest/namedsigns",null);
// br.addRoute("POST","/rest/addsign",null);
// br.addRoute("POST","/rest/removeuser",null);
// br.addRoute("ALL","*",null);
// br.addRoute("USE",new ErrorHandler());
   
   return br;
}




/********************************************************************************/
/*                                                                              */
/*      Session management                                                      */
/*                                                                              */
/********************************************************************************/

private final class SessionStore implements BowerSessionStore {
   
   private IQsignDatabase store_db;
    
   SessionStore(IQsignMain main) {
      store_db = main.getDatabaseManager();
    }
   
   @Override public void saveSession(BowerSession bs) {
      String code = bs.getValue("code");
      if (code == null) {
         code = IQsignMain.randomString(32);
         bs.setValue("code",code);
         store_db.startSession(bs.getSessionId(),code);
       }
    }
   
   @Override public BowerSession loadSession(String sid) {
      BowerSession bs = store_db.checkSession(sid);
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

private final class PingAction implements BowerSessionHandler { 
   
   @Override public String apply(HttpExchange e,BowerSession bs) {  
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
      
      return BowerServer.jsonResponse(bs,"status","OK","authorize",auth,"update",update); 
    }
}


}       // end of class IQsignServer




/* end of IQsignServer.java */

