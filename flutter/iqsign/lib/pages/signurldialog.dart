/********************************************************************************/
/*                                                                              */
/*              signurldialog.dart                                              */
/*                                                                              */
/*      Dialog for show sign and image URL and let user copy them               */
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
import 'package:flutter/services.dart';

Future signUrlDialog(BuildContext context, SignData sd) async {
  BuildContext dcontext = context;

  Future accept() async {
    if (dcontext.mounted) {
      Navigator.of(dcontext).pop();
    }
  }

  void copy(String url) async {
    await Clipboard.setData(ClipboardData(text: url));
  }

  Widget acceptBtn = widgets.submitButton("OK", accept);

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
              "Sign URLS: ",
              style:
                  TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            widgets.fieldSeparator(16),
            Row(
              children: <Widget>[
                const Text(
                  "Sign URL:  ",
                ),
                Expanded(
                  child: Text(
                    sd.getSignUrl(),
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.bold,
                      color: Colors.blue,
                    ),
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.copy),
                  tooltip: "Copy Sign URL to clipboard",
                  onPressed: () {
                    copy(sd.getSignUrl());
                  },
                ),
              ],
            ),
            widgets.fieldSeparator(),
            Row(
              children: <Widget>[
                const Text(
                  "Image URL:  ",
                ),
                Expanded(
                  child: Text(
                    sd.getImageUrl(),
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.bold,
                      color: Colors.blue,
                    ),
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.copy),
                  tooltip: "Copy Image URL to clipboard",
                  onPressed: () {
                    copy(sd.getImageUrl());
                  },
                ),
              ],
            ),
            widgets.fieldSeparator(16),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [acceptBtn],
            ),
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
