/*                                                                              */
/*              CatprogConditionAlways.java                                     */
/*                                                                              */
/*      Condition that is always true                                           */
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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTimeSlotEvent;
import edu.brown.cs.catre.catre.CatreUniverse;

class CatprogConditionAlways extends CatprogCondition
{


/********************************************************************************/
/*                                                                              */
/*      Private storage                                                         */
/*                                                                              */
/********************************************************************************/

private  CatreTimeSlotEvent always_time;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatprogConditionAlways(CatprogProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);
   setValid(true);
   fireOn(null);
}

CatprogConditionAlways(CatprogConditionAlways ca) 
{
   super(ca);
   fireOn(null);
}


@Override public CatreCondition cloneCondition()
{
   return new CatprogConditionAlways(this);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

void addUsedConditions(Set<CatreCondition> rslt)
{
   // don't add this condition as it never changes
}


@Override CatreTimeSlotEvent getTimeSlotEvent()
{
   if (always_time == null) {
      Calendar c1 = Calendar.getInstance();
      c1.set(2000,0,1);
      Calendar c2 = Calendar.getInstance();
      c2.set(2100,0,1);
      Map<String,Object> map = new HashMap<>();
      map.put("FROMDATETIME",c1.getTimeInMillis());
      map.put("TODATETIME",c2.getTimeInMillis());
      map.put("ALLDAY",true);
      CatreUniverse cu = for_program.getUniverse();
      always_time = cu.createTimeSlotEvent(cu.getCatre().getDatabase(),map);
    }
   
   return always_time;
}


/********************************************************************************/
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("TYPE","Always");
   
   return rslt;
}



@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
}


}       // end of class CatprogConditionAlways




/* end of CatprogConditionAlways.java */

