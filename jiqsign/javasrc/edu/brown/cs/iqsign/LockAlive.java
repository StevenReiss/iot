/********************************************************************************/
/*                                                                              */
/*              LockAlive.java                                                  */
/*                                                                              */
/*      Keep file locking active on a machine                                   */
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.util.Date;

public class LockAlive
{



/********************************************************************************/
/*                                                                              */
/*      Main Program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   File f1 = new File(".lockalive");
   f1.deleteOnExit();
   
   FileOutputStream fos1 = setupFile(f1);
   lock(f1,fos1);
   
   for (int i = 1; ; ++i) {
      File f2 = new File(".lockalive" + "_" + i);
      f2.delete();
      FileOutputStream fos2 = setupFile(f2);
      FileLock lock2 = tryLock(f2,fos2);
      try {
         Thread.sleep(60000);
       }
      catch (InterruptedException e) { }
      unlock(lock2);
      f2.delete();
      System.err.println("LockAlive: Lock/Unlock at " + (new Date()));
    }
}


private static FileOutputStream setupFile(File f)
{
   f.setWritable(true,false);
   try {
      FileOutputStream outs = new FileOutputStream(f);
      f.setWritable(true,false); 
      return outs;
    }
   catch (IOException e) {
      System.err.println("LockAlive: Problem setting up file " + f);
    }
   return null;
}

private static FileLock lock(File f,FileOutputStream file)
{
   FileLock lock = null;
   for (int i = 0; i < 256; ++i) {
      try {
         lock = file.getChannel().lock();
         return lock;
       }
      catch (FileLockInterruptionException e) { }
      catch (ClosedChannelException e) {
         System.err.println("LockAlive: Lock file closed: " + f);  
         return null;
       }
      catch (IOException e) {
         e.printStackTrace();
         System.err.println("LockAlive: File lock failed for " + f + ": " + e);
       }
    }
   
   return null;
}


private static FileLock tryLock(File f,FileOutputStream file)
{
   try {
      FileLock lock = file.getChannel().tryLock();
      return lock;
    }
   catch (IOException e) {
      System.err.println("IVY: File lock failed for " + f + ": " + e);
      e.printStackTrace();
    }
   
   return null;
}




private static  void unlock(FileLock lock)
{
   if (lock == null) return;
   
   try {
      lock.release();
    }
   catch (IOException e) {
      System.err.println("IVY: file unlock failed: " + e);
    }
}


}       // end of class LockAlive




/* end of LockAlive.java */

