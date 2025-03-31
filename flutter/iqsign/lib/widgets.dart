/********************************************************************************/
/*                                                                              */
/*              widgets.dart                                                    */
/*                                                                              */
/*      Widget definitions                                                      */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2025 Brown University -- Steven P. Reiss                      */
/********************************************************************************/
/*********************************************************************************
 *                                                                               *
 *  This work is licensed under Creative Commons Attribution-NonCommercial 4.0   *
 *  International.  To view a copy of this license, visit                        *
 *      https://creativecommons.org/licenses/by-nc/4.0/                          *
 *                                                                               *
 ********************************************************************************/

import 'package:flutter/material.dart';
import 'lookandfeel.dart' as laf;
import 'package:intl/intl.dart' as intl;
import 'package:duration_picker/duration_picker.dart';
import 'package:flutter_spinbox/material.dart';

/********************************************************************************/
/*                                                                              */
/*    Text widgets                                                              */
/*                                                                              */
/********************************************************************************/

Widget textFormField({
  String? hint,
  String? label,
  TextEditingController? controller,
  ValueChanged<String>? onChanged,
  VoidCallback? onEditingComplete,
  ValueChanged<String>? onSubmitted,
  String? Function(String?)? onSaved,
  TextInputAction? textInputAction,
  GestureTapCallback? onTap,
  String? Function(String?)? validator,
  bool? showCursor,
  int? maxLines,
  TextInputType? keyboardType,
  bool obscureText = false,
  double fraction = 0,
  BuildContext? context,
  bool? enabled,
  String tooltip = "",
  Widget? suffixIcon,
}) {
  label ??= hint;
  hint ??= label;
  if (obscureText) maxLines = 1;
  double vpadding = 0.0;
  if (maxLines != null && maxLines > 1) {
    vpadding = 8.0;
  }

  Widget w = TextFormField(
    decoration: getDecoration(
      hint: hint,
      label: label,
      vPadding: vpadding,
      suffixIcon: suffixIcon,
    ),
    validator: validator,
    controller: controller,
    onChanged: onChanged,
    onEditingComplete: onEditingComplete,
    onFieldSubmitted: onSubmitted,
    onSaved: onSaved,
    onTap: onTap,
    textInputAction: textInputAction,
    showCursor: showCursor,
    maxLines: maxLines,
    obscureText: obscureText,
    enabled: enabled,
  );
  if (fraction != 0 && context != null) {
    double minw = 100;
    double maxw = 600;
    if (fraction <= 0.4) {
      minw = 150;
      maxw = 350;
    }
    w = Container(
      constraints: BoxConstraints(minWidth: minw, maxWidth: maxw),
      width: MediaQuery.of(context).size.width * fraction,
      child: w,
    );
  }
  w = tooltipWidget(tooltip, w);

  return w;
}

Widget textField({
  String? hint,
  String? label,
  TextEditingController? controller,
  ValueChanged<String>? onChanged,
  VoidCallback? onEditingComplete,
  ValueChanged<String>? onSubmitted,
  TapRegionCallback? onTapOutside,
  FocusNode? focusNode,
  bool? showCursor,
  int? maxLines,
  TextInputType? keyboardType,
  TextInputAction? textInputAction,
  bool? enabled,
  String tooltip = "",
  bool readOnly = false,
  double? height,
  bool collapse = false,
}) {
  label ??= hint;
  hint ??= label;
  if (maxLines == 0) {
    maxLines = null;
  } else {
    maxLines ??= 1;
  }
  keyboardType ??=
      (maxLines == 1 ? TextInputType.text : TextInputType.multiline);

  InputDecoration deco = getDecoration(hint: hint, label: label);
  if (collapse) {
    deco = InputDecoration(
      hintText: hint,
      isCollapsed: true,
      contentPadding: const EdgeInsets.only(left: 8, right: 4),
      border: const OutlineInputBorder(),
    );
  }

  Widget w = TextField(
    controller: controller,
    onChanged: onChanged,
    onEditingComplete: onEditingComplete,
    onSubmitted: onSubmitted,
    onTapOutside: onTapOutside,
    showCursor: showCursor,
    maxLines: maxLines,
    focusNode: focusNode,
    keyboardType: keyboardType,
    textInputAction: textInputAction,
    enabled: enabled,
    readOnly: readOnly,
    decoration: deco,
  );
  if (height != null) {
    w = SizedBox(height: height, child: w);
  }
  w = tooltipWidget(tooltip, w);

  return w;
}

