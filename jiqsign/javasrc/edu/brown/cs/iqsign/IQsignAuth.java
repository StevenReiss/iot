/********************************************************************************/
/*										*/
/*		IQsignAuth.java 						*/
/*										*/
/*	Authentication methods for iQsign					*/
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;

import edu.brown.cs.ivy.bower.BowerRouter;
import edu.brown.cs.ivy.file.IvyLog; 

class IQsignAuth implements IQsignConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IQsignMain	iqsign_main;

// LOOK INTO apache commons email validator

private static final Pattern EMAIL_PATTERN =
   Pattern.compile("(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+" +
	 "(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]" +
	 "|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?" +
	 "[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?" +
	 "[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|" +
	 "(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*" +
	 "(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|" +
	 "(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?" +
	 "[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+" +
	 "(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]" +
	 "\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] " +
	 "\\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|" +
	 "\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*" +
	 "\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+" +
	 "(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]" +
	 "|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:" +
	 "\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:" +
	 "\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*" +
	 "(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?" +
	 "[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\]" +
	 "(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] " +
	 "\\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|" +
	 "\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?" +
	 "[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|" +
	 "(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*" +
	 "\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] " +
	 "\\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:" +
	 "[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)" +
	 "?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|" +
	 "\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?" +
	 "[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+" +
	 "(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]" +
	 "|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)|(?:[^()<>@,;:\\\\\".\\" +
	 "[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|" +
	 "\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*:" +
	 "(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?" +
	 "[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?" +
	 "[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] " +
	 "\\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:" +
	 "[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?" +
	 "[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()" +
	 "<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\." +
	 "(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+" +
	 "|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?" +
	 "[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()" +
	 "<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?" +
	 "[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:" +
	 "\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\]" +
	 "(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+" +
	 "(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|" +
	 "\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\]" +
	 " \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]" +
	 "\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\]" +
	 " \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[" +
	 "([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:" +
	 "[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\"." +
	 "\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\." +
	 "(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|" +
	 "\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))" +
	 "*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+" +
	 "(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*" +
	 "\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+" +
	 "(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)" +
	 "*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)(?:,\\s*(?:(?:[^()<>@,;:\\\\\".\\[\\]" +
	 " \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:" +
	 "[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?" +
	 "[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:" +
	 "\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*" +
	 "@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|" +
	 "(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)" +
	 "(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])" +
	 "+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|" +
	 "(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:" +
	 "\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*" +
	 "\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|" +
	 "\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)" +
	 "(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|" +
	 "\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*" +
	 "(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|" +
	 "\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)" +
	 "(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|" +
	 "\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*" +
	 ":(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|" +
	 "(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)" +
	 "?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?" +
	 "[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*" +
	 "\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:" +
	 "\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?" +
	 "[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])" +
	 "+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))" +
	 "*\\>(?:(?:\\r\\n)?[ \\t])*))*)?;\\s*)");





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

IQsignAuth(IQsignMain main)
{
   iqsign_main = main;
}



/********************************************************************************/
/*										*/
/*	Handle prelogin 							*/
/*										*/
/********************************************************************************/

String handlePreLogin(HttpExchange he,IQsignSession session)
{
   String rslt = BowerRouter.jsonOKResponse(session,"code",session.getCode());

   IvyLog.logD("IQSIGN","PRELOGIN " + rslt);
	
   return rslt;
}



/********************************************************************************/
/*										*/
/*	Handle actual login							*/
/*										*/
/********************************************************************************/

