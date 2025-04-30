/********************************************************************************/
/*                                                                              */
/*              CatreBridge.java                                                */
/*                                                                              */
/*      Bridge from Catre to outside IoT controller                             */
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

import java.util.Collection;
import java.util.Map;

import org.json.JSONObject;


/**
 *      A CatreBridge represents a way of connecting an external hub that manages
 *      a set of devices to CATRE.  Communication with bridges is through CEDES.  
 *      Each bridge can provide a set of devices and handles parameter changes on
 *      these devices as well as providing the facilities to execute transitions
 *      on these devices.  CatreBridge instances are for a particular user.
 **/ 

public interface CatreBridge
{

/**
 *      Return the name of the bridge
 **/

String getName();


/**
 *      Return the id of the bridge.  Each user has a unique ID for a given bridge
 *      as well as authorization information that validates that ID.
 **/

String getBridgeId();


/**
 *      Return information associated with the bridge.  This information is used
 *      by the front end to let the user set up or authorize devices for the bridge.
 **/

JSONObject getBridgeInfo();


/**
 *      Return the set of devices descriptions associated with this bridge.
 **/

Collection<CatreDevice> findDevices();


/**
 *      Create a device within Catre based on a description returned from the bridge.
 **/

CatreDevice createDevice(CatreStore cs,Map<String,Object> map);


/**
 *      Create a transition within Catre based on description returned from the bridge.
 **/ 

CatreTransition createTransition(CatreDevice device,CatreStore cs,Map<String,Object> map);



/**
 *      Apply a transition to handle the action part of a rule for a particular device
 *      associated with the bridge.
 **/ 

void applyTransition(CatreDevice device,CatreTransition t,
      Map<String,Object> vals) throws CatreActionException;



/**
 *      Update the parameter values associated with a given device by asking the bridge
 *      for the current values if needed.
 **/

JSONObject updateParameterValues(CatreDevice cd);


}       // end of interface CatreBridge




/* end of CatreBridge.java */