Widget errorField(String? text) {
  String t1 = (text ?? "");
  return largeText(
    t1,
    color: laf.errorColor,
    scaler: laf.errorFontScale,
  );
}

Widget loginTextField(
  BuildContext context, {
  String? hint,
  String? label,
  TextEditingController? controller,
  ValueChanged<String>? onChanged,
  String? Function(String?)? validator,
  TextInputType? keyboardType,
  bool obscureText = false,
  double fraction = 0,
  String tooltip = "",
  TextInputAction textInputAction = TextInputAction.next,
}) {
  Widget form = textFormField(
    hint: hint,
    label: label,
    controller: controller,
    onChanged: onChanged,
    validator: validator,
    context: context,
    fraction: fraction,
    obscureText: obscureText,
    keyboardType: keyboardType,
    textInputAction: textInputAction,
  );
  Widget w = Container(
    constraints: const BoxConstraints(minWidth: 100, maxWidth: 600),
    width: MediaQuery.of(context).size.width * 0.8,
    child: form,
  );

  w = tooltipWidget(tooltip, w);

  return w;
}

Widget itemWithMenu<T>(
  String lbl,
  List<MenuAction> acts, {
  void Function()? onTap,
  void Function()? onDoubleTap,
  void Function()? onLongPress,
  String tooltip = "",
}) {
  Widget btn = PopupMenuButton(
    icon: const Icon(Icons.menu_open_rounded),
    itemBuilder: (context) => _itemMenuBuilder(acts),
    onSelected: (MenuAction act) => act.action(),
  );
  Widget wt = Text(lbl);
  wt = tooltipWidget(tooltip, wt);
  Widget w = Row(
    mainAxisAlignment: MainAxisAlignment.center,
    children: <Widget>[
      btn,
      wt,
      const Spacer(flex: 10),
    ],
  );

  onDoubleTap ??= onTap;
  onLongPress ??= onDoubleTap;
  if (onTap == null && onDoubleTap == null && onLongPress == null) {
    return w;
  }

  Widget w1 = GestureDetector(
    key: Key(lbl),
    onTap: onTap,
    onDoubleTap: onDoubleTap,
    onLongPress: onLongPress,
    onSecondaryTap: onDoubleTap,
    onTertiaryTapUp: _dummyTapUp(onLongPress),
    child: w,
  );
  return w1;
}

List<PopupMenuItem<MenuAction>> _itemMenuBuilder(
  List<MenuAction> acts,
) {
  return acts.map<PopupMenuItem<MenuAction>>(_menuItemAction).toList();
}

void Function(TapUpDetails) _dummyTapUp(Function? use) {
  return (TapUpDetails v) {
    if (use != null) use();
  };
}

Widget tooltipWidget(String tooltip, Widget w) {
  if (tooltip.isEmpty) return w;
  Widget tt = Tooltip(
    message: tooltip,
    decoration: BoxDecoration(
      borderRadius: BorderRadius.circular(10),
      gradient: LinearGradient(
        colors: <Color>[laf.toolTipLeftColor, laf.toolTipRightColor],
      ),
    ),
    height: laf.toolTipHeight,
    padding: const EdgeInsets.all(8.0),
    preferBelow: true,
    textStyle: const TextStyle(
      fontSize: laf.toolTipFontSize,
      color: Colors.black,
    ),
    showDuration: const Duration(seconds: 2),
    waitDuration: const Duration(seconds: 1),
    child: w,
  );
  return tt;
}

/********************************************************************************/
/*                                                                              */
/*    Buttons                                                                   */
/*                                                                              */
/********************************************************************************/

