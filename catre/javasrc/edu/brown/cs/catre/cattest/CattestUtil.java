/********************************************************************************/
/*                                                                              */
/*              CattestUtil.java                                                */
/*                                                                              */
/*      Utility methods for testing                                             */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2023 Brown University -- Steven P. Reiss                      */
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




package edu.brown.cs.catre.cattest;

import edu.brown.cs.catre.catmain.CatmainMain;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.ivy.file.IvyFile;

import org.json.JSONObject;
import java.util.Map;
import java.util.Random;
import java.util.Base64;
import java.util.HashMap;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.net.URLEncoder;
import java.net.URL;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.io.OutputStream;
import java.io.InputStream;

class CattestUtil implements CattestConstants
{

 

/********************************************************************************/
/*                                                                              */
/*      Private storage                                                         */
/*                                                                              */
/********************************************************************************/

private static String   test_host = TEST_HOST;

private static final Charset UTF8 = Charset.forName("UTF-8");



/********************************************************************************/
/*                                                                              */
/*      Access methodsm                                                         */
/*                                                                              */
/********************************************************************************/

static void setTestHost(String host)
{
   test_host = host;
}


/********************************************************************************/
/*                                                                              */
/*      Send a message and get JSON response                                    */
/*                                                                              */
/********************************************************************************/

static JSONObject sendGet(String file,Object... val) 
{
   Map<String,Object> map = new HashMap<>();
   if (val.length > 1) {
      for (int i = 0; i+1 < val.length; i += 2) {
         String key = val[i].toString();
         Object v = val[i+1];
         map.put(key,v);
       }
    }

   return sendGetOptional(file,map,false);
}


static JSONObject sendJson(String method,String file,Object... val)
{
   Map<String,Object> map = new HashMap<>();
   if (val.length > 1) {
      for (int i = 0; i+1 < val.length; i += 2) {
         String key = val[i].toString();
         Object v = val[i+1];
         map.put(key,v);
       }
    }

   return sendJson(method,file,new JSONObject(map));
}


static JSONObject snedGetLocal(String file,Map<String,Object> map)
{
   return sendGet(file,map,false);
}


static JSONObject sendGetOptional(String file,Map<String,Object> map,boolean errok)
{
   StringBuffer buf = new StringBuffer();
   buf.append(test_host);
   buf.append(file);
   String sep = "?";
   if (map != null) {
      for (Map.Entry<String,Object> ent : map.entrySet()) {
         buf.append(sep);
         sep = "&";
         buf.append(ent.getKey());
         buf.append("=");
         buf.append(URLEncoder.encode(ent.getValue().toString(),UTF8));
       }
    }

   return send("GET",buf.toString(),null,errok);
}


static JSONObject sendJson(String method,String file,JSONObject jo)
{
   String url = test_host + file;
   String body = null;

   if (method.equals("GET")) {
      String sep = "?";
      for (String key : JSONObject.getNames(jo)) {
         String val = jo.getString(key);
         url += sep + key + "=" + val;
         sep = "?";
       }
    }
   else {
      body = jo.toString(2);
    }

   return send(method,url,body,false);
}




private static JSONObject send(String method,String url,String body,boolean errok)
{
   try {
      URL u = new URI(url).toURL();
      HttpURLConnection uc = (HttpURLConnection) u.openConnection();
      if (body != null) uc.addRequestProperty("content-type","application/json");
      uc.addRequestProperty("accept","application/json");
      uc.setDoInput(true);
      uc.setRequestMethod(method);

      if (body != null) {
         uc.setDoOutput(true);
         OutputStream ots = uc.getOutputStream();
         ots.write(body.getBytes());
      }
      InputStream ins = uc.getInputStream();
      String rslts = IvyFile.loadFile(ins);
      uc.disconnect();
      
      if (uc.getResponseCode() >= 300) {
         return null;
       }

      return new JSONObject(rslts);
    }
   catch (ConnectException e) {
      return null;
    }
   catch (Exception e) {
      if (errok) return null;
      e.printStackTrace();
      throw new Error("Problem sending message " + method + " " + url + " " + body,e);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Methods to start CATRE in a thread                                      */
/*                                                                              */
/********************************************************************************/

static void startCatre()
{
   startCatre(null);
}

static void startCatre(String args)
{   
   CatreRunner cr = new CatreRunner(args);

   cr.start();

   for (int i = 0; i < 100; ++i) {
      try {
         JSONObject ping = sendGetOptional("/ping",null,true);
         if (ping != null) break;
      }
      catch (Throwable t) {
         CatreLog.logD("CATTEST","Wait for web server");
       }
      try {
         Thread.sleep(1000);
      }
      catch (InterruptedException e) { }
    }
}



private static class CatreRunner extends Thread {

   private String [] catre_args;
   
   CatreRunner(String args) {
      super("CatreRunnerThread");
      if (args == null) {
         catre_args = new String [] {};
       }
      else {
         catre_args = args.split("\\s+");
       }
    }

   @Override public void run() {
      CatmainMain.main(catre_args);
    }

}       // end of inner class CatreRunner




/********************************************************************************/
/*                                                                              */
/*      Random number generator                                                 */
/*                                                                              */
/********************************************************************************/

private static Random our_random = new Random();
private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

static void setRandomSeed(long v) 
{
   our_random.setSeed(v);
}


static double nextRandomExp(double mean)
{
   double u = our_random.nextDouble(); 
   return -Math.log(u) * mean;
}



static int nextRandom(int v) 
{
   if (v <= 1) return 0;
   
   return our_random.nextInt(v);
}


static double nextRandom()
{
   return our_random.nextDouble();
}


public static String randomString(int len)
{
   StringBuffer buf = new StringBuffer();
   int cln = RANDOM_CHARS.length();
   for (int i = 0; i < len; ++i) {
      int idx = our_random.nextInt(cln);
      buf.append(RANDOM_CHARS.charAt(idx));
    }
   
   return buf.toString();
}


public static String secureHash(String s)
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


public static JSONObject buildJson(Object... args)
{
   JSONObject rslt = new JSONObject();
   for (int i = 0; i < args.length-1; i += 2) {
      String key = args[i].toString();
      Object val = args[i+1];
      rslt.put(key,val);
    }
   
   return rslt;
}



}       // end of class CattestUtil




/* end of CattestUtil.java */

