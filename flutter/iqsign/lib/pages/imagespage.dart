/********************************************************************************/
/*                                                                              */
/*              imagespage.dart                                                 */
/*                                                                              */
/*      Page to let the user select an image for their sign                     */
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

import '../imagedata.dart';
import 'package:flutter/material.dart';
import '../widgets.dart' as widgets;
import '../util.dart' as util;

class IQSignImagesWidget extends StatelessWidget {
  final bool _isBorder;
  final bool _isSvg;

  const IQSignImagesWidget(this._isBorder, this._isSvg, {super.key});

  @override
  Widget build(BuildContext context) {
    return IQSignImagesPage(_isBorder, _isSvg);
  }
}

class IQSignImagesPage extends StatefulWidget {
  final bool _isBorder;
  final bool _isSvg;

  const IQSignImagesPage(this._isBorder, this._isSvg, {super.key});

  @override
  State<IQSignImagesPage> createState() => _IQSignImagesPageState();
}

class _IQSignImagesPageState extends State<IQSignImagesPage> {
  List<ImageData> _imageData = [];
  List<ImageData> _filteredData = [];
  final TextEditingController _filterControl = TextEditingController();

  _IQSignImagesPageState();

  @override
  void initState() {
    _getImageData(widget._isBorder, widget._isSvg);
    _filterControl.text = '';

    super.initState();
  }

  @override
  void dispose() {
    _filterControl.dispose();
    super.dispose();
  }

  Widget build1(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Choose Image For Sign",
            style: TextStyle(
              fontWeight: FontWeight.bold,
              color: Colors.black,
            )),
      ),
      body: widgets.topLevelPage(
        context,
        Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: <Widget>[
            widgets.textFormField(
              context: context,
              hint: "Filter Images",
              label: "Filter",
              controller: _filterControl,
              onChanged: _filterImages,
            ),
            widgets.fieldSeparator(),
            _imageData.isNotEmpty
                ? ListView.builder(
                    padding: const EdgeInsets.all(5.0),
                    itemCount: _filteredData.length,
                    itemBuilder: _getTile,
                  )
                : widgets.circularProgressIndicator(),
          ],
        ),
        true,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Choose Image to Add to Sign",
            style: TextStyle(
              fontWeight: FontWeight.bold,
              color: Colors.black,
            )),
      ),
      body: Container(
        decoration: BoxDecoration(
          border: Border.all(
            width: 8,
            color: const Color.fromARGB(128, 140, 180, 210),
          ),
          image: const DecorationImage(
            image: AssetImage("assets/images/iqsignstlogo.png"),
            fit: BoxFit.fitWidth,
            opacity: 0.05,
          ),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: <Widget>[
            widgets.textFormField(
              context: context,
              hint: "Filter Images",
              label: "Filter",
              controller: _filterControl,
              onChanged: _filterImages,
            ),
            widgets.fieldSeparator(),
            Container(
              decoration: BoxDecoration(
                border: Border.all(
                  width: 4,
                  color: Colors.blueAccent,
                ),
              ),
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  maxWidth: MediaQuery.of(context).size.width * 0.8,
                  maxHeight: MediaQuery.of(context).size.height * 0.8,
                ),
                child: _imageData.isNotEmpty
                    ? ListView.builder(
                        padding: const EdgeInsets.all(5.0),
                        itemCount: _filteredData.length,
                        itemBuilder: _getTile,
                      )
                    : widgets.circularProgressIndicator(),
              ),
            ),
            const Text("Long push to select item"),
          ],
        ),
      ),
    );
  }

  Future _getImageData(bool border, bool svg) async {
    List<ImageData> rslt = await loadImageData(border, svg);
    setState(() {
      _imageData = rslt;
      _filterImages(_filterControl.text);
    });
  }

  ListTile _getTile(context, int i) {
    ImageData id = _filteredData[i];
    return ListTile(
      title: Text(
        id.getName(),
        style: const TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.bold,
        ),
      ),
      subtitle: Text(
        id.getDisplayName(),
        style: const TextStyle(fontSize: 14),
      ),
      trailing: Container(
        decoration: BoxDecoration(
          border: Border.all(width: 5),
        ),
        child: id.getImage(),
      ),
      onLongPress: () => {selectImage(id)},
    );
  }

  void selectImage(ImageData id) {
    Navigator.pop(context, id);
    // go back to previous page with selected image
  }

  void _filterImages(String filter) {
    List<ImageData> lid =
        _imageData.where((ImageData id) => id.filter(filter)).toList();
    setState(() {
      _filteredData = lid;
    });
  }
} // end of inner class _IQSignImagesPageState

Future<List<ImageData>> loadImageData(bool border, bool svg) async {
  dynamic data = {
    'border': border.toString(),
    'svg': svg.toString(),
  };
  Map<String, dynamic> js = await util.postJson(
    "/rest/findimages",
    body: data,
  );
  List<ImageData> rslt = <ImageData>[];
  dynamic jid = js['data'];
  for (final id1 in jid) {
    ImageData id = ImageData(id1);
    rslt.add(id);
  }
  return rslt;
}
