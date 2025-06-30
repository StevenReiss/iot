/********************************************************************************/
/*                                                                              */
/*              programpage.dart                                                */
/*                                                                              */
/*      Overview page fo the user's program                                     */
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

import 'dart:convert' as convert;
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:sherpa/globals.dart' as globals;
import 'package:sherpa/util.dart' as util;
import 'package:sherpa/widgets.dart' as widgets;
import 'package:sherpa/levels.dart' as levels;
import 'package:sherpa/models/catremodel.dart';
import 'package:sherpa/pages/authorizationpage.dart';
import 'package:sherpa/pages/devicepage.dart';
import 'package:sherpa/pages/addweatherpage.dart';
import 'package:sherpa/pages/addrsspage.dart';
import 'loginpage.dart' as login;
import 'rulesetpage.dart';
import 'package:sherpa/lookandfeel.dart' as laf;
import 'package:file_picker/file_picker.dart';

/********************************************************************************/
/*                                                                              */
/*      Widget definitions                                                      */
/*                                                                              */
/********************************************************************************/

class SherpaProgramWidget extends StatefulWidget {
  final CatreUniverse _theUniverse;

  const SherpaProgramWidget(this._theUniverse, {super.key});

  @override
  State<SherpaProgramWidget> createState() =>
      _SherpaProgramWidgetState();
}

class _SherpaProgramWidgetState extends State<SherpaProgramWidget> {
  CatreDevice? _forDevice;
  late CatreUniverse _theUniverse;
  CatreDevice? _removeDevice;

  _SherpaProgramWidgetState();

  @override
  void initState() {
    _theUniverse = widget._theUniverse;
    // possibly save and recall _forDevice name
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    List<Widget> comps = util.skipNulls([
      _createPriorityView(levels.overrideLevel, true),
      _createPriorityView(levels.highLevel, false),
      _createPriorityView(levels.mediumLevel, false),
      _createPriorityView(levels.lowLevel, false),
      _createPriorityView(levels.defaultLevel, true),
    ]);

    return Scaffold(
      appBar: AppBar(
        title: const Text("SherPA Program"),
        actions: [
          widgets.topMenuAction([
            widgets.MenuAction(
              'Restore or Reload Program',
              _reloadProgram,
              "This will reload the program from the server.",
            ),
            widgets.MenuAction(
              'Add or Modify Authorizations',
              _handleAuthorizations,
              "View or change the authorizations for devices in your universe.",
            ),
            widgets.MenuAction(
              'Show Device Status',
              _showStates,
              "Show the current status of the device",
            ),
            widgets.MenuAction(
              'Create Weather Device',
              _showAddWeatherPage,
              "Create a device for the weather at a given location",
            ),
            widgets.MenuAction(
              'Create RSS Device',
              _showAddRssPage,
              "Create a device for the weather at a given location",
            ),
            widgets.MenuAction('Remove Device', _handleRemoveDevice,
                "Remove a device from your universe."),
            // widgets.MenuAction(
            //   'Upload Rule File',
            //   _uploadRules,
            //   "Upload a saved rule file",
            // ),
            // widgets.MenuAction(
            //   'Save Current Rules',
            //   _saveRules,
            //   "Save current rule set into a file",
            // ),
//          widgets.MenuAction(
//            'Create Virtual Condition',
//            _createVirtualCondition,
//          ),
            widgets.MenuAction(
              'Clean Shared Conditions',
              _handleCleanShared,
              "Remove unused shared conditions",
            ),
            widgets.MenuAction('Log Off', _logOff),
          ]),
        ],
      ),
      body: widgets.topLevelPage(
        context,
        Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            children: <Widget>[
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
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
                    value: _forDevice,
                    tooltip:
                        "Select the device whose rules you with to view or edit",
                  )),
                ],
              ),
              widgets.fieldSeparator(),
              Column(
                children: <Widget>[...comps],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _reloadProgram() async {
    CatreModel cm = CatreModel();
    _theUniverse = await cm.loadUniverse();
    setState(() {});
  }

  void _handleAuthorizations() {
    widgets.goto(context, SherpaAuthorizeWidget(_theUniverse));
  }

  void _handleRemoveDevice() async {
    _removeDevice = null;
    bool? sts = await showDialog<bool>(
      context: context,
      builder: (BuildContext context) {
        return SimpleDialog(
          title: const Text("Select Device to Remove"),
          children: <Widget>[
            _createDeviceSelector(
              onChanged: _removeDeviceSelected,
              nullValue: "No Device",
              useAll: true,
              value: null,
              tooltip:
                  "Select the device you wish to remove from your universe. "
                  "This cannot be undone easily.",
            ),
            Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: <Widget>[
                  SimpleDialogOption(
                    onPressed: () {
                      Navigator.pop(context, true);
                    },
                    child: const Text("Remove"),
                  ),
                  SimpleDialogOption(
                    onPressed: () {
                      Navigator.pop(context, false);
                    },
                    child: const Text("Cancel"),
                  ),
                ]),
          ],
        );
      },
    );
    if (sts == null || !sts || _removeDevice == null) return;
    bool sts1 = await _theUniverse.removeDevice(_removeDevice);
    if (sts1) {
      setState(() => {});
    }
  }

  void _removeDeviceSelected(CatreDevice? dev) {
    _removeDevice = dev;
  }

  void _logOff() {
    CatreModel cm = CatreModel();
    cm.removeUniverse();
    widgets.gotoReplace(context, const login.SherpaLoginWidget());
  }

