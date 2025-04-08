/********************************************************************************/
/*                                                                              */
/*              bluetoothdata.dart                                              */
/*                                                                              */
/*      Represent information about a bluetooth finding                         */
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

Map<String, int> _bluetoothIds = {};

class BluetoothData {
  final String _id;
  int rssi;
  final String _name;
  late int _index;

  BluetoothData(this._id, this.rssi, this._name) {
    int? idx = _bluetoothIds[_id];
    if (idx == null) {
      idx = _bluetoothIds.length + 1;
      _bluetoothIds[_id] = idx;
    }
    _index = idx;
  }

  String get id => _id;
  String get name => _name;
  int get index => _index;

  @override
  String toString() {
    return "BT:$_id = $rssi ($_name)";
  }

  Map<String, dynamic> toJson() {
    return {
      "id": _id,
      "rssi": rssi,
      "name": _name,
      "index": _index,
    };
  }
}
