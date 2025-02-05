/*
 *        displaypage.dart
 * 
 *    Display html (about/instructions/...)
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
import 'package:flutter/material.dart';
import 'package:flutter_widget_from_html/flutter_widget_from_html.dart';

class IQSignDisplayWidget extends StatelessWidget {
  final String _title;
  final String _displayHtml;

  const IQSignDisplayWidget(this._title, this._displayHtml, {super.key});

  @override
  Widget build(BuildContext context) {
    return IQSignDisplayPage(_title, _displayHtml);
  }
}

class IQSignDisplayPage extends StatefulWidget {
  final String _title;
  final String _displayHtml;

  const IQSignDisplayPage(this._title, this._displayHtml, {super.key});

  @override
  State<IQSignDisplayPage> createState() => _IQSignDisplayPageState();
}

class _IQSignDisplayPageState extends State<IQSignDisplayPage> {
  String _title = "";
  String _displayHtml = "";

  @override
  void initState() {
    _title = widget._title;
    _displayHtml = widget._displayHtml;
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          _title,
          style: const TextStyle(
            fontWeight: FontWeight.bold,
            color: Colors.black,
          ),
        ),
      ),
      body: widgets.topLevelPage(
        context,
        HtmlWidget(_displayHtml),
      ),
    );
  }
}
