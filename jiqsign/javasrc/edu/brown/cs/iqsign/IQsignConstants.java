/********************************************************************************/
/*										*/
/*		IQsignConstants.java						*/
/*										*/
/*	Constants for iQsign server						*/
/*										*/
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

import org.json.JSONArray;
import org.json.JSONObject;

public interface IQsignConstants
{

long CLEANUP_DELAY = 1000*60*60;
long SESSION_TIMEOUT = 1000*60*60*24*3;

String WEB_DIRECTORY_FILE = "webdirectory";

int HTTP_PORT = 3335;
int HTTPS_PORT = 3336;

String SVG_URL_PREFIX = "/rest/svg/";


/********************************************************************************/
/*                                                                              */
/*     JSON builders                                                            */
/*                                                                              */
/********************************************************************************/

default Object buildJson(Object... val)
{
   JSONObject rslt = new JSONObject();
   
   if (val.length > 1) {
      for (int i = 0; i+1 < val.length; i += 2) {
         String key = val[i].toString();
         Object v = val[i+1];
         rslt.put(key,v);
       }
    }
   
   return rslt;
}

default JSONArray buildJsonArray(Object... val)
{
   JSONArray rslt = new JSONArray();
   for (Object v : val) {
      rslt.put(v);
    }
   
   return rslt;
}



   
}	// end of interface IQsignConstants



/* end of IQsignConstants.java */
