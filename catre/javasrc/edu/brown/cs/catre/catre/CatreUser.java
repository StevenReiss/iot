/********************************************************************************/
/*										*/
/*		CatreUser.java							*/
/*										*/
/*	Information about a user for CATRE					*/
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
 *      Representation of a particular user.  Note that for the same
 *      person to work with multiple universes, they neeed to be separate
 *      users.  While this might be non-intuitive, it avoids having to 
 *      continually check which universe the user is talking about and
 *      ensures that different universes don't conflict.
 **/

public interface CatreUser extends CatreSavable
{


/**
 *	Return the user name
 **/

String getUserName();


/**
 *	Return the homes associated with this user.  Note that thre
 *	returned universes are associated with the given user and
 *	thus have the appropriate permissions.
 **/

CatreUniverse getUniverse();



/**
 *	Get authorization information for a bridge
 **/

CatreBridgeAuthorization getAuthorization(String bridge);


/**
 *      Add an authorization for a particular bridge.
 **/

boolean addAuthorization(String bridge,Map<String,String> map);


/**
 *      Set the universe currently associated with the user
 **/

void setUniverse(CatreUniverse cu);


/**
 *      Change the password for this user
 **/

void setNewPassword(String pwd);


/**
 *      Check whether the password is temporary or not
 **/

boolean isTemporary();


/**
 *      Indicate if the current passwrod is temporary or not.  This
 *      is mainly used to clear the temporary flag for a password.
 **/

void setTemporary(boolean fg);


/**
 *      Set a temporary password for the user.
 **/

void setTemporaryPassword(String  pwd);


/**
 *      Validate a user by code (useful for one id login)
 **/

boolean validateUser(String code);


/**
 *      Setup a validator for this user/universe
 **/

String setupValidator();


/**
 *      Check if this user has been validated
 **/

boolean isValidated();



}	// end of interface CatreUser




/* end of CatreUser.java */