Widget submitButton(
  String name,
  void Function()? action, {
  bool enabled = true,
  String tooltip = "",
}) {
  ButtonStyle style = ElevatedButton.styleFrom(
    backgroundColor: laf.submitBackgroundColor,
    foregroundColor: laf.submitForegroundColor,
    textStyle: const TextStyle(fontWeight: FontWeight.bold),
    //  overlayColor: Colors.brown,
  );
  if (!enabled) action = null;
  ElevatedButton eb = ElevatedButton(
    onPressed: action,
    style: style,
    child: Text(name),
  );
  Widget w = Padding(
    padding: const EdgeInsets.symmetric(
      vertical: 16.0,
      horizontal: 6.0,
    ),
    child: eb,
  );
  w = tooltipWidget(tooltip, w);

  return w;
}

Widget textButton(
  String label,
  void Function()? action, {
  String tooltip = "",
}) {
  Widget w = TextButton(
    style: TextButton.styleFrom(
      textStyle: const TextStyle(fontSize: laf.buttonFontSize),
    ),
    onPressed: action,
    child: Text(label),
  );

  w = tooltipWidget(tooltip, w);

  return w;
}

/********************************************************************************/
/*                                                                              */
/*      Top menus                                                               */
/*                                                                              */
/********************************************************************************/

Widget topMenu(void Function(String)? handler, List labels) {
  return PopupMenuButton(
    icon: const Icon(laf.topMenuIcon),
    itemBuilder: (context) => _topMenuBuilder(labels),
    onSelected: handler,
  );
}

List<PopupMenuItem<String>> _topMenuBuilder(List labels) {
  return labels.map<PopupMenuItem<String>>(_menuItem).toList();
}

Widget topMenuAction(List<MenuAction> labels) {
  return PopupMenuButton(
    icon: const Icon(Icons.menu_sharp),
    itemBuilder: (context) => _topMenuActionBuilder(labels),
    onSelected: (dynamic act) async => await act.action(),
  );
}

List<PopupMenuItem<MenuAction>> _topMenuActionBuilder(
  List<MenuAction> labels,
) {
  return labels
      .map<PopupMenuItem<MenuAction>>(_menuItemAction)
      .toList();
}

PopupMenuItem<MenuAction> _menuItemAction(dynamic val) {
  return PopupMenuItem<MenuAction>(
    value: val,
    child: tooltipWidget(val.tooltip, Text(val.label)),
  );
}

PopupMenuItem<String> _menuItem(dynamic val) {
  String value = 'Unknown';
  String label = 'Unknown';
  String tooltip = '';
  if (val is String) {
    value = val;
    label = val;
  } else if (val is Map<String, dynamic>) {
    if (val['name'] != null && val['label'] != null) {
      label = val['label'];
      value = val['name'];
      String? tt = val['tooltip'];
      if (tt != null) tooltip = tt;
    } else {
      for (String k in val.keys) {
        value = k;
        if (val[k] is String) {
          label = val[k] as String;
        } else if (val[k] is List<String>) {
          List<String> vals = val[k] as List<String>;
          label = vals[0];
          tooltip = vals[1];
        }
      }
    }
  }
  return PopupMenuItem<String>(
    value: value,
    child: tooltipWidget(tooltip, Text(label)),
  );
}

class MenuAction {
  String label;
  void Function() action;
  String tooltip;
  MenuAction(this.label, this.action, [this.tooltip = ""]);
}

/********************************************************************************/
/*                                                                              */
/*      Field separator                                                         */
/*                                                                              */
/********************************************************************************/

Widget fieldSeparator([double ht = 8]) {
  return SizedBox(height: ht);
}

Widget fieldDivider({
  double height = 8,
  double? thickness,
  Color color = laf.topLevelBackground,
}) {
  thickness ??= height;
  return Divider(height: height, thickness: thickness, color: color);
}

/********************************************************************************/
/*                                                                              */
/*      Text fields                                                             */
/*                                                                              */
/********************************************************************************/

Widget largeText(
  String data, {
  TextStyle? style,
  TextAlign? textAlign,
  int? maxLines,
  Color? color,
  Color? backgroundColor,
  Color? selectionColor,
  double scaler = 1.75,
}) {
  style ??= TextStyle(color: color, backgroundColor: backgroundColor);
  return Text(
    data,
    textScaler: TextScaler.linear(scaler),
    style: style,
    textAlign: textAlign,
    maxLines: maxLines,
    selectionColor: selectionColor,
  );
}

