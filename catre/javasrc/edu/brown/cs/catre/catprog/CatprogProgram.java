/********************************************************************************/
/*										*/
/*		CatprogProgram.java						*/
/*										*/
/*	Basic implementation of a program					*/
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

import edu.brown.cs.catre.catre.CatreSubSavableBase;
import edu.brown.cs.catre.catre.CatreTriggerContext;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.ivy.swing.SwingEventListenerList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.catre.catre.CatreAction;
import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionListener;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreException;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterRef;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatreProgramListener;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreRule;
import edu.brown.cs.catre.catre.CatreStore;

class CatprogProgram extends CatreSubSavableBase implements CatreProgram, CatprogConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private SortedSet<CatreRule>	rule_list;
private CatreUniverse		for_universe;
private Set<CatreCondition>	active_conditions;
private Map<CatreCondition,RuleConditionHandler> cond_handlers;
private Updater 		active_updater;
private SwingEventListenerList<CatreProgramListener> program_callbacks;
private Map<String,CatprogCondition> shared_conditions;
private Map<CatreDevice,Set<CatreCondition>> used_conditions;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatprogProgram(CatreUniverse uu)
{
   super("PROG_");

   for_universe = uu;
   rule_list = new ConcurrentSkipListSet<>(new RuleComparator());
   active_conditions = new HashSet<>();
   shared_conditions = new HashMap<>();
   active_updater = null;
   cond_handlers = new WeakHashMap<>();
   program_callbacks = new SwingEventListenerList<>(CatreProgramListener.class);
   used_conditions = new HashMap<>();
}


CatprogProgram(CatreUniverse uu,CatreStore cs,Map<String,Object> map)
{
   this(uu);

   fromJson(cs,map);
   
   setup();
   
   updateConditions();
}



 void setup()
{
    // ensure program is set up correctly
    
   if (shared_conditions.isEmpty()) {
      CatreStore cs = for_universe.getCatre().getDatabase();
      Map<String,Object> map = Map.of("TYPE","Always",
            "NAME","ALWAYS",
            "DESCRIPTION","Always true",
            "LABEL","ALWAYS",
            "SHARED",true);
      CatprogCondition cc = createCondition(cs,map);
      shared_conditions.put(cc.getName(),cc);     
    }
}


/********************************************************************************/
/*										*/
/*	Access methods -- shared conditions                                     */
/*										*/
/********************************************************************************/

@Override public void addSharedCondition(CatreCondition cc)
{
   shared_conditions.put(cc.getName(),(CatprogCondition) cc); 
}


@Override public void removeSharedCondition(String name)
{
   shared_conditions.remove(name); 
}


@Override public void cleanSharedConditions()
{
   Set<String> used = new HashSet<>();
   // Add default shared conditions so we don't remove them
   used.add("ALWAYS");
   
   for (CatreRule cr : rule_list) {
      for (CatreCondition cc : cr.getConditions()) {
         String nm = cc.getSharedName(); 
         if (nm != null) used.add(nm);
       }
    }
   
   boolean chng = false;
   Map<String,CatprogCondition> reps = new HashMap<>();
   Iterator<Map.Entry<String,CatprogCondition>> it = shared_conditions.entrySet().iterator();
   while (it.hasNext()) {
      Map.Entry<String,CatprogCondition> ent = it.next();
      String snm = ent.getKey();
      if (!used.contains(snm)) {
         CatreLog.logD("CATPROG","Remove unused shared condition " + snm);
         it.remove();
         chng = true;
       }
      else {
         CatprogCondition cc = ent.getValue();
         for ( ; ; ) {
            CatprogCondition base = cc.getSubcondition();
            CatreLog.logD("CATPROG","Check shared " + cc.getSharedName() + " " +
                  cc.getName() + " " + base);
            if (base != null && base.getSharedName().equals(cc.getSharedName())) {
               cc = base;
             }
            else break;
          }
         if (cc != ent.getValue()) {
            reps.put(snm,cc);
            CatreLog.logD("CATPROG","Replace refereced shared position with " + cc);
          }
       }
    }
   for (Map.Entry<String,CatprogCondition> ent : reps.entrySet()) {
      shared_conditions.put(ent.getKey(),ent.getValue());
      chng = true;
    }
   
   if (chng) {
      fireProgramUpdated();
    }
}



