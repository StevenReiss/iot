/*
 *        util.dart
 * 
 *    Utility methods
 * 
 */

library iqsign.util;

import 'dart:convert' as convert;
import 'package:crypto/crypto.dart' as crypto;
import 'package:flutter/material.dart';
import 'package:flutter_logs/flutter_logs.dart';
// import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import 'dart:math';

Future<void> setup() async {
  WidgetsFlutterBinding.ensureInitialized();
  await FlutterLogs.initLogs(
      logLevelsEnabled: [
        LogLevel.INFO,
        LogLevel.WARNING,
        LogLevel.ERROR,
        LogLevel.SEVERE
      ],
      timeStampFormat: TimeStampFormat.TIME_FORMAT_READABLE,
      directoryStructure: DirectoryStructure.SINGLE_FILE_FOR_DAY,
      logFileExtension: LogFileExtension.LOG,
      logsWriteDirectoryName: 'AldsLogs',
      logsExportDirectoryName: 'AldsLogs/Exported',
      debugFileOperations: true,
      isDebuggable: true);
}

String hasher(String msg) {
  final bytes = convert.utf8.encode(msg);
  crypto.Digest rslt = crypto.sha512.convert(bytes);
  String srslt = convert.base64.encode(rslt.bytes);
  return srslt;
}

ThemeData getTheme() {
  return ThemeData(
    primarySwatch: Colors.lightBlue,
  );
}

String randomString(int len) {
  var r = Random();
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890';
  return List.generate(len, (index) => chars[r.nextInt(chars.length)]).join();
}

// distance in meters
double calculateDistance(lat1, lon1, lat2, lon2) {
  var p = 0.017453292519943295;
  var a = 0.5 -
      cos((lat2 - lat1) * p) / 2 +
      cos(lat1 * p) * cos(lat2 * p) * (1 - cos((lon2 - lon1) * p)) / 2;
  return 12742 * asin(sqrt(a)) * 1000;
}

void log(String pkg, String msg) {
  FlutterLogs.logInfo('ALDS', pkg, msg);
  sendToCedes({"package": pkg, "message": msg});
  // if (kDebugMode) {
  //   print("ALDS:$pkg:$msg");
  // }
}

void flushLogs() {
  FlutterLogs.exportLogs(exportType: ExportType.ALL);
}

Future<void> sendToCedes(Map<String, dynamic> d) async {
  var url = Uri.https('sherpa.cs.brown.edu:3336', '/alds/data', d);
  await http.get(url);
}