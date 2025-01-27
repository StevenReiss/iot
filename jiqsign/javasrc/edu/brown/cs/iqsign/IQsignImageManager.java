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
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

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
/*										*/
/*	<comment here>								*/
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
/*										*/
/*	Update default images							*/
/*										*/
/********************************************************************************/

void updateDefaultImages()
{
   File fn = iqsign_main.getDefaultImagesFile();
   if (fn == null || !fn.exists()) return;
   if (fn.lastModified() < last_update) return;

   try (BufferedReader br = new BufferedReader(new FileReader(fn))) {
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 ln = ln.trim();
	 if (ln.isEmpty() || ln.startsWith("#") || ln.startsWith("/")) continue;
	 int idx = ln.indexOf(":");
	 if (idx < 0) continue;
	 String name = ln.substring(0,idx).trim();
	 String file = ln.substring(idx+1).trim();
	 iqsign_main.getDatabaseManager().saveOrUpdateImage(name,file);
       }
    }
   catch (IOException e) {
      IvyLog.logE("IQSIGN","Problem reading default images file",e);
    }
}


/********************************************************************************/
/*										*/
/*	Data for SVG image							*/
/*										*/
/********************************************************************************/

private final class SvgTopicData implements Comparable<SvgTopicData> {

   private String topic_name;
   private File topic_directory;
   private Set<SvgData> topic_items;

   SvgTopicData(File dir) {
      topic_name = dir.getName();
      topic_directory = dir;
      topic_items = new TreeSet<>();
    }

   @Override public int compareTo(SvgTopicData sd) {
      return topic_name.compareTo(sd.topic_name);
    }

   void addItem(SvgData sd) {
      topic_items.add(sd);
    }

}	// end of inner class SvgTopicData





private final class SvgData implements Comparable<SvgData> {

   private String svg_name;
   private String svg_url;
   private String svg_topic;
   private String svg_path;

   SvgData(File f) {
      String fnm = f.getName();
      String d = f.getParentFile().getName();
      int idx = fnm.lastIndexOf(".");
      svg_name = fnm.substring(0,idx);
      svg_url = SVG_URL_PREFIX + d + "/" + fnm;
      svg_topic = d;
      svg_path = f.getPath();
    }

   @Override public int compareTo(SvgData sd) {
      return svg_name.compareTo(sd.svg_name);
    }

}	// end of inner class SvgData



}	// end of class IQsignImages




/* end of IQsignImages.java */

