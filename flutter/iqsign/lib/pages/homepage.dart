/********************************************************************************/
/*                                                                              */
/*              homepage.dart                                                   */
/*                                                                              */
/*      Home page for iQsign -- give user access to all their signs             */
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

import 'package:iqsign/pages/createsignpage.dart';

import '../util.dart' as util;
import '../widgets.dart' as widgets;
import '../signdata.dart';
import '../globals.dart' as globals;
import 'signpage.dart';
import 'package:flutter/material.dart';
import 'loginpage.dart';
import 'changepassworddialog.dart';

class IQSignHomeWidget extends StatelessWidget {
  final bool _initial;

  const IQSignHomeWidget(this._initial, {super.key});

  @override
  Widget build(BuildContext context) {
    return IQSignHomePage(_initial);
  }
} // end of class IQSignHomeWidget

class IQSignHomePage extends StatefulWidget {
  final bool _initial;

  const IQSignHomePage(this._initial, {super.key});

  @override
  State<IQSignHomePage> createState() => _IQSignHomePageState();
}

class _IQSignHomePageState extends State<IQSignHomePage> {
  List<SignData> _signData = [];
  bool _initial = false;

  @override
  void initState() {
    _initial = widget._initial;
    _getSigns();
    super.initState();
  }

  Future _getSigns() async {
    List<SignData> rslt = await getSigns();
    _signData = rslt;
    SignData? sd0 = _signData.singleOrNull;
    if (_initial && sd0 != null) {
      _initial = false;
      Future.delayed(Duration.zero, () {
        _gotoSignPage(sd0);
        setState(() {});
      });
    } else {
      setState(() {});
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        flexibleSpace: const Image(
          image: AssetImage('assets/images/iqsignstlogo.png'),
          fit: BoxFit.contain,
        ),
        actions: [
          widgets.topMenuAction(
            <widgets.MenuAction>[
              widgets.MenuAction(
                "Create New Sign",
                _gotoCreateSign,
                "Create a separate sign for a different location",
              ),
              widgets.MenuAction(
                  "Unenroll from iQsign",
                  _removeUserAction,
                  "Delete you iQsign account and all associated information.  "
                      "This cannot be undone"),
              widgets.MenuAction(
                "Change password",
                _changePasswordAction,
                "Change your login password",
              ),
              widgets.MenuAction(
                "Log Out",
                _logoutAction,
                "Log out from iQsign",
              ),
            ],
          ),
        ],
      ),
      body: widgets.topLevelPage(context, _signListWidget(), true),
    );
  }

  Widget _signListWidget() {
    if (_signData.isNotEmpty) {
      return ListView.builder(
        padding: const EdgeInsets.all(10.0),
        itemCount: _signData.length,
        itemBuilder: _getTile,
      );
    } else {
      return widgets.circularProgressIndicator();
    }
  }

  ListTile _getTile(context, int i) {
    SignData sd = _signData[i];
    return ListTile(
      title: Text(
        sd.getName(),
        style: const TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.bold,
        ),
      ),
      subtitle: Text(
        sd.getDisplayName(),
        style: const TextStyle(
          fontSize: 14,
        ),
      ),
      trailing: Container(
        decoration: BoxDecoration(
          border: Border.all(
            width: 5,
          ),
        ),
        child: Image.network(sd.getLocalImageUrl()),
      ),
      onTap: () => {_gotoSignPage(sd)},
    );
  }

  Future<void> _removeUserAction() async {
    await _handleRemoveUser().then(_gotoLogin);
  }

  Future<void> _logoutAction() async {
    await _handleLogout().then(_gotoLogin);
  }

  void _gotoSignPage(SignData sd) async {
    await widgets.gotoThen(context, IQSignSignWidget(sd));
    setState(() {
      _getSigns();
    });
  }

  dynamic _gotoLogin(bool fg) {
    if (!fg) return;
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const IQSignLogin()),
    );
  }

  Future<bool> _handleLogout() async {
    await util.postJsonOnly("/rest/logout");
    globals.iqsignSession = null;
    return true;
  }

  dynamic _gotoCreateSign() async {
    await Navigator.push(
      context,
      MaterialPageRoute(
          builder: (context) => const IQSignSignCreatePage()),
    );
  }

  Future<bool> _handleRemoveUser() async {
    String msg =
        "Thank you for trying iQsign. We are sorry to see you go.\n";
    msg +=
        "If you really meant to leave, then click YES.  If this was a ";
    msg += "mistake then click NO";

    bool fg = await widgets.getValidation(context, msg);
    if (!fg) return false;

    Map<String, dynamic> js = await util.postJson(
      "rest/removeuser",
    );
    if (js['status'] == 'OK') {
      globals.iqsignSession = null;
      fg = true;
    } else {
      fg = false;
    }
    return fg;
  }

  Future _changePasswordAction() async {
    await changePasswordDialog(context);
  }
}

Future<List<SignData>> getSigns() async {
  Map<String, dynamic> js = await util.getJson("/rest/signs");
  var rslt = <SignData>[];
  if (js['status'] == 'OK') {
    var jsd = js['data'];
    for (final sd1 in jsd) {
      SignData sd = SignData(sd1);
      rslt.add(sd);
    }
  }
  return rslt;
}