CatprogCondition getSharedCondition(String name)
{
   return shared_conditions.get(name);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods --rules                                                  */
/*                                                                              */
/********************************************************************************/


@Override public List<CatreRule> getRules()
{
   return new ArrayList<>(rule_list);
}


@Override public CatreRule findRule(String id)
{
   if (id == null) return null;

   for (CatreRule ur : rule_list) {
      if (ur.getDataUID().equals(id) || ur.getName().equals(id))
	 return ur;
    }
   return null;
}


@Override public synchronized void addRule(CatreRule ur)
{
   CatreRule oldcr = findRule(ur.getDataUID());
   if (oldcr != null) {
      CatreLog.logD("CATPROG","Remove old rule " + oldcr.toJson());
      rule_list.remove(oldcr);
      for (CatreCondition cc : oldcr.getConditions()) {
         if (!cc.isShared()) {
            CatprogCondition cp = (CatprogCondition) cc;
            cp.setValid(false);
            active_conditions.remove(cc);
          }
       }
    }
	
   CatreLog.logD("CATPROG","Add new rule " + ur.toJson());
   rule_list.add(ur);
   updateConditions();

   CatreLog.logD("CATPROG","Add rule " + ur.toJson());
   
   fireProgramUpdated();
}


@Override public void removeRule(CatreRule ur)
{
   synchronized (this) {
      if (ur != null) {
         rule_list.remove(ur);
         updateConditions();
       }
      else {
         CatreLog.logD("CATPROG","Remove null rule to trigger update");
       }
    }
   
   fireProgramUpdated();
}



/********************************************************************************/
/*                                                                              */
/*      General access methods                                                  */
/*                                                                              */
/********************************************************************************/

@Override public CatreUniverse getUniverse()
{
   return for_universe;
}


public Set<CatreParameterRef> getActiveSensors()  
{
   Set<CatreParameterRef> rslt = new HashSet<>();
   
   CatreLog.logD("CATPROG","Get active sensors " +
         active_conditions.size() + " " +
         active_conditions);
   
   for (CatreCondition cc : active_conditions) {
      CatprogCondition cpc = (CatprogCondition) cc;
      CatreParameterRef ref = cpc.getActiveSensor(); 
      CatreLog.logD("CATPROG","Active sensors for " + cpc.getName() + " " +
            ref);
      if (ref != null) {
         rslt.add(ref);
         CatreParameter cp = ref.getParameter();
         if (cp != null) {
           CatreParameterRef cpp = cp.getActiveSensor(); 
           if (cpp != null) rslt.add(cpp);
          }
       }
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Validate methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public JSONObject errorCheckRule(CatreRule cr)
{ 
   CatprogRule cpr = (CatprogRule) cr;
   CatprogErrorChecker checker = new CatprogErrorChecker(this,cpr);
   List<RuleError> errors = checker.analyzeRule();
   
   JSONArray errs = new JSONArray();
   for (RuleError re : errors) {
      JSONObject jo = buildJson("Level",re.getErrorLevel(),
            "Message",re.getMessage());
      errs.put(jo);
    }
   
   // might want to include error level in the result
   return buildJson("ERRORS",errs);
}



/********************************************************************************/
/*										*/
/*	Factory methods 							*/
/*										*/
/********************************************************************************/

@Override public CatprogCondition createCondition(CatreStore cs,Map<String,Object> map)
{
   CatprogCondition cc = null;
   String typ = getSavedString(map,"TYPE","UNDEFINED");
   switch (typ) {
      case "CalendarEvent" :
         cc = new CatprogConditionCalendarEvent(this,cs,map);
         break;
      case "Duration" :
         cc = new CatprogConditionDuration(this,cs,map);
         break;
      case "Latch" :
         cc = new CatprogConditionLatch(this,cs,map);
         break;
      case "Debounce" :
         cc = new CatprogConditionDebounce(this,cs,map);
         break;
      case "Parameter" :
         cc = new CatprogConditionParameter(this,cs,map); 
         break;
      case "Range" :
         cc = new CatprogConditionRange(this,cs,map);
         break;
      case "Time" :
      case "Timer" :
         cc = new CatprogConditionTime(this,cs,map);
         break;
      case "TriggerTime" :
      case "TriggerTimer" :
         cc = new CatprogConditionTriggerTime(this,cs,map);
         break;
      case "Disabled" :
         cc = new CatprogConditionDisabled(this,cs,map);
         break;
      case "Reference" :
         cc = new CatprogConditionRef(this,cs,map);
         break;
      case "Always" :
         cc = new CatprogConditionAlways(this,cs,map); 
         break;
      case "UNDEFINED" :
         break;
      case "Or" :
         cc = new CatprogConditionOr(this,cs,map); 
         break;
      default :
         CatreLog.logE("CATPROG","Unknown condition type " + typ);
         break;
    }
   
   if (cc != null && cc.isUndefined()) {
      CatreLog.logD("CATPROG","Removing undefined condition " + cc.getLabel());
      cc = null; 
    }
   
   if (cc != null) {
      cc.activate();
      if (cc.isShared()) shared_conditions.put(cc.getName(),cc);
    }
   

   return cc;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   rslt.put("RULES",getSubObjectArrayToSave(rule_list));
   rslt.put("SHARED",getSubObjectArrayToSave(shared_conditions.values()));
   return rslt;
}



@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);

   List<CatreRule> rls = new ArrayList<>();
   rls = getSavedSubobjectList(cs,map,"RULES",
	 this::createRule,rls);
   rls = removeDuplicates(rls);
  
   rule_list.clear();
   rule_list.addAll(rls);
   
   List<CatreCondition> shared = new ArrayList<>();
   shared = getSavedSubobjectList(cs,map,"SHARED",
         this::createCondition,shared);
   shared_conditions.clear();
   for (CatreCondition cc : shared) {
      if (!cc.getName().equals("Undefined")) {
         shared_conditions.put(cc.getName(),(CatprogCondition) cc);
       }
    }
}


private List<CatreRule> removeDuplicates(List<CatreRule> rls)
{
   List<CatreRule> rslt = new ArrayList<>();
   for (CatreRule cr : rls) {
      boolean dup = false;
      for (CatreRule cr0 : rslt) {
         if (ruleIsDuplicate(cr0,cr)) dup = true;
       }
      if (!dup) rslt.add(cr);
    }
   return rslt;
}


private boolean ruleIsDuplicate(CatreRule cr0,CatreRule cr1)
{
   if (cr0.getTargetDevice() != cr1.getTargetDevice()) return false;
   if (cr0.getPriority() != cr1.getPriority()) return false;
   CatreLog.logD("CATPROG","Remove duplicate rule " + cr1);
   return true;
}


/********************************************************************************/
/*										*/
/*	Maintain active conditions						*/
/*										*/
/********************************************************************************/

private void updateConditions()
{
   CatreLog.logD("CATPROG","Update conditions " + active_conditions.size() +
         " " + rule_list.size());
   
   used_conditions.clear();
   
   Set<CatreCondition> del = new HashSet<>(active_conditions);

   for (CatreRule ur : rule_list) {
      CatreLog.logD("CATPROG","Work on rule " + ur.getDescription());
      for (CatreCondition cc : ur.getConditions()) {
         CatreLog.logD("CATPROG","Work on condition " + cc.getDescription());
         markActive(cc,del);
       }
    }

   for (CatreCondition uc : del) {
      if (uc.isShared()) continue;
      CatreLog.logD("CATPROG","Mark condition " + uc.getName() + " inactive");
      RuleConditionHandler rch = cond_handlers.get(uc);
      if (rch != null) uc.removeConditionHandler(rch);
      active_conditions.remove(uc);
    }
   
   conditionChange(null,false,null);
}


private void markActive(CatreCondition cc0,Set<CatreCondition> todel)
{
   if (cc0 == null) return;
   
   CatprogCondition cc = (CatprogCondition) cc0;
   
   todel.remove(cc);
   if (!active_conditions.contains(cc)) {
      CatreLog.logD("CATPROG","Mark condition " + cc.getName() + " active");
      active_conditions.add(cc);
      RuleConditionHandler rch = new RuleConditionHandler();
      cond_handlers.put(cc,rch);
      cc.addConditionHandler(rch);
    }
   CatreCondition sub = cc.getSubcondition();
   markActive(sub,todel);
}



private void conditionChange(CatreCondition c)
{
   conditionChange(c,false,null);
}


private void conditionChange(CatreCondition c,boolean istrig,CatrePropertySet ps)
{
   String nm = (c == null ? "<NONE>" : c.getName());
   CatreLog.logD("CATPROG","Condition changed " + nm + " " +
         istrig + " " + ps);
   
   Set<CatreDevice> devices = null;
   for (CatreRule cr : rule_list) {
      CatprogRule cpr = (CatprogRule) cr;
      CatreDevice cd = cpr.getTargetDevice();
      if (cd == null) continue;
      if (devices != null && devices.contains(cd)) continue;
      if (cpr.getCheckedConditions().contains(c)) {
         Set<CatreCondition> used = used_conditions.get(cd);
         if (used != null && !used.contains(c)) {
            CatreLog.logD("CATPROG","Condition isn't relevant to current value " + c);
            continue;
          }
         if (devices == null) devices = new HashSet<>();
         devices.add(cd);
       }
    }
   CatreLog.logD("CATPROG","Relevant target devices " + devices);
   
   for_universe.updateLock();
   try {
      if (istrig && c != null) for_universe.addTrigger(c,ps);
      Updater upd = active_updater;
      if (upd != null) {
	 upd.runAgain(devices);
       }
      else {
         CatreLog.logD("CATPROG","Create updater for " + devices);
	 upd = new Updater(devices);
	 active_updater = upd;
	 for_universe.getCatre().submit(upd);
       }
    }
   finally {
      for_universe.updateUnlock();
    }
}




private class Updater implements Runnable {

   private boolean run_again;
   private long last_request;
   private Set<CatreDevice> for_devices;

   Updater(Set<CatreDevice> devices) {
      run_again = true;
      last_request = System.currentTimeMillis();
      for_devices = devices;
    }

   void runAgain(Set<CatreDevice> devices) {
      CatreLog.logD("CATPROG","Run again for " + devices);
      for_universe.updateLock();
      try {
         run_again = true;
         if (devices != null && !devices.isEmpty()) {
            if (for_devices == null) for_devices = devices;
            else for_devices.addAll(devices);
          }
         last_request = System.currentTimeMillis();
       }
      finally {
         for_universe.updateUnlock();
       }
      CatreLog.logD("CATPROG","Run again finished " + for_devices);
    }

   @Override public void run() {
      CatreLog.logD("CATPROG","Start updater for " + for_universe.getName() + (new Date()));
      
      Set<CatreDevice> used = null;
      for_universe.updateLock();
      try {
         run_again = true;
         while (run_again) {
            run_again = false;
            long now = System.currentTimeMillis();
            if (now - last_request > RUN_DELAY) break;
            for_universe.updateUnlock();                // allow run again while waiting
            try {
               Thread.sleep(now-last_request);
             }
            catch (InterruptedException e) { }
            for_universe.updateLock();
          }
         used = for_devices;
         for_devices = null;
         run_again = false;
       }
      finally {
         for_universe.updateUnlock();
       }
      
      if (used != null && used.isEmpty()) used = null;
      CatreLog.logD("CATPROG","Ready to do update for " + for_universe.getName() + 
            " " + used);
      
      for ( ; ; ) {
         CatreTriggerContext ctx = null;
         for_universe.updateLock();
         try {
            ctx = for_universe.waitForUpdate();
            if (run_again) {
               if (for_devices != null) {
                  CatreLog.logD("CATPROG","Add relevant devices to run against " + for_devices);
                  if (used == null) used = for_devices;
                  else used.addAll(for_devices);
                }
             }
            run_again = false;
            for_devices = null;
          }
         finally {
            for_universe.updateUnlock();
          }
        
         try {
            runOnce(ctx,used);
          }
         catch (Throwable t) {
            CatreLog.logE("CATPROG","Problem running program",t);
          }
        
         for_universe.updateLock();
         CatreLog.logD("CATPROG","Finished program run " + run_again + " " + for_devices);
         try {
            if (!run_again) {
               active_updater = null;
               resetTriggers();
               break;
             }
            else {
               last_request = System.currentTimeMillis();       // ensure delay
             }
          }
         finally {
            for_universe.updateUnlock();
          }
       }
   
    }

}	// end of inner class Updater




private final class RuleConditionHandler implements CatreConditionListener {

   @Override public void conditionOn(CatreCondition cc,CatrePropertySet p) {
      conditionChange(cc);
    }

   @Override public void conditionOff(CatreCondition cc) {
      conditionChange(cc);
    }

   @Override public void conditionTrigger(CatreCondition cc,CatrePropertySet p) {
      if (p == null) p = for_universe.createPropertySet();
      conditionChange(cc,true,p);
    }

}	// end of inner class RuleConditionHandler




/********************************************************************************/
/*										*/
/*	Program run methods							*/
/*										*/
/********************************************************************************/

@Override public synchronized boolean runOnce(CatreTriggerContext ctx,Set<CatreDevice> relevant)
{
   CatreLog.logD("CATPROG","Run program " +
         rule_list.size() + " " + ctx + " " + relevant);
   
   boolean rslt = false;

   Set<CatreDevice> entities = new HashSet<>();
   Map<CatreDevice,Set<CatreCondition>> usedcondmap = new HashMap<>();

   Collection<CatreRule> rules = new ArrayList<>(rule_list);

   CatreLog.logI("CATPROG","CHECK RULES at " + new Date()); 

   for (CatreRule r : rules) {
      if (r.isDisabled()) continue;
      CatreDevice rent = r.getTargetDevice();
      if (entities.contains(rent)) continue;
      if (relevant != null && !relevant.contains(rent)) {
         CatreLog.logD("CATPROG","Skip rule " + r.getName() + " due to relevancy");
         continue;
       }
      try {
         Set<CatreCondition> usedconds = usedcondmap.get(rent);
         if (usedconds == null) {
            usedconds = new HashSet<>();
            usedcondmap.put(rent,usedconds);
          }
	 if (startRule(r,ctx,usedconds)) {
	    rslt = true;
	    entities.add(rent);
	  }
       }
      catch (CatreException e) {
	 CatreLog.logE("CATPROG","Problem with rule " + r.getName(),e);
       }
    }
   
   CatreLog.logD("CATPROG","Used conditions: " + usedcondmap.size() + " " + usedcondmap);
   used_conditions.putAll(usedcondmap);

   return rslt;
}



private boolean startRule(CatreRule r,CatreTriggerContext ctx,
      Set<CatreCondition> usedconds)
	throws CatreException
{
   return r.apply(ctx,usedconds);
}


private void resetTriggers()
{
   // reset any triggers after rules run completely
}



/********************************************************************************/
/*										*/
/*	Rule priority comparator						*/
/*										*/
/********************************************************************************/

private static final class RuleComparator implements Comparator<CatreRule> {

   @Override public int compare(CatreRule r1,CatreRule r2) {
      double v = r1.getPriority() - r2.getPriority();
      if (v > 0) return -1;
      if (v < 0) return 1;
      // what is this for?
      if (r1.getPriority() >= 100) {
         long t1 = r1.getCreationTime() - r2.getCreationTime();
         if (t1 > 0) return -1;
         if (t1 < 0) return 1;
       }
      int v1 = r1.getName().compareTo(r2.getName());
      if (v1 != 0) return v1;
      return r1.getDataUID().compareTo(r2.getDataUID());
    }

}	// end of inner class RuleComparator



/********************************************************************************/
/*										*/
/*	Input methods								*/
/*										*/
/********************************************************************************/

@Override public CatreRule createRule(CatreStore cs,Map<String,Object> map)
{
   try {
      return new CatprogRule(this,cs,map);
    }
   catch (Throwable t) {
      CatreLog.logE("Problem with rule definition",t);
      return null;
    }
}


CatreAction createAction(CatreStore cs,Map<String,Object> map)
{
   return new CatprogAction(this,cs,map);
}



/********************************************************************************/
/*                                                                              */
/*      Listener methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public void addProgramListener(CatreProgramListener l)
{
   program_callbacks.add(l);
}


@Override public void removeProgramListener(CatreProgramListener l)
{
   program_callbacks.remove(l);
}

protected void fireProgramUpdated()
{
   CatreLog.logD("CATPROG","Fire program updated");

   for (CatreProgramListener pl : program_callbacks) {
      pl.programUpdated();
    }
}







}	// end of class CatprogProgram




/* end of CatprogProgram.java */

