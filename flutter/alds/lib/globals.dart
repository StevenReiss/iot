/********************************************************************************/
/*                                                                              */
/*              globals.dart                                                    */
/*                                                                              */
/*      Global definitions for ALDS                                             */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2023 Brown University -- Steven P. Reiss                      */
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/

library alds.globals;

const String storageName = "alds_app.json";

const int recheckEverySeconds = 20; // should be larger
const int pingEverySeconds = 10;
const int accessEverySeconds = 60;

const List<String> defaultLocations = [
  'Office',
  'Home',
  'Meals',
  'Meeting',
  'Class',
  'Driving',
  'Gym',
  'Bed',
  'Shopping',
  'Home Office',
  'Other',
];

const btFraction = 0.6;
const locFraction = 0.2;
const wifiFraction = 0.2;
const useThreshold = 0.90;
const stableCount = 1;
const int significantNumber = 100;
const btTopScore = 0.55;

const int numberLocationEntries = 20; // 100?
const int numberSampleEntries = 10; // 50?
