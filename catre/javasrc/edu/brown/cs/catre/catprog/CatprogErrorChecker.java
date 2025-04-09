/********************************************************************************/
/*                                                                              */
/*              CatprogErrorChecker.java                                        */
/*                                                                              */
/*      Check a new or modified rule for potential errors                       */
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import edu.brown.cs.catre.catre.CatreAction;
import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreRule;
import edu.brown.cs.catre.catre.CatreTimeSlotEvent;

class CatprogErrorChecker implements CatprogConstants
{


/********************************************************************************/

/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CatprogProgram  for_program;
private CatprogRule     for_rule;
private List<TimeInterval> rule_intervals;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatprogErrorChecker(CatprogProgram pgm,CatprogRule rule)
{
   for_program = pgm;
   for_rule = rule;
   
   rule_intervals = getTimeSlots(for_rule);
   
   CatreLog.logD("CATPROG","Error check " + rule.getDescription());
   CatreLog.logD("CATPROG","\tIntervals: " + rule_intervals);
}


/********************************************************************************/
/*                                                                              */
/*      Do the actual checking                                                  */
/*                                                                              */
/********************************************************************************/

List<RuleError> analyzeRule()
{
   List<RuleError> errors = new ArrayList<>();
   
   checkCanFire(errors);
   checkParameterConditions(errors);
   checkValid(errors);
   checkOccludedRules(errors);
  
   for (RuleError re : errors) {
      CatreLog.logD("CATPROG","Rule Error: " + re);
    }
   
   return errors;
}



/********************************************************************************/
/*                                                                              */
/*      Checker:  The rule can file in the next year                            */
/*                                                                              */
/********************************************************************************/

private void checkCanFire(List<RuleError> errors)
{
   if (rule_intervals.isEmpty()) {
      CheckError ce = new CheckError(ErrorLevel.ERROR,
            "Rule can not fire in the next year");
      errors.add(ce);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Checker: No contradictory conditions                                    */
/*                                                                              */
/********************************************************************************/

private void checkParameterConditions(List<RuleError> errors)
{
   List<CatreCondition> conds = for_rule.getConditions();
   
   for (int i = 0; i < conds.size(); ++i) {
      CatprogCondition cc0 = (CatprogCondition) conds.get(i);
      for (int j = i+1; j < conds.size(); ++j) {
         CatprogCondition cc1 = (CatprogCondition) conds.get(j);
         if (cc0.contradicts(cc1)) {
            CheckError ce = new CheckError(ErrorLevel.ERROR,
                  "Rule contains contradictor conditions: " +
                  cc0.getDescription() + " AND " + cc1.getDescription());
            errors.add(ce);
          }
       }
    }
}


/********************************************************************************/
/*                                                                              */
/*      Checker: check conditions are valid                                     */
/*                                                                              */
/********************************************************************************/

private void checkValid(List<RuleError> errors)
{
   for (CatreCondition cc : for_rule.getConditions()) {
      if (!cc.isValid()) {
         CheckError ce = new CheckError(ErrorLevel.ERROR,
               "Condition not valid: " + cc.getDescription());
         errors.add(ce);
       }
    }
   
   for (CatreAction ca :  for_rule.getActions()) {
      if (!ca.isValid()) {
         CheckError ce = new CheckError(ErrorLevel.ERROR,
               "Action not valid: " + ca.getDescription());
         errors.add(ce);
       }
    }
   
   List<CatreCondition> conds = for_rule.getConditions();
   boolean trig = false;
   for (int i = 0; i < conds.size(); ++i) {
      CatreCondition cc = conds.get(i);
      if (cc.isTrigger()) {
         if (trig) {
            CheckError ce = new CheckError(ErrorLevel.ERROR,
                  "Multiple trigger conditions -- rule won't fire");
            errors.add(ce);
          }
         else if (i > 0) {
            CheckError ce = new CheckError(ErrorLevel.ERROR,
                  "Trigger condition should be first condition");
            errors.add(ce);
          }
         else {
            trig = true;
          }
       }
    }
   
   for (CatreAction act : for_rule.getActions()) {
      if (act.isTriggerAction() && !trig) {
         CheckError ce = new CheckError(ErrorLevel.WARNING,
               "Trigger action associated with non-trigger rule");
         errors.add(ce);
       }
      else if (trig && !act.isTriggerAction()) {
         CheckError ce = new CheckError(ErrorLevel.WARNING,
               "Non-trigger action associated with trigger rule");
         errors.add(ce);
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Check for occluded rules                                                */
/*                                                                              */
/********************************************************************************/

private void checkOccludedRules(List<RuleError> errors)
{
   boolean higher = true;
   for (CatreRule cr : for_program.getRules()) {
      boolean othercond = false;
      boolean havetime = false;
      for (CatreCondition ccc : cr.getConditions()) {
         CatprogCondition cc = (CatprogCondition) ccc;
         CatreTimeSlotEvent evt = cc.getTimeSlotEvent();
         if (evt == null) othercond = true;
         else havetime = true;
       }
      if (cr == for_rule) {
         higher = false;
         if (othercond) break;          // no need to check lower priorities
         continue;
       }
      if (higher && havetime) {
         if (othercond) continue;       // this rule is conditional -- don't check   
         List<TimeInterval> crtimes = getTimeSlots(cr);
         List<TimeInterval> usetimes = intersectIntervals(crtimes,rule_intervals);
         if (usetimes.isEmpty()) {
            CheckError ce = new CheckError(ErrorLevel.ERROR,
                  "Higher priority rule " + cr.getName() + " prevents this rule from occurring");
            errors.add(ce);
          }
       } 
      else if (!higher && havetime) {
         List<TimeInterval> crtimes = getTimeSlots(cr);
         List<TimeInterval> usetimes = intersectIntervals(crtimes,rule_intervals);
         if (usetimes.isEmpty()) {
            CheckError ce = new CheckError(ErrorLevel.ERROR,
                  "This rule prevents the rule " + cr.getName() + " from occurring");
            errors.add(ce);
          }
       }
    }
}
   

/********************************************************************************/
/*                                                                              */
/*      Get the possible time slots for this rule                               */
/*                                                                              */
/********************************************************************************/

private List<TimeInterval> getTimeSlots(CatreRule cr)
{
   Calendar now = Calendar.getInstance();
   now.add(Calendar.DAY_OF_YEAR,-1);
   Calendar later = Calendar.getInstance();
   later.add(Calendar.YEAR,1);
   
   List<TimeInterval> rslt = null;
   
   for (CatreCondition ccc : cr.getConditions()) {
      CatprogCondition cc = (CatprogCondition) ccc;
      CatreTimeSlotEvent evt = cc.getTimeSlotEvent();
      if (evt == null) continue;
      List<Calendar> slots = evt.getSlots(now,later);
      List<TimeInterval> intervals = buildIntervals(evt,slots);
      if (rslt == null) rslt = intervals;
      else {
         rslt = intersectIntervals(rslt,intervals);
       }
    }
   if (rslt == null) {
      // if no time slot for the rule, then it can always fire
      // might want to do this first, and then depend on intersection
      rslt = new ArrayList<>();
      rslt.add(new TimeInterval(now,later));
    }
   
   return rslt;
}


private List<TimeInterval> buildIntervals(CatreTimeSlotEvent evt,List<Calendar> slots)
{
   List<TimeInterval> rslt = new ArrayList<>();
   
   for (int i = 0; i < slots.size(); i += 2) {
      Calendar start = slots.get(i);
      Calendar end = slots.get(i+1);
      TimeInterval ti = new TimeInterval(start,end);
      rslt.add(ti);
    }
   
   return rslt;
}


private List<TimeInterval> intersectIntervals(List<TimeInterval> ta,List<TimeInterval> tb)
{
   int i = 0;
   int j = 0;
   List<TimeInterval> rslt = new ArrayList<>();
   
   while (i < ta.size() && j < tb.size()) {
      TimeInterval tia = ta.get(i);
      TimeInterval tib = tb.get(j);
      long start= Math.max(tia.getStartTime(),tib.getStartTime());
      long end = Math.min(tia.getEndTime(),tib.getEndTime());
      if (start <= end) {
         rslt.add(new TimeInterval(start,end));
       }
      if (tia.getEndTime() < tib.getEndTime()) ++i;
      else ++j;
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Time Interval representation                                            */
/*                                                                              */
/********************************************************************************/

private static class TimeInterval {
   
   private long start_time;
   private long end_time;
   
   TimeInterval(Calendar start,Calendar end) {
      start_time = start.getTimeInMillis();
      end_time = end.getTimeInMillis();
    }
   
   TimeInterval(long start,long end) {
      start_time = start;
      end_time = end;
    }
   
   long getStartTime()          { return start_time; }
   long getEndTime()            { return end_time; }
   
   @Override public String toString() {
      return "[" + new Date(start_time) + "-" + new Date(end_time) + "]";
    } 
  
}       // end of inner class TimeInterval



/********************************************************************************/
/*                                                                              */
/*      Rule Error representation                                               */
/*                                                                              */
/********************************************************************************/

private static class CheckError implements RuleError {
   
   private ErrorLevel error_level;
   private String error_message;
   
   CheckError(ErrorLevel lvl,String msg) {
      error_level = lvl;
      error_message = msg;
    }
   
   @Override public ErrorLevel getErrorLevel()          { return error_level; }
   
   @Override public String getMessage()                 { return error_message; }
   
   @Override public String toString() {
      return error_level + ": " + error_message;
    }
   
}       // end of inner class CheckError



}       // end of class CatprogErrorChecker




/* end of CatprogErrorChecker.java */

