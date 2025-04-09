/********************************************************************************/
/*                                                                              */
/*              locationdata.dart                                               */
/*                                                                              */
/*      Representation of the current locations                                 */
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

import 'package:geolocator/geolocator.dart';
import 'bluetoothdata.dart';
import 'wifidata.dart';
import 'dart:math';

class LocationData {
  Map<String, BluetoothData> _bluetoothData = {};
  Position? _gpsPosition;
  WifiData? _wifiData;
  DateTime _when = DateTime.now();
  int _count = 1;

  LocationData(
      this._gpsPosition, List<BluetoothData> bts, WifiData? wifi) {
    _bluetoothData = {
      for (BluetoothData bt in bts) bt.id: bt,
    };
    _wifiData = wifi;
  }

  Position? get gpsPosition => _gpsPosition;
  Map<String, BluetoothData> get bluetoothData => _bluetoothData;
  WifiData? get wifiData => _wifiData;

  List<BluetoothData> getBluetoothValues() {
    List<BluetoothData> rslt = [];
    for (BluetoothData x in _bluetoothData.values) {
      rslt.add(x);
    }
    return rslt;
  }

  LocationData? merge(Position? pos, List<BluetoothData> btdata) {
    int ct = 0;
    Map<String, BluetoothData> nmap = {};
    for (BluetoothData bd in btdata) {
      if (bd.rssi == 127) bd.rssi = -127;
      BluetoothData? match = _bluetoothData[bd.id];
      if (match == null) return null; // new bluetooth item
      int delta = (match.rssi - bd.rssi).abs();
      if (delta > 4) return null;
      int nrssi = ((match.rssi * _count + bd.rssi) ~/ (_count + 1));
      nmap[bd.id] = BluetoothData(bd.id, nrssi, bd.name);
      ++ct;
    }
    if (ct != _bluetoothData.length) return null;
    Position? gpos = _gpsPosition;
    Position? npos;
    if (pos != null && gpos != null) {
      if (_gpsPosition != null) {
        double d1 = (pos.latitude - gpos.latitude).abs();
        if (d1 > pos.accuracy / 2) return null;
        double d2 = (pos.longitude - gpos.longitude).abs();
        if (d2 > pos.accuracy / 2) return null;
        double d3 = (pos.altitude - gpos.altitude).abs();
        if (d3 > pos.accuracy / 4) return null;
        double d4 = (pos.speed - gpos.speed).abs();
        if (d4 > pos.speedAccuracy / 2) return null;
      }
      npos = Position(
        latitude:
            (gpos.latitude * _count + pos.latitude) / (_count + 1),
        longitude:
            (gpos.longitude * _count + pos.longitude) / (_count + 1),
        accuracy: max(gpos.accuracy, pos.accuracy),
        timestamp: gpos.timestamp,
        altitude:
            (gpos.altitude * _count + pos.altitude) / (_count + 1),
        altitudeAccuracy: max(
          gpos.altitudeAccuracy,
          pos.altitudeAccuracy,
        ),
        heading: gpos.heading,
        headingAccuracy: max(
          gpos.headingAccuracy,
          pos.headingAccuracy,
        ),
        speed: max(gpos.speed, pos.speed),
        speedAccuracy: max(
          gpos.speedAccuracy,
          pos.speedAccuracy,
        ),
      );
    }

    _bluetoothData = nmap;
    _gpsPosition = npos;
    _count++;
    _when = DateTime.now();

    return this;
  }

  Map<String, dynamic> toJson() {
    List btdata = _bluetoothData.values
        .map(
          (BluetoothData bd) => bd.toJson(),
        )
        .toList();
    return {
      "bluetoothData": btdata,
      "gpsPosition": _gpsPosition?.toJson(),
      "count": _count,
      "date": _when.toString(),
    };
  }
}
