/********************************************************************************/
/*                                                                              */
/*              addrsspage.dart                                                 */
/*                                                                              */
/*      Page to define a new RSS device                                         */
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

import 'package:flutter/material.dart';
import 'package:sherpa/models/catredevice.dart';
import 'package:sherpa/models/catreuniverse.dart';
import '../widgets.dart' as widgets;
import '../util.dart' as util;
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;

class SherpaAddRssPage extends StatefulWidget {
  final CatreUniverse _theUniverse;

  const SherpaAddRssPage(this._theUniverse, {super.key});

  @override
  State<SherpaAddRssPage> createState() => _SherpaAddRssPageState();
}

class _SherpaAddRssPageState extends State<SherpaAddRssPage> {
  final TextEditingController _urlControl = TextEditingController();
  final TextEditingController _nameControl = TextEditingController();
  final TextEditingController _descControl = TextEditingController();
  late CatreUniverse _theUniverse;

  _SherpaAddRssPageState();

  @override
  void initState() {
    _theUniverse = widget._theUniverse;
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    Widget w1 = widgets.textField(
      label: "Rss URL",
      hint: "Enter the URL of the target RSS feed",
      controller: _urlControl,
      maxLines: 0,
      enabled: true,
      tooltip:
          "Enter the URL of the RSS feed you want to detect new posts from",
    );
    Widget w4 = widgets.textField(
      label: "Name",
      hint: "Enter a name for this weather device",
      enabled: true,
      controller: _nameControl,
    );
    Widget w5 = widgets.textField(
      label: "Description",
      controller: _descControl,
      hint: "Enter a detailed description of this device",
      enabled: true,
    );
    Widget bottom = Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: <Widget>[
        widgets.submitButton(
          "Cancel",
          _cancelCreate,
          enabled: true,
          tooltip: "Go back without creating the device",
        ),
        widgets.submitButton(
          "Create",
          _createDevice,
          enabled: _checkCanCreate(),
          tooltip: "Create this RSS device",
        )
      ],
    );
    return Scaffold(
      appBar: AppBar(
        title: const Text("Add Open-Meteo Weather"),
      ),
      body: widgets.topLevelPage(
        context,
        Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            children: <Widget>[
              w1,
              widgets.fieldSeparator(16),
              w4,
              widgets.fieldSeparator(),
              w5,
              widgets.fieldSeparator(16),
              bottom,
            ],
          ),
        ),
      ),
    );
  }

  void _cancelCreate() {
    if (context.mounted) {
      Navigator.pop(context, "CANCEL");
    }
  }

  bool _checkCanCreate() {
    if (_urlControl.text != "" && _nameControl.text != "") {
      return true;
    }
    return false;
  }

  Future<void> _createDevice() async {
    Uri u = Uri.http(_urlControl.text);
    dynamic resp = await http.get(u);
    if (resp.statusCode < 400) return;

    BuildContext dcontext = context;
    Map<String, String?> dev = {
      "VTYPE": "RssFeed",
      "URL": _urlControl.text,
      "NAME": _nameControl.text,
      "DESCRIPTION": _descControl.text,
      "USERDESC": "false",
    };
    Map<String, String?> body = {
      "DEVICE": convert.jsonEncode(dev),
    };
    Map<String, dynamic> rslt =
        await util.postJson("/universe/addvirtual", body);
    // might want to check the result
    if (rslt["STATUS"] == "OK") {
      Map<String, dynamic> dev = rslt["DEVICE"];
      CatreDevice cd = CatreDevice.build(_theUniverse, dev);
      _theUniverse.addDevice(cd);
    }
    if (dcontext.mounted) {
      Navigator.pop(dcontext, "OK");
    }
  }
}



/* end of module addrsspage.dart */