Widget largeBoldText(
  String data, {
  TextAlign? textAlign,
  int? maxLines,
  Color? selectionColor,
  Color? color,
  Color? backgroundColor,
  double scaler = 1.75,
}) {
  TextStyle style = TextStyle(
    color: color,
    backgroundColor: backgroundColor,
    fontWeight: FontWeight.bold,
  );
  return largeText(
    data,
    style: style,
    textAlign: textAlign,
    maxLines: maxLines,
    selectionColor: selectionColor,
    scaler: scaler,
  );
}

/********************************************************************************/
/*                                                                              */
/*      Drop down selectors                                                     */
/*                                                                              */
/********************************************************************************/

Widget dropDown(
  List<String> items, {
  String? value,
  Function(String?)? onChanged,
  TextAlign textAlign = TextAlign.left,
  String tooltip = "",
}) {
  value ??= items[0];
  Widget w = DropdownButton<String>(
    value: value,
    onChanged: onChanged,
    items: items.map<DropdownMenuItem<String>>((String value) {
      return DropdownMenuItem<String>(
        value: value,
        child: Text(value, textAlign: textAlign),
      );
    }).toList(),
  );
  w = tooltipWidget(tooltip, w);
  return w;
}

Widget dropDownMenu(
  List<String> items, {
  String? value,
  Function(String?)? onChanged,
  textAlign = TextAlign.left,
  String tooltip = "",
}) {
  value ??= items[0];
  Widget w = DropdownMenu<String>(
    initialSelection: value,
    requestFocusOnTap: true,
    onSelected: onChanged,
    dropdownMenuEntries:
        items.map<DropdownMenuEntry<String>>((String value) {
      return DropdownMenuEntry<String>(value: value, label: value);
    }).toList(),
  );
  w = tooltipWidget(tooltip, w);
  return w;
}

Widget dropDownWidget<T>(
  List<T> items, {
  String Function(T)? labeler,
  T? value,
  void Function(T?)? onChanged,
  textAlign = TextAlign.left,
  String? nullValue,
  String? label,
  String? hint,
  String tooltip = "",
}) {
  String Function(T) lbl = (x) => x.toString();
  if (labeler != null) lbl = labeler;
  List<DropdownMenuItem<T?>> itmlst = [];
  if (nullValue != null) {
    itmlst.add(
      DropdownMenuItem<T?>(
        value: null,
        child: Text(nullValue, textAlign: textAlign),
      ),
    );
  } else {
    value ??= items[0];
  }

  itmlst.addAll(
    items.map<DropdownMenuItem<T>>((T v) {
      return DropdownMenuItem<T>(
        value: v,
        enabled: true,
        child: Text(lbl(v)),
      );
    }).toList(),
  );

  DropdownButtonFormField<T?> fld = DropdownButtonFormField<T?>(
    value: value,
    onChanged: onChanged,
    items: itmlst,
    isDense: true,
    decoration: getDecoration(label: label, hint: hint),
  );
  return tooltipWidget(tooltip, fld);
}

/********************************************************************************/
/*                                                                              */
/*      Boolean buttons                                                         */
/*                                                                              */
/********************************************************************************/

Widget booleanField({
  String? label,
  bool value = false,
  void Function(bool?)? onChanged,
  String tooltip = "",
  compact = false,
}) {
  label ??= "";
  Widget w1 = Checkbox(value: value, onChanged: onChanged);
  if (compact) {
    w1 = SizedBox(
      height: 24.0,
      width: 24.0,
      child: Transform.scale(scale: 0.8, child: w1),
    );
  }
  Widget w = Row(
    mainAxisSize: MainAxisSize.min,
    mainAxisAlignment: MainAxisAlignment.center,
    crossAxisAlignment: CrossAxisAlignment.center,
    children: <Widget>[w1, Text(label)],
  );
  return tooltipWidget(tooltip, w);
}

/********************************************************************************/
/*                                                                              */
/*      Page navigation assistance                                              */
/*                                                                              */
/********************************************************************************/

void goto(BuildContext context, Widget w) {
  // if (!context.mounted) return;
  Navigator.of(
    context,
  ).push(MaterialPageRoute(builder: (context) => w));
}

Future<dynamic> gotoThen(BuildContext context, Widget w) async {
  await Navigator.of(
    context,
  ).push(MaterialPageRoute(builder: (context) => w));
}

