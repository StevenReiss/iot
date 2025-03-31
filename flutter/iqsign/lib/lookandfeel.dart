/********************************************************************************/
/*                                                                              */
/*              lookandfeel.dart                                                */
/*                                                                              */
/*      Defintions controlling the look and feel for iqsign                     */
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

import 'package:flutter/material.dart';

const Color errorColor = Colors.red;
const double errorFontSize = 16;
const double errorFontScale = 1.25;

final Color toolTipLeftColor = Colors.yellow.shade50;
final Color toolTipRightColor = Colors.yellow.shade200;
const double toolTipHeight = 50;
const double toolTipFontSize = 18;

const Color submitForegroundColor = Colors.black;
const double buttonFontSize = 14;

const IconData topMenuIcon = Icons.menu_sharp;

const Color decorationHoverColor = Colors.amber;
const Color decorationBorderColor = Colors.yellow;
const Color decorationInputColor = Colors.white;

// APPLICATION-SPECIFIC Definitions

const Color topLevelBackground = Color.fromARGB(128, 140, 180, 210);
const String topLevelImage = "assets/images/iqsignstlogo.png";

const Color submitBackgroundColor = Color.fromARGB(255, 204, 224, 249);

const Color themeSeedColor = Color.fromRGBO(72, 85, 121, 1);
const Color borderColor = Colors.lightBlue;
const Color labelColor = Colors.lightBlue;

// end of lookandfeel.dart
