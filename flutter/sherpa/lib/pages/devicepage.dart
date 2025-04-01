/********************************************************************************/
/*                                                                              */
/*              devicepage.dart                                                 */
/*                                                                              */
/*      Page to show properties and status of a device                          */
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

import 'package:flutter/material.dart';
import 'package:sherpa/models/catremodel.dart';
import '../widgets.dart' as widgets;
import '../util.dart' as util;

class SherpaDevicePage extends StatefulWidget {
  final CatreUniverse _forUniverse;
  final CatreDevice? _forDevice;
  final Map<String, dynamic>? _states;

  const SherpaDevicePage(
    this._forUniverse,
    this._forDevice,
    this._states, {
    super.key,
  });

  @override
  State<SherpaDevicePage> createState() => _SherpaDevicePageState();
}

class _SherpaDevicePageState extends State<SherpaDevicePage> {
  late final CatreUniverse _forUniverse;
  late CatreDevice _forDevice;
  late Map<String, dynamic> _states;

  _SherpaDevicePageState();

  @override
  void initState() {
    _forUniverse = widget._forUniverse;
    if (widget._forDevice == null) {
      List<CatreDevice> devs = _forUniverse.getDevices();
      _forDevice = devs[0];
      _states = {};
      util.postJson(
        "/universe/deviceStates",
        {"DEVICEID": _forDevice.getDeviceId()},
      ).then((Map<String, dynamic>? states) {
        if (states != null) {
          setState(() {
            _states = states;
          });
        }
      });
    } else {
      _forDevice = widget._forDevice as CatreDevice;
      _states = widget._states as Map<String, dynamic>;
    }
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    List<TableRow> rows = [];
    addRow(rows, "Name", _forDevice.getName());
    addRow(rows, "Label", _forDevice.getLabel());
    addRow(rows, "Description", _forDevice.getDescription());
    addRow(rows, "Enabled", _forDevice.isEnabled() ? "true" : "false");
    for (CatreParameter cp in _forDevice.getParameters()) {
      if (!cp.isSensor()) continue;
      dynamic v1 = _states[cp.getName()];
      String val = "<Unknown>";
      if (v1 != null) val = v1.toString();
      String units = cp.getDefaultUnit() ?? "";
      if (units.isNotEmpty) {
        val += " $units";
      }
      addRow(rows, cp.getName(), val);
    }
    Widget sel = Row(
      mainAxisAlignment: MainAxisAlignment.start,
      children: <Widget>[
        const Text(
          "Rules for Device:   ",
          style: TextStyle(
            fontWeight: FontWeight.bold,
            color: Colors.brown,
          ),
        ),
        widgets.fieldSeparator(),
        Expanded(
          child: _createDeviceSelector(
            useAll: true,
            tooltip: "Select the device to see its current status",
          ),
        ),
      ],
    );

    Map<int, TableColumnWidth> widths = {
      0: const IntrinsicColumnWidth(),
      1: const FlexColumnWidth(),
    };

    Widget table = Table(
      children: rows,
      columnWidths: widths,
      defaultVerticalAlignment: TableCellVerticalAlignment.middle,
      border: TableBorder.all(),
    );

    return Scaffold(
      appBar: AppBar(
        title: Text("Device ${_forDevice.getName()}"),
        actions: const [], // possibly add device-specific actions
      ),
      body: widgets.topLevelPage(
        context,
        Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            children: <Widget>[
              sel,
              widgets.fieldSeparator(),
              table,
              // possibly add bottom buttons to go back
            ],
          ),
        ),
      ),
    );
  }

  void addRow(List<TableRow> rows, String lbl, String value) {
    Widget lblw = Text(
      lbl,
      style: const TextStyle(
        fontWeight: FontWeight.bold,
      ),
    );
    Widget fldw = Text(
      value,
      overflow: TextOverflow.visible,
    );
    Widget w1 = Container(
      margin: const EdgeInsets.only(left: 5, right: 5, bottom: 2),
      child: fldw,
    );
    Widget w2 = Container(
      margin: const EdgeInsets.only(left: 5, right: 5),
      child: lblw,
    );
    TableRow row = TableRow(
      children: <Widget>[w2, w1],
    );
    rows.add(row);
  }

  void _updateDevice(CatreDevice? cd) async {
    if (cd == null || cd == _forDevice) return;
    Map<String, dynamic>? states = await util.postJson(
      "/universe/deviceStates",
      {"DEVICEID": cd.getDeviceId()},
    );
    setState(() {
      _forDevice = cd;
      _states = states;
    });
  }

  Widget _createDeviceSelector({
    void Function(CatreDevice?)? onChanged,
    String? nullValue = "All Devices",
    bool useAll = false,
    String tooltip = "",
  }) {
    onChanged ??= _updateDevice;
    List<CatreDevice> devs = _forUniverse.getOutputDevices().toList();
    if (useAll) devs = _forUniverse.getDevices();
    devs.sort(_deviceSorter);
    return widgets.dropDownWidget<CatreDevice>(
      devs,
      labeler: (CatreDevice d) => d.getLabel(),
      onChanged: onChanged,
      value: _forDevice,
      nullValue: nullValue,
      tooltip: tooltip,
    );
  }

  int _deviceSorter(CatreDevice cd1, CatreDevice cd2) {
    String s1 = cd1.getLabel().toLowerCase();
    String s2 = cd2.getLabel().toLowerCase();
    return s1.compareTo(s2);
  }
}
