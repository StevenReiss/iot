/********************************************************************************/
/*                                                                              */
/*              setnamedialog.dart                                              */
/*                                                                              */
/*      Dialog to let the user change the name of one of their signs            */
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
import '../signdata.dart';

Future setNameDialog(BuildContext context, SignData sd) async {
  String name = sd.getName();
  TextEditingController controller = TextEditingController(text: name);
  BuildContext dcontext = context;

  void cancel() {
    Navigator.of(dcontext).pop("CANCEL");
  }

  Future updateSign() async {
    await sd.updateSign(name: controller.text);
    if (dcontext.mounted) {
      Navigator.of(dcontext).pop("OK");
    }
  }

  Widget cancelBtn = widgets.submitButton("Cancel", cancel);
  Widget acceptBtn = widgets.submitButton("OK", updateSign);

  Dialog dlg = Dialog(
    child: Padding(
      padding: const EdgeInsets.all(20.0),
      child: SizedBox(
        width: MediaQuery.of(context).size.width * 0.8,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            const Text("Set Sign Name",
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            const SizedBox(height: 15),
            widgets.textField(label: "Sign Name", controller: controller),
            const SizedBox(height: 15),
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

