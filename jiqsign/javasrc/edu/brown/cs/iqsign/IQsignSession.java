/********************************************************************************/
/*                                                                              */
/*              IQsignSession.java                                              */
/*                                                                              */
/*      description of class                                                    */
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


import org.json.JSONObject;

import edu.brown.cs.ivy.bower.BowerSessionBase;

class IQsignSession extends BowerSessionBase implements IQsignConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BowerSessionStore<IQsignSession> session_store;
private IQsignUser session_user;
private Number     session_userid;
private String     session_code;
private long       last_time;
private long       create_time;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

IQsignSession(BowerSessionStore<IQsignSession> bss)
{
   session_store = bss;
   session_user = null;
   session_userid = null;
   session_code = IQsignMain.randomString(32);
   last_time = System.currentTimeMillis();
   create_time = last_time;
   
}


IQsignSession(BowerSessionStore<IQsignSession> bss,JSONObject data)
{
   session_store = bss;
   session_user = null;
   session_userid = data.optNumber("userid",null);
   last_time = data.optLong("last_time");
   session_code = data.optString("code",null);
   create_time = data.optLong("creation_time");
}



/********************************************************************************/
/*                                                                              */
/*      Access Methodsm                                                         */
/*                                                                              */
/********************************************************************************/

@Override public BowerSessionStore<IQsignSession> getSessionStore()
{
   return session_store;
}


IQsignUser getUser()                    { return session_user; }
Number getUserId()                      { return session_userid; }
void setUser(IQsignUser u) 
{
   session_user = u;
   session_userid = (u == null ? null : u.getUserId()); 
}
void setUserId(Number uid)              { session_userid = uid; }

String getCode()                        { return session_code; }
void setCode(String code)               { session_code = code; }



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

JSONObject toJson()
{
   JSONObject rslt = buildJson("session",getSessionId(),
         "userid",session_userid,
         "code",session_code,
         "creation_time",create_time,
         "last_used",last_time);
    
   return rslt;
}




}       // end of class IQsignSession




/* end of IQsignSession.java */

