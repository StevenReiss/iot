/********************************************************************************/
/*                                                                              */
/*              catremodel.dart                                                 */
/*                                                                              */
/*      top-level interface to Catre models                                     */
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

import 'catreuniverse.dart';
import "package:sherpa/util.dart" as util;
export 'catredata.dart';
export 'catredevice.dart';
export 'catreparameter.dart';
export 'catreprogram.dart';
export 'catreuniverse.dart';
export 'catrebridge.dart';
export 'triggertime.dart';

class CatreModel {
  static final CatreModel _catreModel = CatreModel._internal();

  CatreUniverse? _theUniverse;

  factory CatreModel() {
    return _catreModel;
  }
  CatreModel._internal();

  Future<CatreUniverse> loadUniverse() async {
    Map<String, dynamic> jresp = await util.getJson("/universe");
    if (jresp["STATUS"] != "OK") {
      throw Exception("Lost connection to CATRE");
    }
    CatreUniverse u = CatreUniverse.fromJson(jresp);
    _theUniverse = u;

    Map<String, dynamic> jresp1 = await util.getJson("/bridge/list");
    if (jresp1["STATUS"] != "OK") {
      throw Exception("Lost connection to CATRE");
    }
    _theUniverse?.addBridges(jresp1);
    return u;
  }

  Future<CatreUniverse> getUniverse() async {
    CatreUniverse? u = _theUniverse;
    if (u != null) {
      return u;
    } else {
      return await loadUniverse();
    }
  }

  void removeUniverse() {
    _theUniverse = null;
  }
}
