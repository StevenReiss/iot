/********************************************************************************/
/*                                                                              */
/*              locationpage.dart                                               */
/*                                                                              */
/*      Page to edit the set of available locations                             */
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

import '../widgets.dart' as widgets;
import 'package:flutter/material.dart';
import '../storage.dart' as storage;

class AldsLocationWidget extends StatelessWidget {
  const AldsLocationWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return const AldsLocationPage();
  }
}

class AldsLocationPage extends StatefulWidget {
  const AldsLocationPage({super.key});

  @override
  State<AldsLocationPage> createState() {
    return _AldsLocationPageState();
  }
}

class _AldsLocationPageState extends State<AldsLocationPage> {
  List<String> _locations = [];
  bool _isUpdated = false;

  _AldsLocationPageState();

  @override
  void initState() {
    _locations = List<String>.of(storage.getLocations());
    _locations.sort();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    Widget w = widgets.topLevelPage(
      context,
      Column(
        mainAxisAlignment: MainAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          widgets.fieldSeparator(),
          _listWidget(),
          widgets.fieldSeparator(),
          _bottomButtons(),
        ],
      ),
    );
    String ttl = "Edit Abstract Locations";
    Widget top = Scaffold(
      appBar: AppBar(
        title: Text(ttl),
        actions: [
          widgets.topMenuAction([
            widgets.MenuAction(
              "Save Changes",
              _updateLocations,
              "Save the location changes you've made",
            ),
            widgets.MenuAction(
              "Add Location",
              _addLocation,
              "Add a new abstract location to your set of locations",
            ),
          ])
        ],
      ),
      body: w,
    );
    return top;
  }

  Widget _bottomButtons() {
    Widget w = Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: <Widget>[
        widgets.submitButton("Accept", _saveLocations,
            enabled: _isUpdated),
        widgets.submitButton("Cancel", _revertLocations),
      ],
    );
    return w;
  }

  Widget _listWidget() {
    Widget w = Flexible(
      child: widgets.listBox<String>(
        "Location",
        _locations,
        _locationBuilder,
        _addLocation,
      ),
    );
    return w;
  }

  Widget _locationBuilder(String s) {
    List<widgets.MenuAction> acts = [];
    acts.add(widgets.MenuAction(
      'Edit',
      () {
        _editLocation(s);
      },
    ));
    if (_locations.length > 1) {
      acts.add(widgets.MenuAction('Remove', () {
        _removeLocation(s);
      }));
    }

    Widget w = widgets.itemWithMenu(
      s,
      acts,
      onTap: () {
        _editLocation(s);
      },
    );
    return w;
  }

  void _saveLocations() async {
    _updateLocations();
    Navigator.of(context).pop();
  }

  void _revertLocations() async {
    Navigator.of(context).pop();
  }

  void _updateLocations() async {
    storage.setLocations(_locations);
  }

  void _addLocation() async {
    _editLocation("");
  }

  void _editLocation(String s) async {
    String? newloc = await _editLocationDialog(context, s);
    if (newloc == null) return;
    _updateLocation(s, newloc);
  }

  void _updateLocation(String s, String newloc) {
    if (newloc == s) return;
    bool change = false;
    if (s.isEmpty && newloc.isNotEmpty) {
      if (!_locations.contains(newloc)) {
        _locations.add(newloc);
        change = true;
        _locations.sort();
      }
    } else if (s.isNotEmpty) {
      _locations.remove(s);
      if (newloc.isNotEmpty && !_locations.contains(newloc)) {
        _locations.add(newloc);
      }
      change = true;
      _locations.sort();
    }
    setState(() {
      _isUpdated = change;
    });
  }

  void _removeLocation(String s) {
    _updateLocation(s, "");
  }
}

Future<String?> _editLocationDialog(
  BuildContext context,
  String? origname,
) async {
  TextEditingController loccontrol = TextEditingController();
  String name = origname ?? "";

  loccontrol.text = name;

  String? rslt = await showDialog(
    context: context,
    builder: (context) {
      return _AldsEditLocationDialog(
        context,
        loccontrol,
      );
    },
  );

  if (rslt == "OK") {
    return loccontrol.text;
  } else {
    return null;
  }
}

class _AldsEditLocationDialog extends AlertDialog {
  final BuildContext context;
  final TextEditingController locControl;

  _AldsEditLocationDialog(
    this.context,
    this.locControl,
  ) : super(
            title: const Text("Edit/Add Location"),
            content: Padding(
                padding: const EdgeInsets.all(20.0),
                child: SizedBox(
                    width: MediaQuery.of(context).size.width * 0.8,
                    child: Column(
                        mainAxisSize: MainAxisSize.min,
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: <Widget>[
                          widgets.textField(
                            label: "Location",
                            controller: locControl,
                          )
                        ]))),
            actions: <Widget>[
              widgets.submitButton("Cancel", () {
                Navigator.of(context).pop("CANCEL");
              }),
              widgets.submitButton("Accept", () {
                Navigator.of(context).pop("OK");
              }),
            ]);
}
