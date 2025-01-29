/********************************************************************************/
/*										*/
/*		IQsignMain.java 						*/
/*										*/
/*	Main program for iQsign server						*/
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import edu.brown.cs.ivy.bower.BowerMailer;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;

public final class IQsignMain implements IQsignConstants
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   IQsignMain main = new IQsignMain(args);

   main.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IQsignDatabase	database_manager;
private IQsignImageManager	image_manager;
private IQsignDefaults	default_manager;
private IQsignServer	web_server;
private IQsignAuth	iqsign_auth;
private IQsignMaker	sign_maker;
private File		base_directory;
private File		web_directory;
private File		default_signs;
private File		default_images;
private File            default_borders;
private boolean 	is_testing;

private static Pattern IMAGE_PATTERN = Pattern.compile("image(.*)\\.png");
private static Pattern HTML_PATTERN = Pattern.compile("sign(.*)\\.html");
private static Pattern PREVIEW_PATTERN = Pattern.compile("imagePreview(.*)\\.png");

private static Random rand_gen = new Random();
private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private IQsignMain(String [] args)
{
   base_directory = null;
   web_directory = null;
   default_signs = null;

   scanArgs(args);

   if (base_directory == null) {
      base_directory = findBaseDirectory();
    }
   if (base_directory == null || !base_directory.isDirectory()) {
      reportError("Can't find base directory for iot/iqsign");
    }

   File f2 = new File(base_directory,"secret");
   File f4 = new File(f2,"iqsign.props");
   Properties props = new Properties();
   try (FileInputStream fis = new FileInputStream(f4)) {
      props.loadFromXML(fis);
    }
   catch (IOException e) { }

   if (web_directory == null) {
      web_directory = findWebDirectory(props.getProperty("webdirectory"));
    }
   if (web_directory == null) {
      reportError("Can't find web directory for iqsign");
    }
   if (!web_directory.exists() || !web_directory.isDirectory()) {
      reportError("Bad web directory for iqsign");
    }
   File f0 = new File(getWebDirectory(),"signs");
   if (!f0.exists()) f0.mkdir();

   if (default_signs == null) {
      default_signs = findDefaultSigns();
    }
   if (default_signs == null) {
      reportError("Can't find default signs file");
    }

   if (default_images == null) {
      default_images = findDefaultImages();
    }
   if (default_images == null) {
      reportError("Can't find default images file");
    }
   
   if (default_borders == null) {
      default_borders = findDefaultBorders();
    }
   if (default_borders == null) {
      reportError("Can't find default backgrounds file");
    }
   
   String db = props.getProperty("database");
   if (db == null) db = "iqsign";
   database_manager = new IQsignDatabase(this,db);
   image_manager = new IQsignImageManager(this);
   default_manager = new IQsignDefaults(this);
   String jkspwd = props.getProperty("jkspwd");
   iqsign_auth = new IQsignAuth(this);
   sign_maker = new IQsignMaker(this);
   web_server = new IQsignServer(this,jkspwd);
   is_testing = (jkspwd == null);
}