void gotoDirect(BuildContext context, Widget w) {
  MaterialPageRoute route = MaterialPageRoute(builder: (context) => w);
  Navigator.of(context).pushReplacement(route);
}

void gotoReplace(BuildContext context, Widget w) {
  Navigator.of(context).popUntil((route) => false);
  MaterialPageRoute route = MaterialPageRoute(builder: (context) => w);
  Navigator.of(context).push(route);
}

dynamic gotoResult(BuildContext context, Widget w) async {
  return goto(context, w);
}

/********************************************************************************/
/*                                                                              */
/*      Lists and list boxes                                                    */
/*                                                                              */
/********************************************************************************/

Widget listBox<T>(
  String what,
  List<T> data,
  Widget Function(T) itemBuilder,
  void Function() add, {
  String tooltip = "",
  String addToolTip = "",
}) {
  List<Widget> widgets = data.map(itemBuilder).toList();
  Widget view = ListBody(children: widgets);
  view = tooltipWidget(tooltip, view);
  // ListView view = ListView.builder(
  //     padding: const EdgeInsets.all(2),
  //     itemCount: data.length,
  //     itemBuilder: (BuildContext context, int idx) {
  //       return itemBuilder(data[idx]);
  //     });
  String label = "${what}s";
  return Column(
    mainAxisAlignment: MainAxisAlignment.start,
    mainAxisSize: MainAxisSize.min,
    children: <Widget>[
      Row(
        mainAxisAlignment: MainAxisAlignment.start,
        children: <Widget>[Text(label, style: getLabelStyle())],
      ),
      view,
      Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: <Widget>[
          tooltipWidget(
            addToolTip,
            IconButton(
              icon: const Icon(Icons.add_box_outlined),
              tooltip: 'Add New $what',
              onPressed: add,
            ),
          ),
        ],
      ),
    ],
  );
}

/********************************************************************************/
/*                                                                              */
/*      Date and time fields                                                    */
/*                                                                              */
/********************************************************************************/

class DateFormField {
  late final BuildContext context;
  late final TextEditingController _editControl;
  late TextFormField _textField;
  final void Function(DateTime)? onChanged;
  late final DateTime _startDate;
  late final DateTime _endDate;
  late final String? _helpText;

  DateFormField(
    this.context, {
    String? hint,
    String? label,
    DateTime? startDate,
    DateTime? endDate,
    DateTime? initialDate,
    this.onChanged,
  }) {
    _editControl = TextEditingController();
    label ??= hint;
    hint ??= label;
    _helpText = label;
    initialDate ??= DateTime.now();
    startDate ??= DateTime(2020);
    endDate ??= DateTime(2030);
    _startDate = startDate;
    _endDate = endDate;
    _editControl.text = _formatDate(initialDate);
    _textField = TextFormField(
      controller: _editControl,
      decoration: getDecoration(hint: hint, label: label),
      keyboardType: TextInputType.datetime,
      onTap: _handleTap,
      onChanged: _handleChange,
    );
  }

  Widget get widget => _textField;

  void _handleTap() async {
    DateTime? newd = _decodeDate(_editControl.text);
    newd ??= _endDate;
    //  newd ??= DateTime.now();
    DateTime? nextd = await showDatePicker(
      context: context,
      initialDate: newd,
      firstDate: _startDate,
      lastDate: _endDate,
      helpText: _helpText,
    );
    if (nextd != null) {
      _editControl.text = _formatDate(nextd);
      onChanged!(nextd);
    }
    // bring up date picker here
  }

  void _handleChange(String s) {
    DateTime? newd = DateTime.tryParse(_editControl.text);
    if (newd != null) {
      onChanged!(newd);
    }
  }

  static String _formatDate(DateTime t) {
    t = t.toLocal();
    intl.DateFormat dfmt = intl.DateFormat("EEE MMM d, yyyy");
    return dfmt.format(t);
  }

  static DateTime? _decodeDate(String txt) {
    intl.DateFormat dfmt = intl.DateFormat("EEE MMM d, yyyy");
    try {
      DateTime t = dfmt.parseLoose(txt);
      t = t.toUtc();
      return t;
    } catch (e) {
      return null;
    }
  }
}

