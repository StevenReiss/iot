/********************************************************************************/
/*                                                                              */
/*              CatreTransition.java                                            */
/*                                                                              */
/*      Transition definitions for CATRE                                        */
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

/**
 *	A transition describes a potential action or change request for a
 *	particular entity.
 *
 *      There should be a way of designating a transition as only suitable
 *      for a trigger rule?
 *
 **/

public interface CatreTransition extends CatreDescribable, CatreSubSavable
{

/**
 *	Get the set of parameters associated with this transition.
 **/

Collection<CatreParameter> getParameterSet();


/**
 *      Get the entity parameter set by this transition if any
 **/

CatreParameter getEntityParameter();


/**
 *	Find a parameter by name
 **/

CatreParameter findParameter(String nm);

/**
 *	Get the default parameter values for this transition. This returns a
 *	copy of the default parameters that the caller is free to change.
 **/

CatreParameterSet getDefaultParameters();



/**
 *      Return the device associated with this transition
 **/

CatreDevice getDevice();


/**
 *      Return the universe associated with this transition
 **/

CatreUniverse getUniverse();

/**
 *      Update this transition with a new descriptioin
 **/
boolean update(CatreTransition ct);


/**
 *	Execute the transition on the given world.
 **/

void perform(CatreParameterSet ps,CatrePropertySet p)
        throws CatreActionException;


}       // end of interface CatreTransition




/* end of CatreTransition.java */

