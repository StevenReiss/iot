/********************************************************************************/
/*                                                                              */
/*      CatreSession.java                                                       */
/*                                                                              */
/*     Extended session implementation 		                        */     
/*                                                                              */
/********************************************************************************/
/* Copyright 2023 Brown University -- Steven P. Reiss, Molly E. McHenry         */
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.                           *
 *                                                                              *
 *         All Rights Reserved                                                  *
 *                                                                              *
 *  Permission to use, coy, and distribute this software and its       *
 *  documentation for any purpose other than its incorporation into a           *
 *  commercial product is hereby granted without fee, provided that the         *
 *  above copyright notice appear in all copies and that both that              *
 *  copyright notice and this permission notice appear in supporting            *
 *  documentation, and that the name of Brown University not be used in         *
 *  advertising or publicity pertaining to distribution of the software         *
 *  without specific, written prior permission.                                 *
 *                                                                              *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS               *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND           *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY     *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY         *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,             *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS              *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE         *
 *  OF THIS SOFTWARE.                                                           *
 *                                                                              *
 ********************************************************************************/





package edu.brown.cs.catre.catre;

import com.sun.net.httpserver.HttpExchange;

/**
 *      Representation of a session for use in the RESTful
 *      web service provided by Catre.
 **/

public interface CatreSession extends CatreSavable { 

/**
 *      Return the user associated with the session
 **/

CatreUser getUser(CatreController cc);


/**
 *      Return the universe associated with the session
 **/

CatreUniverse getUniverse(CatreController cc);


/**
 *      Determine if the session is currently valid.
 **/

boolean isValid();


/**
 *      Get the session ID
 **/

String getSessionId();


/**
 *      Set up the session for a particular user
 **/

void setupSession(CatreUser user);


/**
 *      Save the session in case Catre crashes and restarts
 **/

void saveSession(CatreController cc);


/**
 *      Set the value of a session parameter
 **/

void setValue(String key,String val);


/**
 *      Get the value of a session parameter
 **/

String getStringValue(String key);


/**
 *      Generate a JSON response from a set of key-value pairs
 **/

String jsonResponse(Object... val);


/**
 *      Get the value of a parameter associated with the session.
 **/

String getParameter(HttpExchange e,String id);

}	// end of class CatreSession




/* end of CatreSession.java */

