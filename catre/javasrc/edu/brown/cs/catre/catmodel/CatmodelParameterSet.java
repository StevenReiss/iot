/********************************************************************************/
/*										*/
/*		CatmodelParameterSet.java					*/
/*										*/
/*	description of class							*/
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




package edu.brown.cs.catre.catmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterSet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreSubSavable;
import edu.brown.cs.catre.catre.CatreUniverse;

class CatmodelParameterSet implements CatreSubSavable, CatreParameterSet, CatmodelConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<CatreParameter,Object> parameter_values;
private Set<CatreParameter>	valid_parameters;
private CatmodelUniverse	for_universe;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatmodelParameterSet(CatreUniverse cu)
{
   valid_parameters = new HashSet<>();
   parameter_values = new HashMap<>();
   for_universe = (CatmodelUniverse) cu;
}


CatmodelParameterSet(CatmodelUniverse cu,Collection<CatreParameter> valids)
{
   this(cu);

   if (valids != null) valid_parameters.addAll(valids);
}


CatmodelParameterSet(CatmodelUniverse cu,CatreParameterSet ps)
{
   this(cu);

   if (ps != null) {
      CatmodelParameterSet cps = (CatmodelParameterSet) ps;
      parameter_values.putAll(cps.parameter_values);
      valid_parameters = new HashSet<CatreParameter>(ps.getValidParameters());
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Collection<CatreParameter> getValidParameters()
{
   return valid_parameters;
}


@Override public void addParameter(CatreParameter up)
{
   valid_parameters.add(up);
}


public void addParameters(Collection<CatreParameter> ups)
{
   valid_parameters.addAll(ups);
}


@Override public Object putValue(CatreParameter up,Object o)
{
   if (up == null) return null;
   
   addParameter(up);
   o = up.normalize(o);
   return parameter_values.put(up,o);
}

@Override public void putValues(CatreParameterSet ps)
{
   CatmodelParameterSet cps = (CatmodelParameterSet) ps;
   for (Map.Entry<CatreParameter,Object> ent : cps.parameter_values.entrySet()) {
      putValue(ent.getKey(),ent.getValue());
    }
}

@Override public Object putValue(String pname,Object o)
{
   for (CatreParameter cp : parameter_values.keySet()) {
      if (cp.getName().equals(pname)) {
	 Object oval = parameter_values.get(cp);
	 if (o instanceof String) {
	    o = cp.normalize(o);
	  }
	 putValue(cp,o);
	 return oval;
       }
    }

   return null;
}


@Override public void clearValues()
{
   parameter_values.clear();
}


@Override public Object getValue(CatreParameter p)
{
   return parameter_values.get(p);
}


@Override public String getStringValue(CatreParameter p)
{
   Object v = parameter_values.get(p);
   if (v == null) return null;

   return p.unnormalize(v);
}


@Override public void setParameter(String nm,Object val)
{
   CatreParameter parm = null;
   for (CatreParameter up : getValidParameters()) {
      if (up.getName().equals(nm)) {
	 parm = up;
	 break;
       }
    }
   if (parm == null) {
      return;
    }

   if (val == null) {
      parameter_values.remove(parm);
      return;
    }

   putValue(parm,val);
}


/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = new HashMap<>();

   List<Object> plst = new ArrayList<>();
   for (CatreParameter up : valid_parameters) {
      Object val = for_universe.getValue(up);
      if (val == null) {
         val = parameter_values.get(up);
       }
      Map<String,Object> pval = up.toJson();
      String sval = up.unnormalize(val);
      pval.put("VALUE",sval);
      plst.add(pval);
    }

   rslt.put("PARAMETERS",plst);

   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   valid_parameters = getSavedSubobjectSet(cs,map,"PARAMETERS",
	 this::createParameter,valid_parameters);
   // this needs to save values too
}


private CatreParameter createParameter(CatreStore cs,Map<String,Object> map)
{
   CatreParameter cp = for_universe.createParameter(cs,map);
   if (cp == null) return null;
   
   String val = getSavedString(map,"VALUE",null);
   putValue(cp,val);
   return cp;
}


@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append("{ ");
   for (CatreParameter cp : valid_parameters) {
      Object val = parameter_values.get(cp);
      buf.append(cp.getName());
      if (val != null) {
         buf.append("=");
         buf.append(val);
       }
      buf.append("; ");
    }
   buf.append("}");
   return buf.toString();
}




}	// end of class CatmodelParameterSet




/* end of CatmodelParameterSet.java */

