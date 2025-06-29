/********************************************************************************/
/*										*/
/*		CatbridgeBase.java						*/
/*										*/
/*	Base implementation of a CatreBridge					*/
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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.catre.catre.CatreActionException;
import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreBridgeAuthorization;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreJson;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterRef;
import edu.brown.cs.catre.catre.CatreProgramListener;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTransition;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUniverseListener;
import edu.brown.cs.catre.catre.CatreUser;
import edu.brown.cs.catre.catre.CatreUtil;

abstract class CatbridgeBase implements CatreBridge, CatbridgeConstants,
      CatreProgramListener, CatreUniverseListener, CatreJson
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<CatreUniverse,CatbridgeBase> known_instances;

protected CatreUniverse 	for_universe;
protected Map<String,CatreDevice> device_map;
protected String		bridge_id;
protected boolean               is_registered;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatbridgeBase()
{
   for_universe = null;
   device_map = null;
   known_instances = new HashMap<>();
   bridge_id = null;
   is_registered = false;
}



protected CatbridgeBase(CatbridgeBase base,CatreUniverse cu)
{
   for_universe = cu;
   device_map = new HashMap<>();
   known_instances = null;
   bridge_id = CatreUtil.randomString(24);
   is_registered = false;
   
   if (cu.getProgram() != null) {
      CatreLog.logD("CATBRIDGE","Add program listener for " + getName() + " " + bridge_id);
      cu.getProgram().addProgramListener(this);
    }
   else {
      CatreLog.logD("CATBRIDGE","Add universe listener for " + getName() + " " + bridge_id);
      cu.addUniverseListener(this);
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

CatreUniverse getUniverse()		{ return for_universe; }

@Override public String getBridgeId()	{ return bridge_id; }


@Override public JSONObject getBridgeInfo()
{
   JSONObject obj = buildJson("BRIDGE",getName());
   
   return obj;
}




/********************************************************************************/
/*										*/
/*	Methods to create an instance						*/
/*										*/
/********************************************************************************/

protected CatbridgeBase createBridge(CatreUniverse u)
{
   if (for_universe != null) return null;

   CatbridgeBase cb = known_instances.get(u);

   CatreUser cu = u.getUser();
   if (cu == null) return null;

   CatreBridgeAuthorization ba = cu.getAuthorization(getName());
   if (ba == null) {
      if (cb != null) known_instances.remove(u);
      if (u.getProgram() != null) {
         u.getProgram().removeProgramListener(this);
       }
      else {
         u.removeUniverseListener(this);
       }
      return null;
    }

   if (cb == null) {
      cb = createInstance(u,ba);
      known_instances.put(u,cb);
    }

   return cb;
}


protected abstract CatbridgeBase createInstance(CatreUniverse u,CatreBridgeAuthorization auth);



/********************************************************************************/
/*										*/
/*	Methods to talk to CEDES						*/
/*										*/
/********************************************************************************/

protected void registerBridge()
{
   CatreLog.logD("CATBRIDGE","Register bridge " + getBridgeId());
   
   Map<String,Object> authdata = getAuthData();

   if (useCedes()) {
      JSONObject rslt = sendCedesMessage("catre/addBridge",
            "authdata",new JSONObject(authdata));
      CatreLog.logD("CATBRIDGE","Registration result: " + rslt);
    }
   
   is_registered = true;
}


protected Map<String,Object> getAuthData()
{
   return new HashMap<>();
}



protected synchronized JSONObject sendCedesMessage(String cmd,Object... data)
{
   Map<String,Object> map = new HashMap<>();
   map.put("bridge",getName());
   map.put("bridgeid",getBridgeId());
   for (int i = 0; i < data.length-1; i += 2) {
      String key = data[i].toString();
      Object val = data[i+1];
      map.put(key,val);
    }
   
   if (!cmd.contains("/")) {
      String nm = getName().toLowerCase();
      cmd = nm + "/" + cmd;
    }
   
   return CatbridgeFactory.sendCedesMessage(cmd,map,this);
}


@Override public void programUpdated()
{
   CatreLog.logD("CATBRIDGE","Program updated " + getName() + " " + useCedes() + " " +
         for_universe);
   
   if (!useCedes()) return;
   if (for_universe == null) return;
   
   Set<CatreParameterRef> refs = for_universe.getProgram().getActiveSensors();
   CatreLog.logD("CATBRIDGE","Active sensors " + refs);
   
   JSONArray use = new JSONArray();
   for (CatreParameterRef ref : refs) {
      CatreDevice cd = ref.getDevice();
      if (cd == null) {
         CatreLog.logD("CATBRIDGE","Device not found for parameter ref " + ref);
         continue;
       }
      if (cd.getBridge() == null) continue;
      CatreLog.logD("CATBRIDGE","Check sensor " + cd.getName() + " " + cd.getBridge().getName());
      if (!getName().equals(cd.getBridge().getName())) continue;
      CatreParameter cp = ref.getParameter();
      if (cp == null) continue;
      if (!cp.isSensor()) continue;
      use.put(buildJson("DEVICE",ref.getDeviceId(),"PARAMETER",ref.getParameterName()));
    }
   
   CatreLog.logD("CATBRIDGE","Update sensor for " + getName() + " " + use.toString(2));
   if (use.isEmpty()) return;
   
   sendCedesMessage("catre/activesensors","active",use,"uid",getUserId());
}



@Override public void universeSetup()  
{
   CatreLog.logD("CATBRIDGE","Universe setup " + getName() + " " + for_universe.getName() + 
         " " + for_universe.getProgram());
   
   if (for_universe != null && for_universe.getProgram() != null) {
      for_universe.getProgram().addProgramListener(this);
      for_universe.removeUniverseListener(this);
      programUpdated();
    }
}



/********************************************************************************/
/*										*/
/*	Methods to update devices						*/
/*										*/
/********************************************************************************/

protected void handleDevicesFound(JSONArray devs)
{
   CatreController cc = for_universe.getCatre();
   CatreStore cs = cc.getDatabase();

   Map<String,CatreDevice> newdevmap = new LinkedHashMap<>();
   for (int i = 0; i < devs.length(); ++i) {
      JSONObject devobj = devs.getJSONObject(i);
      Map<String,Object> devmap = devobj.toMap();
      
      CatreLog.logD("CATBRIDGE","WORK ON DEVICE " + devobj.toString(2));
      CatreDevice newcd = createDevice(cs,devmap);
      if (newcd != null && !newcd.validateDevice()) {
         CatreLog.logD("CATBRIDGE","Device is not valid");
         newcd = null;
       }
      
      if (newcd != null) {
         Map<String,Object> jsonmap = newcd.toJson();
         JSONObject jo = new JSONObject(jsonmap);
	 CatreLog.logD("CATBRIDGE","ADD DEVICE " + newcd.getDeviceId() + "\n" +
               jo.toString(2));
	 newdevmap.put(newcd.getDeviceId(),newcd);
       }
      else {
         CatreLog.logD("CATBRIDGE","DEVICE not found or not valid");
       }
    }

   device_map = newdevmap;

   for_universe.updateDevices(this,true);
}


@Override public Collection<CatreDevice> findDevices()
{
   return device_map.values();
}



protected CatreDevice findDevice(String id)
{
   return device_map.get(id);
}


protected String getUserId()		        { return null; }

protected boolean useCedes()                    { return true; }


/********************************************************************************/
/*                                                                              */
/*      Update parameter values                                                 */
/*                                                                              */
/********************************************************************************/

@Override public JSONObject updateParameterValues(CatreDevice dev) 
{
   JSONObject rslt = sendCedesMessage("catre/parameter",
         "deviceid",dev.getDeviceId(),
         "uid",getUserId());
   
   return rslt;
}


/********************************************************************************/
/*                                                                              */
/*      Handle events --parameter value changes                                 */
/*                                                                              */
/********************************************************************************/

protected void handleEvent(JSONObject evt)
{ 
   EventHandler hdlr = new EventHandler(evt);
   for_universe.getCatre().submit(hdlr);
}



private class EventHandler implements Runnable {
   
   private JSONObject for_event;
   
   EventHandler(JSONObject evt) {
      for_event = evt;
    }
   
   @Override public void run() {
      CatreLog.logD("CATBRIDGE","Handle event " + for_event.toString(2));
      String typ = for_event.getString("TYPE");
      CatreDevice dev = for_universe.findDevice(for_event.getString("DEVICE"));
      if (dev == null) {
         CatreLog.logD("CATBRIDGE","Device not found for event");
         return;
       }
      
      switch (typ) {
         case "PARAMETER" :
            CatreParameter param = dev.findParameter(for_event.getString("PARAMETER"));
            if (param == null) {
               CatreLog.logD("CATBRIDGE","Parameter not found for event");
               return;
             }
            Object val = for_event.get("VALUE");
            if (val == JSONObject.NULL) val = null;
            try {
               dev.setParameterValue(param,val);
             }
            catch (CatreActionException e) {
               CatreLog.logE("CATBRIDGE","Problem with parameter event",e);
             }
            break;
         default :
            CatreLog.logD("CATBRIDGE","Unknown event type " + typ);
            break;
       }
    }
   
}       // end of inner class EventHandler


/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public CatreTransition createTransition(CatreDevice device,CatreStore cs,Map<String,Object> map)
{
   // let device create it, nothing extra needed
   return null; 
}



@Override public void applyTransition(CatreDevice dev,CatreTransition t,Map<String,Object> values)
	throws CatreActionException
{
   
   if (!is_registered) {
      CatreLog.logI("CATBRIDGE","Command before device is registered");
      return;       
    }
   
   sendCedesMessage("catre/command",
         "deviceid",dev.getDeviceId(),
         "uid",getUserId(),
         "command",t.getName(),
         "values",values);
}




}	// end of class CatbridgeBase




/* end of CatbridgeBase.java */

