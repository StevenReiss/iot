/*
 *      recheck.dart
 * 
 *    Code to try to compute location periodically
 * 
 */
/*      Copyright 2023 Brown University -- Steven P. Reiss                      */
/// *******************************************************************************
///  Copyright 2023, Brown University, Providence, RI.                           *
///                                                                              *
///                       All Rights Reserved                                    *
///                                                                              *
///  Permission to use, copy, modify, and distribute this software and its       *
///  documentation for any purpose other than its incorporation into a           *
///  commercial product is hereby granted without fee, provided that the         *
///  above copyright notice appear in all copies and that both that              *
///  copyright notice and this permission notice appear in supporting            *
///  documentation, and that the name of Brown University not be used in         *
///  advertising or publicity pertaining to distribution of the software         *
///  without specific, written prior permission.                                 *
///                                                                              *
///  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS               *
///  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND           *
///  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY     *
///  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY         *
///  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,             *
///  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS              *
///  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE         *
///  OF THIS SOFTWARE.                                                           *
///                                                                              *
///******************************************************************************

library alds.recheck;

import 'dart:io';
import 'dart:async';
import 'package:geolocator/geolocator.dart';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';
import 'locator.dart';
import 'package:mutex/mutex.dart';
import 'util.dart' as util;
import 'package:flutter/foundation.dart';

bool _checkLocation = false;
bool _checkBluetooth = false;
bool _haveBluetooth = false;
Locator _locator = Locator();
final _doingRecheck = Mutex();
dynamic _subscription;
bool _geolocEnabled = false;
BluetoothAdapterState _adapterState = BluetoothAdapterState.unknown;

Future<void> initialize() async {
  FlutterBluePlus.setLogLevel(LogLevel.verbose, color: false);
  FlutterBluePlus.logs.listen((String s) {
    util.log(s);
  });

  _geolocEnabled = await Geolocator.isLocationServiceEnabled();
  LocationPermission perm = LocationPermission.denied;
  if (_geolocEnabled) {
    perm = await Geolocator.checkPermission();
    if (perm == LocationPermission.denied) {
      perm = await Geolocator.requestPermission();
    }
    if (perm != LocationPermission.denied) _checkLocation = true;
  }
  util.log("CHECK GEOLOCATION $perm $_checkLocation");
  _haveBluetooth = await FlutterBluePlus.isSupported;
  _subscription = FlutterBluePlus.adapterState.listen(_bluetoothSub);

  if (_haveBluetooth && !kIsWeb && Platform.isAndroid) {
    await FlutterBluePlus.turnOn();
  }

  String state = _adapterState.toString().split(".").last;
  util.log("CHECK BT $_checkBluetooth $_haveBluetooth $_checkBluetooth $state");
}

void dispose() {
  if (_subscription != null) {
    _subscription.cancel();
  }
}

void _bluetoothSub(BluetoothAdapterState state) {
  _adapterState = state;
  if (state == BluetoothAdapterState.on) {
    _checkBluetooth = true;
  } else {
    _checkBluetooth = false;
  }
}

Future<LocationData> recheck() async {
  await _doingRecheck.acquire();
  try {
    util.log("START RECHECK");
    Position? curpos;
    if (_checkLocation) {
      try {
        curpos = await Geolocator.getCurrentPosition().timeout(const Duration(seconds: 5));
        double lat = curpos.latitude;
        double long = curpos.longitude;
        double elev = curpos.altitude;
        double speed = curpos.speed;
        double speeda = curpos.speedAccuracy;
        double posa = curpos.accuracy;
        util.log("GEO FOUND $lat $long $elev $speed $speeda $posa");
      } catch (e) {
        util.log("NO GEO LOCATION $e");
      }
    }

    List<BluetoothData> btdata = [];

    var sub1 = FlutterBluePlus.onScanResults.listen(
      (List<ScanResult> scanrslts) {
        _btscan0(scanrslts, btdata);
      },
      onError: (e) => util.log("Bluetooth scan error $e"),
      onDone: _btscanDone,
    );
    FlutterBluePlus.cancelWhenScanComplete(sub1);
    await FlutterBluePlus.adapterState.where((val) => val == BluetoothAdapterState.on).first;

    await FlutterBluePlus.startScan(
      timeout: const Duration(seconds: 6),
    );

    await FlutterBluePlus.isScanning.where((val) => val == false).first;

    //  Stream<ScanResult> st = _flutterBlue.scan(timeout: const Duration(seconds: 6),);
    //  List<BluetoothData> btdata = await st.fold([], _btscan2);

    // no way to scan wifi access points on ios

    LocationData rslt = _locator.updateLocation(curpos, btdata);
    util.log("FINISHED RECHECK)");
    return rslt;
  } finally {
    _doingRecheck.release();
  }
}

void _btscan0(List<ScanResult> btrslts, List<BluetoothData> rslt) {
  for (ScanResult sr in btrslts) {
    _btscan1(sr);
    _btscan2(rslt, sr);
  }
}

void _btscanDone() {
  util.log("Bluetooth Scan Done");
}

void _btscan1(ScanResult r) async {
  int rssi = r.rssi;
  BluetoothDevice dev = r.device;

  String name = dev.platformName;
  String mac = dev.remoteId.str;
  String typ = dev.advName;
  String svd = r.advertisementData.serviceData.toString();
  String mfd = r.advertisementData.manufacturerData.toString();
  bool conn = r.advertisementData.connectable;
  String lname = r.advertisementData.advName;
  util.log("BT FOUND $mac $conn $rssi $typ $svd $mfd $name $lname");
}

List<BluetoothData> _btscan2(List<BluetoothData> bl, ScanResult r) {
  _btscan1(r);
  BluetoothData btd = BluetoothData(
    r.device.remoteId.str,
    r.rssi,
    r.device.platformName,
  );
  bl.add(btd);
  return bl;
}
