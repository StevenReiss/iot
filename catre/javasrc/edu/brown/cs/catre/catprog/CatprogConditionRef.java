/********************************************************************************/
/*                                                                              */
/*              CatprogConditionRef.java                                        */
/*                                                                              */
/*      Condition that references a shared condition                            */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2023 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.catre.catprog;

import java.util.Map;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionException;
import edu.brown.cs.catre.catre.CatreConditionListener;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTimeSlotEvent;

class CatprogConditionRef extends CatprogCondition
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CatprogCondition  base_condition;
private String          shared_name;
private CondChanged     cond_handler;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatprogConditionRef(CatprogProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);
   setValid(base_condition.isValid());
   cond_handler = null;
}


CatprogConditionRef(CatprogConditionRef cr)
{
   super(cr);
   base_condition = cr.base_condition;
   cond_handler = null;
}


@Override public CatreCondition cloneCondition()
{
   return new CatprogConditionRef(this);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override CatprogCondition getSubcondition() 
{
   return base_condition;
}


@Override public boolean isValid()
{
   if (base_condition == null) return false;
   if (!base_condition.isValid()) return false;
   
   if (cond_handler == null && hasConditionHandlers()) {
      CatreLog.logD("CATPROG","Add validation condition handler for ref");
      cond_handler = new CondChanged();
      base_condition.addConditionHandler(cond_handler);
    }
   
   return super.isValid();
}

@Override public boolean isTrigger()
{
   if (base_condition == null) return super.isTrigger();
   
   return base_condition.isTrigger();
}


@Override public String getSharedName()
{
   return shared_name; 
}

@Override public CatreCondition getActiveCondition()
{
   if (base_condition != null) return base_condition;
   
   return this;
}


@Override public void setTime()
{
   if (base_condition != null) {
      base_condition.setTime();
    }
}

@Override public final CatrePropertySet getCurrentStatus()  
   throws CatreConditionException
{
   if (base_condition == null) return super.getCurrentStatus();
   
   return base_condition.getCurrentStatus();
}


@Override public CatreTimeSlotEvent getTimeSlotEvent()
{
   if (base_condition == null) return super.getTimeSlotEvent();
   
   return base_condition.getTimeSlotEvent();
}



/********************************************************************************/
/*                                                                              */
/*      Handle changes to the condition                                         */
/*                                                                              */
/********************************************************************************/

@Override public void addConditionHandler(CatreConditionListener hdlr) 
{
   super.addConditionHandler(hdlr);
   
   if (cond_handler == null) {
      CatreLog.logD("CATPROG","Add handler condition handler for ref");
      cond_handler = new CondChanged();
      if (base_condition != null) base_condition.addConditionHandler(cond_handler);
    }
}


@Override public void removeConditionHandler(CatreConditionListener hdlr)
{
   super.removeConditionHandler(hdlr);
   
   if (cond_handler != null && !hasConditionHandlers()) {
      CatreLog.logD("CATPROG","Remove handler condition handler for ref");
      if (base_condition != null) base_condition.removeConditionHandler(hdlr);
      cond_handler = null;
    }
}


private final class CondChanged implements CatreConditionListener {

   @Override public void conditionError(CatreCondition cc,Throwable t) {
      fireError(t);
    } 
   
   @Override public void conditionOn(CatreCondition cc,CatrePropertySet ps) {
      fireOn(ps);
    }
   
   @Override public void conditionOff(CatreCondition cc) {
      fireOff();
    }
   
   @Override public void conditionTrigger(CatreCondition cc,CatrePropertySet ps) {
      fireTrigger(ps);
    }
   
   @Override public void conditionValidated(CatreCondition cc,boolean valid) {
      setValid(valid);
      fireValidated();
    }
   
}	// end of inner class CondChanged




/********************************************************************************/
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("TYPE","Reference");
   rslt.put("SHAREDNAME",base_condition.getName());
   rslt.put("CONDITION",base_condition.toJson());
   
   return rslt;
}



@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   shared_name = getSavedString(map,"SHAREDNAME",shared_name);
   base_condition = for_program.getSharedCondition(shared_name);
   if (base_condition == null) {
      CatreCondition cc = getSavedSubobject(cs,map,"CONDITION",for_program::createCondition, null);
      base_condition = (CatprogCondition) cc;
      shared_name = cc.getName();
    }
}




}       // end of class CatprogConditionRef




/* end of CatprogConditionRef.java */

