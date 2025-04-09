/********************************************************************************/
/*                                                                              */
/*              storage.dart                                                    */
/*                                                                              */
/*      Persistent storage for ALDS                                             */
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

library alds.storage;

import 'package:hive_flutter/hive_flutter.dart';
import 'globals.dart' as globals;
import 'util.dart' as util;

AuthData _authData = AuthData('*', '*', false);
List<String> _locations = globals.defaultLocations;
String _deviceId = "*";

class AuthData {
  final String _userId;
  final String _userPass;
  final bool _userSet;

  AuthData(this._userId, this._userPass, this._userSet);

  String get userId => _userId;
  String get password => _userPass;
  bool get userSet => _userSet;
} // end of inner class AuthData

Future<void> setupStorage() async {
  await Hive.initFlutter();
  var appbox = await Hive.openBox('appData');
//   appbox.clear(); // REMOVE IN PRODUCTION
//   appbox.delete("locdata"); // REMOVE IN PRODUTION
  bool setup = await appbox.get(
    "setup",
    defaultValue: false,
  );
  String uid = await appbox.get(
    "userid",
    defaultValue: util.randomString(12),
  );
  String upa = await appbox.get(
    "userpass",
    defaultValue: util.randomString(16),
  );
  bool uset = await appbox.get(
    "userset",
    defaultValue: false,
  );

  _authData = AuthData(uid, upa, uset);
  _locations = appbox.get(
    "locations",
    defaultValue: globals.defaultLocations,
  );
  _deviceId = appbox.get(
    "deviceid",
    defaultValue: "ALDS_${util.randomString(20)}",
  );
  if (!setup) {
    await saveData(appbox);
  }
}

Future<void> saveData([
  dynamic appbox,
]) async {
  appbox ??= Hive.box('appData');
  await appbox.put('setup', true);
  if (_authData.userId == '*') {
    await appbox.delete('userid');
  } else {
    await appbox.put('userid', _authData.userId);
  }
  if (_authData.password == '*') {
    await appbox.delete('userpass');
  } else {
    await appbox.put('userpass', _authData.password);
  }
  await appbox.put('userset', _authData.userSet);
  await appbox.put('locations', _locations);
  await appbox.put('deviceid', _deviceId);
}

AuthData getAuthData() {
  return _authData;
}

bool isAuthUserSet() {
  return _authData.userSet;
}

List<String> getLocations() {
  return _locations;
}

String getDeviceId() {
  return _deviceId;
}

Future<void> saveLocatorData(String json) async {
  var appbox = Hive.box('appData');
  await appbox.put("locdata", json);
}

Future<String?> readLocationData() async {
  var appbox = Hive.box('appData');
  return await appbox.get('locdata');
}

Future<void> setAuthData(String uid, String pwd) async {
  _authData = AuthData(uid, pwd, true);
  await saveData();
}

Future<void> setLocations(List<String> locations) async {
  _locations = List<String>.from(locations);
  await saveData();
}
