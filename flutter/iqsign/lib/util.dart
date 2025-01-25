/*
 *        util.dart
 * 
 *    Utility methods
 * 
 */
/*	Copyright 2023 Brown University -- Steven P. Reiss			*/
/// *******************************************************************************
///  Copyright 2023, Brown University, Providence, RI.				 *
///										 *
///			  All Rights Reserved					 *
///										 *
///  Permission to use, copy, modify, and distribute this software and its	 *
///  documentation for any purpose other than its incorporation into a		 *
///  commercial product is hereby granted without fee, provided that the 	 *
///  above copyright notice appear in all copies and that both that		 *
///  copyright notice and this permission notice appear in supporting		 *
///  documentation, and that the name of Brown University not be used in 	 *
///  advertising or publicity pertaining to distribution of the software 	 *
///  without specific, written prior permission. 				 *
///										 *
///  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
///  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
///  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
///  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
///  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
///  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
///  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
///  OF THIS SOFTWARE.								 *
///										 *
///******************************************************************************

library iqsign.util;

import 'dart:convert' as convert;
import 'package:crypto/crypto.dart' as crypto;
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';

bool debugServer = true;

String hasher(String msg) {
  final bytes = convert.utf8.encode(msg);
  crypto.Digest rslt = crypto.sha512.convert(bytes);
  String srslt = convert.base64.encode(rslt.bytes);
  return srslt;
}

bool validateEmail(String email) {
  const res =
      r'^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$';
  final regExp = RegExp(res);
  if (!regExp.hasMatch(email)) return false;
  return true;
}

bool validatePassword(String? pwd) {
  if (pwd == null || pwd == '') return false;
  // check length, contents
  return true;
}

ThemeData getTheme() {
  return ThemeData(
    primarySwatch: Colors.lightBlue,
  );
}

Uri getServerUri(String path, [Map<String, dynamic>? query]) {
  if (kDebugMode && debugServer) {
    return Uri.http("localhost:3336", path, query);
  }
  return Uri.https("sherpa.cs.brown.edu:3336", path, query);
}