String handleLogin(HttpExchange he,IQsignSession session)
{
   session.setUser(null);

   IQsignDatabase db = iqsign_main.getDatabaseManager();
   IQsignUser user = null;
   try {
      IvyLog.logD("IQSIGN","LOGIN ");
      String uid = BowerRouter.getParameter(he,"username");
      if (uid == null || uid.isEmpty()) {
	 return errorResponse(session,"User name must be given");
       }
      uid = uid.toLowerCase();
      user = db.findUser(uid);
      if (user == null) {
	 return errorResponse(session,"Invalid username or password");
       }
      String actok = BowerRouter.getParameter(he,"accesstoken");
      String accod = BowerRouter.getParameter(he,"accesscode");
      if (actok != null && !actok.isEmpty()) { 
	 IQsignLoginCode tokinfo = db.checkAccessToken(actok,null,null);
	 if (tokinfo == null || !tokinfo.getUserId().equals(user.getUserId())) {
	    return errorResponse(session,"Invalid access token");
	  }
       }
      else if (accod != null && !accod.isEmpty() && session.getCode() != null) {
         IQsignLoginCode tokinfo = db.checkAccessToken(actok,
               user.getUserId(),session.getCode()); 
	 if (tokinfo == null || !tokinfo.getUserId().equals(user.getUserId())) {
	    return errorResponse(session,"Invalid access token");
	  } 
       }
      else {
	 String pwd = user.getPassword();
         String tpwd = user.getTempPassword();
	 String upwd = BowerRouter.getParameter(he,"password");
	 if (!uid.equals(user.getEmail()) && uid.equals(user.getUserName())) {
	    pwd = user.getAltPassword();
	  }
	 String s = BowerRouter.getParameter(he,"padding");
	 String pwd1 = IQsignMain.secureHash(pwd + s);
         if (tpwd != null && !tpwd.isEmpty()) {
            tpwd = IQsignMain.secureHash(tpwd + s);
            if (tpwd.equals(upwd)) {
               pwd1 = tpwd;
               db.updatePassword(user.getUserId(),user.getPassword(),
                     user.getAltPassword());
             }
          }
         
	 if (!pwd1.equals(upwd)) {
	    return errorResponse(session,"Invalid username or password");
	  }
       }

      user.clearPasswords();
      session.setUser(user);

      return BowerRouter.jsonOKResponse(session);
    }
   catch (Throwable t) {
      IvyLog.logE("IQSIGN","Problem with login",t);
      return BowerRouter.errorResponse(he,session,500,"Login problem");
    }
}



/********************************************************************************/
/*										*/
/*	Handle registration							*/
/*										*/
/********************************************************************************/

String handleRegister(HttpExchange he,IQsignSession session)
{
   IQsignDatabase db = iqsign_main.getDatabaseManager();

   String email = BowerRouter.getParameter(he,"email");
   email = email.toLowerCase();
   BowerRouter.setParameter(he,"email",email);
   String uid = BowerRouter.getParameter(he,"username");
   uid = uid.toLowerCase();
   BowerRouter.setParameter(he,"username",uid);
   String pwd = BowerRouter.getParameter(he,"password");
   String altpwd = BowerRouter.getParameter(he,"altpassword");
   String signname = BowerRouter.getParameter(he,"signname");
   String valid = IQsignMain.randomString(48);
   if (uid == null || uid.isEmpty()) uid = email;

   boolean undoneeded = false;
   try {
      IvyLog.logD("IQSIGN","REGISTER ");
      if (email == null || email.isEmpty()) {
	 return errorResponse(session,"Email must be given");
       }
      else if (!validateEmail(email)) {
	 return errorResponse(session,"Invalid email addresss");
       }
      else if (validateEmail(uid) && !uid.equals(email)) {
	 return errorResponse(session,"User id must not be an email address");
       }
      else if (signname == null || signname.isEmpty()) {
	 return errorResponse(session,"Sign name must be given");
       }
      String err = db.checkNoUser(uid,email);
      if (err != null) {
	 return errorResponse(session,err);
       }
      boolean isvalid = false;
      boolean ok = db.registerUser(email,uid,pwd,altpwd,isvalid);
      if (!ok) {
         return errorResponse(session,"Problem registering new user");
       }
      undoneeded = true;
      IQsignSign sign = IQsignSign.setupSign(iqsign_main,signname,email,null,null);
      boolean ok1 = db.registerValidator(email,valid);
      boolean ok2 = sendRegistrationEmail(he,session,email,valid);
      if (sign == null || !ok1 || !ok2) {
	 return errorResponse(session,"Problem registering new user (email not accepted)");
       }
      undoneeded = false;
    }
   catch (Throwable t) {
      IvyLog.logE("IQSIGN","Problem while registering user",t);
      return errorResponse(session,"Problem registering new user");
    }
   finally {
      if (undoneeded) {
	 db.unregisterUser(email);
       }
    }

   return BowerRouter.jsonOKResponse(session);
}


private String errorResponse(IQsignSession sess,String msg)
{
   return BowerRouter.buildResponse(sess,"ERROR","message",msg);
}


private boolean validateEmail(String data)
{
   Matcher m = EMAIL_PATTERN.matcher(data.toLowerCase());
   return m.matches();
}


