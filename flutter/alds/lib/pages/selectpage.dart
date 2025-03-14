/*
 *      selectpage.dart 
 *    
 *    Main page for specifying/viewing/selecting room
 * 
 */
/*      Copyright 2023 Brown University -- Steven P. Reiss                      */
/// *******************************************************************************
///  Copyright 2023, Brown University, Providence, RI.                           *
///                                                                              *
///                       All Rights Reserved                                    *
///                                                                              *
///  Permission to use, copy, modify, and distribute this software and its       *
///  documentation for any purpose other than its incorporation into a           *
///  commercial product is hereby granted without fee, provided that the         *
///  above copyright notice appear in all copies and that both that              *
///  copyright notice and this permission notice appear in supporting            *
///  documentation, and that the name of Brown University not be used in         *
///  advertising or publicity pertaining to distribution of the software         *
///  without specific, written prior permission.                                 *
///                                                                              *
///  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS               *
///  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND           *
///  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY     *
///  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY         *
///  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,             *
///  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS              *
///  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE         *
///  OF THIS SOFTWARE.                                                           *
///                                                                              *
///******************************************************************************

import 'package:flutter/material.dart';

import '../storage.dart' as storage;
import '../util.dart' as util;
import '../widgets.dart' as widgets;
import "../locator.dart";
import "logindialog.dart";
import "locationpage.dart";

class AldsSelectPage extends StatelessWidget {
  const AldsSelectPage({super.key});

  @override
  Widget build(BuildContext context) {
    return const AldsSelectWidget();
  }
}

class AldsSelectWidget extends StatefulWidget {
  const AldsSelectWidget({super.key});

  @override
  State<AldsSelectWidget> createState() => _AldsSelectWidgetState();
}

class _AldsSelectWidgetState extends State<AldsSelectWidget> {
  final TextEditingController _curController = TextEditingController();

  @override
  void initState() {
    Locator loc = Locator();
    _curController.text = loc.lastLocation ?? "";
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Check Location"),
        actions: [
          widgets.topMenuAction(
            <widgets.MenuAction>[
              widgets.MenuAction(
                "Show/Edit Login Data",
                _showLoginDataAction,
                "Specify the user name and password for use with Sherpa",
              ),
              widgets.MenuAction(
                "Edit Locations",
                _editLocationAction,
                "Add or remove abstract locations from the set of available locations",
              ),
              widgets.MenuAction(
                "Clear Location Data",
                _clearDataAction,
                'Clear old location information; learn new locations.',
              ),
            ],
          ),
        ],
      ),
      body: widgets.topLevelPage(
        context,
        RefreshIndicator(
          onRefresh: _handleUpdate,
          child: Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              mainAxisSize: MainAxisSize.max,
              children: <Widget>[
                widgets.fieldSeparator(),
                const Image(
                  image: AssetImage('assets/images/aldsicon.png'),
                  fit: BoxFit.contain,
                ),
                widgets.fieldSeparator(),
                widgets.fieldSeparator(),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    const Text("Current Location:   "),
                    Expanded(
                        child: widgets.textField(
                      controller: _curController,
                      readOnly: true,
                    )),
                  ],
                ),
                widgets.fieldSeparator(),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    const Text('Alternatives:  '),
                    Expanded(
                      child: _createLocationSelector(),
                    ),
                  ],
                ),
                widgets.submitButton("Validate", _handleValidate),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _createLocationSelector() {
    String? cur = _curController.text;
    if (cur == '') cur = null;
    List<String> locs = List.of(storage.getLocations());
    locs.sort();
    return widgets.dropDown(
      locs,
      value: cur,
      onChanged: _locationSelected,
    );
  }

  Future<void> _locationSelected(String? value) async {
    util.log("SET CURRENT TO $value");
    setState(() {
      _curController.text = value ?? "";
    });
  }

  void _showLoginDataAction() async {
    await showLoginDialog(context);
  }

  void _editLocationAction() async {
    await widgets.gotoThen(context, const AldsLocationPage());
    setState(() {});
  }

  void _clearDataAction() async {
    bool fg = await widgets.getValidation(
      context,
      'Do you really want to clear location data?',
    );
    if (!fg) return;
    Locator loc = Locator();
    await loc.clear();
  }

  void _handleValidate() async {
    String txt = _curController.text;
    Locator loc = Locator();
    loc.noteLocation(txt);
    util.log("VALIDATE location as $txt");
  }

  Future<void> _handleUpdate() async {
    Locator loc = Locator();
    String? where = await loc.findLocation();
    await _locationSelected(where);
  }
}