class TimeFormField {
  late final BuildContext context;
  late final TextEditingController _editControl;
  late TextFormField _textField;
  final void Function(TimeOfDay)? onChanged;
  late final String? _helpText;

  TimeFormField(
    this.context, {
    String? hint,
    String? label,
    TimeOfDay? initialTime,
    DateTime? current,
    this.onChanged,
  }) {
    _editControl = TextEditingController();
    label ??= hint;
    hint ??= label;
    _helpText = label;
    if (current != null) {
      initialTime ??= TimeOfDay.fromDateTime(current);
    }
    initialTime ??= TimeOfDay.now();
    _editControl.text = _formatTime(initialTime);

    _textField = TextFormField(
      controller: _editControl,
      decoration: getDecoration(hint: hint, label: label),
      keyboardType: TextInputType.datetime,
      onTap: _handleTap,
      onChanged: _handleChange,
    );
  }

  Widget get widget => _textField;

  void _handleTap() async {
    TimeOfDay? newd = parseTime(_editControl.text);
    newd ??= TimeOfDay.now();
    TimeOfDay? nextd = await showTimePicker(
      context: context,
      initialTime: newd,
      helpText: _helpText,
    );
    if (nextd != null) {
      _editControl.text = _formatTime(nextd);
      onChanged!(nextd);
    }
    // bring up date picker here
  }

  void _handleChange(String s) {
    TimeOfDay? newd = parseTime(_editControl.text);
    if (newd != null) {
      onChanged!(newd);
    }
  }

  String _formatTime(TimeOfDay tod) {
    return tod.format(context);
  }

  TimeOfDay? parseTime(String t) {
    DateTime dt = DateTime.now();
    String txt = intl.DateFormat.yMd().format(dt);
    txt += " $t";
    DateTime? dt1 = DateTime.tryParse(txt);
    if (dt1 == null) return null;
    return TimeOfDay.fromDateTime(dt1);
  }
}

class DurationFormField {
  late final BuildContext context;
  late final TextEditingController _editControl;
  late TextFormField _textField;
  final void Function(Duration)? onChanged;

  DurationFormField(
    this.context, {
    String? hint,
    String? label,
    Duration? initialDuration,
    this.onChanged,
  }) {
    _editControl = TextEditingController();
    label ??= hint;
    hint ??= label;
    initialDuration ??= const Duration(minutes: 5);
    _editControl.text = _formatDuration(initialDuration);

    _textField = TextFormField(
      controller: _editControl,
      decoration: getDecoration(hint: hint, label: label),
      keyboardType: TextInputType.datetime,
      onTap: _handleTap,
      onChanged: _handleChange,
    );
  }

  Widget get widget => _textField;

  void _handleTap() async {
    Duration? newd = parseDuration(_editControl.text);
    newd ??= const Duration(minutes: 5);
    Duration? nextd = await showDurationPicker(
      context: context,
      initialTime: newd,
      baseUnit: BaseUnit.minute,
      upperBound: const Duration(hours: 12),
      lowerBound: const Duration(minutes: 1),
    );
    if (nextd != null) {
      _editControl.text = _formatDuration(nextd);
      // onChanged!(nextd);
    }
    // bring up date picker here
  }

  void _handleChange(String s) {
    Duration? newd = parseDuration(_editControl.text);
    if (newd != null) {
      onChanged!(newd);
    }
  }

  String _formatDuration(Duration tod) {
    int hrs = tod.inHours;
    int mins = tod.inMinutes.remainder(60);
    String shrs = "${twoDigits(hrs)}:";
    String smins = twoDigits(mins);
    int sec = tod.inSeconds.remainder(60);
    String ssec = ":${twoDigits(sec)}";

    return shrs + smins + ssec;
  }

  Duration? parseDuration(String t) {
    List<String> timeparts = t.split(":");
    int hrs = 0;
    int pt = 0;
    if (timeparts.length == 3) hrs = int.parse(timeparts[pt++]);
    int mins = int.parse(timeparts[pt++]);
    int secs = int.parse(timeparts[pt++]);
    return Duration(hours: hrs, minutes: mins, seconds: secs);
  }

  String twoDigits(int n) {
    if (n >= 10) {
      return "$n";
    } else {
      return "0$n";
    }
  }
} // end of DurationFormField