private boolean sendRegistrationEmail(HttpExchange he,IQsignSession session,String email,String valid)
{
   String pfx = iqsign_main.getURLLocalPrefix(); 
   // need to get host from he
   String msg = "Thank you for registering with iQsign.\n";
   msg += "To complete the reqistration process, please click on or paste the link:\n";
   msg += "   " + pfx + "/validate?";
   msg += "email=" + IQsignMain.encodeURIComponent(email);
   msg += "&code=" + valid;
   msg += "\n";

   IvyLog.logD("IQSIGN","SEND EMAIL to " + email + " " + msg);
   
   boolean sts = iqsign_main.sendEmail(email,"Verify your Email for iQsign",msg); 

   return sts;
}



String handleValidationRequest(HttpExchange he,IQsignSession session)
{
   String email = BowerRouter.getParameter(he,"email");
   String code = BowerRouter.getParameter(he,"code");
   
   IQsignDatabase db = iqsign_main.getDatabaseManager();
   if (code == null || email == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad validation request");
    }
   email = email.toLowerCase();
   
   boolean fg = db.validateUser(email,code); 
   
   if (!fg) {
      return BowerRouter.errorResponse(he,session,400,
            "Outdated or bad validation request");
    }
   
   String result = "<html>" +
   "<p>Thank you for validating your email. </p> " + 
   "<p>You should be able to log into iQsign now.</p>" +
   "<p>WELCOME to iQsign !!!</p>" +
   "</html>";
   
   return result;
}


String handleForgotPassword(HttpExchange he,IQsignSession session) 
{
   IQsignDatabase db = iqsign_main.getDatabaseManager();
   
   String email = BowerRouter.getParameter(he,"email");
   email = email.toLowerCase();
   if (!validateEmail(email)) {
      return BowerRouter.errorResponse(he,session,400,"Bad email");
    }
   
   // Code should be 12 long
   // Email should say login with the given code and then change your password.
   // Note that the code is only valid once.
   // compute a temporary password using email and this code
   // update the user's temppassword field to the resul
   
   String code = IQsignMain.randomString(12);
   String pwd = IQsignMain.secureHash(code);
   pwd = IQsignMain.secureHash(pwd + email);

   IQsignUser user = db.findUser(email);
   if (user != null) {
      db.setTemporaryPassword(user.getUserId(),pwd);
      String msg = "To log into iQsign, please use the one-time password " + code + "\n\n";
      msg += "Note that to use this you must log in with your email, not your user name.\n\n";
      msg += "Once you have logged in with this code, please change your password.\n\n";
      msg += "Thank you for using iQsign.\n";
      msg += "\n";
      iqsign_main.sendEmail(email,"Password request for iQsign",msg);
    }
   
   return BowerRouter.jsonOKResponse(session);
}


String handleResetPassword(HttpExchange he,IQsignSession session)
{
   IQsignDatabase db = iqsign_main.getDatabaseManager();
   
   String email = BowerRouter.getParameter(he,"email");
   email = email.toLowerCase();
   String code = BowerRouter.getParameter(he,"code");
   String pwd = BowerRouter.getParameter(he,"password");
   String altpwd = BowerRouter.getParameter(he,"altpassword");
   
   if (db.validateUser(email,code)) {
      IQsignUser user = db.findUser(email);
      db.updatePassword(user.getUserId(),pwd,altpwd); 
    }
   
   return BowerRouter.jsonOKResponse(session);
}


String handleChangePassword(HttpExchange he,IQsignSession session)
{
   IQsignDatabase db = iqsign_main.getDatabaseManager();
   
   Number uid = session.getUserId();
   String pwd = BowerRouter.getParameter(he,"password");
   String altpwd = BowerRouter.getParameter(he,"altpassword");
   String usrpwd = BowerRouter.getParameter(he,"userpwd");
   
   IQsignUser user = db.findUser(uid);
   if (user == null) {
      return BowerRouter.errorResponse(he,session,402,"Bad user");
    }
   if ((pwd == null || pwd.isEmpty()) && usrpwd != null) {
      pwd = IQsignMain.secureHash(usrpwd + user.getEmail()); 
    }
   if ((altpwd == null || altpwd.isEmpty()) && usrpwd != null) {
      altpwd = IQsignMain.secureHash(usrpwd + user.getUserName());
    }
   
   db.updatePassword(user.getUserId(),pwd,altpwd); 
   
   return BowerRouter.jsonOKResponse(session);
}

}	// end of class IQsignAuth




/* end of IQsignAuth.java */

