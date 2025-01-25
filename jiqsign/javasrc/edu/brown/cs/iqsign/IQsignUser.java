/********************************************************************************/
/*                                                                              */
/*              IQsignUser.java                                                 */
/*                                                                              */
/*      Representation of a user                                                */
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


class IQsignUser implements IQsignConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private JSONObject user_data;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

IQsignUser(JSONObject json)
{
   user_data = json;
}



/********************************************************************************/
/*                                                                              */
/*      Access methodsm                                                         */
/*                                                                              */
/********************************************************************************/

String getUserId()              { return IQsignMain.getId(user_data,"id"); }
String getEmail()                       { return user_data.getString("email"); }
String getUserName()                    { return user_data.getString("username"); }
String getPassword()                    { return user_data.getString("password"); }
String getAltPassword()                 { return user_data.getString("altpassword"); }
int getMaxSigns()                       { return user_data.getInt("maxsigns"); }
boolean isAdmin()                       { return user_data.getBoolean("admin"); }
boolean isValid()                       { return user_data.getBoolean("valid"); }


void clearPasswords()
{
   user_data.put("password","");
   user_data.put("altpassword","");
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

JSONObject toJson()
{
   return user_data;
}



}       // end of class IQsignUser




/* end of IQsignUser.java */

