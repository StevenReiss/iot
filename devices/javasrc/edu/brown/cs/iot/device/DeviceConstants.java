/********************************************************************************/
/*										*/
/*		DeviceConstants.java						*/
/*										*/
/*	Constants for simpole generic devices for CATRE 			*/
/*										*/
/********************************************************************************/
/*	Copyright 2023 Brown University -- Steven P. Reiss			*/
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/




package edu.brown.cs.iot.device;




public interface DeviceConstants
{

/**
 *	URL for CEDES driver support
 **/

String BASE_URL = "https://sherpa.cs.brown.edu:3333/generic/";

String CONFIG_DIR = "sherpa";
String CONFIG_FILE = "generic.json";
String NAME_FILE = "device_";
String CONFIG_UID = "auth_uid";
String CONFIG_PAT = "auth_pat";
String DEVICE_UID = "device_uid";

String LOCK_DIR = ".locks";

long PING_TIME = 30000;
long ACCESS_TIME = 60000;

}	// end of interface DeviceConstants





/* end of DeviceConstants.java */
