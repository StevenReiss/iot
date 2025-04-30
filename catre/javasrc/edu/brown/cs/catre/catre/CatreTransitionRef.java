/********************************************************************************/
/*                                                                              */
/*              CatreTransitionRef.java                                         */
/*                                                                              */
/*      Reference to a device transition                                        */
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
 *      Represent a reference to a transition.  Rules will generally
 *      have such a reference when a transition is needed to account
 *      for changes to the underlying devices.  This is similar to
 *      a parameter reference.
 **/

public interface CatreTransitionRef extends CatreSubSavable
{

/**
 *      Set up the transition reference, making it valid if possible.
 **/

void initialize();

/**
 *      Test if the reference is valid
 **/

boolean isValid();


/**
 *      Return the device associated with the reference if valid.
 **/

CatreDevice getDevice();


/**
 *      Return the actual transition associed with the reference if valid.
 **/

CatreTransition getTransition();



/**
 *      Return the device ID being referred to
 **/

String getDeviceId();


/**
 *      Return the name of the transition being referred to.
 **/

String getTransitionName();



}       // end of interface CatreTransitionRef




/* end of CatreTransitionRef.java */

