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

public interface CatreSession extends CatreSavable { 

   CatreUser getUser(CatreController cc);

   CatreUniverse getUniverse(CatreController cc);
   
   boolean isValid();

   String getSessionId();

   void setupSession(CatreUser user);

   void saveSession(CatreController cc);

   void setValue(String key,String val);

   String getStringValue(String key);

   String jsonResponse(Object... val);

   String getParameter(HttpExchange e,String id);

}	// end of class CatreSession




/* end of CatreSession.java */

