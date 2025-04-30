/********************************************************************************/
/*                                                                              */
/*              CatreParameterRef.java                                          */
/*                                                                              */
/*      Reference to a device parameter                                         */
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



/**
 *      This interface represents a reference to a parameter in
 *      some device.  This is used both inside devices (where one
 *      parameter defines the range of values for another as an
 *      example) and in rules where a condition or action can refer
 *      to a parameter.  Linking directly to a parameter might not
 *      be safe as devices can come and go and can go off-line.  
 *      Instead, Catre uses a CatreParameterRef that provides an
 *      indirect link that needs to be validated and can be checked
 *      for validity
 **/

public interface CatreParameterRef extends CatreSubSavable
{

/**
 *      Check if this reference is valid
 **/

boolean isValid();


/**
 *      Return the device associated with a valid reference
 **/

CatreDevice getDevice();


/**
 *      Return the parameter associated with a valid reference
 **/

CatreParameter getParameter();

/**
 *      Return the device ID for this parameter reference.
 **/

String getDeviceId();

/**
 *      Return the parameter name for this reference.
 **/

String getParameterName();


/**
 *      Initialize the reference and try to validate.  This also
 *      sets up callbacks to detect changes in validity.
 **/

void initialize();              // check if valid initially

}       // end of interface CatreParameterRef




/* end of CatreParameterRef.java */

