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
import 'dart:math';
import 'locationdata.dart';

class NormalLocationData {
  Map<String, double> _bluetoothMap = {};
  Position? _gpsPosition;
  final DateTime _when = DateTime.now();

  NormalLocationData(this._gpsPosition, List<BluetoothData> bts) {
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

  NormalLocationData.update(this._gpsPosition, this._bluetoothMap);

  NormalLocationData.fromLocation(LocationData loc)
      : this(loc.gpsPosition, loc.getBluetoothValues());

  Position? get gpsPosition => _gpsPosition;
  Map<String, double> get bluetoothMap => _bluetoothMap;

  Map<String, dynamic> toJson() {
    return {
      "bluetoothData": _bluetoothMap,
      "gpsPosition": _gpsPosition?.toJson(),
      "date": _when.toString(),
    };
  }
}
