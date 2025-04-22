/********************************************************************************/
/*                                                                              */
/*              loginpage.dart                                                  */
/*                                                                              */
/*      Page (not dialog) to handle login credentials                           */
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
import 'selectpage.dart';

class AldsLoginWidget extends StatelessWidget {
  const AldsLoginWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return const AldsLoginPage();
  }
}

class AldsLoginPage extends StatefulWidget {
  const AldsLoginPage({super.key});

  @override
  State<AldsLoginPage> createState() {
    return _AldsLoginPageState();
  }
}

class _AldsLoginPageState extends State<AldsLoginPage> {
  late storage.AuthData authdata;
  TextEditingController idcontrol = TextEditingController();
  TextEditingController pwdcontrol = TextEditingController();

  _AldsLoginPageState();

  @override
  void initState() {
    authdata = storage.getAuthData();
    idcontrol.text = authdata.userId;
    pwdcontrol.text = authdata.password;
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    Widget w = widgets.topLevelPage(
      context,
      Column(
        mainAxisAlignment: MainAxisAlignment.start,
        children: <Widget>[
          widgets.fieldSeparator(),
          widgets.textField(
            hint: "Generic User Id",
            controller: idcontrol,
          ),
          widgets.fieldSeparator(),
          widgets.textField(
            hint: "Generic Password",
            controller: pwdcontrol,
          ),
          widgets.fieldSeparator(),
          _bottomButtons(),
        ],
      ),
    );
    String ttl = "Set Login Credentials";
    Widget top = Scaffold(
      appBar: AppBar(
        title: Text(ttl),
      ),
      body: w,
    );
    return top;
  }

  Widget _bottomButtons() {
    Widget w = Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: <Widget>[
        widgets.submitButton("Accept", _saveCreds),
        widgets.submitButton("Cancel", _cancel),
      ],
    );
    return w;
  }

  void _saveCreds() async {
    BuildContext dcontext = context;
    await storage.setAuthData(idcontrol.text, pwdcontrol.text);
    if (dcontext.mounted) {
      widgets.gotoDirect(dcontext, const AldsSelectWidget());
    }
  }

  void _cancel() async {
    widgets.gotoDirect(context, const AldsSelectWidget());
  }
}
