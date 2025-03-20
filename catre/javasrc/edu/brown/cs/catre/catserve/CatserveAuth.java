/********************************************************************************/
/*										*/
/*		CatserveAuth.java						*/
/*										*/
/*	Handle login/register							*/
/*										*/
/********************************************************************************/
/* Copyright 2023 Brown University -- Steven P. Reiss, Molly E. McHenry         */
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




package edu.brown.cs.catre.catserve;

import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreException;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUser;
import edu.brown.cs.catre.catre.CatreUtil;
import edu.brown.cs.ivy.bower.BowerRouter;

import com.sun.net.httpserver.HttpExchange;



class CatserveAuth implements CatserveConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreStore	data_store;
private CatreController catre_control;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatserveAuth(CatreController cc)
{
   catre_control = cc;
   data_store = cc.getDatabase();
}



/********************************************************************************/
/*										*/
/*	Handle register 							*/
/*										*/
/********************************************************************************/

public String handleRegister(HttpExchange he, CatserveSessionImpl session) 
{
   CatreLog.logT("CATSERVE","Handle register entered");
   
   if (session == null) {
      return BowerRouter.errorResponse(he,session,402,
            "Bad session");
    }
   if (session.getUser(catre_control) != null) {
      return BowerRouter.errorResponse(he,session,400,
            "Can't register while logged in");
   }

   String userid = BowerRouter.getParameter(he,"username");
   String email = BowerRouter.getParameter(he,"email");
   String pwd = BowerRouter.getParameter(he,"password");
   String unm = BowerRouter.getParameter(he,"universe");

   CatreLog.logD("AUTH", "userid: " + userid + " email: " + email + " pwd: " + pwd + " unm: " + unm);

   CatreUser cu = null;
   boolean remove = false;
   try {
      cu = data_store.createUser(userid,email,pwd);
      remove = true;

      if (catre_control.createUniverse(unm,cu) == null) {
         return BowerRouter.errorResponse(he,session,500,
               "problem creating universe");
      }
      String code = cu.setupValidator();
      String msg = "Thank you for registering with Sherpa.\n\n";
      msg += "To complete the reqistration process, please click on or paste the link:\n";
      msg += "   " + catre_control.getUrlPrefix() + "/validate?"; 
      msg += "email=" + CatreUtil.escape(email);
      msg += "&code=" + code;
      msg += "\n"; 
      if (!CatreUtil.sendEmail(email,"Complete SHERPA registration",msg)) {
         return BowerRouter.errorResponse(he,session,500,
               "problem sending email");
       }
      
      remove = false;
      
      session.setupSession(cu);
      session.saveSession(catre_control);
      return BowerRouter.jsonOKResponse(session);
   }
   catch (CatreException err) {
      String msg = err.getMessage();
      return BowerRouter.errorResponse(he,session,500,msg);
   }
   finally {
      if (remove && cu != null) {
         data_store.removeObject(cu.getDataUID());
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Handle change password                                                  */
/*                                                                              */
/********************************************************************************/

public String handleChangePassword(HttpExchange he,CatserveSessionImpl session) 
{
   CatreLog.logD("CATSERVE","handleChangePasswrd entered");
   
   String npwd = BowerRouter.getParameter(he,"password");
   if (npwd == null || npwd.isEmpty()) {
      return BowerRouter.errorResponse(he,session,400,
            "Bad password");
    }
   
   CatreUser cu = session.getUser(catre_control);
   if (cu == null) {
      return BowerRouter.errorResponse(he,session,402,
            "Unauthorized access");
    }
   
   cu.setNewPassword(npwd);
   
   return BowerRouter.jsonOKResponse(session,"STATUS","OK","TEMPORARY",false);
}


/********************************************************************************/
/*                                                                              */
/*      Handle FORGOT PASSWORD request                                          */
/*                                                                              */
/********************************************************************************/

public String handleForgotPassword(HttpExchange he,CatserveSessionImpl session)
{
   CatreLog.logD("CATSERVE","handleForgotPasswrd entered");
   
   String email = BowerRouter.getParameter(he,"email");
   if (email == null || email.isEmpty()) {
      return BowerRouter.errorResponse(he,session,400,
            "No email given");
    }
   
   CatreUser cu = catre_control.getDatabase().findUserByEmail(email);
   if (cu == null) {
      return BowerRouter.errorResponse(he,session,400,
         "Unknown email given");
    }
   
   String npwd = CatreUtil.randomString(12);
   
   cu.setTemporaryPassword(npwd);
   
   String body = "Sherpa has set up a temporary, one-time password that you ";
   body += "can use to log-in and change your password. ";
   body += "\n\n";
   body += "To do so, login to sherpa with your username (" + cu.getUserName() + ") ";
   body += "and the password " + npwd + ".";
         
   if (CatreUtil.sendEmail(email,"SHERPA password request",body)) {
      return BowerRouter.jsonOKResponse(session);
    }
   
   return BowerRouter.errorResponse(he,session,500,"Email failed");
}


public String handleValidateUser(HttpExchange he,CatserveSessionImpl session)
{
   CatreLog.logD("CATSERVE","Handle verify user");
   
   String email = BowerRouter.getParameter(he,"email");
   String code = BowerRouter.getParameter(he,"code");
   if (email == null || email.isEmpty() || code == null || code.isEmpty()) {
      return BowerRouter.errorResponse(he,session,400,
            "Bad validation request");
    }
   email = email.toLowerCase();
   CatreUser cu = catre_control.getDatabase().findUserByEmail(email);
   if (cu == null) {
      return BowerRouter.errorResponse(he,session,402,
            "Bad validation request");
    }
   if (!cu.validateUser(code)) {
      return BowerRouter.errorResponse(he,session,402,
            "Bad validation request");
    }
   
   return BowerRouter.jsonOKResponse(session); 
}




/********************************************************************************/
/*										*/
/*	Handle Login								*/
/*										*/
/********************************************************************************/
 
public String handleLogin(HttpExchange he, CatserveSessionImpl session)
{
   if (session == null) {
      return BowerRouter.errorResponse(he,session,402,
            "Bad session");
    }
   
   String username = BowerRouter.getParameter(he,"username");
   String pwd = BowerRouter.getParameter(he,"password");
   if (username == null || pwd == null) {
      return BowerRouter.errorResponse(he,session,402,
            "Bad username or password");
    }
   String salt = BowerRouter.getParameter(he,"SALT");
   String salt1 = session.getStringValue("SALT");
   CatreLog.logD("CATSERVE","LOGIN " + username + " " + pwd + " " + salt);
   
   if (username == null || pwd == null) {
      return BowerRouter.errorResponse(he,session,400,
            "Missing username or password");
    }
   else if (salt == null || salt1 == null || !salt.equals(salt1)) {
      return BowerRouter.errorResponse(he,session,400,
            "Bad setup");
    }
   else {
      CatreUser cu = catre_control.getDatabase().findUser(username,pwd,salt);
      if (cu == null) {
         return BowerRouter.errorResponse(he,session,402,"Bad user name or password");
       }
      else {
         session.setupSession(cu);
         session.saveSession(catre_control);
         return BowerRouter.jsonOKResponse(session,"TEMPORARY",cu.isTemporary());
       }
    }
}


}	// end of class CatserveAuth




/* end of CatserveAuth.java */

