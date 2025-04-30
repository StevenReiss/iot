/********************************************************************************/
/*                                                                              */
/*              CatdevFactory.java                                              */
/*                                                                              */
/*      description of class                                                    */
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




package edu.brown.cs.catre.catdev;

import java.util.Map;

import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;

public class CatdevFactory implements CatdevConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CatreUniverse for_universe;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public CatdevFactory(CatreUniverse cu)
{
   for_universe = cu;
}


/********************************************************************************/
/*                                                                              */
/*      Methods to create virtual devices                                       */
/*                                                                              */
/********************************************************************************/

/**
 *      Create an internal (virtual) device from a JSON description
 **/

public CatreDevice createDevice(CatreStore cs,Map<String,Object> map)
{
   CatdevDevice device = null;
   
   String typ = map.get("VTYPE").toString();
   if (typ != null) {
      switch (typ) {
         case "Weather" :
            device = new CatdevWeatherDevice(for_universe,cs,map);
            break;
         case "OpenMeteo" :
            device = new CatdevMeteoDevice(for_universe,cs,map); 
            break;
         case "RssFeed" :
            device = new CatdevDeviceRssFeed(for_universe,cs,map); 
            break;
            
         default : 
            CatreLog.logE("CATDEV","Unknown device type " + typ);
            break;
       }
    }
   
   if (device != null && !device.validateDevice()) device = null;
   
   return device;
}


}       // end of class CatdevFactory




/* end of CatdevFactory.java */

