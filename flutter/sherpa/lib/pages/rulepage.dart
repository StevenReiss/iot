/********************************************************************************/
/*                                                                              */
/*              rulepage.dart                                                   */
/*                                                                              */
/*      Page to let the user view and edit a single rule                        */
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
import 'package:sherpa/widgets.dart' as widgets;
import 'package:sherpa/models/catremodel.dart';
import 'conditionpage.dart';
import 'actionpage.dart';

/********************************************************************************/
/*                                                                              */
/*      Widget definitions                                                      */
/*                                                                              */
/********************************************************************************/

class SherpaRuleWidget extends StatefulWidget {
  final CatreRule _forRule;

  const SherpaRuleWidget(this._forRule, {super.key});

  @override
  State<SherpaRuleWidget> createState() => _SherpaRuleWidgetState();
}

class _SherpaRuleWidgetState extends State<SherpaRuleWidget> {
  late CatreRule _forRule;
  late CatreDevice _forDevice;
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _labelControl = TextEditingController();
  final TextEditingController _descControl = TextEditingController();
  bool _isUserDescription = false;

  _SherpaRuleWidgetState();

  @override
  initState() {
    _forRule = widget._forRule;
    _forDevice = _forRule.getDevice();
    super.initState();
    _resetRule();
    _labelControl.addListener(_labelListener);
    _descControl.addListener(_descriptionListener);
  }

