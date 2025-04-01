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

class SherpaDevicePage extends StatefulWidget {
  final CatreDevice _forDevice;
  final Map<String, dynamic> _states;

  const SherpaDevicePage(this._forDevice, this._states, {super.key});

  @override
  State<SherpaDevicePage> createState() => _SherpaDevicePageState();
}

class _SherpaDevicePageState extends State<SherpaDevicePage> {
  late CatreDevice _forDevice;
  late Map<String, dynamic> _states;

  _SherpaDevicePageState();

  @override
  void initState() {
    _forDevice = widget._forDevice;
    _states = widget._states;
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
}
