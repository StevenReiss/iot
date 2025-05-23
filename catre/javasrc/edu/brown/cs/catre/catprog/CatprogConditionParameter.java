/********************************************************************************/
/*										*/
/*		CatprogConditionParameter.java					*/
/*										*/
/*	Handle conditions of PARAMETER = VALUE (or PARAMETER)			*/
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




package edu.brown.cs.catre.catprog;

import java.util.List;
import java.util.Map;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceListener;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterRef;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreReferenceListener;
import edu.brown.cs.catre.catre.CatreStore;

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
private Object		for_state;
private Boolean 	is_on;
private boolean 	is_trigger;
private CatreDevice	last_device;
private boolean 	needs_name;
private Operator	check_operator;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatprogConditionParameter(CatprogProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);

   needs_name = false;
   last_device = null;

   setConditionName();

   param_ref.initialize();

   setValid(checkValid());

   is_on = null;
}


private CatprogConditionParameter(CatprogConditionParameter cc)
{
   super(cc);
   param_ref = cc.getUniverse().createParameterRef(this,cc.param_ref.getDeviceId(),
	 cc.param_ref.getParameterName());
   for_state = cc.for_state;
   is_trigger = cc.is_trigger;
   last_device = null;
   needs_name = false;	
   check_operator = cc.check_operator;
   param_ref.initialize();
   setValid(checkValid());
   is_on = null;
}


@Override public CatreCondition cloneCondition()
{
   return new CatprogConditionParameter(this);
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



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public boolean isTrigger()
{
   return is_trigger;
}


public Object getState()		      { return for_state; }


private CatrePropertySet getResultProperties()
{
   CatrePropertySet ps = getUniverse().createPropertySet();
   ps.put(param_ref.getParameterName(),for_state);
   return ps;
}


@Override CatreParameterRef getActiveSensor() 
{
   return param_ref;
}



/********************************************************************************/
/*                                                                              */
/*      Checking methods                                                        */
/*                                                                              */
/********************************************************************************/

private boolean checkValid()
{
   if (param_ref == null || !param_ref.isValid()) return false;
   
   CatreParameter cp = param_ref.getParameter();
   switch (cp.getParameterType()) {
      case ENUM :
         List<Object> vals = cp.getValues();
         if (!vals.contains(for_state)) {
            return false;
          }
         break;
      default:
         break;
    }
   
   return true;
}



@Override boolean contradicts(CatreCondition cc)
{
   if (cc instanceof CatprogConditionParameter) {
      CatprogConditionParameter ccp = (CatprogConditionParameter) cc;
      if (param_ref.isValid() && ccp.param_ref.isValid()) {
         if (param_ref.getDevice() == ccp.param_ref.getDevice()) {
            if (param_ref.getParameter() == ccp.param_ref.getParameter()) {
               if (check_operator == Operator.EQL && ccp.check_operator == Operator.EQL) {
                  if (!for_state.equals(ccp.for_state)) {
                     return true;
                   }
                }
               if (check_operator == Operator.NEQ && ccp.check_operator == Operator.EQL) {
                  if (for_state.equals(ccp.for_state)) {
                     return true;
                   }
                }
               if (check_operator == Operator.EQL && ccp.check_operator == Operator.NEQ) {
                  if (for_state.equals(ccp.for_state)) {
                     return true;
                   }
                }
               // check bounds for >=, <= etc
             }
          }
       }
    }
   
   return super.contradicts(cc);
}



/********************************************************************************/
/*										*/
/*	Handle state changes							*/
/*										*/
/********************************************************************************/

@Override public void stateChanged(CatreParameter p)
{
   CatreLog.logD("CATPROG","Condition state changed " + param_ref.isValid() + " " +
         param_ref.getDevice().isEnabled());
   
   if (!param_ref.isValid()) return;

   if (!param_ref.getDevice().isEnabled()) {
      if (is_on == null) return;
      if (is_on == Boolean.TRUE && !is_trigger) fireOff();
      is_on = null;
    }
   Object cvl = param_ref.getDevice().getParameterValue(param_ref.getParameter());
   boolean rslt = computeResult(cvl);
   CatreLog.logD("CATPROG","Condition result " + rslt + " " + is_on);
   if (is_on != null && rslt == is_on) return;
   is_on = rslt;

   CatreLog.logI("CATPROG","CONDITION: " + getName() + " " + is_on);
   if (rslt) {
      if (is_trigger) fireTrigger(getResultProperties());
      else fireOn(getResultProperties());
    }
   else if (!is_trigger) fireOff();
}


private boolean computeResult(Object cvl)
{
   Object tgt = param_ref.getParameter().normalize(for_state);
   
   CatreLog.logD("CATPROG","COMPUTE PARAMETER CONDITION " + 
         tgt + "=" + cvl);

   switch (check_operator) {
      case EQL :
	 if (tgt == null) return cvl == null;
         else if (cvl == null) return false;
         CatreLog.logD("CATPROG","CHECK EQL " + tgt.getClass() + " " + cvl.getClass());
	 return tgt.equals(cvl);
      case NEQ :
	 if (tgt == null) return cvl != null;
         else if (cvl == null) return false;
         CatreLog.logD("CATPROG","CHECK NEQ " + tgt.getClass() + " " + cvl.getClass());
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
   CatreLog.logD("CATPROG","Parameter reference valid " + 
         getName() + " " + fg + " " + isValid());
   
   if (fg == isValid()) return;

   if (needs_name) setConditionName();

   setValid(checkValid());

   fireValidated();
   
   CatreParameter cp = param_ref.getParameter();
   if (cp != null) stateChanged(cp);
}


@Override protected void localStartCondition()
{
   CatreLog.logD("CATPROG","Start parameter condition " + param_ref);
   
   if (param_ref == null) return;

   last_device = param_ref.getDevice();
   last_device.addDeviceListener(this);
   
   CatreParameter cp = param_ref.getParameter();
   if (cp != null) stateChanged(cp);
}


@Override protected void localStopCondition()
{
   if (last_device != null) last_device.removeDeviceListener(this);
   last_device = null;
   
   CatreParameter cp = param_ref.getParameter();
   if (cp != null) stateChanged(cp);
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
}



private CatreParameterRef createParamRef(CatreStore cs,Map<String,Object> map)
{
   return getUniverse().createParameterRef(this,cs,map);
}


@Override boolean isUndefined()
{
   if (param_ref == null) return true;
   if (param_ref.getDeviceId().equals("Undefined")) return true;
   if (param_ref.getParameterName().equals("Undefined")) return true;
   
   return super.isUndefined();
}


}	// end of class CatprogConditionParameter




/* end of CatprogConditionParameter.java */

