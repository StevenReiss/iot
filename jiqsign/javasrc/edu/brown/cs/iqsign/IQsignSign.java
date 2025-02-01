/********************************************************************************/
/*										*/
/*		IQsignSign.java 						*/
/*										*/
/*	Sign methods								*/
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
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.function.Consumer;

import org.json.JSONObject;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;

class IQsignSign implements IQsignConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IQsignMain iqsign_main;
private IQsignDatabase for_database;
private JSONObject sign_data;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

IQsignSign(IQsignMain main,JSONObject data)
{
   iqsign_main = main;
   for_database = iqsign_main.getDatabaseManager();
   sign_data = data;
}



/********************************************************************************/
/*										*/
/*	Setup a new sign							*/
/*										*/
/********************************************************************************/

static IQsignSign setupSign(IQsignMain main,String name,String email,String contents,
      Consumer<Boolean> next)
{
   IvyLog.logD("IQSIGN","SETUP SIGN " + name + " " + email + " " + contents);

   if (contents == null) {
      contents = normalizeContents(INITIAL_SIGN);
    }
   String namekey = IQsignMain.randomString(8);

   IQsignDatabase db = main.getDatabaseManager();
   IQsignUser u = db.findUser(email);
   if (u == null) {
      IvyLog.logD("IQSIGN","SETUP SIGN: Bad user email");
      return null;
    }
   Number uid = u.getUserId();

   IQsignSign sign = db.createSign(uid,name,namekey,contents);
   if (sign == null) {
      return null;
    }
   String dname = sign.computeDisplayName();
   db.addDefineName(uid,dname,contents,false);

   sign.setupWebPage();
   sign.updateSign(next,false,false);

   return sign;
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getContents()		{ return sign_data.getString("lastsign"); }

String setContents(String c)
{
   c = normalizeContents(c);
   sign_data.put("lastsign",c);
   sign_data.put("displayname",(String) null);
   return c;
}

Number getUserId()
{
   return sign_data.getNumber("userid");
}

Number getId()
{
   return sign_data.getNumber("id");
}

String getNameKey()
{
   return sign_data.getString("namekey");
}


File getHtmlFile()
{
   File f1 = iqsign_main.getWebDirectory();
   File f2 = new File(f1,"signs");
   File f3 = new File(f2,"sign" + getNameKey() + ".html");
   return f3;
}


File getImageFile(boolean preview)
{
   String p = (preview ? "PREVIEW" : "");
   File f1 = iqsign_main.getWebDirectory();
   File f2 = new File(f1,"signs");
   File f3 = new File(f2,"image" + p + getNameKey() + ".png");
   return f3;
}

String getImageUrl()
{
   return iqsign_main.getURLHostPrefix() + "/iqsign/signs/image" + getNameKey() + ".png";
}

String getSignUrl()
{
   return iqsign_main.getURLHostPrefix() + "/iqsign/signs/sign" + getNameKey() + ".html";
}

String getLocalImageUrl()
{
   return iqsign_main.getURLLocalPrefix() + "/rest/signimage/image" + getNameKey() + ".png";
}


int getWidth()			{ return sign_data.getInt("width"); }
int getHeight() 		{ return sign_data.getInt("height"); }
String getSignName()		{ return sign_data.getString("name"); }
String getDimension()		{ return IQsignMain.getAsString(sign_data,"dimension"); }



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

void changeSign(String cnts,Consumer<Boolean> next)
{
   cnts = setContents(cnts);
   for_database.changeSign(getId(),cnts);
   String dname = computeDisplayName();
   setDisplayName(dname);
   for_database.addDefineName(getUserId(),dname,cnts,false);
   setupWebPage();
   updateSign(next,true,false);
}


void updateProperties(String name,String dim,int width,int height)
{
   if (name != null && !name.isEmpty()) {
      sign_data.put("signname",name);
    }
   if (dim != null && !dim.isEmpty()) {
      sign_data.put("signdim",dim);
    }
   if (width > 0) {
      sign_data.put("signwidth",width);
    }
   if (height > 0) {
      sign_data.put("signheight",height);
    }
  for_database.updateSignProperties(this);
}



void updateSign(Consumer<Boolean> next,boolean counts,boolean preview)
{
   IQsignMaker maker = iqsign_main.getSignMaker();
   maker.requestUpdate(this,counts,preview,next);
}


/********************************************************************************/
/*										*/
/*	Web and image methods							*/
/*										*/
/********************************************************************************/

private void setupWebPage()
{
   String cnts = iqsign_main.loadResource("iqsigntemplate.html",this);
   if (cnts != null) {
      try (FileWriter fw = new FileWriter(getHtmlFile())) {
	 fw.write(cnts);
       }
      catch (IOException e) {
         IvyLog.logE("IQSIGN","Problem copying to initial html page",e);
       }
    }
   
   File f5 = getImageFile(false);
   if (!f5.exists()) {
      File f1 = iqsign_main.getBaseDirectory();
      File f2 = new File(f1,"resources");
      File f6 = new File(f2,"iqsignimagetemplate.png");
      File f7 = getImageFile(false);
      try {
         IvyFile.copyFile(f6,f7);
       }
      catch (IOException e) {
         IvyLog.logE("IQSIGN","Problem setting up image file for sign",e);
       }
    }
   
}



/********************************************************************************/
/*										*/
/*	Display name methods							*/
/*										*/
/********************************************************************************/

String computeDisplayName()
{
   String sname = null;
   String dname = null;
   StringTokenizer tok = new StringTokenizer(getContents(),"\n");
   while (tok.hasMoreTokens()) {
      String line = tok.nextToken().trim();
      if (line.startsWith("=")) {
	 int i = line.indexOf("=",1);
	 sname = line.substring(1);
	 if (i > 0) {
	    while (i > 0) {
	       char c = line.charAt(i);
	       if (Character.isWhitespace(c)) {
		  sname = line.substring(1,i).trim();
		}
	     }
	  }
       }
      else if (line.startsWith("@") || line.startsWith("%")) continue;
      else if (dname == null) {
	 String [] wds = line.split("\\s");
	 for (String wd : wds) {
	    if (wd.startsWith("#")) continue;
	    if (dname == null) dname = wd;
	    else dname += " " + wd;
	  }
       }
    }

   if (sname == null) {
      sname = for_database.getDefineName(getContents(),getUserId());
      if (sname == null) sname = dname;
    }

   setDisplayName(sname);

   return sname;
}


String getDisplayName()
{
   String s = sign_data.optString("displayname",null);
   if (s == null || s.isEmpty()) {
      s = computeDisplayName();
    }
   return s;
}

void setDisplayName(String name)
{
   sign_data.put("displayname",name);
   for_database.updateDisplayName(getId(),name);
}


/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

JSONObject toJson()
{
   sign_data.put("displayname",getDisplayName());
   sign_data.put("dim",getDimension());
   sign_data.put("signurl",getSignUrl());
   sign_data.put("imageurl",getImageUrl());
   sign_data.put("signbody",getContents());
   sign_data.put("signuser",getUserId());
   sign_data.put("signid",getId());
   sign_data.put("localimageurl",getLocalImageUrl());
   return sign_data;
}



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

static String normalizeContents(String c)
{
   c = c.replace("\r","");
   c = c.replace("\t"," ");
   if (!c.endsWith("\n")) c = c + "\n";
   return c;
}

}	// end of class IQsignSign




/* end of IQsignSign.java */

