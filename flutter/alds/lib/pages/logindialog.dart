/********************************************************************************/
/*                                                                              */
/*              logindialog.dart                                                */
/*                                                                              */
/*      Dialog to get the user's SHERPA/CATRE credentials                       */
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

Future<String?> showLoginDialog(
  BuildContext context,
) async {
  storage.AuthData authdata = storage.getAuthData();
  TextEditingController idcontrol = TextEditingController(
    text: authdata.userId,
  );
  TextEditingController pwdcontrol = TextEditingController(
    text: authdata.password,
  );

  String? rslt = await showDialog(
    context: context,
    builder: (context) {
      return _AldsLoginDialog(
        context,
        authdata,
        idcontrol,
        pwdcontrol,
      );
    },
  );
  if (rslt == 'OK') {
    await storage.setAuthData(idcontrol.text, pwdcontrol.text);
  }
  return rslt;
}

class _AldsLoginDialog extends AlertDialog {
  final BuildContext context;

  _AldsLoginDialog(
    this.context,
    storage.AuthData authdata,
    TextEditingController idcontrol,
    TextEditingController pwdcontrol,
  ) : super(
          title: const Text("Set Sherpa Credentials"),
          content: Padding(
            padding: const EdgeInsets.all(20.0),
            child: SizedBox(
              width: MediaQuery.of(context).size.width * 0.8,
              child: Column(
                mainAxisAlignment: MainAxisAlignment.start,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: <Widget>[
                  widgets.textField(
                    hint: "Generic User Id",
                    controller: idcontrol,
                  ),
                  widgets.fieldSeparator(),
                  widgets.textField(
                    hint: "Generic Password",
                    controller: pwdcontrol,
                  ),
                ],
              ),
            ),
          ),
          actions: <Widget>[
            widgets.submitButton("Cancel", () {
              Navigator.of(context).pop("CANCEL");
            }),
            widgets.submitButton(
              "UPDATE",
              () {
                Navigator.of(context).pop("OK");
              },
            ),
          ],
        );
}
