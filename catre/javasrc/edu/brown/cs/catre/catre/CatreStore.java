/********************************************************************************/
/*                                                                              */
/*              CatreStore.java                                                 */
/*                                                                              */
/*      Catre calss to store user-project data                                  */
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

/**
 *      Interface representing the Catre data store.  Currently this is
 *      implemented using MONGODB, but it could be done otherwise as long as
 *      this interface is maintained.
 **/

public interface CatreStore
{



/**
 *      Register a table for the database
 **/

void register(CatreTable ct);


/**
 *      Create a new user.  This will fail if the user name is already
 *      taken or there is some other database problem.  It returns the
 *      new user id
 **/

CatreUser createUser(String name,String email,String pwd) 
        throws CatreException;


/*
 *      Find and validate an existing user
 **/

CatreUser findUser(String name,String pwd,String salt);


/*
 *      Find user given only email (for forgot password)
 */

CatreUser findUserByEmail(String email);



/*
 *      Find all users
 **/

List<CatreUser> findAllUsers();


/**
 *      Attempt to validate a Google calendar.
 **/

Boolean validateCalendar(CatreUser cu,String id,String pwd);


/********************************************************************************/
/*                                                                              */
/*      Generic methods                                                         */
/*                                                                              */
/********************************************************************************/

/**
 *      Save an object into the store
 **/

String saveObject(CatreSavable obj);


/**
 *      Load a saveable object from the store
 **/

CatreSavable loadObject(String iid);

/**
 *      Remove an object from the store
 **/

void removeObject(String iid);

/**
 *      Note that an object is in the store or was retrieved from the store.
 **/

void recordObject(CatreSavable object);


/**
 *      Return the controller associated with this store
 **/

CatreController getCatre();


/**
 *      Return the Oauth server associated with the store.
 **/

CatreOauth getOauth();


}       // end of interface CatreStore




/* end of CatreStore.java */

