/********************************************************************************/
/*                                                                              */
/*              knownlocation.dart                                              */
/*                                                                              */
/*      Representation of a known abstract location                             */
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
import 'normallocationdata.dart';
import 'dart:math';
import 'dart:convert';
import 'globals.dart' as globals;

class KnownLocation {
  late NormalLocationData _averageLocation;
  late String _locationName;
  int _count = 1;
  List<NormalLocationData> _samples = [];
  final Random _rand = Random();

  KnownLocation(this._locationName, NormalLocationData nloc) {
    _averageLocation = nloc;
    for (int i = 0; i < globals.numberLocationEntries; ++i) {
      _samples.add(nloc);
    }
  }

  void addLocation(
    NormalLocationData nld, [
    bool force = false,
  ]) {
    if (_count < globals.numberLocationEntries) {
      _samples[_count] = nld;
    } else {
      int max = _count;
      if (force) max = globals.numberLocationEntries;
      int idx = _rand.nextInt(max);
      if (idx < _samples.length) {
        _samples[idx] = nld;
      } else if (idx <
          globals.numberLocationEntries + globals.numberSampleEntries) {
        _samples.add(nld);
      }
      // still gathering original sample set
    }
    _updateAverage(nld);
    _count++;
  }

  void _updateAverage(NormalLocationData nld) {
    Map<String, double> btmap = {};
    double ct = _count.toDouble();
    double kct = 1;
    double tct = ct + kct;
    for (MapEntry<String, double> ent
        in _averageLocation.bluetoothMap.entries) {
      double dv = nld.bluetoothMap[ent.key] ?? 0;
      btmap[ent.key] = (ent.value * ct + dv * kct) / tct;
    }
    for (MapEntry<String, double> ent in nld.bluetoothMap.entries) {
      if (btmap[ent.key] == null) {
        btmap[ent.key] = ent.value * kct / tct;
      }
    }
    Position? p0 = _averageLocation.gpsPosition;
    Position? p1 = nld.gpsPosition;
    Position? npos = p0;
    // if the distance is too great, clear position so it isn't used
    if (p0 != null && p1 != null) {
      // accuracy should include max distance as well
      npos = Position(
        latitude: (p0.latitude * ct + p1.latitude * kct) / tct,
        longitude: (p0.longitude * ct + p1.longitude * kct) / tct,
        accuracy: max(p0.accuracy, p1.accuracy),
        timestamp: p0.timestamp,
        altitude: (p0.altitude * ct + p1.altitude * kct) / tct,
        altitudeAccuracy: max(
          p0.altitudeAccuracy,
          p1.altitudeAccuracy,
        ),
        heading: p0.heading,
        headingAccuracy: max(
          p0.headingAccuracy,
          p1.headingAccuracy,
        ),
        speed: max(p0.speed, p1.speed),
        speedAccuracy: max(
          p0.speedAccuracy,
          p1.speedAccuracy,
        ),
      );
    } else if (p1 != null) {
      npos = p1;
    }
    _averageLocation = NormalLocationData.update(npos, btmap);
  }

  Map toJson() {
    return {
      "samples": jsonEncode(_samples),
      "location": _locationName,
      "count": _count,
    };
  }

  KnownLocation.fromJson(Map<String, dynamic> json) {
    _count = json['count'];
    _locationName = json['location'] as String;
    _samples = jsonDecode(json['samples']);
  }
}
