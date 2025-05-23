/********************************************************************************/
/*                                                                              */
/*              CatreSavedDescribableBase.java                                  */
/*                                                                              */
/*      Base class for a Savable, Describable object                            */
/*                                                                              */
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




package edu.brown.cs.catre.catre;

import java.util.Map;

/**
 *      Base implementation of a CatreSaveable that is also a Catre Describable.
 *      This adds the basic methods for accessing the name, label, and description.
 **/

public class CatreSavedDescribableBase extends CatreSavableBase
      implements CatreDescribable, CatreSavable
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  base_name;
private String  base_label;
private String  base_description;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected CatreSavedDescribableBase(String pfx)
{
   super(pfx);
   base_name = null;
   base_label = null;
   base_description = null;
}


protected CatreSavedDescribableBase(CatreStore cs)
{
   super(cs);
   base_name = null;
   base_label = null;
   base_description = null;
}


protected CatreSavedDescribableBase(CatreStore cs,Map<String,Object> map)
{
   super(cs,map);
}


/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

/**
 *      Return the name
 **/

@Override public String getName()
{
   return base_name;
}


/**
 *      Set the name of the object.
 **/

protected void setName(String nm)
{
   base_name = nm;
}


/**
 *      Return the label
 **/

@Override public String getLabel()
{
   if (base_label == null) {
      String lbl = getName();
      return lbl.replace("_"," ");
    }
   
   return base_label;
}


/**
 *      Set the label of the object
 **/

protected void setLabel(String lbl)
{
   base_label = lbl;
}


/**
 *      Get the description of the object
 **/

@Override public String getDescription()
{
   if (base_description == null) {
      return getLabel();
    }
   
   return base_description;
}

/**
 *      Set the description of the object
 **/

public  void setDescription(String d)
{
   base_description = d;
}



/********************************************************************************/
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

/**
 *      Convert the object to a Map so it can be saved in the data store.  Normally
 *      called from a subclass.
 **/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   rslt.put("NAME",getName());
   rslt.put("LABEL",getLabel());
   rslt.put("DESCRIPTION",getDescription());
   
   return rslt;
}

/**
 *      Set up the object based on its saved implmentation.  Normally called from
 *      a subclass
 **/

@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   base_name = getSavedString(map,"NAME",base_name);
   base_label = getSavedString(map,"LABEL",base_label);
   base_description = getSavedString(map,"DESCRIPTION",base_description);
}



}       // end of class CatreSavedDescribableBase




/* end of CatreSavedDescribableBase.java */

