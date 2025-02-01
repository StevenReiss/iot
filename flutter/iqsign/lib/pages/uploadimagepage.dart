/*
 *        uploadimagepage.dart
 * 
 *    Upload user image
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
import 'package:image_picker/image_picker.dart';
import 'dart:convert' as convert;
import 'dart:io';
import 'dart:typed_data';

class IQSignUploadImageWidget extends StatelessWidget {
  const IQSignUploadImageWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return const IQSignUploadImagePage();
  }
}

class IQSignUploadImagePage extends StatefulWidget {
  const IQSignUploadImagePage({super.key});

  @override
  State<IQSignUploadImagePage> createState() => _IQSignUploadImagePageState();
}

class _IQSignUploadImagePageState extends State<IQSignUploadImagePage> {
  final TextEditingController _nameControl = TextEditingController();
  final TextEditingController _descControl = TextEditingController();
  bool _isBorder = false;
  XFile? _image;
  final ImagePicker _picker = ImagePicker();

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          "Upload Your Image for Your Signs",
          style: TextStyle(
            fontWeight: FontWeight.bold,
            color: Colors.black,
          ),
        ),
      ),
      body: widgets.iqsignPage(
        context,
        Column(
          mainAxisAlignment: MainAxisAlignment.center,
          mainAxisSize: MainAxisSize.max,
          children: <Widget>[
            widgets.textFormField(
              hint: "Enter name to refer to image",
              label: "Image Name",
              controller: _nameControl,
            ),
            widgets.fieldSeparator(),
            _imageWidget(),
            widgets.submitButton(
              "Select Image",
              _doSelectImage,
            ),
            widgets.fieldSeparator(),
            widgets.textFormField(
              hint: "Enter descriptionn of image",
              label: "Image Description",
              controller: _descControl,
            ),
            widgets.booleanField(
              label: "This is a border image",
              value: _isBorder,
              onChanged: _handleBorderSet,
            ),
            widgets.submitButton(
              "Select Image",
              _doUploadImage,
              enabled: _isReady,
            ),
          ],
        ),
      ),
    );
  }

  Widget _imageWidget() {
    Widget c = const Text("No image selected.");
    if (_image != null) {
      c = Image.file(File(_image!.path));
    }
    return Center(child: c);
  }

  void _handleBorderSet(bool? fg) async {
    if (fg == null) return;
    setState(() {
      _isBorder = fg;
    });
  }

  void _doSelectImage() async {
    final XFile? selimg = await _picker.pickImage(
      source: ImageSource.gallery,
    );
    setState(() {
      _image = selimg;
    });
  }

  bool _isReady() {
    if (_image == null) return false;
    if (_nameControl.text.isEmpty) return false;
    if (_descControl.text.isEmpty) return false;
    return true;
  }

  void _doUploadImage() async {
    if (!_isReady()) return;

    XFile? xim = _image;
    if (xim == null) return;

    BuildContext dcontext = context;

    Uint8List filebytes = await xim.readAsBytes();
    String filedata = convert.base64.encode(filebytes);

    var data = {
      'imagefile': xim.path,
      'imagevalue': filedata,
      'imagename': _nameControl.text,
      '_imagedescription': _descControl.text,
      '_imageborder': _isBorder,
    };
    Map<String, dynamic> js = await util.postJson(
      '/rest/defineimage',
      body: data,
    );
    if (js['status'] == 'OK') {
      if (dcontext.mounted) {
        Navigator.pop(dcontext, true);
      }
    }
  }
}
