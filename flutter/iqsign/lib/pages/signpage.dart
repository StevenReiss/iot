/*
 *        signpage.dart
 * 
 *    Page for a single sign
 * 
 */
/*	Copyright 2023 Brown University -- Steven P. Reiss			*/
/// *******************************************************************************
///  Copyright 2023, Brown University, Providence, RI.				 *
///										 *
///			  All Rights Reserved					 *
///										 *
///  Permission to use, copy, modify, and distribute this software and its	 *
///  documentation for any purpose other than its incorporation into a		 *
///  commercial product is hereby granted without fee, provided that the 	 *
///  above copyright notice appear in all copies and that both that		 *
///  copyright notice and this permission notice appear in supporting		 *
///  documentation, and that the name of Brown University not be used in 	 *
///  advertising or publicity pertaining to distribution of the software 	 *
///  without specific, written prior permission. 				 *
///										 *
///  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
///  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
///  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
///  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
///  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
///  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
///  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
///  OF THIS SOFTWARE.								 *
///										 *
///******************************************************************************

import '../signdata.dart';
import 'package:flutter/material.dart';
import '../globals.dart' as globals;
import '../widgets.dart' as widgets;
import 'loginpage.dart';
import 'editsignpage.dart';
import '../util.dart' as util;
import 'setnamedialog.dart' as setname;
import 'setsizedialog.dart' as setsize;
import 'loginkeydialog.dart' as loginkey;

class IQSignSignWidget extends StatelessWidget {
  final SignData _signData;

  const IQSignSignWidget(this._signData, {super.key});

  @override
  Widget build(BuildContext context) {
    return IQSignSignPage(_signData);
  }
}

class IQSignSignPage extends StatefulWidget {
  final SignData _signData;

  const IQSignSignPage(this._signData, {super.key});

  @override
  State<IQSignSignPage> createState() => _IQSignSignPageState();
}

class _IQSignSignPageState extends State<IQSignSignPage> {
  SignData _signData = SignData.unknown();
  List<String> _signNames = [];
  late Future<List<String>> _signNamesFuture;
  _IQSignSignPageState();
  final TextEditingController _extraControl = TextEditingController();
  bool _preview = false;
  String? _baseSign;
  SignData _originalSign = SignData.unknown();

  @override
  void initState() {
    _signData = widget._signData;
    _originalSign = SignData.clone(_signData);
    _signNamesFuture = _getNames();
    _analyzeSign();

    super.initState();
  }