  @override
  Widget build(BuildContext context) {
    String ttl = "Rule Editor for ${_forDevice.getName()}";
    Widget ww = Scaffold(
      appBar: AppBar(title: Text(ttl), actions: [
        widgets.topMenuAction(
          [
            widgets.MenuAction(
              'Save Changes',
              _saveRule,
              "Save the changes made to this rule",
            ),
            widgets.MenuAction(
              'Revert rule',
              _revertRule,
              "Undo any changes made to this rule and restore to previous "
                  "saved state",
            ),
          ],
        ),
      ]),
      body: widgets.topLevelPage(
        context,
        Column(
          mainAxisAlignment: MainAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  widgets.fieldSeparator(16),
                  _ruleLabel(),
                  widgets.fieldSeparator(),
                  _ruleDescription(),
                  widgets.fieldSeparator(),
                  _ruleConditions(),
                  widgets.fieldSeparator(),
                  _ruleActions(),
                  widgets.fieldSeparator(),
                  _ruleBottomButtons(),
                ],
              ),
            ),
          ],
        ),
      ),
    );

    Widget w1 = PopScope(
      canPop: true,
      onPopInvokedWithResult: _checkPop,
      child: ww,
    );

    return w1;
  }

  Widget _ruleLabel() {
    return widgets.textFormField(
      hint: "Descriptive label for rule",
      label: "Rule Label",
      validator: _labelValidator,
      controller: _labelControl,
      tooltip: "Provide a short but meaningful label for this rule.",
    );
  }

  Widget _ruleDescription() {
    return widgets.textFormField(
      hint: "Detailed rule description",
      label: "Rule Description",
      controller: _descControl,
      maxLines: 3,
      tooltip: "Provide a detailed description of this rule. "
          "This will be automatically generated, but you may override "
          "that by editing it directly.",
    );
  }

  Widget _ruleConditions() {
    Widget w1 = widgets.listBox(
      "Condition",
      _forRule.getConditions(),
      _conditionBuilder,
      _addCondition,
    );
    return Flexible(child: w1);
  }

  Widget _ruleActions() {
    Widget w1 = widgets.listBox(
      "Action",
      _forRule.getActions(),
      _actionBuilder,
      _addNewAction,
    );
    return Flexible(
      child: w1,
    );
  }

  Widget _ruleBottomButtons() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: <Widget>[
        widgets.submitButton("Validate", _validateRule),
        widgets.submitButton(
          "Accept",
          _saveRule,
          enabled: _isRuleAcceptable(),
        ),
        widgets.submitButton("Cancel", _revertRule),
      ],
    );
  }

  Widget _conditionBuilder(CatreCondition cc) {
    List<widgets.MenuAction> acts = [];
    acts.add(widgets.MenuAction('Edit Condition', () {
      _editCondition(cc);
    }));

    if (_forRule.getConditions().length > 1 && !cc.isTrigger()) {
      acts.add(widgets.MenuAction('Remove Condition', () {
        _removeCondition(cc);
      }));
    }

    Widget w = widgets.itemWithMenu(
      cc.getLabel(),
      acts,
      tooltip: cc.getDescription(),
      onLongPress: () {
        if (cc.getLabel().startsWith("Undefined")) {
          _editCondition(cc);
        } else {
          _showCondition(cc);
        }
      },
      onTap: () => _editCondition(cc),
      onDoubleTap: () => _editCondition(cc),
    );

    Widget w1 = widgets.tooltipWidget(
      "Tap on condition to edit it.  Other "
      "options available from the menu on the left.",
      w,
    );
    return w1;
  }

  Widget _actionBuilder(CatreAction ca) {
    List<widgets.MenuAction> acts = [];

    acts.add(widgets.MenuAction('Edit Action', () {
      _editAction(ca);
    }));

    if (_forRule.getActions().length > 1) {
      acts.add(widgets.MenuAction('Remove Action', () {
        _removeAction(ca);
      }));
    }

    Widget w1 = widgets.itemWithMenu(
      ca.getLabel(),
      acts,
      tooltip: ca.getDescription(),
      onLongPress: () {
        if (ca.getLabel().startsWith("Undefined")) {
          _editAction(ca);
        } else {
          _showAction(ca);
        }
      },
      onDoubleTap: () => _editAction(ca),
      onTap: () => _editAction(ca),
    );

    Widget w2 = widgets.tooltipWidget(
      "Tap on rule to edit it.  Other options available "
      "using the menu on the left",
      w1,
    );
    return w2;
  }

  void _saveRule() async {
    if (_formKey.currentState!.validate()) {
      _updateRuleData();
      await _forRule.addOrEditRule();
      // ensure validation has been run, run it if not
      // ensure validation status is ok
    }

    setState(() {
      _forRule.push();
      Navigator.pop(context);
    });
  }

  void _revertRule() {
    setState(() {
      if (!_forRule.pop()) {
        _forRule.revert();
        _resetRule();
        Navigator.of(context).pop();
      }
    });
  }

  _validateRule() async {
    BuildContext dcontext = context;
    _updateRuleData();
    Map<String, dynamic>? jresp = await _forRule.issueCommand(
      "/rule/validate",
      "RULE",
    );

    if (jresp == null || jresp["STATUS"] == "OK") {
      Map<String, dynamic> valid = jresp?["VALIDATION"];
      List<dynamic>? errors = valid["ERRORS"];
      String text = "";
      if (errors == null || errors.isEmpty) {
        text = "Rule ${_forRule.getName()} is VALID";
      } else {
        text = "Possible issues for ${_forRule.getName()}:\n";
        for (Map<String, dynamic> v in errors) {
          text += "${v['Level']}: ${v['Message']}\n";
        }
      }
      if (dcontext.mounted) {
        await widgets.displayDialog(
          dcontext,
          "Validation for ${_forRule.getName()}",
          text,
        );
      }
    }
  }

  _resetRule() {
    _labelControl.text = _forRule.getLabel();
    _descControl.text = _forRule.getDescription();
    _isUserDescription = _forRule.isUserDescription();
  }

  _checkPop(bool didpop, dynamic result) async {
    await _forRule.addOrEditRule();
  }

  _updateDescription() {
    String d = _forRule.getDescription();
    if (d != _descControl.text) {
      setState(() {
        _descControl.text = d;
        _updateRuleData();
      });
    }
  }

  _updateRuleData() {
    _forRule.setLabel(_labelControl.text);
    _forRule.setName(_labelControl.text);
    _forRule.setDescription(_descControl.text, _isUserDescription);
  }

  String? _labelValidator(String? lbl) {
    if (lbl == null || lbl.isEmpty) {
      return "Rule must have a label";
    }
    return null;
  }

  void _addCondition() async {
    CatreCondition? cond;
    setState(() {
      cond = _forRule.addNewCondition();
    });
    if (cond != null) {
      await _editCondition(cond!);
      _updateDescription();
      setState(() {});
    }
  }

  void _removeCondition(CatreCondition cc) {
    setState(() {
      _forRule.removeCondition(cc);
      _updateDescription();
    });
  }

  Future<void> _editCondition(CatreCondition cc) async {
    await widgets.gotoThen(
        context, SherpaConditionWidget(_forRule, cc));
    _updateDescription();
    setState(() {});
  }

  void _showCondition(CatreCondition cc) {
    widgets.displayDialog(
        context, "Condition Description", cc.getDescription());
  }

  void _addNewAction() async {
    CatreAction? act;
    setState(() {
      act = _forRule.addNewAction(_forDevice);
    });
    if (act != null) {
      await _editAction(act!);
      _updateDescription();
    }
  }

  void _removeAction(CatreAction ca) {
    setState(() {
      _forRule.removeAction(ca);
      _updateDescription();
    });
  }

  Future<void> _editAction(CatreAction ca) async {
    CatreDevice cd = _forRule.getDevice();
    await widgets.gotoThen(
        context, SherpaActionWidget(cd, _forRule, ca));
    _updateDescription();
    setState(() {});
  }

  void _showAction(CatreAction ca) {
    widgets.displayDialog(
        context, "Action Description", ca.getDescription());
  }

  void _labelListener() {
    _updateDescription();
    _updateRuleData();
    setState(() {});
  }

  void _descriptionListener() {
    String d = _descControl.text;
    if (_forRule.getDescription() == d) return;
    if (d.isEmpty) {
      _isUserDescription = false;
      _forRule.setDescription("", false);
      d = _forRule.getDescription();
    } else {
      _isUserDescription = true;
    }
    _forRule.setDescription(d, _isUserDescription);
    _updateDescription();
    setState(() {});
  }

  bool _isRuleAcceptable() {
    if (_labelControl.text.isEmpty) return false;
    if (_labelControl.text.startsWith('Undefined')) return false;
    if (_descControl.text.isEmpty) return false;
    if (_descControl.text.startsWith('Undefined')) return false;
    bool havecond = false;
    for (CatreCondition cc in _forRule.getConditions()) {
      if (!cc.getConditionType().isEmpty()) havecond = true;
    }
    if (!havecond) return false;
    // might want to check other items
    return true;
  }
}