  Widget _createDeviceSelector({
    void Function(CatreDevice?)? onChanged,
    String? nullValue = "All Devices",
    bool useAll = false,
    String tooltip = "",
    CatreDevice? value,
  }) {
    List<CatreDevice> devs = _theUniverse.getOutputDevices().toList();
    if (useAll) devs = _theUniverse.getDevices();
    devs.sort(_deviceSorter);
    onChanged ??= _deviceSelected;
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

  Future<void> _deviceSelected(CatreDevice? device) async {
    if (device != null) {
      await device.updateValues();
    }
    setState(() => _forDevice = device);
  }

  void _handleSelect(levels.PriorityLevel lvl) async {
    CatreProgram pgm = _theUniverse.getProgram();
    if (_forDevice != null) {
      List<CatreRule> all = pgm.getSelectedRules(null, _forDevice);
      if (all.length <= 5) lvl = levels.allLevel;
    }
    await widgets.gotoThen(
        context,
        SherpaRulesetWidget(
          _theUniverse,
          _forDevice,
          lvl,
        ));
    setState(() {});
  }

  void _showStates() async {
    BuildContext bcontext = context;

    CatreDevice? cd = _forDevice;
    Map<String, dynamic>? states;
    if (cd != null) {
      states = await util.postJson(
        "/universe/deviceStates",
        {"DEVICEID": cd.getDeviceId()},
      );
    }
    if (bcontext.mounted) {
      await widgets.gotoThen(
          bcontext,
          SherpaDevicePage(
            _theUniverse,
            cd,
            states,
          ));
    }
    setState(() {});
  }

  void _showAddWeatherPage() async {
    await widgets.gotoThen(
      context,
      SherpaAddWeatherPage(_theUniverse),
    );
  }

  void _showAddRssPage() async {
    await widgets.gotoThen(context, SherpaAddRssPage(_theUniverse));
  }

  Widget? _createPriorityView(levels.PriorityLevel lvl, bool optional) {
    CatreProgram pgm = _theUniverse.getProgram();
    int ct = 0;
    List<String> rules = [];
    List<String> tips = [];
    for (CatreRule cr in pgm.getRules()) {
      if (cr.getPriority() < lvl.lowPriority ||
          cr.getPriority() >= lvl.highPriority) {
        continue;
      }
      CatreDevice? cd = cr.getDevice();
      if (_forDevice != null && cd != _forDevice) continue;
      ++ct;
      if (ct <= globals.numRulesToDisplay) {
        rules.add(cr.getLabel());
        tips.add(cr.getDescription());
      } else if (ct == globals.numRulesToDisplay + 1) {
        String s = rules[globals.numRulesToDisplay - 2];
        rules[globals.numRulesToDisplay - 2] = "$s ...";
        rules[globals.numRulesToDisplay - 1] = cr.getLabel();
        tips[globals.numRulesToDisplay - 1] = cr.getDescription();
      } else {
        rules[globals.numRulesToDisplay - 1] = cr.getLabel();
        tips[globals.numRulesToDisplay - 1] = cr.getDescription();
      }
    }
    if (ct == 0 && optional) return null;
    String nrul = "$ct Rule${(ct == 1) ? '' : 's'}";
    TextStyle lblstyle = const TextStyle(
      fontWeight: FontWeight.bold,
      color: laf.labelColor,
      fontSize: 20.0,
    );
    List<Widget> rulew = [];
    for (int i = 0; i < rules.length; ++i) {
      Widget tw = Text(rules[i]);
      Widget tw1 = widgets.tooltipWidget(tips[i], tw);
      rulew.add(tw1);
    }
    Text label = Text(
      "${lvl.name} Rules",
      textAlign: TextAlign.left,
      style: lblstyle,
    );
    Widget w1 = GestureDetector(
      onTap: () => _handleSelect(lvl),
      onDoubleTap: () => _handleSelect(lvl),
      child: Container(
        padding: const EdgeInsets.all(4.0),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          border: Border.all(
            width: 4,
            color: laf.borderColor,
          ),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Row(
              children: <Widget>[
                Expanded(child: label),
                Text(nrul),
              ],
            ),
            ...rulew,
          ],
        ),
      ),
    );

    Widget w2 = widgets.tooltipWidget(
      "Tap to select the priority level to work on",
      w1,
    );
    return w2;
  }

  // ignore: unused_element
  void _saveRules() async {
    String? result = await FilePicker.platform.saveFile(
      dialogTitle: "Select json file to write rules",
      type: FileType.any,
    );
    if (result != null) {
      List<dynamic> rules = [];
      for (CatreRule cr in _theUniverse.getProgram().getRules()) {
        Map<String, dynamic> crd = cr.getCatreOutput();
        rules.add(crd);
      }
      String data = convert.jsonEncode(rules);
      try {
        File file = File(result);
        await file.writeAsString(data);
      } catch (e) {
        return;
      }
    }
  }

  // ignore: unused_element
  void _uploadRules() async {
    FilePickerResult? result = await FilePicker.platform.pickFiles(
      dialogTitle: "Select rule file to upload",
    );
    if (result != null) {
      try {
        File file = File(result.files.single.path!);
        String cnts = await file.readAsString();
        List<dynamic> rules = convert.jsonDecode(cnts);
        await _uploadRuleArray(rules);
        await _reloadProgram();
      } catch (e) {
        return;
      }
    }
  }

  Future<void> _uploadRuleArray(List<dynamic> rules) async {
    for (Map<String, dynamic> rule in rules) {
      CatreRule cr = CatreRule.build(_theUniverse, rule);
      await cr.addOrEditRule();
    }
  }

  Future<void> _handleCleanShared() async {
    await util.postJson("/universe/cleanShared", null);
    await _reloadProgram();
  }
}
