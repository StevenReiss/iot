/********************************************************************************/
/*										*/
/*		CatreUniverse.java						*/
/*										*/
/*	Set of devices/sensors for a single user/home				*/
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




package edu.brown.cs.catre.catre;

import java.util.Map;

/**
 *	The home is a set of devices and sensors that are available for a single
 *	instance (or house).  This defines the program that should be run to
 *	control these devices.
 **/

public interface CatreUniverse extends CatreSavable, CatreDescribable
{




/**
 *      Find a device by id
 **/

CatreDevice findDevice(String id);


/**
 *      Create a virtual device based on a JSON representation
 **/

CatreDevice createVirtualDevice(CatreStore cs,Map<String,Object> map);




/**
 *	Add an event listener for the universe
 **/

void addUniverseListener(CatreUniverseListener l);


/**
 *	Remove an event listener
 **/

void removeUniverseListener(CatreUniverseListener l);



/**
 *	Start the universe running
 **/

void start();


/**
 *	Return global controller
 **/

CatreController getCatre();



/**
 *      Return the user associated with the universe
 **/

CatreUser getUser();


/**
 *      Return the program associated with the universe
 **/

CatreProgram getProgram();


/**
 *      Create an empty parameter set
 **/

CatreParameterSet createParameterSet();


/**
 *      Create a parameter set from a JSON representation
 **/

CatreParameterSet createSavedParameterSet(CatreStore cs,Map<String,Object> map);


/**
 *      Create empty parameter set
 **/

CatrePropertySet createPropertySet();


/**
 *      Create a set of action values for a transition
 **/

CatreActionValues createActionValues(CatreParameterSet ps);


/**
 *      Create a parameter representation from a JSON representation
 **/

CatreParameter createParameter(CatreStore cs,Map<String,Object> map);


/**
 *      Find a bridge by name
 **/

CatreBridge findBridge(String name);


/**
 *      Create a universe-specific instance of a generic bridge.
 **/

void addBridge(String name);



/**
 *      Update the devices for a particular bridge
 **/

void updateDevices(CatreBridge bridge,boolean disable);


/**
 *      Update all devices of the universe.
 **/

void updateDevices(boolean disable);


/**
 *      Add a new device to the universe.
 **/

void addDevice(CatreDevice device);


/**
 *      Remove a device from the univsere
 **/

void removeDevice(CatreDevice device);


/**
 *      Create a boolean parameter for an internal device.
 **/

CatreParameter createBooleanParameter(String name,boolean issensor,String label);


/**
 *      Create an enum parameter for an internal device.
 **/

CatreParameter createEnumParameter(String name,Enum<?> e);


/**
 *      Create an enum parameter for an internal device.
 **/

CatreParameter createEnumParameter(String name,Iterable<String> vals,boolean sorted);


/**
 *      Create an enum parameter for an internal device.
 **/

CatreParameter createEnumParameter(String name,String [] v,boolean sorted);

/**
 *      Create a set parameter for an internal device.
 **/

CatreParameter createSetParameter(String name,Iterable<String> vals);



/**
 *      Create an int parameter for an internal device.
 **/

CatreParameter createIntParameter(String name,int min,int max);



/**
 *      Create a real parameter for an internal device.
 **/

CatreParameter createRealParameter(String name,double min,double max);



/**
 *      Create a real parameter for an internal device.
 **/

CatreParameter createRealParameter(String name);



/**
 *      Create a color parameter for an internal device.
 **/

CatreParameter createColorParameter(String name);



/**
 *      Create a string parameter for an internal device.
 **/

CatreParameter createStringParameter(String name);


/**
 *      Create an events parameter (Calendar event) for an internal device.
 **/

CatreParameter createEventsParameter(String name);


/**
 *      Create a new trigger context
 **/

CatreTriggerContext createTriggerContext();


/**
 *      Create a time slot event from its stored JSON representation
 **/

CatreTimeSlotEvent createTimeSlotEvent(CatreStore cs,Map<String,Object> map);


/**
 *      Create a new parameter reference
 **/

CatreParameterRef createParameterRef(CatreReferenceListener ref,String device,String parameter);


/**
 *      Create a parameter reference from its stored JSON representation
 **/

CatreParameterRef createParameterRef(CatreReferenceListener ref,CatreStore cs,Map<String,Object> map);


/**
 *      Create a new transition reference
 **/

CatreTransitionRef createTransitionRef(CatreReferenceListener ref,String device,String transition);


/**
 *      Create a transition reference from its stored representation
 **/

CatreTransitionRef createTransitionRef(CatreReferenceListener ref,CatreStore cs,Map<String,Object> map);


/********************************************************************************/
/*										*/
/*	World actions								*/
/*										*/
/********************************************************************************/

/**
 *      Lock this universe for update
 **/

void updateLock();


/**
 *      Release the universe update lock
 **/

void updateUnlock();


/**
 *      Indicate we are starting to update the universe
 **/

void startUpdate();


/**
 *      Indicate we are done updating the universe
 **/

void endUpdate();


/**
 *      Wait for the universe update to be completed
 **/

CatreTriggerContext waitForUpdate();


/**
 *      Get the current time in the universe
 **/

long getTime();


/**
 *      Add a trigger condition with a set of properties.  The universe
 *      keeps track of conditions that have triggered for the next time the
 *      program is evaluated.  Note that a trigger condition (e.g., at 3:00pm)
 *      will generally not be true when the rule is actually evaluated even 
 *      though it might have been triggered moments before.
 **/

void addTrigger(CatreCondition condition,CatrePropertySet properties);

/********************************************************************************/
/*                                                                              */
/*      Parameter methods                                                       */
/*                                                                              */
/********************************************************************************/


/**
 *      Set the value of a given parameter for this universe
 **/

void setValue(CatreParameter parameter,Object value);


/**
 *      Get the value of a given parameter for this universe
 **/

Object getValue(CatreParameter parameter);




}	// end of interface CatreUniverse




/* end of CatreHome.java */

