/********************************************************************************/
/*										*/
/*		CatbridgeGeneric.java						*/
/*										*/
/*	Bridge to handle generic devices					*/
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




package edu.brown.cs.catre.catbridge;

import java.util.Map;

import org.json.JSONObject;

import edu.brown.cs.catre.catdev.CatdevDevice;
import edu.brown.cs.catre.catre.CatreActionException;
import edu.brown.cs.catre.catre.CatreBridgeAuthorization;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUtil;

/**
 *      This class implements the generic bridge for devices.   
 *
 *      You should set a JSON file in ~/.config/sherpa/generic.json with two
 *      String fields: auth_uid and auth_pat (for password).  If you don't
 *      set it up manually, it will be set up automatically with two random
 *      strings.  If you need to run the devices on multiple machines (e.g.
 *      the computer monitor), then the two files should have the same
 *      information in them.
 *
 *      Then this uid and pat should be used in configuring the bridge for
 *      this particular user.  (/bridge/add -- and the user interface for 
 *      this when it is ready).
 *
 **/

class CatbridgeGeneric extends CatbridgeBase
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		auth_uid;
private String		auth_pat;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatbridgeGeneric(CatreController cc)
{
   auth_uid = null;
   auth_pat = null;
}


CatbridgeGeneric(CatbridgeBase base,CatreUniverse u,CatreBridgeAuthorization ba)
{
   super(base,u);
   auth_uid = ba.getValue(0,"AUTH_UID");
   auth_pat = ba.getValue(0,"AUTH_PAT");
}



protected CatbridgeBase createInstance(CatreUniverse u,CatreBridgeAuthorization ba)
{
   return new CatbridgeGeneric(this,u,ba);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()		{ return "generic"; }


@Override public JSONObject getBridgeInfo()
{
   JSONObject rslt = super.getBridgeInfo();
   
   JSONObject f1 = buildJson("KEY","AUTH_UID","LABEL","User","VALUE",auth_uid,
         "HINT","auth_uid from ~/.config/sherpa/generic.json","TYPE","STRING");
   JSONObject f2 = buildJson("KEY","AUTH_PAT","LABEL","Password","VALUE",auth_pat,
         "HINT","auth_pat from ~/.config/sherpa/generic.json","TYPE","STRING");
   rslt.put("FIELDS",buildJsonArray(f1,f2));
   rslt.put("SINGLE",true);
   
   String desc = "You should set a JSON file in ~/.config/sherpa/generic.json with two " +
         "String fields: auth_uid and auth_pat (for password).  If you don't " +
         "set it up manually, it will be set up automatically with two random " +
         "strings.  If you need to run the devices on multiple machines (e.g. " +
         "the computer monitor), then the two files should have the same " +
         "information in them.\n" +
         "Then this uid and pat should be used in configuring the bridge for " +
         "this particular user.";
   rslt.put("HELP",desc);
                     
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Event methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override protected void handleEvent(JSONObject evt)
{
   String typ = evt.getString("TYPE");
   CatreDevice dev = for_universe.findDevice(evt.getString("DEVICE"));
   if (dev == null) {
      CatreLog.logD("CATBRIDGE","GENERIC EVENT DEVICE NOT FOUND " +
            evt.toString(2));
      return;
    }
   CatreLog.logD("CATBRIDGE","GENERIC EVENT " + typ + " " +
         dev.getDeviceId());
   
   switch (typ) {
      case "PARAMETER" :
	 CatreParameter param = dev.findParameter(evt.getString("PARAMETER"));
	 if (param == null) {
            CatreLog.logE("CATBRIDGE","Generic event parameter not found " + 
                  evt.getString("PARAMETER") + " for " + dev.getName());
            return;
          }
	 Object val = evt.opt("VALUE");
         if (val == null) {
            CatreLog.logE("CATBRIDGE","Generic event value  not found " + 
                  evt.getString("PARAMETER") + " for " + dev.getName());
            return;
          }
	 try {
	    dev.setParameterValue(param,val);
	  }
	 catch (CatreActionException e) {
	    CatreLog.logE("CATBRIDGE","Problem with parameter event",e);
	  }
	 break;
      default :
	 break;
    }
}


@Override protected Map<String,Object> getAuthData()
{
   Map<String,Object> rslt = super.getAuthData();

   rslt.put("uid",auth_uid);
   String p0 = CatreUtil.secureHash(auth_pat);
   String p1 = CatreUtil.secureHash(p0 + auth_uid);
   rslt.put("pat",p1);
   CatreLog.logD("CATBRIDGE","GENERIC AUTH " + auth_uid + " " + auth_pat + " " + p0 + " " + p1);

   return rslt;
}


@Override protected String getUserId()		{ return auth_uid; }



/********************************************************************************/
/*										*/
/*	Generic bridge device							*/
/*										*/
/********************************************************************************/

@Override public CatreDevice createDevice(CatreStore cs,Map<String,Object> map)
{
   CatdevDevice cd = new GenericDevice(this,cs,map);

   return cd;
}



private static class GenericDevice extends CatdevDevice {

   private long ping_time;
   
   GenericDevice(CatbridgeBase bridge,CatreStore cs,Map<String,Object> map) {
      super(bridge.getUniverse(),bridge);
      fromJson(cs,map);
    }
   
   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
      ping_time = getSavedLong(map,"PINGTIME",0);
    }
   
   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = super.toJson();
      rslt.put("PINGTIME",ping_time);
      return rslt;
    }

}

}	// end of class CatbridgeGeneric




/* end of CatbridgeGeneric.java */