/********************************************************************************/
/*										*/
/*	Argument processing							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-b") && i+1 < args.length) {           // -b <base directory>
	    base_directory = new File(args[++i]);
	  }
	 else if (args[i].startsWith("-w") && i+1 < args.length) {      // -w <web directory>
	    web_directory = new File(args[++i]);
	  }
	 else if (args[i].startsWith("-ds") && i+1 < args.length) {     // -ds <default signs>
	    default_signs = new File(args[++i]);
	  }
	 else if (args[i].startsWith("-di") && i+1 < args.length) {     // -di <default images>
	    default_images = new File(args[++i]);
	  }
         else if (args[i].startsWith("-db") && i+1 < args.length) {     // -db <default borders>
	    default_borders = new File(args[++i]);
	  }
         else if (args[i].startsWith("-s")) {                          // -server
            // nothing needed
          }
	 else if (args[i].startsWith("-LD")) {                          // -LDebug
	    IvyLog.setLogLevel(IvyLog.LogLevel.DEBUG);
	  }
	 else if (args[i].startsWith("-LI")) {                          // -LInfo
	    IvyLog.setLogLevel(IvyLog.LogLevel.INFO);
	  }
	 else if (args[i].startsWith("-LW")) {                          // -LWarning
	    IvyLog.setLogLevel(IvyLog.LogLevel.WARNING);
	  }
	 else if (args[i].startsWith("-L") && i+1 < args.length) {      // -Log <file>
	    IvyLog.setLogFile(args[++i]);
	  }
	 else if (args[i].startsWith("-S")) {                           // -Stderr
	    IvyLog.useStdErr(true);
	  }
	 else {
	    badArgs();
	  }
       }
      else {
	 badArgs();
       }
    }
}



private void badArgs()
{
   reportError("iQsign [-d <base_directory>] [-w <web_directory>]");
}


static void reportError(String msg)
{
   System.err.println("IQSIGN: " + msg);
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

IQsignDatabase getDatabaseManager()		{ return database_manager; }

IQsignAuth getAuthenticator()			{ return iqsign_auth; }

IQsignMaker getSignMaker()			{ return sign_maker; }

IQsignServer getWebServer()			{ return web_server; }

IQsignImageManager getImageManager()			{ return image_manager; }

File getBaseDirectory() 			{ return base_directory; }

File getWebDirectory()				{ return web_directory; }

File getDefaultSignsFile()			{ return default_signs; }

File getDefaultImagesFile()			{ return default_images; }
File getDefaultBordersFile()                    { return default_borders; }

File getSvgLibrary()		
{
   File f1 = new File(base_directory,"svgimagelib");
   File f2 = new File(f1,"svg");

   return f2;
}

String getURLHostPrefix()	
{
   String hn = IvyExecQuery.getHostName();
   if (is_testing) hn = "localhost";
   
   return "http://" + hn;
}


String getURLLocalPrefix()
{
   String hn = IvyExecQuery.getHostName();
   String pfx = "https";
   if (is_testing) {
      pfx = "http";
      hn = "localhost";
    }

   return pfx + "://" + hn + ":" + HTTPS_PORT;
}


/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

static String randomString(int len)
{
   StringBuffer buf = new StringBuffer();
   int cln = RANDOM_CHARS.length();
   for (int i = 0; i < len; ++i) {
      int idx = rand_gen.nextInt(cln);
      buf.append(RANDOM_CHARS.charAt(idx));
    }

   return buf.toString();
}


static String encodeURIComponent(String v)
{
   try {
      return URLEncoder.encode(v,"UTF-8");
    }
   catch (UnsupportedEncodingException e) {
      IvyLog.logE("IQSIGN","Problem with URI encoding",e);
    }

   return v;
}


static String secureHash(String s)
{
   try {
      MessageDigest md = MessageDigest.getInstance("SHA-512");
      byte [] dvl = md.digest(s.getBytes());
      String rslt = Base64.getEncoder().encodeToString(dvl);
      return rslt;
    }
   catch (Exception e) {
      throw new Error("Problem with sha-512 encoding of " + s);
    }
}


static String getAsString(JSONObject json,String key)
{
   Object o = json.get(key);
   if (o == null) return null;
   return o.toString();
}




boolean sendEmail(String sendto,String subj,String body)
{
   if (sendto == null || subj == null && body == null) return false;
   
   File f2 = new File(base_directory,"secret");
   File f4 = new File(f2,"iqsign.props");
   Properties props = new Properties();
   try (FileInputStream fis = new FileInputStream(f4)) {
      props.loadFromXML(fis);
    }
   catch (IOException e) { }
   
   BowerMailer mi = new BowerMailer(sendto,subj,body);
   mi.setSender(props.getProperty("email.from"),
         props.getProperty("email.user"),
         props.getProperty("email.password"));
   mi.setReplyTo(props.getProperty("email.replyto"));
   mi.send();
   
   return false;
}
  





 



/********************************************************************************/
/*										*/
/*	Processing method							*/
/*										*/
/********************************************************************************/

