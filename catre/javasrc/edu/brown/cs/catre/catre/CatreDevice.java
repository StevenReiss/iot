/********************************************************************************/
/*                                                                              */
/*              CatreDevice.java                                                */
/*                                                                              */
/*      Representation of a device (sensor, entity or both)                     */
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

/**
 *      Abstract representation of a device (sensor or output or both) within Catre.  Devices
 *      have a set of parameters which they set as a sensor.  They also have a set of 
 *      transactions which represent the actions they can perform.  
 **/

public interface CatreDevice extends CatreDescribable, CatreIdentifiable, CatreSubSavable 
{



/**
 *	Add a trigger that is called when device changes state.
 **/

void addDeviceListener(CatreDeviceListener hdlr);


/**
 *	Remove a trigger.
 **/

void removeDeviceListener(CatreDeviceListener hdlr);



/**
 *	Return the set of parameters that can be displayed to show the
 *	state of this entity.  Parameters are used here because they are
 *	typed.	The actual valu8es are in the property set of the world.
 **/

Collection<CatreParameter> getParameters();


/**
 *	Find a parameter by name
 **/

CatreParameter findParameter(String id);

/**
 *	Get the value of a parameter in the given world.  If the world is curernt
 *	this needs to get the current state of the parameter.
 **/

Object getParameterValue(CatreParameter p);


/**
 *	Set the value of a parameter in the given world.  If the world is current,
 *	this will actually affect the device.
 **/

void setParameterValue(CatreParameter p,Object val) throws CatreActionException;


/**
 *      Update the value of all parameters so they are current
 **/

void updateParameterValues();



/**
 *	Return the set of all transitions for this device
 **/

Collection<CatreTransition> getTransitions();


/**
 *      Find a transition by name
 **/
CatreTransition findTransition(String name);

/**
 *	Indicates if there are any transitions for the device
 **/

boolean hasTransitions();


/**
 *	Find a transition by name
 **/

// CatreTransition findTransition(String name);




/**
 *	Actually apply a transition to the entity in the given world
 **/

void apply(CatreTransition t,Map<String,Object> props) throws CatreActionException;

/**
 *	Check if the device is enabled
 **/

boolean isEnabled();


/**
 *      Check if the device accepts calendar events
 **/

boolean isCalendarDevice();



/**
 *      Return the universe associated with the device
 **/ 

CatreUniverse getUniverse();
 


/**
 *      Return the contoller
 **/

default CatreController getCatre()       { return getUniverse().getCatre(); }



/**
 *      Check if this device is dependent on another
 **/

boolean isDependentOn(CatreDevice device);



/**
 *      Return bridge if this is a basic device.  Otherwise return null.
 **/

CatreBridge getBridge();


/**
 *      Return a unique id for this device
 **/

String getDeviceId();


/**
 *      Check if the device is valid, forcing a behavioral check if needed.
 **/

boolean validateDevice();


/**
 *      Create a new transition for this device from the JSON describing it.
 **/

CatreTransition createTransition(CatreStore cs,Map<String,Object> map);


/**
 *      Mark the device as either enabled or disabled.
 **/

void setEnabled(boolean fg);


/**
 *      Take a new description of the device and update any parameters or values
 *      that have changed.
 **/

boolean update(CatreDevice newdev);

/**
 *	Start running the device (after it has been added to universe)
 **/

void startDevice();


}       // end of interface CatreDevice




/* end of CatreDevice.java */

