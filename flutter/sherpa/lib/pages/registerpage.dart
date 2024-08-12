/*
 * Registration Page
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
///*******************************************************************************/

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;
import 'package:sherpa/globals.dart' as globals;
import 'package:sherpa/util.dart' as util;
import 'package:sherpa/widgets.dart' as widgets;
import 'loginpage.dart';

class SherpaRegister extends StatelessWidget {
  const SherpaRegister({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SherPA Registration',
      theme: widgets.getTheme(),
      home: const SherpaRegisterWidget(),
    );
  }
}

class SherpaRegisterWidget extends StatefulWidget {
  const SherpaRegisterWidget({super.key});

  @override
  State<SherpaRegisterWidget> createState() => _SherpaRegisterWidgetState();
}

class _SherpaRegisterWidgetState extends State<SherpaRegisterWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  String? _curUser;
  String? _curEmail;
  String? _curPassword;
  String? _curUniverse;
  late String _registerError;

  _SherpaRegisterWidgetState() {
    _curUser = null;
    _curEmail = null;
    _curPassword = null;
    _curUniverse = "MyWorld";
    _registerError = '';
  }

  Future<String?> _registerUser() async {
    String pwd = (_curPassword as String);
    String usr = (_curUser as String).toLowerCase();
    String? em = _curEmail;
    if (em == null || em.isEmpty) em = usr;
    String email = em.toLowerCase();
    String univ = (_curUniverse as String);
    String p1 = util.hasher(pwd);
    String p2 = util.hasher(p1 + usr);

    var body = {
      globals.catreSession: globals.sessionId,
      'email': email,
      'username': usr,
      'password': p2,
      'universe': univ,
    };
    var url = Uri.https(util.getServerURL(), "/register");
    var resp = await http.post(url, body: body);
    var jresp = convert.jsonDecode(resp.body) as Map<String, dynamic>;
    if (jresp['status'] == "OK") return null;
    return jresp['message'];
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Sign Up"),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: <Widget>[
                  Image.asset(
                    "assets/images/sherpaimage.png",
                    width: MediaQuery.of(context).size.width * 0.5,
                    height: MediaQuery.of(context).size.width * 0.2,
                  ),
                  const Padding(
                    padding: EdgeInsets.all(16.0),
                  ),
                  widgets.textFormField(
                    hint: "Valid Email Address",
                    label: "Email",
                    validator: _validateEmail,
                    context: context,
                    fraction: 0.8,
                  ),
                  widgets.fieldSeparator(),
                  widgets.textFormField(
                    hint: "Username",
                    label: "Username",
                    validator: _validateUserName,
                    context: context,
                    fraction: 0.8,
                  ),
                  widgets.fieldSeparator(),
                  widgets.textFormField(
                    hint: "Password",
                    label: "Password",
                    validator: _validatePassword,
                    obscureText: true,
                    context: context,
                    fraction: 0.8,
                  ),
                  widgets.fieldSeparator(),
                  widgets.textFormField(
                    hint: "Confirm Password",
                    label: "Confirm Passwrd",
                    validator: _validateConfirmPassword,
                    obscureText: true,
                    context: context,
                    fraction: 0.8,
                  ),
                  widgets.fieldSeparator(),
                  widgets.textFormField(
                    hint: "Universe Name (e.g. MyWorld)",
                    label: "Name of Universe",
                    validator: _validateUniverseName,
                    context: context,
                    fraction: 0.8,
                  ),
                  widgets.fieldSeparator(),
                  widgets.errorField(_registerError),
                  Container(
                    constraints:
                        const BoxConstraints(minWidth: 150, maxWidth: 350),
                    width: MediaQuery.of(context).size.width * 0.4,
                    child: widgets.submitButton("Submit", _handleRegister),
                  ),
                ],
              ),
            ),
            widgets.textButton("Already a user, login", _gotoLogin),
          ],
        ),
      ),
    );
  }

  void _handleRegister() async {
    setState(() {
      _registerError = '';
    });
    if (_formKey.currentState!.validate()) {
      String? rslt = await _registerUser();
      if (rslt != null) {
        setState(() {
          _registerError = rslt;
        });
      } else {
        _gotoLogin();
      }
    }
  }

  void _gotoLogin() {
    widgets.goto(context, const SherpaLogin());
  }

  String? _validateUniverseName(String? value) {
    _curUniverse = value;
    if (value == null || value.isEmpty) {
      return "Must provide a name for initial sign";
    }
    return null;
  }

  String? _validateConfirmPassword(String? value) {
    if (value != _curPassword) {
      return "Passwords must match";
    }
    return null;
  }

  String? _validatePassword(String? value) {
    _curPassword = value;
    if (value == null || value.isEmpty) {
      return "Password must not be null";
    } else if (!util.validatePassword(value)) {
      return "Invalid password";
    }
    return null;
  }

  String? _validateUserName(String? value) {
    _curUser = value;
    return null;
  }

  String? _validateEmail(String? value) {
    _curEmail = value;
    if (value == null || value.isEmpty) {
      return "Email must not be null";
    } else if (!util.validateEmail(value)) {
      return "Invalid email address";
    }
    return null;
  }
}

