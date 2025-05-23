/********************************************************************************/
/*                                                                              */
/*              signdata.dart                                                   */
/*                                                                              */
/*      Holder of information about a sign                                      */
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


import 'dart:math';
import '../globals.dart' as globals;
import '../util.dart' as util;

class SignData {
  late String _name;
  late String _displayName;
  late int _width;
  late int _height;
  late String _nameKey;
  late String _signDim;
  late String _signUrl;
  late String _imageUrl;
  late String _localImageUrl;
  late String _signBody;
  late int _signId;
  late int _signUser;

  SignData(d) {
    update(d);
  }

  SignData.unknown() {
    _name = "UNKNOWN";
    _displayName = "UNKNOWN";
    _width = 0;
    _height = 0;
    _nameKey = "";
    _signDim = "16by9";
    _signUrl = "";
    _imageUrl = "";
    _localImageUrl = "";
    _signId = 0;
    _signBody = "";
    _signUser = 0;
  }
  SignData.clone(SignData d) {
    _name = d._name;
    _displayName = d._displayName;
    _width = d._width;
    _height = d._height;
    _nameKey = d._nameKey;
    _signDim = d._signDim;
    _signUrl = d._signUrl;
    _imageUrl = d._imageUrl;
    _localImageUrl = d._localImageUrl;
    _signId = d._signId;
    _signBody = d._signBody;
    _signUser = d._signUser;
  }

  void update(d) {
    _name = d['name'] as String;
    _displayName = d['displayname'] as String;
    _width = d['width'] as int;
    _height = d['height'] as int;
    _nameKey = d['namekey'] as String;
    _signDim = d['dim'] as String;
    _signUrl = d['signurl'] as String;
    _imageUrl = d['imageurl'] as String;
    _localImageUrl = d['localimageurl'] as String;
    _signId = d['signid'] as int;
    _signBody = d['signbody'] as String;
    _signUser = d['signuser'] as int;
  }

  String getName() {
    return _name;
  }

  String getDisplayName() {
    return _displayName;
  }

  int getWidth() {
    return _width;
  }

  int getHeight() {
    return _height;
  }

  String getNameKey() {
    return _nameKey;
  }

  String getDimension() {
    return _signDim;
  }

  String getSignUrl() {
    return _signUrl;
  }

  String getImageUrl() {
    return "$_imageUrl?${Random().nextInt(1000000)}";
  }

  String getLocalImageUrl([bool preview = false]) {
    String s = _localImageUrl;
    if (preview) {
      s = s.replaceAll("/image", "/imagePREVIEW");
    }
    String url = "$s?${Random().nextInt(1000000)}";
    String? sess = globals.iqsignSession;
    if (sess != null) {
      url += "&session=$sess";
    }
    return url;
  }

  String getSignBody() {
    return _signBody;
  }

  int getSignId() {
    return _signId;
  }

  int getSignUserId() {
    return _signUser;
  }

  void setContents(String cnts) {
    _signBody = cnts;
  }

  void setDisplayName(String name) {
    _displayName = name;
  }

  void setName(String name) {
    _name = name;
  }

  void setSize(int wd, int ht, String dim) {
    _width = wd;
    _height = ht;
    _signDim = dim;
  }

  Future<bool> updateSign({String? name, String? dim, int? width, int? height}) async {
    String name0 = name ?? getName();
    String dim0 = dim ?? getDimension();
    int width0 = width ?? getWidth();
    int height0 = height ?? getHeight();
    var body = {
      'signdata': getSignBody(),
      'signuser': getSignUserId().toString(),
      'signname': name0,
      'signdim': dim0,
      'signwidth': width0.toString(),
      'signheight': height0.toString(),
      'signkey': getNameKey(),
      'signid': getSignId().toString(),
    };
    Map<String, dynamic> js = await util.postJson(
      "/rest/sign/update",
      body: body,
    );
    if (js['status'] != "OK") {
      if (name != null) setName(name);
      if (dim != null || width != null || height != null) {
        setSize(width0, height0, dim0);
      }
      return true;
    }
    return false;
  }
}     // end of signdata.dart

