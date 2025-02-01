/*  
 *       changepassworddialog.dart 
 * 
 *    Dialog for changing user password
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

import '../widgets.dart' as widgets;
import '../util.dart' as util;
import 'package:flutter/material.dart';
import '../signdata.dart';

Future changePasswordDialog(BuildContext context, SignData sd) async {
  TextEditingController p1controller = TextEditingController();
  TextEditingController p2controller = TextEditingController();
  BuildContext dcontext = context;
  String? pwdError;

  void cancel() {
    Navigator.of(dcontext).pop("CANCEL");
  }

  String? validatePassword(String? value) {
    if (!util.validatePassword(value)) {
      return "Invalid password";
    }
    return null;
  }

  Future updatePassword() async {
    pwdError = null;
    String p1 = p1controller.text;
    String p2 = p2controller.text;
    String? err = validatePassword(p1);
    if (err == null && p1 != p2) err = "Passwords don't match";
    if (err != null) {
      pwdError = err;
      return;
    }
    var data = {
      'userpwd': util.hasher(p1),
    };
    // need to get username and user email to encode the password here
    // or we will send the password over clear text (using https is ok)

    await util.postJsonOnly("/rest/changepassword", body: data);
    if (dcontext.mounted) {
      Navigator.of(dcontext).pop("OK");
    }
  }

  Widget cancelBtn = widgets.submitButton("Cancel", cancel);
  Widget acceptBtn = widgets.submitButton("OK", updatePassword);

  Dialog dlg = Dialog(
    child: Padding(
      padding: const EdgeInsets.all(20.0),
      child: SizedBox(
        width: MediaQuery.of(context).size.width * 0.8,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            const Text("Change Password",
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                )),
            const SizedBox(height: 15),
            widgets.textFormField(
              label: "New Password",
              controller: p1controller,
              obscureText: true,
            ),
            const SizedBox(height: 15),
            widgets.textFormField(
              label: "Verify Password",
              controller: p2controller,
              obscureText: true,
            ),
            const SizedBox(height: 8),
            widgets.errorField(pwdError),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [cancelBtn, const SizedBox(width: 15), acceptBtn],
            )
          ],
        ),
      ),
    ),
  );

  return showDialog(
      context: context,
      builder: (context) {
        dcontext = context;
        return dlg;
      });
}
