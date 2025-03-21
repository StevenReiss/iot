// ignore_for_file: curly_braces_in_flow_control_structures

/********************************************************************************/
/*                                                                              */
/*              loginkeydialog.dart                                             */
/*                                                                              */
/*      Dialog for generating a login key so iQsign can be used with SHERPA     */
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

import 'dart:async';

import '../widgets.dart' as widgets;
import 'package:flutter/material.dart';
import '../signdata.dart';
import '../util.dart' as util;
import 'package:flutter/services.dart';

Future loginKeyDialog(BuildContext context, SignData sd) async {
  BuildContext dcontext = context;

  String code = util.randomString(24);

  var body = {
    'signuser': sd.getSignUserId().toString(),
    'signid': sd.getSignId().toString(),
    'signkey': sd.getNameKey(),
    'code': code,
  };
  await Clipboard.setData(ClipboardData(text: code));

  if (!dcontext.mounted) return;

  Future accept() async {
    Map<String, dynamic> js = await util.postJson(
      "/rest/createcode",
      body: body,
    );
    if (js['status'] != 'OK') {
      // handle error
    }
    if (dcontext.mounted) {
      Navigator.of(dcontext).pop("OK");
    }
  }

  Future cancel() async {
    if (dcontext.mounted) {
      Navigator.of(dcontext).pop("CANCEL");
    }
  }

  Widget acceptBtn = widgets.submitButton("OK", accept);
  Widget cancelBtn = widgets.submitButton("Cancel", cancel);

  Dialog dlg = Dialog(
    child: Padding(
      padding: const EdgeInsets.all(20.0),
      child: SizedBox(
        width: MediaQuery.of(context).size.width * 0.8,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            const Text(
              "Login Code: ",
              style:
                  TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: 15),
            Text(
              code,
              style: const TextStyle(
                  fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: 15),
            const Text(
              "Code has been posted to clipboard.  Accepting this "
              "code will remove all previous codes for this sign",
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [cancelBtn, acceptBtn],
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
