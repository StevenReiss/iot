/********************************************************************************/
/*                                                                              */
/*              CatprogConditionRange.java                                      */
/*                                                                              */
/*      Check the range of a value-based parameter                              */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.catre.catprog;

import java.util.Collection;
import java.util.Map;

import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceListener;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameterRef;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreReferenceListener;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreWorld;

class CatprogConditionRange extends CatprogCondition implements CatreDeviceListener,
      CatreReferenceListener
{

/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreParameterRef param_ref;
private Number		low_value;
private Number		high_value;
private Boolean 	is_on;
private boolean 	is_trigger;
private CatreDevice     last_device;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatprogConditionRange(CatprogProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);
      
   is_on = null;
   last_device = null;
   
   param_ref.initialize();
   
   setValid(param_ref.isValid());
}



private static String getUniqueName(String devid,String pname,Number low,Number high,boolean trigger)
{
   return devid + "_" + pname + "_" + low + "_" + high + "_" + trigger;
}
      


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public void getDevices(Collection<CatreDevice> rslt)
{
   if (isValid()) rslt.add(param_ref.getDevice());
}



@Override public boolean isTrigger()			{ return is_trigger; }



/********************************************************************************/
/*										*/
/*	Handle state changes							*/
/*										*/
/********************************************************************************/

@Override protected void localStartCondition()
{
   last_device = param_ref.getDevice();
   last_device.addDeviceListener(this);
}

@Override protected void localStopCondition()
{
   if (last_device != null) last_device.removeDeviceListener(this);
   last_device = null;
}


@Override public void stateChanged(CatreWorld w)
{
   if (!isValid()) return;
   
   if (!param_ref.getDevice().isEnabled()) {
      if (is_on == null) return;
      if (is_on == Boolean.TRUE) fireOff(w);
      is_on = null;
    }
   
   Object cvl = param_ref.getDevice().getValueInWorld(param_ref.getParameter(),w);
   boolean rslt = false;
   if (cvl != null && cvl instanceof Number) {
      Number nvl = (Number) cvl;
      double vl = nvl.doubleValue();
      if (low_value == null || vl >= low_value.doubleValue()) {
	 if (high_value == null || vl <= high_value.doubleValue()) {
	    rslt = true;
	  }
       }
    }
   
   // don't trigger on initial setting
   if (is_on == null && is_trigger) is_on = rslt;
   
   if (is_on != null && rslt == is_on && w.isCurrent()) return;
   is_on = rslt;
   
   CatreLog.logI("CATPROG","CONDITION: " + getName() + " " + is_on);
   
   if (is_trigger) {
      fireTrigger(w,getResultProperties(cvl));
    }
   else if (rslt) {
      fireOn(w,getResultProperties(cvl));
    }
   else {
      fireOff(w);
    }
}



private CatrePropertySet getResultProperties(Object val)
{
   CatrePropertySet ps = getUniverse().createPropertySet();
   ps.put(param_ref.getParameterName(),val.toString());
   return ps;
}


@Override public void setTime(CatreWorld w)
{ }



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("TYPE","Range");
   rslt.put("PARAMREF",param_ref.toJson());
   if (low_value != null) rslt.put("LOW",low_value);
   if (high_value != null) rslt.put("HIGH",high_value);
   rslt.put("TRIGGER",is_trigger);
   
   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   param_ref = getSavedSubobject(cs,map,"PARAMREF",this::createParamRef,param_ref);
   String v = getSavedString(map,"LOW",null);
   if (v != null) low_value = Double.valueOf(v);
   else low_value = null;
   v = getSavedString(map,"HIGH",null);
   if (v != null) high_value = Double.valueOf(v);
   else high_value = null;
   is_trigger = getSavedBool(map,"TRIGGER",is_trigger);
   
   setUID(getUniqueName(param_ref.getDeviceId(),param_ref.getParameterName(),
         low_value,high_value,is_trigger));
}


private CatreParameterRef createParamRef(CatreStore cs,Map<String,Object> map)
{
   return getUniverse().createParameterRef(this,cs,map);
}



@Override public String toString() {
   return getName();
}



}       // end of class CatprogConditionRange




/* end of CatprogConditionRange.java */

