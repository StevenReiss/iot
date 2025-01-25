/********************************************************************************/
/*                                                                              */
/*              IQsinMaker.java                                                 */
/*                                                                              */
/*      Handle creating sign image for a sign                                   */
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

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.json.JSONObject;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;

public class IQsignMaker implements IQsignConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,MakerQueue> work_queue;
private Map<String,MakerQueue> preview_queue;
private ThreadPoolExecutor thread_pool;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

IQsignMaker(IQsignMain main)
{
   work_queue = new HashMap<>();
   preview_queue = new HashMap<>();
   thread_pool = new ThreadPoolExecutor(4,100,
         5,  TimeUnit.MINUTES,new LinkedBlockingQueue<>());
}



/********************************************************************************/
/*                                                                              */
/*      Submit a task                                                           */
/*                                                                              */
/********************************************************************************/

public void requestUpdateNone(IQsignSign sign,boolean counts,boolean preview)
{
   MakerTask task = new MakerTask(sign,counts,preview,null);
   thread_pool.execute(task);
}


public void requestUpdate(IQsignSign sign,
      boolean counts,boolean preview,Consumer<Boolean> next)
{
   MakerTask task = new MakerTask(sign,counts,preview,next);
   thread_pool.execute(task);
}



/********************************************************************************/
/*                                                                              */
/*      Task to build a sign                                                    */
/*                                                                              */
/********************************************************************************/

private class MakerTask implements Runnable {
   
   private IQsignSign for_sign;
   private boolean do_counts;
   private boolean is_preview;
   private Consumer<Boolean> post_run;
   
   MakerTask(IQsignSign sign,boolean counts,boolean preview,Consumer<Boolean> next) {
      for_sign = sign;
      do_counts = counts;
      is_preview = preview;
      post_run = next;
    }
   
   @Override public void run() {
      boolean ok = false;
      try {
         makeImage();
         ok = true;
       }
      catch (Throwable t) {
         IvyLog.logE("Problem making sign image",t);
       }
      finally {
         if (post_run != null) post_run.accept(ok);
       }
    }
   
   private void makeImage() {
      MakerQueue queue = null;
      String uid = for_sign.getUserId();
      synchronized (work_queue) {
         if (is_preview) {
            queue = preview_queue.get(uid);
            if (queue == null) {
               queue = new MakerQueue();
               preview_queue.put(uid,queue);
             }
          }
         else {
            queue = work_queue.get(uid);
            if (queue == null) {
               queue = new MakerQueue();
               work_queue.put(uid,queue);
             }
          }
       }
      if (!queue.waitForReady()) return;
      
      JSONObject pass = buildJson("width",for_sign.getWidth(),
           "height",for_sign.getHeight(),
           "userid",for_sign.getUserId(), 
           "contents",for_sign.getContents(),
           "outfile",for_sign.getImageFile(is_preview),
           "counts",do_counts);
      String datastr = pass.toString();
      byte [] datab = datastr.getBytes();
      IvyLog.logD("Start sign update socket");
      try (Socket s = new Socket("localhost",MAKER_PORT)) {
         s.getOutputStream().write(datab);
         String cnts = IvyFile.loadFile(s.getInputStream());
         JSONObject obj = new JSONObject(cnts);
         IvyLog.logD("RESULT OF MAKER TASK: " + obj.toString(2));
       }
      catch (IOException e) {
         IvyLog.logE("Maker task failed",e);
       }
      
      queue.finishWork();
    }
   
}       // end of inner class MakerTask



/********************************************************************************/
/*                                                                              */
/*      Queue data                                                              */
/*                                                                              */
/********************************************************************************/

private final class MakerQueue {
   
   private boolean is_busy;
   private int work_counter;
   
   MakerQueue() {
      work_counter = 0;
      is_busy = false;
    }
   
   synchronized boolean waitForReady() {
      int c = ++work_counter;
      while (is_busy && work_counter == c) {
         try {
            notifyAll();
            wait(5000);
          }
         catch (InterruptedException e) { }
       }
      if (work_counter != c) {
         IvyLog.logD("SIGN UPDATE SKIP");
         return false;
       }
      is_busy = true;
      
      return true;
    }
   
   synchronized void finishWork() {
      is_busy = false;
      notifyAll();
    }
   
}       // end of inner class MakerQueue

}       // end of class IQsinMaker




/* end of IQsinMaker.java */

