/********************************************************************************/
/*                                                                              */
/*              imagedata.dart                                                  */
/*                                                                              */
/*      Holder of information about an image                                    */
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
import 'package:flutter_svg/flutter_svg.dart';
import '../globals.dart' as globals;
import '../util.dart' as util;

class ImageData {
  late String _name;
  late String _description;
  late String _url;
  late String? _svg;
  late String _imageString;
  late bool _isSvg;

  ImageData(dynamic d) {
    _name = d['name'] as String;
    _description = d['description'] as String;
    _url = d['url'] as String;
    _svg = d['svg'] as String?;
    _imageString = d['imagestring'] as String;
    bool? bs = d['issvg'] as bool?;
    _isSvg = (bs ?? false);
  }

  String getName() {
    return _name;
  }

  String getDisplayName() {
    return _description;
  }

  Widget getImage() {
    if (!_isSvg) {
      Uri url3 = util.getServerUri(_url, {
        'session': globals.iqsignSession,
      });
      return Image.network(
        url3.toString(),
        width: 150,
        height: 150,
      );
    } else {
      if (_svg == null) {
        Uri url4 = util.getServerUri(_url, {
          'session': globals.iqsignSession,
        });
        return SvgPicture.network(
          url4.toString(),
          width: 150,
          height: 150,
        );
      }
      String svg2 = _svg ?? "";
      if (svg2 != '') {
        return SvgPicture.string(
          svg2,
          width: 150,
          height: 150,
        );
      }
    }
    return const Text("image");
  }

  bool filter(String s) {
    if (s == '') return true;
    if (_name.contains(s) || _description.contains(s)) return true;
    return false;
  }

  String getImageString() {
    return _imageString;
  }
}     // end of imagedata.dart

