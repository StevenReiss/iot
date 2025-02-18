/*
 *        editsignpage.dart
 * 
 *    Page for editing a sign
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

import '../signdata.dart';
import 'package:flutter/material.dart';
import '../widgets.dart' as widgets;
import '../util.dart' as util;
import 'imagespage.dart';
import 'uploadimagepage.dart';
import '../imagedata.dart';
import 'displaypage.dart';

class IQSignSignEditWidget extends StatelessWidget {
  final SignData _signData;
  final List<String> _signNames;

  const IQSignSignEditWidget(this._signData, this._signNames, {super.key});

  @override
  Widget build(BuildContext context) {
    return IQSignSignEditPage(_signData, _signNames);
  }
}

class IQSignSignEditPage extends StatefulWidget {
  final SignData _signData;
  final List<String> _signNames;

  const IQSignSignEditPage(this._signData, this._signNames, {super.key});

  @override
  State<IQSignSignEditPage> createState() => _IQSignSignEditPageState();
}

class _IQSignSignEditPageState extends State<IQSignSignEditPage> {
  SignData _signData = SignData.unknown();
  List<String> _signNames = [];
  List<String> _knownNames = [];
  List<String> _refNames = [];
  final TextEditingController _controller = TextEditingController();
  final TextEditingController _nameController = TextEditingController();
  bool _preview = false;
  bool _changed = false;

  _IQSignSignEditPageState();

  @override
  void initState() {
    _signData = widget._signData;
    _knownNames = widget._signNames;
    _nameController.text = _signData.getDisplayName();
    _controller.text = _signData.getSignBody();

    super.initState();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    _signNames = ['Current Sign', ...widget._signNames];
    _refNames = ['< NONE >', ...widget._signNames];
    bool repl = _knownNames.contains(_nameController.text);
    String accept = repl ? "Update" : "Create";
    if (repl && _controller.text.isEmpty) accept = "Delete";

    String btnname = "$accept Saved Image: ${_nameController.text}";
    Widget namefield = widgets.textField(
      label: "SignName",
      controller: _nameController,
      onChanged: _nameChanged,
    );
    Widget cntsfield = widgets.textField(
      controller: _controller,
      maxLines: 8,
      showCursor: true,
      onChanged: _signUpdated,
    );

    String imageurl = _signData.getLocalImageUrl(_preview);
    return Scaffold(
      appBar: AppBar(
        title: Text("Customize Sign: ${_signData.getName()}",
            style: const TextStyle(
              fontWeight: FontWeight.bold,
              color: Colors.black,
            )),
        actions: [
          widgets.topMenuAction(
            <widgets.MenuAction>[
              widgets.MenuAction(
                "Sign Instructions",
                _helpAction,
                "Show instructions for creating a sign",
              ),
              widgets.MenuAction(
                "Add Image From Image Library",
                _addSvgImage,
                "Browse image library to find an image for your sign",
              ),
              widgets.MenuAction(
                "Add Image From My Images",
                _addMyImage,
                "Browse uploaded images to find image for your sign",
              ),
              widgets.MenuAction(
                "Add Border Image",
                _addBorderImage,
                "Browse border images for your sign",
              ),
              widgets.MenuAction(
                "Upload New Image to Image Library",
                _imageUploadAction,
                "Choose new images to upload to your image library",
              ),
              widgets.MenuAction(
                "About iQsign",
                _aboutAction,
                "Get information about your sign",
              ),
            ],
          ),
        ],
      ),
      body: widgets.topLevelPage(
        context,
        Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: <Widget>[
            Image.network(
              imageurl,
              width: MediaQuery.of(context).size.width * 0.4,
              height: MediaQuery.of(context).size.height * 0.25,
            ),
            widgets.fieldSeparator(),
            SizedBox(
              width: MediaQuery.of(context).size.width * 0.8,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  const Text("Start with:         "),
                  Expanded(child: _createNameSelector()),
                ],
              ),
            ),
            SizedBox(
              width: MediaQuery.of(context).size.width * 0.8,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  const Text("Refer to:            "),
                  Expanded(child: _createReferenceSelector()),
                ],
              ),
            ),
            widgets.fieldSeparator(),
            SizedBox(
              width: MediaQuery.of(context).size.width * 0.8,
              child: cntsfield,
            ),
            widgets.fieldSeparator(),
            SizedBox(
              width: MediaQuery.of(context).size.width * 0.8,
              child: Row(mainAxisAlignment: MainAxisAlignment.center, children: <Widget>[
                const Text("Saved Name:    "),
                Expanded(child: namefield),
              ]),
            ),
            Container(
              constraints: const BoxConstraints(minWidth: 150, maxWidth: 350),
              width: MediaQuery.of(context).size.width * 0.4,
              child: widgets.submitButton(
                btnname,
                _handleUpdate,
                enabled: _updateValid(),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _addSvgImage() async {
    ImageData? id = await gotoImagePage(false, true);
    insertImage(id);
  }

  void _addMyImage() async {
    ImageData? id = await gotoImagePage(false, false);
    insertImage(id);
  }

  void _addBorderImage() async {
    ImageData? id = await gotoImagePage(true, false);
    insertImage(id);
  }

  void _imageUploadAction() async {
    await _gotoUploadImagePage();
  }

  void _helpAction() async {
    await _gotoDisplay("Instructions", "/rest/instructions");
  }

  void _aboutAction() async {
    await _gotoDisplay("About Page", "/rest/about");
  }

  Future<ImageData?> gotoImagePage(bool border, bool svg) async {
    dynamic rslt = await Navigator.push(context, MaterialPageRoute<ImageData?>(
      builder: (BuildContext context) {
        return IQSignImagesPage(border, svg);
      },
    ));
    if (rslt.runtimeType == ImageData) {
      return rslt as ImageData;
    }
    return null;
  }

  dynamic _gotoDisplay(String title, String url) async {
    BuildContext dcontext = context;
    Map<String, dynamic> js = await util.postJson(url, body: {
      "signid": _signData.getSignId().toString(),
    });
    if (js['status'] == 'OK' && dcontext.mounted) {
      String html = js['html'] as String;
      widgets.goto(dcontext, IQSignDisplayWidget(title, html));
    }
  }

  dynamic _gotoUploadImagePage() async {
    widgets.goto(context, const IQSignUploadImageWidget());
  }

  void insertImage(ImageData? id) {
    if (id == null) return;
    String txt = id.getImageString();
    if (!_controller.text.endsWith("\n")) txt = "\n$txt";
    _controller.text += txt;
    _signUpdated();
  }

  Widget _createNameSelector({String? val}) {
    List<String> base = _signNames;
    val ??= base.first;
    return widgets.dropDownMenu(
      base,
      value: val,
      onChanged: (String? val) async {
        if (val != null) await _setSignToSaved(val);
      },
    );
  }

  Future _setSignToSaved(String? name) async {
    if (name == null) return;
    if (name == "Current Sign") name = "*Current*";
    Map<String, dynamic> js = await util.postJson(
      "/rest/loadsignimage",
      body: {
        'signname': name,
        'signid': _signData.getSignId().toString(),
      },
    );
    if (js['status'] == "OK") {
      String cnts = js['contents'] as String;
      String sname = js['name'] as String;
      setState(() {
        _nameController.text = sname;
        _controller.text = cnts;
        _changed = false;
      });
      await _previewAction();
    }
  }

  Widget _createReferenceSelector({String? val}) {
    List<String> base = _refNames;
    val ??= base.first;
    return widgets.dropDownMenu(
      base,
      value: val,
      onChanged: (String? val) async {
        if (val != null) await _setSignToReference(val);
      },
    );
  }

  Future _setSignToReference(String? name) async {
    if (name == null) return;
    setState(() {
      _controller.text = "=$name";
      _nameController.text = name;
      _changed = false;
    });

    await _previewAction();
  }

  void _signUpdated([String txt]) async {
    if (_controller.text.isEmpty) {
      bool repl = _knownNames.contains(_nameController.text);
      if (!repl) _nameController.text = "";
    }
    await _previewAction();
    setState(() {
      _changed = true;
    });
  }

  Future _saveSignImage(String name, String cnts) async {
    Map<String, dynamic> js = await util.postJson(
      "/rest/savesignimage",
      body: {
        'name': name,
        'signid': _signData.getSignId().toString(),
        'signnamekey': _signData.getNameKey(),
        'signuser': _signData.getSignUserId().toString(),
        'signbody': cnts,
      },
    );
    if (js['status'] != "OK") {
      // handle errors here
    }
  }

  Future _removeSignImage(String name) async {
    Map<String, dynamic> js = await util.postJson(
      "/rest/removesignimage",
      body: {
        'name': name,
        'signid': _signData.getSignId().toString(),
        'signnamekey': _signData.getNameKey(),
        'signuser': _signData.getSignUserId().toString(),
      },
    );
    if (js['status'] != "OK") {
      // handle errors here
    }
  }

  Future _handleUpdate() async {
    String name = _nameController.text;
    String cnts = _controller.text;

    // update sign names
    if (name.isNotEmpty && !_signNames.contains(name)) {
      _signNames.add(name);
      _signNames.sort();
    } else if (name.isNotEmpty && cnts.isEmpty) {
      _signNames.remove(name);
    }

    // update sign image in server
    if (name.isNotEmpty && cnts.isNotEmpty) {
      await _saveSignImage(name, cnts);
    } else if (name.isNotEmpty && cnts.isEmpty) {
      await _removeSignImage(name);
      setState(() {
        _nameController.text = "";
      });
    }
    setState(() => () {
          _changed = false;
        });
  }

  void _nameChanged(String val) {
    setState(() {
      _changed = true;
    });
  }

  bool _updateValid() {
    if (!_changed) return false;
    if (_nameController.text.isEmpty) return false;
    if (_controller.text.isEmpty) {
      if (!_knownNames.contains(_nameController.text)) return false;
    }

    return true;
  }

  Future _previewAction() async {
    if (_controller.text.isEmpty) return;
    var body = {
      'signdata': _controller.text,
      'signuser': _signData.getSignUserId().toString(),
      'signid': _signData.getSignId().toString(),
      'signkey': _signData.getNameKey(),
    };
    Map<String, dynamic> js = await util.postJson(
      "/rest/sign/preview",
      body: body,
    );
    if (js['status'] == 'OK') {
      setState(() {
        _preview = true;
      });
    }
  }
}