private void process()
{
   web_server.start();

   Timer t = new Timer("IQSIGN TIMER");
   t.schedule(new CleanupTask(),CLEANUP_DELAY,CLEANUP_DELAY);
}



/********************************************************************************/
/*										*/
/*	Cleanup task								*/
/*										*/
/********************************************************************************/

private final class CleanupTask extends TimerTask {

   @Override public void run() {
      IvyLog.logD("IQSIGN","Begin cleanup " + new Date());
   
      default_manager.updateDefaults();
   
      database_manager.deleteOutOfDateData();
      
      Set<String> currentsigns = database_manager.getAllSignNameKeys();
      File f = new File(getWebDirectory(),"signs");
   
      for (File file : f.listFiles()) {
         Matcher m1 = IMAGE_PATTERN.matcher(file.getName());
         Matcher m2 = HTML_PATTERN.matcher(file.getName());
         Matcher m3 = PREVIEW_PATTERN.matcher(file.getName());
         Matcher m = null;
         if (m1.matches()) m = m1;
         else if (m2.matches()) m = m2;
         else if (m3.matches()) m = m3;
         if (m == null) continue;
         String key = m.group(1);
         if (currentsigns.contains(key)) {
            IvyLog.logD("IQSIGN","Cleanup: Keep sign file " + file);
          }
         else {
            IvyLog.logD("IQSIGN","Cleanup: Remvoe sign file " + file);
            file.delete();
          }
       }
   
      IvyLog.logD("IQSIGN","Cleanup complete");
    }

}	// end of inner task CleanupTask



/********************************************************************************/
/*										*/
/*	Find base directory							*/
/*										*/
/********************************************************************************/

private File findBaseDirectory()
{
   File f1 = new File(System.getProperty("user.dir"));
   for (File f2 = f1; f2 != null; f2 = f2.getParentFile()) {
      if (isBaseDirectory(f2)) return f2;
    }
   File f3 = new File(System.getProperty("user.home"));
   if (isBaseDirectory(f3)) return f3;

   File fc = new File("/vol");
   File fd = new File(fc,"iot");
   if (isBaseDirectory(fd)) return fd;

   File fa = new File("/pro");
   File fb = new File(fa,"iot");
   if (isBaseDirectory(fb)) return fb;

   return null;
}


private static boolean isBaseDirectory(File dir)
{
   File f2 = new File(dir,"secret");
   if (!f2.exists()) return false;

   File f3 = new File(f2,"Database.props");
   File f4 = new File(f2,"iqsign.props");
   File f5 = new File(dir,"svgimagelib");
   if (f3.exists() && f4.exists() && f5.exists()) return true;

   return false;
}


/********************************************************************************/
/*										*/
/*	Find web directory							*/
/*										*/
/********************************************************************************/

private File findWebDirectory(String wd)
{
   if (wd != null && !wd.isEmpty()) return new File(wd);

   File f0 = new File(base_directory,"secret");
   File f1 = new File(f0,WEB_DIRECTORY_FILE);
   try {
      String cnts = IvyFile.loadFile(f1);
      cnts = cnts.trim();
      return new File(cnts);
    }
   catch (IOException e) { }

   return null;

}


/********************************************************************************/
/*										*/
/*	Find default signs and images files					*/
/*										*/
/********************************************************************************/

private File findDefaultSigns()
{
   File f0 = new File(base_directory,"resources");
   File f1 = new File(f0,"defaultsigns");
   if (f1.exists() && f1.canRead() && !f1.isDirectory()) return f1;

   return null;
}

private File findDefaultImages()
{
   File f0 = new File(base_directory,"resources");
   File f1 = new File(f0,"defaultimages");
   if (f1.exists() && f1.canRead() && !f1.isDirectory()) return f1;

   return null;
}



private File findDefaultBorders()
{
   File f0 = new File(base_directory,"resources");
   File f1 = new File(f0,"defaultborders");
   if (f1.exists() && f1.canRead() && !f1.isDirectory()) return f1;
   
   return null;
}



}	// end of class IQsignMain




/* end of IQsignMain.java */

