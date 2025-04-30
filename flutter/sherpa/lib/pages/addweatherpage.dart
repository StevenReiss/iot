/********************************************************************************/
/*                                                                              */
/*              addweatherpage.dart                                             */
/*                                                                              */
/*      Page to define a new weather device                                     */
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
import '../widgets.dart' as widgets;
import '../util.dart' as util;
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;

class SherpaAddWeatherPage extends StatefulWidget {
  const SherpaAddWeatherPage({super.key});

  @override
  State<SherpaAddWeatherPage> createState() => _SherpaAddWeatherPageState();
}

class _SherpaAddWeatherPageState extends State<SherpaAddWeatherPage> {
  final TextEditingController _locationControl = TextEditingController();
  final TextEditingController _nameControl = TextEditingController();
  final TextEditingController _descControl = TextEditingController();
  final TextEditingController _latControl = TextEditingController();
  final TextEditingController _longControl = TextEditingController();
  bool _locChanged = false;
  bool _nameSet = false;
  bool _descSet = false;
  String _units = "Imperial";

  _SherpaAddWeatherPageState();

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    Widget w1 = widgets.textField(
      label: "Location",
      hint: "Enter location (e.g., city state or city country or postal code)",
      controller: _locationControl,
      maxLines: 0,
      enabled: true,
      onChanged: (String s) {
        _locChanged = true;
      },
      tooltip: "Enter location name.  Either click elsewhere or click the "
          "Locate button to convert this to latitude and longitude.  If the location "
          "might be ambiguous, check the other fields for correctness. "
          "State postal codes might not work.  Use part of the state name instead",
    );
    w1 = Focus(onFocusChange: _locationUpdate, child: w1);
    Widget w2 = widgets.textField(
      label: "Latitude",
      controller: _latControl,
      enabled: true,
      readOnly: true,
      tooltip: "This will be computed from the location",
    );
    w2 = Row(
      mainAxisAlignment: MainAxisAlignment.start,
      children: <Widget>[
        const SizedBox(width: 50),
        Expanded(child: w2),
      ],
    );
    Widget w3 = widgets.textField(
      label: "Longitude",
      controller: _longControl,
      enabled: true,
      readOnly: true,
      tooltip: "This will be computed from the location",
    );
    w3 = Row(
      mainAxisAlignment: MainAxisAlignment.start,
      children: <Widget>[
        const SizedBox(width: 50),
        Expanded(child: w3),
      ],
    );
    Widget w4 = widgets.textField(
      label: "Name",
      hint: "Enter a name for this weather device",
      enabled: true,
      controller: _nameControl,
      onChanged: (String s) {
        _nameSet = true;
      },
    );
    Widget w5 = widgets.textField(
      label: "Description",
      controller: _descControl,
      hint: "Enter a detailed description of this device",
      enabled: true,
      onChanged: (String s) {
        _descSet = true;
      },
    );
    Widget w6 = widgets.dropDownWidget<String>(
      ["Imperial", "Metric"],
      label: "Units",
      value: _units,
      hint: "Choose type of units for weather measurements",
      onChanged: (String? t) {
        if (t != null) _units = t;
      },
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
          "Locate",
          _locationUpdate,
          tooltip: "Recompute the latitude and longitude from location",
        ),
        widgets.submitButton(
          "Create",
          _createDevice,
          enabled: _checkCanCreate(),
          tooltip: "Create this weather device",
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
              widgets.fieldSeparator(),
              w2,
              widgets.fieldSeparator(),
              w3,
              widgets.fieldSeparator(16),
              w4,
              widgets.fieldSeparator(),
              w5,
              widgets.fieldSeparator(),
              w6,
              widgets.fieldSeparator(16),
              bottom,
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _locationUpdate([
    bool fg = false,
  ]) async {
    if (!fg && _locChanged) {
      _locChanged = false;
      String name = _locationControl.text;
      String? check;
      int idx = name.indexOf(",");
      if (idx > 0) {
        check = name.substring(idx + 1).trim();
        name = name.substring(0, idx);
        name = name.trim();
      }
      Map<String, dynamic> q = {
        "name": name,
        "count": "10",
        "language": "en",
        "format": "json",
      };
      Uri u = Uri.https("geocoding-api.open-meteo.com", "v1/search", q);
      dynamic resp = await http.get(u);
      if (resp.statusCode < 400) {
        Map<String, dynamic> json0 =
            convert.jsonDecode(resp.body) as Map<String, dynamic>;
        List<dynamic>? json1 = json0["results"];
        if (json1 != null && json1.isNotEmpty) {
          Map<String, dynamic> json = _findBestMatch(json1, check);
          _latControl.text = json["latitude"].toString();
          _longControl.text = json["longitude"].toString();
          if (!_nameSet) {
            _nameControl.text = "${_locationControl.text} Weather";
          }
          if (!_descSet) {
            String d = "Weather for ${json['name']}";
            d += _addOn(json, "admin1");
            d += _addOn(json, "admin2");
            // d += _addOn(json, "admin3");
            // d += _addOn(json, "admin4");
            d += _addOn(json, "country_code");
            _descControl.text = d;
          }
        }
      }
      _locChanged = false;
      setState(() {});
    }
  }

  String _addOn(Map<String, dynamic> json, String key) {
    String? v = json[key];
    if (v == null || v.isEmpty) return "";
    return ", $v";
  }

  Map<String, dynamic> _findBestMatch(List<dynamic> items, String? check) {
    if (check == null || check.isEmpty || items.length == 1) {
      return items.first;
    }
    Map<String, dynamic> best = items.first;
    int bestct = 0;
    List<String> keys = check.split(",");
    for (Map<String, dynamic> itm in items) {
      int ct = _matchCount(itm, keys);
      if (ct > bestct) {
        best = itm;
        bestct = ct;
      }
    }
    return best;
  }

  int _matchCount(Map<String, dynamic> itm, List<String> keys) {
    int ct = 0;
    for (String s in keys) {
      s = s.toLowerCase().trim();
      if (_occursIn(itm, s)) ++ct;
    }
    return ct;
  }

  bool _occursIn(Map<String, dynamic> itm, String v) {
    if (_occursInElt(itm, v, "country_code")) return true;
    if (_occursInElt(itm, v, "country")) return true;
    if (_occursInElt(itm, v, "admin1")) return true;
    if (_occursInElt(itm, v, "admin2")) return true;
    if (_occursInElt(itm, v, "admin3")) return true;
    if (_occursInElt(itm, v, "admin4")) return true;

    return false;
  }

  bool _occursInElt(Map<String, dynamic> itm, String key, String fld) {
    String? v = itm[fld];
    if (v == null || v.isEmpty) return false;
    v = v.toLowerCase().trim();
    if (v == key) return true;
    if (v.startsWith(key)) return true;
    if (v.contains(" $key")) return true;
    // might want to expand state abbreviations
    return false;
  }

  Future<void> _createDevice() async {
    BuildContext dcontext = context;
    Map<String, String?> dev = {
      "VTYPE": "OpenMeteo",
      "UNITS": _units,
      "LATITUDE": _latControl.text,
      "LONGITUDE": _longControl.text,
      "LOCATION": _locationControl.text,
      "NAME": _nameControl.text,
      "DESCRIPTION": _descControl.text,
      "USERDESC": _descSet.toString(),
    };
    Map<String, String?> body = {
      "DEVICE": convert.jsonEncode(dev),
    };
    Map<String, dynamic> rslt =
        await util.postJson("/universe/addvirtual", body);
    // might want to check the result
    if (rslt["STATUS"] != "OK") {}
    if (dcontext.mounted) {
      Navigator.pop(dcontext, "OK");
    }
  }

  void _cancelCreate() {
    if (context.mounted) {
      Navigator.pop(context, "CANCEL");
    }
  }

  bool _checkCanCreate() {
    if (_latControl.text != "" &&
        _longControl.text != "" &&
        _nameControl.text != "" &&
        _descControl.text != "") {
      return true;
    }
    return false;
  }
}

