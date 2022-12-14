/********************************************************************************/
/*                                                                              */
/*              CatprogConditionParameter.java                                  */
/*                                                                              */
/*      Handle conditions of PARAMETER = VALUE (or PARAMETER)                   */
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
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreReferenceListener;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreWorld;

class CatprogConditionParameter extends CatprogCondition 
      implements CatreDeviceListener, CatreReferenceListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

enum Operator { EQL, NEQ, GTR, LSS, GEQ, LEQ };

private CatreParameterRef param_ref;
private Object          for_state;
private Boolean 	is_on;
private boolean 	is_trigger;
private CatreDevice     last_device;
private boolean         needs_name;
private Operator        check_operator;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatprogConditionParameter(CatreProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);
   
   needs_name = false;
   last_device = null;
   
   setConditionName();
   
   param_ref.initialize();
   
   setValid(param_ref.isValid());
   
   is_on = null;
}



private void setConditionName()
{
   if (!needs_name && getName() != null && !getName().equals("")) return;
   
   needs_name = false;
   
   String dnm = param_ref.getDeviceId();
   if (param_ref.isValid()) {
      dnm = param_ref.getDevice().getName();
    }
   else needs_name = true;
   
   if (is_trigger) {
      setName(dnm + "." + param_ref.getParameterName() +  "->" + for_state);
    }
   else {
      setName(dnm + param_ref.getParameterName() + "=" + for_state);
    }
}


private static String getUniqueName(String devid,String pname,Operator op,Object s,boolean trig)
{
   return devid + "_" + pname + "_" +  op + "_" + s.toString() + "_" + trig;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public void getDevices(Collection<CatreDevice> rslt)
{
   if (param_ref.isValid()) rslt.add(param_ref.getDevice());
}


@Override public void setTime(CatreWorld world)
{ }



@Override public boolean isTrigger()
{
   return is_trigger;
}


public Object getState()                      { return for_state; }


private CatrePropertySet getResultProperties() 
{
   CatrePropertySet ps = getUniverse().createPropertySet();
   ps.put(param_ref.getParameterName(),for_state);
   return ps;
}



/********************************************************************************/
/*										*/
/*	Handle state changes							*/
/*										*/
/********************************************************************************/

@Override public void stateChanged(CatreWorld w)
{
   if (!param_ref.isValid()) return;
   
   if (!param_ref.getDevice().isEnabled()) {
      if (is_on == null) return;
      if (is_on == Boolean.TRUE && !is_trigger) fireOff(w);
      is_on = null;
    }
   Object cvl = param_ref.getDevice().getValueInWorld(param_ref.getParameter(),w);
   boolean rslt = computeResult(cvl);
   if (is_on != null && rslt == is_on && w.isCurrent()) return;
   is_on = rslt;
   
   CatreLog.logI("CATPROG","CONDITION: " + getName() + " " + is_on);
   if (rslt) {
      if (is_trigger) fireTrigger(w,getResultProperties());
      else fireOn(w,getResultProperties());
    }
   else if (!is_trigger) fireOff(w);
}


private boolean computeResult(Object cvl)
{
   Object tgt = param_ref.getParameter().normalize(for_state);
   
   switch (check_operator) {
      case EQL :
         if (tgt == null) return cvl == null;
         return tgt.equals(cvl);
      case NEQ :
         if (tgt == null) return cvl != null;
         return !tgt.equals(cvl);
    }
   if (cvl instanceof Number && tgt instanceof Number) {
      double v1 = ((Number) tgt).doubleValue();
      double v0 = ((Number) cvl).doubleValue();
      switch (check_operator) {
         case EQL :
            return v0 == v1;
         case NEQ :
            return v0 != v1;
         case GTR :
            return v0 > v1;
         case GEQ :
            return v0 >= v1;
         case LEQ :
            return v0 <= v1;
         case LSS :
            return v0 < v1;
       }
    }
   
   return false;
}

@Override public void referenceValid(boolean fg)
{
   if (fg == isValid()) return;
   
   if (needs_name) setConditionName();
   
   setValid(fg);
   
   fireValidated();
}


@Override protected void localStartCondition()
{
   if (param_ref == null) return;
   
   last_device = param_ref.getDevice();
   last_device.addDeviceListener(this);
}


@Override protected void localStopCondition() 
{
   if (last_device != null) last_device.removeDeviceListener(this);
   last_device = null;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("TYPE","Parameter");
   rslt.put("PARAMREF",param_ref.toJson());
   rslt.put("STATE",for_state.toString());
   rslt.put("TRIGGER",is_trigger);
   rslt.put("OPERATOR",check_operator);
   
   return rslt;
}

@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   param_ref = getSavedSubobject(cs,map,"PARAMREF",this::createParamRef,param_ref);
   for_state = getSavedString(map,"STATE",null);
   is_trigger = getSavedBool(map,"TRIGGER",is_trigger);
   check_operator = getSavedEnum(map,"OPERATOR",Operator.EQL);
   
   setUID(getUniqueName(param_ref.getDeviceId(),param_ref.getParameterName(),
         check_operator,for_state,is_trigger));
}



private CatreParameterRef createParamRef(CatreStore cs,Map<String,Object> map)
{
   return getUniverse().createParameterRef(this,cs,map);
}




}       // end of class CatprogConditionParameter




/* end of CatprogConditionParameter.java */