/********************************************************************************/
/*                                                                              */
/*      Numeric fields                                                          */
/*                                                                              */
/********************************************************************************/

Widget integerField({
  required int min,
  required int max,
  required int value,
  TextAlign textAlign = TextAlign.left,
  required String label,
  Function(dynamic)? onChanged,
  String tooltip = "",
}) {
  return numberField(
    min: min.toDouble(),
    max: max.toDouble(),
    value: value.toDouble(),
    textAlign: textAlign,
    label: label,
    onChanged: onChanged,
    decimals: 0,
    isInt: true,
    tooltip: tooltip,
  );
}

Widget doubleField({
  required double min,
  required double max,
  required double value,
  TextAlign textAlign = TextAlign.left,
  required String label,
  Function(dynamic)? onChanged,
  int decimals = 1,
  String tooltip = "",
}) {
  return numberField(
    min: min,
    max: max,
    value: value,
    textAlign: textAlign,
    label: label,
    onChanged: onChanged,
    decimals: decimals,
    isInt: false,
    tooltip: tooltip,
  );
}

Widget numberField({
  required double min,
  required double max,
  required double value,
  TextAlign textAlign = TextAlign.left,
  required String label,
  Function(dynamic)? onChanged,
  int decimals = 1,
  bool isInt = false,
  String tooltip = "",
}) {
  InputDecoration d = getDecoration(label: label);
  Widget w1 = SpinBox(
    min: min,
    max: max,
    value: value,
    textAlign: textAlign,
    decoration: d,
    decimals: decimals,
    onChanged: onChanged,
  );
  Widget w2 = Slider.adaptive(
    min: min,
    max: max,
    value: value,
    onChanged: onChanged,
    label: (isInt ? value.round().toString() : value.toString()),
  );
  SliderThemeData sd = SliderThemeData.fromPrimaryColors(
    primaryColor: const Color.fromARGB(185, 121, 85, 72),
    primaryColorDark: Colors.brown,
    primaryColorLight: const Color.fromARGB(73, 121, 85, 72),
    valueIndicatorTextStyle: const TextStyle(color: Colors.white),
  );
  sd = sd.copyWith(showValueIndicator: ShowValueIndicator.always);
  Widget w3 = SliderTheme(data: sd, child: w2);
  Widget w4 = Row(
    children: <Widget>[
      Expanded(flex: 3, child: w1),
      Expanded(flex: 10, child: w3),
    ],
  );

  w4 = tooltipWidget(tooltip, w4);

  return w4;
}

/********************************************************************************/
/*                                                                              */
/*      Dialog setup                                                            */
/*                                                                              */
/********************************************************************************/

Future<void> displayDialog(
  BuildContext context,
  String title,
  String description,
) async {
  return showDialog<void>(
    context: context,
    builder: (BuildContext context) {
      return AlertDialog(
        title: Text(title),
        content: description.isNotEmpty
            ? Text(description, maxLines: 10)
            : null,
        actions: <Widget>[
          TextButton(
            child: const Text("OK"),
            onPressed: () {
              Navigator.of(context).pop();
            },
          ),
        ],
      );
    },
  );
}

Future<bool> getValidation(
  BuildContext context,
  String title, [
  String description = "",
]) async {
  bool? sts = await showDialog<bool>(
    context: context,
    builder: (BuildContext context) {
      return AlertDialog(
        title: Text(title),
        content: description.isNotEmpty
            ? Text(description, maxLines: 10)
            : null,
        actions: <Widget>[
          TextButton(
            child: const Text("YES"),
            onPressed: () {
              Navigator.pop(context, true);
            },
          ),
          TextButton(
            child: const Text("NO"),
            onPressed: () {
              Navigator.pop(context, false);
            },
          ),
        ],
      );
    },
  );
  return (sts ?? false);
}

Future<bool> getValidationOld(
  BuildContext context,
  String title,
) async {
  bool? sts = await showDialog<bool>(
    context: context,
    builder: (BuildContext context) {
      return SimpleDialog(
        title: Text(title),
        children: <Widget>[
          SimpleDialogOption(
            onPressed: () {
              Navigator.pop(context, true);
            },
            child: const Text("Yes"),
          ),
          SimpleDialogOption(
            onPressed: () {
              Navigator.pop(context, false);
            },
            child: const Text("No"),
          ),
        ],
      );
    },
  );
  if (sts != null) return sts;
  return false;
}

