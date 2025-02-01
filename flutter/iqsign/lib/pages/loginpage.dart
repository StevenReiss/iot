/*
 * Login Page
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

import 'package:flutter/material.dart';
import '../globals.dart' as globals;
import '../util.dart' as util;
import '../widgets.dart' as widgets;
import 'registerpage.dart';
import 'homepage.dart' as home;
import 'signpage.dart' as signpage;
import 'forgotpasswordpage.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../signdata.dart';

//
//    Private Variables
//
bool _loginValid = false;

//
//    Check login using preferences or prior login
//

Future<bool> testLogin() async {
  if (_loginValid) return true;
  SharedPreferences prefs = await SharedPreferences.getInstance();
  String? uid = prefs.getString('uid');
  String? pwd = prefs.getString('pwd');
  if (uid != null && pwd != null) {
    _HandleLogin login = _HandleLogin(uid, pwd);
    String? rslt = await login.authUser();
    if (rslt == null) {
      _loginValid = true;
      return true;
    }
  }
  return false;
}

//
//      Logiin Widget
//

class IQSignLogin extends StatelessWidget {
  const IQSignLogin({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'iQSign Login',
      theme: widgets.getTheme(),
      home: const IQSignLoginWidget(),
    );
  }
}

class IQSignLoginWidget extends StatefulWidget {
  const IQSignLoginWidget({super.key});

  @override
  State<IQSignLoginWidget> createState() => _IQSignLoginWidgetState();
}

class _IQSignLoginWidgetState extends State<IQSignLoginWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  String? _curUser;
  String? _curPassword;
  String _loginError = '';
  final TextEditingController _userController = TextEditingController();
  final TextEditingController _pwdController = TextEditingController();
  bool _rememberMe = false;

  _IQSignLoginWidgetState() {
    _loginValid = false;
  }

  @override
  void initState() {
    _loadUserAndPassword();
    super.initState();
  }

  void _gotoFirstPage() {
    home.getSigns().then((List<SignData> signs) async {
      SignData? sd = signs.singleOrNull;
      if (mounted) {
        if (sd != null) {
          widgets.goto(context, signpage.IQSignSignWidget(sd));
        } else {
          widgets.goto(context, const home.IQSignHomePage());
        }
      }
    });
  }

  void _gotoRegister() {
    Navigator.push(context, MaterialPageRoute(builder: (context) => const IQSignRegister()));
  }

  void _gotoForgotPassword() {
    Navigator.push(context, MaterialPageRoute(builder: (context) => const IQSignPasswordWidget()));
  }

  void _gotoChangePassword() {
//  widgets.goto(context, IQSignChangePasswordWidget(cu));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: widgets.appBar("Login"),
      body: widgets.iqsignPage(
        context,
        Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            mainAxisSize: MainAxisSize.max,
            children: <Widget>[
              Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: <Widget>[
                    widgets.getIqsignLogo(context),
                    widgets.getPadding(16),
                    widgets.loginTextField(
                      context,
                      hint: "Username or email",
                      label: "Username or email",
                      validator: _validateUserName,
                      controller: _userController,
                      fraction: 0.8,
                    ),
                    widgets.fieldSeparator(),
                    widgets.loginTextField(
                      context,
                      hint: "Password",
                      label: "Password",
                      validator: _validatePassword,
                      controller: _pwdController,
                      fraction: 0.8,
                      obscureText: true,
                    ),
                    widgets.errorField(_loginError),
                    Container(
                      constraints: const BoxConstraints(minWidth: 150, maxWidth: 350),
                      width: MediaQuery.of(context).size.width * 0.4,
                      child: widgets.submitButton("Login", _handleLogin),
                    ),
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      mainAxisAlignment: MainAxisAlignment.center,
                      crossAxisAlignment: CrossAxisAlignment.center,
                      children: <Widget>[
                        Checkbox(
                          value: _rememberMe,
                          onChanged: _handleRememberMe,
                        ),
                        const Text("Remember Me"),
                      ],
                    ),
                  ],
                ),
              ),
              widgets.getPadding(16),
              widgets.textButton("Not a user? Register Here.", _gotoRegister),
              widgets.textButton("Forgot Password?", _gotoForgotPassword),
            ],
          ),
        ),
      ),
    );
  }

  void _handleLogin() async {
    _loginValid = false;
    setState(() {
      _loginError = '';
    });
    if (_formKey.currentState!.validate()) {
      _formKey.currentState!.save();
      _HandleLogin login = _HandleLogin(_curUser as String, _curPassword as String);
      String? rslt = await login.authUser();
      if (rslt == 'TEMPORARY') {
        _loginValid = true;
        _gotoChangePassword();
      } else if (rslt != null) {
        setState(() {
          _loginError = rslt;
        });
      } else {
        _loginValid = true;
        _saveData();
        _gotoFirstPage();
      }
    }
  }

  String? _validatePassword(String? value) {
    _curPassword = value;
    if (value == null || value.isEmpty) {
      return "Password must not be null";
    }
    return null;
  }

  String? _validateUserName(String? value) {
    _curUser = value;
    if (value == null || value.isEmpty) {
      return "Username must not be null";
    }
    return null;
  }

  void _handleRememberMe(bool? fg) async {
    if (fg == null) return;
    _rememberMe = fg;
    _saveData();
    setState(() {
      _rememberMe = fg;
    });
  }

  void _saveData() {
    SharedPreferences.getInstance().then((prefs) {
      prefs.setBool('remember_me', _rememberMe);
      prefs.setString('uid', _rememberMe ? _userController.text : "");
      prefs.setString('pwd', _rememberMe ? _pwdController.text : "");
    });
  }

  void _loadUserAndPassword() async {
    try {
      SharedPreferences prefs = await SharedPreferences.getInstance();
      var uid = prefs.getString('uid');
      var pwd = prefs.getString('pwd');
      var rem = prefs.getBool('remember_me');
      if (rem != null && rem) {
        setState(() {
          _rememberMe = true;
        });
        _userController.text = uid ?? "";
        _pwdController.text = pwd ?? "";
      }
    } catch (e) {
      _rememberMe = false;
      _userController.text = "";
      _pwdController.text = "";
    }
  }
}

//
//    Class to actually handle loggin in
//

class _HandleLogin {
  String? _curPadding;
  String? _curSession;
  final String _curPassword;
  final String _curUser;

  _HandleLogin(this._curUser, this._curPassword);

  Future _prelogin() async {
    Map<String, dynamic> js = await util.getJson("/rest/login");
    _curPadding = js['code'];
    _curSession = js['session'];
    globals.iqsignSession = _curSession;
  }

  Future<String?> authUser() async {
    if (_curPadding == null) {
      await _prelogin();
    }
    String pwd = _curPassword;
    String usr = _curUser.toLowerCase();
    String pad = _curPadding as String;
    String p1 = util.hasher(pwd);
    String p2 = util.hasher(p1 + usr);
    String p3 = util.hasher(p2 + pad);

    var body = {
      'session': _curSession,
      'username': usr,
      'padding': pad,
      'password': p3,
    };
    Map<String, dynamic> jresp = await util.postJson(
      "/rest/login",
      body: body,
    );
    if (jresp['status'] == "OK") {
      globals.iqsignSession = jresp['session'];
      _curSession = jresp['session'];
      var temp = jresp['TEMPORARY'];
      if (temp != null) return "TEMPORARY";
      return null;
    }
    return jresp['message'];
  }
}
