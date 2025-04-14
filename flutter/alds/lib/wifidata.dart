/********************************************************************************/
/*                                                                              */
/*              recheck.dart                                                    */
/*                                                                              */
/*      Code to recompute the location periodially                              */
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

import 'package:network_info_plus/network_info_plus.dart';

Map<String, int> _wifiIds = {};
//  This should probably use wifi_scan and set up an array of items
//    similar to that used by bluetooth.   However, this will not
//    work on current mac and possibly future android, so its not
//    worth it for now.

class WifiData {
  String? _bssId;
  final _networkInfo = NetworkInfo();

  WifiData();

  WifiData.fromJson(Map<String, dynamic> json) {
    _bssId = json['bssId'];
  }

  String? get bssId => _bssId;
  int get wifiId => _bssId == null ? 0 : _wifiIds[bssId] as int;

  Future<void> update() async {
    String? newid = await _networkInfo.getWifiBSSID();
    if (newid != null) {
      int? idx = _wifiIds[newid];
      if (idx == null) {
        idx = _wifiIds.length + 1;
        _wifiIds[newid] = idx;
      }
      _bssId = newid;
    }
  }

  Map<String, dynamic> toJson() {
    return {
      "bssId": _bssId,
    };
  }
}