  Future<List<String>> _getNames() async {
    Map<String, dynamic> js = await util.getJson("/rest/namedsigns");
    var jsd = js['data'];
    List<String> rslt = <String>[];
    for (final sd1 in jsd) {
      String s = sd1['name'];
      if (!rslt.contains(s)) rslt.add(s);
    }
    rslt.sort((String a, String b) {
      return a.toLowerCase().compareTo(b.toLowerCase());
    });
    setState(() {
      _signNames = rslt;
    });
    return rslt;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_signData.getName(),
            style: const TextStyle(
              fontWeight: FontWeight.bold,
              color: Colors.black,
            )),
        actions: [
          widgets.topMenuAction(
            <widgets.MenuAction>[
              widgets.MenuAction(
                "Create of Edit Saved Sign",
                _gotoEdit,
                "Create a new basic sign or edit this one",
              ),
              widgets.MenuAction(
                "Change Sign Size",
                _changeSignSize,
                "Change the size or shape of this sign",
              ),
              widgets.MenuAction(
                "Change Sign Name",
                _changeSignName,
                "Rename this sign",
              ),
              widgets.MenuAction(
                "Generate Login Key for Sherpa",
                _generateLoginKey,
                "Create a login key so that your sign can be set from Sherpa",
              ),
              widgets.MenuAction(
                "Delete this Sign",
                _deleteSign,
                "Remove this sign.  This can't be undone",
              ),
              widgets.MenuAction(
                "Log Out",
                _logoutAction,
              ),
            ],
          ),
        ],
      ),
      body: widgets.topLevelPage(
        context,
        FutureBuilder<List<String>>(
          future: _signNamesFuture,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const CircularProgressIndicator();
            } else if (snapshot.hasError) {
              return Text('Error: ${snapshot.error}');
            } else {
              _signNames = snapshot.data!;
              String url = _signData.getLocalImageUrl(_preview);
              return Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  widgets.fieldSeparator(),
                  getSignWidget(context, url),
                  Row(mainAxisAlignment: MainAxisAlignment.center, children: <Widget>[
                    const Text("Set Sign to "),
                    _createNameSelector(),
                  ]),
                  widgets.fieldSeparator(),
                  widgets.textFormField(
                    hint: "Additional text for the sign",
                    label: "Additional Text",
                    controller: _extraControl,
                    maxLines: 3,
                    onChanged: _handleOtherText,
                    enabled: _canHaveOtherText(),
                    tooltip: "Enter additional text to display on the sign, "
                        "for example, the time you will be back.",
                  ),
                  widgets.fieldSeparator(),
                  Row(mainAxisAlignment: MainAxisAlignment.end, children: <Widget>[
                    widgets.submitButton(
                      "Reset",
                      _resetAction,
                      enabled: !_preview,
                    ),
                    widgets.submitButton(
                      "Update",
                      _updateAction,
                      enabled: _isSignValid(),
                    ),
                  ])
                ],
              );
            }
          },
        ),
      ),
    );
  }

  Widget getSignWidget(context, url) {
    return Container(
      padding: const EdgeInsets.all(24),
      child: Container(
        decoration: BoxDecoration(
          border: Border.all(
            width: 3,
            color: (_preview ? Colors.black : Colors.blue),
          ),
        ),
        child: Image.network(
          url,
          width: MediaQuery.of(context).size.width * 0.4,
          //                    height: MediaQuery.of(context).size.height * 0.4,
        ),
      ),
    );
  }

  dynamic _gotoLogin(bool fg) {
    if (!fg) return;
    Navigator.push(context, MaterialPageRoute(builder: (context) => const IQSignLogin()));
  }

  dynamic _gotoHome(dynamic fg) {
    if (fg == false) return;
    Navigator.pop(context);
  }

  Future<bool> _handleLogout() async {
    await util.postJsonOnly("/rest/logout");
    globals.iqsignSession = null;
    return true;
  }

  dynamic _gotoEdit() {
    Navigator.push(
        context,
        MaterialPageRoute(
            builder: (context) => IQSignSignEditWidget(
                  _signData,
                  _signNames,
                )));
  }

  void _changeSignSize() async {
    final result = await setsize.showSizeDialog(context, _signData);
    if (result == "OK") await _updateAction();
  }

  void _changeSignName() async {
    final result = await setname.setNameDialog(context, _signData);
    if (result == "OK") await _updateAction();
  }

  void _generateLoginKey() async {
    await loginkey.loginKeyDialog(context, _signData);
  }

  void _deleteSign() async {
    _removeSignAction().then(_gotoHome);
  }

  void _logoutAction() async {
    _handleLogout().then(_gotoLogin);
  }

  void _analyzeSign() {
    List<String> lines = _signData.getSignBody().split("\n");
    _baseSign = null;
    _extraControl.text = "";
    for (String line in lines) {
      line = line.trim();
      if (_baseSign == null && line.startsWith(RegExp(r'=\w+'))) {
        int idx = line.indexOf('=');
        if (idx > 0) line = line.substring(0, idx).trim();
        line = line.substring(1);
        _baseSign = line;
      } else if (_baseSign != null) {
        if (_extraControl.text.isEmpty) {
          _extraControl.text = line;
        } else {
          _extraControl.text += "\n$line";
        }
      }
    }
  }

  Widget _createNameSelector() {
    String? val = _signData.getDisplayName();
    if (!_signNames.contains(val)) val = null;

    return widgets.dropDown(
      _signNames,
      onChanged: _handleChangeBaseSign,
      value: val,
      tooltip: "Select the starting sign you want to display",
    );
  }

  void _handleChangeBaseSign(String? name) async {
    if (name == null) return;
    setState(() {
      _signData.setDisplayName(name);
      _extraControl.clear();
    });
    String cnts = "=$name\n${_extraControl.text}";
    _signData.setContents(cnts);
    _signData.setDisplayName(name);
    _baseSign = name;
    await _previewAction();
  }

  void _handleOtherText(String txt) async {
    if (_baseSign == null) return;
    String cnts = "=$_baseSign\n${_extraControl.text}";
    _signData.setContents(cnts);
    await _previewAction();
  }

  bool _canHaveOtherText() {
    return _baseSign != null;
  }

  Future _previewAction() async {
    var body = {
      'signdata': _signData.getSignBody(),
      'signuser': _signData.getSignUserId().toString(),
      'signid': _signData.getSignId().toString(),
      'signkey': _signData.getNameKey(),
    };
    Map<String, dynamic> js = await util.postJson(
      "/rest/sign/preview",
      body: body,
    );
    if (js['status'] == 'OK') {
      setState(() {
        _preview = true;
      });
    }
  }

  Future _resetAction() async {
    _signData = SignData.clone(_originalSign);
    _analyzeSign();
    setState(() {
      _preview = false;
    });
  }

  Future _updateAction() async {
    var body = {
      'signdata': _signData.getSignBody(),
      'signuser': _signData.getSignUserId().toString(),
      'signid': _signData.getSignId().toString(),
      'signkey': _signData.getNameKey(),
      'signname': _signData.getName(),
      'signdim': _signData.getDimension(),
      'signwidth': _signData.getWidth().toString(),
      'signheight': _signData.getHeight().toString(),
    };
    Map<String, dynamic> js = await util.postJson(
      "/rest/sign/update",
      body: body,
    );
    if (js['status'] == "OK") {
      setState(() {
        _preview = false;
      });
    }
  }

  Future<bool> _removeSignAction() async {
    String msg = "This will completely remove the sign ";
    msg += "'${_signData.getName()}'.\n";
    msg += "If you are sure, click YES, otherwise click NO.";

    bool fg = await widgets.getValidation(context, msg);
    if (!fg) return false;

    Map<String, dynamic> js = await util.postJson(
      "/rest/removersign",
      body: {
        "signid": _signData.getSignId(),
      },
    );
    if (js['status'] != "OK") return false;

    return true;
  }

  bool _isSignValid() {
    if (_baseSign == null) return false;
    return true;
  }
}
