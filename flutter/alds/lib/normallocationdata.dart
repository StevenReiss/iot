/********************************************************************************/
/*                                                                              */
/*              normallocationdata.dart                                         */
/*                                                                              */
/*      Representation of normalized location dcata                             */
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
import 'locationdata.dart';
import 'util.dart' as util;
import 'globals.dart' as globals;

class NormalLocationData {
  Map<String, double> _bluetoothMap = {};
  Position? _gpsPosition;
  WifiData? _wifiData;
  final DateTime _when = DateTime.now();

  NormalLocationData(
      this._gpsPosition, List<BluetoothData> bts, this._wifiData) {
    Map<String, double> bmap = {};
    double totsq = 0;
    for (BluetoothData btd in bts) {
      double v = btd.rssi.toDouble();
      v = (v + 128) / 100;
      if (v < 0) continue;
      if (v > 1) v = 1;
      totsq += v * v;
      bmap[btd.id] = v;
    }
    totsq = sqrt(totsq);
    _bluetoothMap = bmap.map(
      (k, v) => MapEntry(
        k,
        v / totsq,
      ),
    );
  }

  NormalLocationData.update(
    this._gpsPosition,
    this._bluetoothMap,
    this._wifiData,
  );

  NormalLocationData.fromLocation(LocationData loc)
      : this(loc.gpsPosition, loc.getBluetoothValues(), loc.wifiData);

  NormalLocationData.fromJson(Map<String, dynamic> json) {
    _bluetoothMap = json["bluetoothData"];
    _gpsPosition = json['gpsPosition'];
    _wifiData = json['wifiData'];
  }

  Position? get gpsPosition => _gpsPosition;
  Map<String, double> get bluetoothMap => _bluetoothMap;
  WifiData? get wifiData => _wifiData;

  double computeScore(NormalLocationData nld) {
    double score0 = _btScore(nld);
    double score1 = _posScore(nld);
    double score2 = _wifiScore(nld);
    double btpart = 0.6;
    double geopart = 0.2;
    double wifipart = 0.2;
    if (score2 == 0) {
      btpart += wifipart / 2;
      geopart += wifipart / 2;
      wifipart = 0;
    }
    if (score1 == 0) {
      if (wifipart == 0) {
        btpart += geopart;
      } else {
        btpart += geopart / 2;
        wifipart += geopart / 2;
      }
    }

    return score0 * btpart + score1 * geopart + score2 * wifipart;
  }

  double _btScore(NormalLocationData nld) {
    double score = 0;
    for (MapEntry<String, double> ent in _bluetoothMap.entries) {
      double? kval = nld._bluetoothMap[ent.key];
      if (kval != null) score += kval * ent.value;
    }
    return score;
  }

  double _posScore(NormalLocationData nld) {
    Position? p0 = _gpsPosition;
    Position? p1 = nld._gpsPosition;
    double score = 0;
    if (p0 != null && p1 != null) {
      double d0 = util.calculateDistance(
        p0.latitude,
        p0.longitude,
        p1.latitude,
        p1.longitude,
      );
      double d1 = max(p0.accuracy, p1.accuracy);
      util.log("GPS DISTANCE $d0 $d1 $p0 $p1");
      double d2 = d0 / (2 * d1);
      d2 = (1.0 - d2);
      if (d2 < 0) d2 = 0;
      double a0 = (p0.altitude - p1.altitude).abs() / 5;
      double a1 = (1.0 - a0);
      if (a1 < 0) a1 = 0;
      util.log("GPS SCORES $d2 $a1");
      score = (globals.locFraction + globals.altFraction) / 2.0;
    }
    return score;
  }

  double _wifiScore(NormalLocationData nld) {
    if (nld._wifiData == null && _wifiData == null) {
      return 0.5;
    } else if (nld._wifiData == null || _wifiData == null) {
      return 0.25;
    } else if (nld._wifiData == _wifiData) {
      return 1.0;
    } else {
      return 0.1;
    }
  }

  Map<String, dynamic> toJson() {
    return {
      "bluetoothData": _bluetoothMap,
      "gpsPosition": _gpsPosition?.toJson(),
      "wifiData": _wifiData?.toJson(),
      "date": _when.toString(),
    };
  }
}