PreferredSizeWidget appBar(String title) {
  return AppBar(
    title: Text(
      title,
      style: const TextStyle(
        fontWeight: FontWeight.bold,
        color: Colors.black,
      ),
    ),
  );
}

Widget circularProgressIndicator() {
  return const Center(child: CircularProgressIndicator());
}

/********************************************************************************/
/*                                                                              */
/*      Top level pages                                                         */
/*                                                                              */
/********************************************************************************/

Widget topLevelPage(
  BuildContext context,
  Widget child, [
  bool scrollable = false,
]) {
  return LayoutBuilder(
    builder: (BuildContext context, BoxConstraints cnst) {
      return _topLevelPageBuilder(context, cnst, child, scrollable);
    },
  );
}

Widget _topLevelPageBuilder(
  BuildContext context,
  BoxConstraints constraints,
  Widget child,
  bool scrollable,
) {
  BoxConstraints bc = BoxConstraints(minWidth: constraints.maxWidth);
  if (scrollable) {
    bc = BoxConstraints(
      minWidth: constraints.maxWidth,
      maxHeight: MediaQuery.of(context).size.height * 0.8,
      // maxHeight: 400,
    );
  }
  return Container(
    decoration: BoxDecoration(
      border: Border.all(width: 8, color: laf.topLevelBackground),
      image: const DecorationImage(
        image: AssetImage(laf.topLevelImage),
        fit: BoxFit.fitWidth,
        opacity: 0.05,
      ),
    ),
    child: SingleChildScrollView(
      child: ConstrainedBox(constraints: bc, child: child),
    ),
  );
}

Widget boxWidgets(List<Widget> wlist, {double width = 8}) {
  return Container(
    decoration: BoxDecoration(
      border: Border.all(width: width, color: laf.topLevelBackground),
    ),
    child: Column(
      mainAxisAlignment: MainAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: wlist,
    ),
  );
}

Widget topLevelNSPage(BuildContext context, Widget child) {
  return Container(
    decoration: BoxDecoration(
      border: Border.all(width: 8, color: laf.topLevelBackground),
      image: const DecorationImage(
        image: AssetImage(laf.topLevelImage),
        fit: BoxFit.fitWidth,
        opacity: 0.05,
      ),
    ),
    child: child,
  );
}

/********************************************************************************/
/*                                                                              */
/*      Utility methods                                                         */
/*                                                                              */
/********************************************************************************/

ThemeData getTheme() {
  return ThemeData(
    colorScheme: ColorScheme.fromSeed(seedColor: laf.themeSeedColor),
  );
}

InputDecoration getDecoration({
  String? hint,
  String? label,
  double vPadding = 0.0,
  double hPadding = 4.0,
  String? error,
  Widget? suffixIcon,
}) {
  hint ??= label;
  label ??= hint;
  return InputDecoration(
    hintText: hint,
    labelText: label,
    labelStyle: getLabelStyle(),
    hoverColor: laf.decorationHoverColor,
    focusedBorder: const OutlineInputBorder(
      borderSide: BorderSide(
        width: 4,
        color: laf.decorationBorderColor,
      ),
    ),
    border: const OutlineInputBorder(
      borderSide: BorderSide(width: 2, color: laf.decorationInputColor),
    ),
    contentPadding: EdgeInsets.symmetric(
      horizontal: hPadding,
      vertical: vPadding,
    ),
    suffixIcon: suffixIcon,
  );
}

TextStyle getLabelStyle() {
  return const TextStyle(
    color: laf.labelColor,
    fontWeight: FontWeight.bold,
  );
}

Widget getPadding(double size) {
  return Padding(padding: EdgeInsets.all(size));
}

Widget getTopLevelLogo(BuildContext context) {
  return SizedBox(
    width: MediaQuery.of(context).size.width * 0.3,
    height: MediaQuery.of(context).size.height * 0.25,
    child: Center(
      child: Image.asset(laf.topLevelImage, fit: BoxFit.contain),
    ),
  );
}

// end of widgets.dart
