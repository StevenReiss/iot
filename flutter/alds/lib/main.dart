/********************************************************************************/
/*                                                                              */
/*              main.dart                                                       */
/*                                                                              */
/*      Main program for ALDS: Abstract Location Determination Service?         */
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
import 'package:phone_state/phone_state.dart';
import 'pages/selectpage.dart';
import 'pages/loginpage.dart';
import 'storage.dart' as storage;
import 'globals.dart' as globals;
import 'recheck.dart' as recheck;
import 'device.dart' as device;
import 'util.dart' as util;
import "locationmanager.dart";
import 'dart:async';

void main() {
  initialize(false).then(_startApp);
//   Widget w = storage.isAuthUserSet()
//       ? const AldsSelectPage()
//       : const AldsLoginPage();
//   runApp(
//     MaterialApp(
//       title: "ALDS Location Selector",
//       home: w,
//     ),
//   );
}

FutureOr<dynamic> _startApp(void x) async {
  Widget w = storage.isAuthUserSet()
      ? const AldsSelectPage()
      : const AldsLoginPage();
  runApp(
    MaterialApp(
      title: "ALDS Location Selector",
      home: w,
    ),
  );
}

Future<void> initialize(bool flag) async {
  await util.setup();
  await storage.setupStorage();
  recheck.initialize();
  LocationManager loc = LocationManager();
  await loc.setup();

  Timer.periodic(
    const Duration(seconds: globals.pingEverySeconds),
    _handleDevice,
  );

  handlePhone();
}

Future handlePhone() async {
  PhoneState.stream.forEach(handlePhoneStream);
}

void _handleDevice(Timer timer) async {
  device.Cedes cedes = device.Cedes();
  await cedes.ping();
}

void handlePhoneStream(PhoneState state) {
  PhoneStateStatus sts = state.status;
  sts != PhoneStateStatus.NOTHING;

  switch (sts) {
    case PhoneStateStatus.CALL_STARTED:
      device.Cedes().updatePhoneState(true);
      break;
    case PhoneStateStatus.CALL_ENDED:
      device.Cedes().updatePhoneState(false);
      break;
    case PhoneStateStatus.CALL_INCOMING:
    case PhoneStateStatus.NOTHING:
      break;
  }
}
