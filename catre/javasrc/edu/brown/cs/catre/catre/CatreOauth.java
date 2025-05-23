/********************************************************************************/
/*                                                                              */
/*              CatreOauth.java                                                 */
/*                                                                              */
/*      Interface for OAUTH authentication with CATRE                           */
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

import org.json.JSONObject;

/**
 *      This interface defines the OAUTH services that Catre
 *      provides for other components (e.g. CEDES) to act as
 *      an OAUTH authenticator.  This could be used, for example,
 *      to let iQsign be a device for SmartThings.  Note that
 *      this is not currently usedf as iQsign has its own OAUTH
 *      client written in Node.JS.
 *
 *      This should probably either be used directly by iQsign,
 *      or we should think of a way of using it to let others talk
 *      directly to Catre, or it should be removed from Catre.
 *
 **/


public interface CatreOauth
{

JSONObject getToken(JSONObject data) throws CatreException;
JSONObject saveToken(JSONObject data) throws CatreException;
JSONObject revokeToken(JSONObject data) throws CatreException;
JSONObject getRefreshToken(JSONObject data) throws CatreException;

JSONObject saveCode(JSONObject data) throws CatreException;
JSONObject getCode(JSONObject data) throws CatreException;
JSONObject revokeCode(JSONObject data) throws CatreException;
JSONObject verifyScope(JSONObject data) throws CatreException;

JSONObject handleLogin(JSONObject data) throws CatreException;



}       // end of interface CatreOauth




/* end of CatreOauth.java */

