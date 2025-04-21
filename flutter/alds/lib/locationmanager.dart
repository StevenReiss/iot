/********************************************************************************/
/*                                                                              */
/*              locationmanager.dart                                            */
/*                                                                              */
/*      Manage the set of locations and the current location                    */
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

import 'knownlocation.dart';
import 'locationdata.dart';
import 'bluetoothdata.dart';
import 'wifidata.dart';
import 'storage.dart' as storage;
import 'recheck.dart' as recheck;
import 'util.dart' as util;
import 'globals.dart' as globals;
import 'device.dart' as device;
import 'dart:convert';
import 'package:geolocator/geolocator.dart';

class LocationManager {
  Map<String, KnownLocation> _knownLocations = {};
  KnownLocation? _lastLocation;
  KnownLocation? _nextLocation;
  int _nextCount = 0;
  DateTime _lastTime = DateTime.now();

  static final LocationManager _locationManager =
      LocationManager._internal();

  factory LocationManager() {
    return _locationManager;
  }

  LocationManager._internal();

  DateTime get lastTime => _lastTime;
  KnownLocation? get currentLocation => _lastLocation;
  String? get currentLocationName => _lastLocation?.name;
  double get score => _lastLocation?.score ?? 0;

  Future<void> setup() async {
    String? s = await storage.readLocationData();
    _knownLocations = {};
    if (s != null) {
      Map<String, dynamic> map = jsonDecode(s);
      for (MapEntry<String, dynamic> x in map.entries) {
        String nm = x.key;
        Map<String, dynamic> v = x.value;
        KnownLocation kl = KnownLocation.fromJson(v);
        _knownLocations[nm] = kl;
      }
    }
  }

  Future<void> _saveData() async {
    String data = jsonEncode(_knownLocations);
    await storage.saveLocatorData(data);
  }

  Future<void> noteLocation(String loc) async {
    await recheck.recheck(loc);
  }

  Future<void> recheckLocation() async {
    await recheck.recheck();
  }

  Future<void> clear() async {
    _knownLocations.clear();
    _lastLocation = null;
    _nextLocation = null;
    _nextCount = 0;
    _lastTime = DateTime.now();
    await recheckLocation();
    await _saveData();
    util.log("DATA CLEARED");
  }

  Future<void> updateLocation(
    Position? gps,
    List<BluetoothData> btdata,
    WifiData? wifi,
    String? userloc,
  ) async {
    LocationData nld = LocationData(gps, btdata, wifi);
    _lastTime = DateTime.now();

    util.log("Update location at $_lastTime to $userloc");

    bool used = false;
    KnownLocation? kl;
    if (userloc != null) {
      kl = _knownLocations[userloc];
      if (kl == null) {
        kl = KnownLocation(userloc, nld);
        _knownLocations[userloc] = kl;
        used = true;
      } else {
        kl.score = 1.0;
      }
    } else {
      kl = _findBestLocation(nld);
    }

    if (_lastLocation == null || userloc != null) {
      await _changeLocation(kl);
    } else if (kl == _lastLocation) {
      _nextLocation = null;
      _nextCount = 0;
    } else if (kl == _nextLocation) {
      if (++_nextCount >= globals.stableCount ||
          globals.stableCount == 1) {
        await _changeLocation(kl);
        _nextLocation = null;
        _nextCount = 0;
      } else {
        _nextLocation = kl;
        _nextCount == 1;
      }
    }

    if (kl != null && !used && kl.score > globals.useThreshold) {
      kl.addLocation(nld);
      used = true;
    }

    if (used) {
      _saveData();
      device.Cedes().rawData({
        "sample": nld,
        "location": kl?.name,
        "set": userloc,
        "next": _nextLocation?.name,
        "nextCount": _nextCount,
        "data": _knownLocations,
      });
    }
  }

  KnownLocation? _findBestLocation(LocationData nld) {
    bool smartsearch = true;
    int ct = _knownLocations.length;
    for (KnownLocation kl in _knownLocations.values) {
      if (kl.count < globals.significantNumber) {
        smartsearch = false;
        break;
      }
    }
    if (ct < 4) smartsearch = false;
    if (smartsearch) {
      // should do K-nearest-neighbor search here
    }
    return _bestAverageLocation(nld);
  }

  KnownLocation? _bestAverageLocation(LocationData nld) {
    KnownLocation? best;
    double bestscore = -1;
    for (KnownLocation kl in _knownLocations.values) {
      double score = kl.computeAverageScore(nld);
      util.log("Compute score $score for ${kl.name}");
      if (score > bestscore) {
        bestscore = score;
        best = kl;
      }
    }
    if (best != null) {
      best.score = bestscore;
      return best;
    }
    return null;
  }

  Future<void> _changeLocation(KnownLocation? kl) async {
    String nm = kl?.name ?? "";
    util.log("Change location to $nm");
    _lastLocation = kl;
    await device.Cedes().updateLocation(nm);
  }
}
