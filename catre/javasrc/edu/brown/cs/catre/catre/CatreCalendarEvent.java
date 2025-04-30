/********************************************************************************/
/*                                                                              */
/*              CatreCalendarEvent.java                                         */
/*                                                                              */
/*      description of class                                                    */
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

import java.util.Map;

/**
 *      Represent an event from a calendar, e.g. Google Calendar
 **/

public interface CatreCalendarEvent
{

/**
 *      Return the start time of the event
 **/

long getStartTime();


/**
 *      Return the end time of the event
 **/

long getEndTime();


/*
 *      Return a map of other properties associated with the event, for example
 *      The ID, the STATUS, the TRANSPARENCY, the VISIBILITY, the CONTENT, the
 *      WHERE (location), the CALENDAR (calendar name), LINKS associated with the
 *      event, and a boolbean ALLDAY flag.  (These might vary for different calendars.)
 *      These can be used as values to pass to actions or as items to test for
 *      checking conditions.
 **/

Map<String,String> getProperties();



}       // end of interface CatreCalendarEvent




/* end of CatreCalendarEvent.java */

