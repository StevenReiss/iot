/********************************************************************************/
/*										*/
/*		IQsignImages.java						*/
/*										*/
/*	Manage the various sets of images					*/
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.ivy.file.IvyLog;

class IQsignImageManager implements IQsignConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IQsignMain	iqsign_main;
private Set<SvgTopicData> svg_topics;
private long		last_update;

private static final Set<String> SKIP_TOPICS;

static {
   SKIP_TOPICS = new HashSet<>();
   SKIP_TOPICS.add("arrows");
   SKIP_TOPICS.add("flowcharts");
   SKIP_TOPICS.add("fullsize");
   SKIP_TOPICS.add("game_elements");
   SKIP_TOPICS.add("special");
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

IQsignImageManager(IQsignMain main)
{
   iqsign_main = main;
   loadSvgImages(main.getSvgLibrary());
   last_update = 0;
   updateDefaultImages();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

File getLocalImage(String name)
{
   int idx = name.indexOf("?");
   if (idx > 0) name = name.substring(0,idx);
   File f1 = new File(name);
   if (f1.isAbsolute()) return f1;
   File f2 = getImagesDirectory();
   File f3 = new File(f2,f1.getPath());
   return f3;
}


File getSvgImage(String topic,String name)
{
   File root = iqsign_main.getSvgLibrary();
   File f1 = new File(root,topic);
   if (!name.endsWith(".svg")) {
      name = name + ".svg";
    }
   File f2 = new File(f1,name);
   return f2;
}


/********************************************************************************/
/*										*/
/*	Load Svg Images from directory                         		*/
/*										*/
/********************************************************************************/

private void loadSvgImages(File root)
{
    svg_topics = new TreeSet<>();
    for (File topicfile : root.listFiles()) {
       if (!topicfile.isDirectory()) continue;
       if (SKIP_TOPICS.contains(topicfile.getName())) continue;
       SvgTopicData std = new SvgTopicData(topicfile);
       svg_topics.add(std);
       for (File svgfile : topicfile.listFiles()) {
	  if (svgfile.getName().endsWith(".svg")) {
	     SvgData sd = new SvgData(svgfile);
	     std.addItem(sd);
	   }
	}
     }
}



/********************************************************************************/
/*                                                                              */
/*      Define user image                                                       */
/*                                                                              */
/********************************************************************************/

String saveUserImage(Number uid,String name,String typ,String url,String data,
      String desc,boolean border)
{
   IQsignDatabase db = iqsign_main.getDatabaseManager();
   
   String file = null;
   if (url == null) {
      int idx = data.indexOf(",");
      if (idx > 0) data = data.substring(idx+1);
      byte [] buf = Base64.getDecoder().decode(data);
      File f1 = getImageFileName(uid,name,typ);
      file = f1.getPath();
      try (FileOutputStream ots = new FileOutputStream(f1)) {
         ots.write(buf);
       }
      catch (IOException e) {
         return "Problem writing file";
       }
    }
   db.saveOrUpdateUserImage(uid,name,file,url,desc,border);
   return null;
}


private File getImageFileName(Number uid,String name,String typ)
{
   String ran = IQsignMain.randomString(16);
   File base = getImagesDirectory();
   String nam = "image_" + uid + "_" + name + "_" + ran + "." + typ;
   File f1 = new File(base,nam);
   return f1;
}



/********************************************************************************/
/*                                                                              */
/*      Return set of images to browser                                         */
/*                                                                              */
/********************************************************************************/ 

JSONArray getImageSet(Number uid,boolean border,boolean svg)
{
   JSONArray rslt = new JSONArray();
   
   if (border) {
      getSvgImageSet(uid,true,rslt);
      getUserImageSet(uid,true,rslt);
    }
   else if (svg) {
      getSvgImageSet(uid,false,rslt);
    }
   else {
      getUserImageSet(uid,false,rslt);
    }
   
   return rslt;
}


JSONArray getUserImageSet(Number uid,boolean border,JSONArray rslt)
{
   IQsignDatabase db = iqsign_main.getDatabaseManager();
   List<IQsignImage> imgs = db.findImages(uid,border); 
   for (IQsignImage img : imgs) {
      String url = img.getUrl();
      String file = img.getFile();
      if (url == null && file != null) {
         File f = new File(file);
         url = IMAGE_URL_PREFIX + f.getName();
       }
      JSONObject jo = buildJson("name",img.getName(),
            "description",img.getDescription(),
            "imagestring",img.getInsertion(), 
            "issvg",false,
            "url",url);
      rslt.put(jo);           
    }
   
   return rslt;
}


JSONArray getSvgImageSet(Number uid,boolean border,JSONArray arr)
{
   for (SvgTopicData topdata : svg_topics) {
      if (border && !topdata.getName().equals("borders")) continue;
      if (!border && topdata.getName().equals("borders")) continue;
      for (SvgData svd : topdata.getDataItems()) {
         JSONObject jo = buildJson("name",svd.getName(),
               "description",svd.getDescription(),
//             "svg",svd .getSvg(),
               "imagestring",svd.getInsertion(),
               "issvg",true,
               "url",svd.getUrl());
         arr.put(jo);      
       }
    }
      
   return arr;
}



/********************************************************************************/
/*										*/
/*	Update default images							*/
/*										*/
/********************************************************************************/

void updateDefaultImages()
{
   File fn = iqsign_main.getDefaultImagesFile();
   updateDefaultImages(fn,false);
   File bfn = iqsign_main.getDefaultBordersFile();
   updateDefaultImages(bfn,true);
}



private void updateDefaultImages(File defaultfile,boolean border)
{
   if (defaultfile == null || !defaultfile.exists()) return;
   if (defaultfile.lastModified() < last_update) return;
   
   try (BufferedReader br = new BufferedReader(new FileReader(defaultfile))) {
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 ln = ln.trim();
	 if (ln.isEmpty() || ln.startsWith("#") || ln.startsWith("/")) continue;
	 int idx = ln.indexOf(":");
	 if (idx < 0) continue;
	 String name = ln.substring(0,idx).trim();
     	 String file = ln.substring(idx+1).trim();
         String desc = null;
         String url = null;
         int idx1 = file.indexOf(",");
         if (idx1 > 0) {
            desc = file.substring(idx1+1).trim();
            file = file.substring(0,idx1).trim();
          }
         if (file.startsWith("http:") || 
               file.startsWith("https:") ||
               file.startsWith("ftp:")) {
            url = file;
            file = null;
          }
         if (file != null) {
            File f = new File(file);
            if (!f.isAbsolute()) {
               File f1 = getImagesDirectory();
               File f2 = new File(f1,f.getPath());
               file = f2.getAbsolutePath();
             }
          }
	 iqsign_main.getDatabaseManager().saveOrUpdateImage(name,
               file,url,desc,border); 
       }
    } 
   catch (IOException e) {
      IvyLog.logE("IQSIGN",
            "Problem reading default images/borders file " + defaultfile,e);
    }
}



private File getImagesDirectory()
{
   File f1 = new File(iqsign_main.getBaseDirectory(),"savedimages");
   
   return f1;
}




/********************************************************************************/
/*										*/
/*	Data for SVG image							*/
/*										*/
/********************************************************************************/

private final class SvgTopicData implements Comparable<SvgTopicData> {

   private String topic_name;
   private Set<SvgData> topic_items;

   SvgTopicData(File dir) {
      topic_name = dir.getName();
      topic_items = new TreeSet<>();
    }

   @Override public int compareTo(SvgTopicData sd) {
      return topic_name.compareTo(sd.topic_name);
    }

   void addItem(SvgData sd) {
      topic_items.add(sd);
    }
   
   String getName() {
      return topic_name; 
    }
   
   Collection<SvgData> getDataItems() {
      return topic_items;
    }

}	// end of inner class SvgTopicData





private final class SvgData implements Comparable<SvgData> {

   private String svg_name;
   private String svg_url;
   private String svg_topic;

   SvgData(File f) {
      String fnm = f.getName();
      String d = f.getParentFile().getName();
      int idx = fnm.lastIndexOf(".");
      svg_name = fnm.substring(0,idx);
      svg_url = SVG_URL_PREFIX + d + "/" + fnm;
      svg_topic = d;
    }

   @Override public int compareTo(SvgData sd) {
      return svg_name.compareTo(sd.svg_name);
    }
   
   String getName() {
      return svg_name;
    }
   
   String getDescription() {
      return svg_topic + " : " + svg_name;
    }
   
   String getUrl() {
      return svg_url;
    }
   
   String getInsertion() {
      return "@ sv-" + svg_name;
    }

}	// end of inner class SvgData



}	// end of class IQsignImages




/* end of IQsignImages.java */

