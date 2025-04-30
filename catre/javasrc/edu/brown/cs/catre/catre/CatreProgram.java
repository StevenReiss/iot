/********************************************************************************/
/*                                                                              */
/*              CatreProgram.java                                               */
/*                                                                              */
/*      Representation of a runnable program                                    */
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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

/**
 *      Representation of a the program (set of rules with conditions and
 *      actions) associated with a given universe.
 **/

public interface CatreProgram extends CatreSubSavable
{


/**
 *	Return the set of rules in this program in priority order.
 **/

List<CatreRule> getRules();


/**
 *      Add a shared condition for the program.  A shared condition can
 *      be used in multiple rules.
 **/

void addSharedCondition(CatreCondition cc);


/**
 *      Remove a shared condition from the program.  Any rule that is using
 *      this condition will then have its own unshared copy.
 **/

void removeSharedCondition(String name);

/**
 *      Return a particular rule by id or name
 **/

CatreRule findRule(String id);

/**
 *      Add a new rule:  should only be called by CatreUnniverse, not directly
 **/

void addRule(CatreRule ur);

/**
 *      Remove a rule
 **/

void removeRule(CatreRule ur);

/**
 *      Provide validation information on a rule
 **/

JSONObject errorCheckRule(CatreRule cr);



/**
 *	Run the current program once on the current world.  This function
 *	returns true if a rule is triggered.
 **/

boolean runOnce(CatreTriggerContext ctx,Set<CatreDevice> relevant);


/**
 *      Return a list of parameter references used by this program.  This
 *      can be used by CEDES to determine what messages need to be sent.
 **/

Set<CatreParameterRef> getActiveSensors();


/**
 *      Return the universe associated with this program
 **/

CatreUniverse getUniverse();


/**
 *      Create a new rule from it data store Map representation
 **/

CatreRule createRule(CatreStore cs,Map<String,Object> map);

/**
 *      Create a new condition from its data store Map representation
 **/

CatreCondition createCondition(CatreStore cs,Map<String,Object> map);


/**
 *      Add a listener to handle changes in the program
 **/

void addProgramListener(CatreProgramListener listener);


/**
 *      Remove a program listener.
 **/

void removeProgramListener(CatreProgramListener listener);


}       // end of interface CatreProgram




/* end of CatreProgram.java */

