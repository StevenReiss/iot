/********************************************************************************/
/*                                                                              */
/*              IQsignDefaults.java                                             */
/*                                                                              */
/*      Manage the set of default signs                                         */
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
import java.io.IOException;
import java.util.StringTokenizer;

import edu.brown.cs.ivy.file.IvyFile;

class IQsignDefaults implements IQsignConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IQsignMain      iqsign_main;
private long            last_update;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

IQsignDefaults(IQsignMain main)
{
   iqsign_main = main;
   last_update = 0;
   
   updateDefaults();
}


/********************************************************************************/
/*                                                                              */
/*      Update default signs                                                    */
/*                                                                              */
/********************************************************************************/

void updateDefaults() 
{
   File f = iqsign_main.getDefaultSignsFile();
   long dlm = f.lastModified();
   if (dlm < last_update) return;
   
   String cnts = null;
   try {
     cnts = IvyFile.loadFile(f);
    }
   catch (IOException e) { }
   if (cnts == null) return;
   
   StringTokenizer tok = new StringTokenizer(cnts,"\n");
   String name = null;
   StringBuffer body = null;
   boolean eqok = true;
   while (tok.hasMoreTokens()) {
      String line = tok.nextToken();
      line = line.trim();
      if (line.isEmpty()) {
         eqok = true;
       }
      else if (line.startsWith("=") && eqok) {
         if (body == null) {
            saveSign(name,body,dlm);
            body = null;
          }
         name = line.substring(1).trim();
         eqok = false;
       }
      else {
         if (body == null) body = new StringBuffer();
         body.append(line);
         body.append("\n");
         eqok = false;
       }
    }
   if (body != null) {
      saveSign(name,body,dlm);
      body = null;
    }
   
   last_update = dlm;
}


private void saveSign(String name,StringBuffer body,long dlm)
{
   if (name == null || body == null || body.isEmpty()) return;
   
   iqsign_main.getDatabaseManager().saveOrUpdateSign(name,
         body.toString(),dlm);
}


}       // end of class IQsignDefaults




/* end of IQsignDefaults.java */

