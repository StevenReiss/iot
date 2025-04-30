/********************************************************************************/
/*                                                                              */
/*              CatreBridgeAuthorization.java                                   */
/*                                                                              */
/*      Authorization information for a bridge                                  */
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
 *      This represents the authorization information for a particular
 *      user for a particular bridge.  The fields in the authorization
 *      are specific to the bridge and are listed as part of the bridge
 *      information.  Multiple authorizations for a particular user are
 *      allowed for a bridge.
 **/

public interface CatreBridgeAuthorization extends CatreSubSavable
{

/**
 *      Return the name of the bridge.
 **/

String getBridgeName();


/*  
 *      In the case where the user has multiple authorizations for a
 *      bridge (for example accessing multiple Google calendars), this 
 *      retrns the index of the calendar.  If there is only one 
 *      authorization, it returns 0;
 **/

int getAuthorizationCount();


/*
 *      Return the value of a given authorization parameter for a given
 *      index for this user/bridge.
 **/

String getValue(int idx,String key);


/**
 *      Return the value of a given authoriuzation parameter for this 
 *      user/bridge.
 **/

default String getValue(String key)     { return getValue(0,key); }



}       // end of interface CatreBridgeAuthorization




/* end of CatreBridgeAuthorization.java */

