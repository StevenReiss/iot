/********************************************************************************/
/*                                                                              */
/*              CatreUniverse.java                                              */
/*                                                                              */
/*      Universe for a single user                                              */
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

import java.io.File;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;


/**
 *      This calls represents the main control for CATRE.  It provides a set of
 *      operations that are generally useful for multiple portions of the system.
 **/
      
public interface CatreController 
{


/********************************************************************************/
/*                                                                              */
/*      Background processes                                                    */
/*                                                                              */
/********************************************************************************/

/**
 *      Schedule a timer-based task to run once after a given delay.
 **/

ScheduledFuture<?> schedule(Runnable task,long delay);


/**
 *      Schedule a timer-based task to run periodically after a delay with
 *      a given interval period.
 **/

ScheduledFuture<?> schedule(Runnable task,long delay,long period);


/**
 *      Submit a task to be run whenever possible
 **/

Future<?> submit(Runnable task);


/**
 *      Submit a Runnable task that will return a result via a Java Future.
 **/

<T> Future<T> submit(Runnable task,T result);


/**
 *      Submit a Callable task that will return a result via a Java future.
 **/

<T> Future<T> submit(Callable<T> task);



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

/**
 *      Return the active data store.
 **/

CatreStore getDatabase();


/**
 *      Register a database table (Mongo collection) with the data store.
 **/

void register(CatreTable tbl);



/**
 *      Return the set of active bridges for the current universe.
 **/

Collection<CatreBridge> getAllBridges(CatreUniverse universe);


/**
 *      Create a new bridge for the current universe (i.e., user) of
 *      a given type. 
 **/

CatreBridge createBridge(String name,CatreUniverse universe);



/**
 *      Create a new universe for the given user
 **/

CatreUniverse createUniverse(String name,CatreUser user);


/**
 *      Return the base directory so we can find various resources as needed
 **/

File findBaseDirectory();


/**
 *      Return the URL prefix that should be used for external requests to CATRE.
 **/

String getUrlPrefix();





}       // end of interface CatreUniverse




/* end of CatreUniverse.java */

