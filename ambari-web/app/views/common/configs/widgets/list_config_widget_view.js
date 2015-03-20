/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');

var numberUtils = require('utils/number_utils');

/**
 * Template for list-option
 * @type {Em.object}
 */
var configOption = Em.Object.extend({
  label: '',
  value: '',
  description: '',
  isSelected: false,
  isDisabled: false,
  order: 0
});

/**
 * Config Widget for List
 * Usage:
 *  <code>{{view App.ListConfigWidgetView configBinding="someObject"}}</code>
 * @type {App.ConfigWidgetView}
 */
App.ListConfigWidgetView = App.ConfigWidgetView.extend({

  /**
   * Counter used to determine order of options selection (<code>order<code>-field in the <code>configOption</code>)
   * Greater number - later selection
   * @type {number}
   */
  orderCounter: 1,

  /**
   * Maximum length of the <code>displayVal</code>
   * If its length is greater, it will cut to current value and ' ...' will be added to the end
   * @type {number}
   */
  maxDisplayValLength: 45,

  /**
   * <code>options</code> where <code>isSelected</code> is true
   * @type {configOption[]}
   */
  val: [],

  /**
   * List of options for <code>config.value</code>
   * @type {configOption[]}
   */
  options: [],

  /**
   * String with selected options labels separated with ', '
   * If result string is too long (@see maxDisplayValLength) it's cut and ' ...' is added to the end
   * If nothing is selected, default placeholder is used
   * @type {string}
   */
  displayVal: function () {
    var v = this.get('val').sortProperty('order').mapProperty('label');
    if (v.length > 0) {
      var output = v.join(', '),
        maxDisplayValLength = this.get('maxDisplayValLength');
      if (output.length > maxDisplayValLength - 3) {
        return output.substring(0, maxDisplayValLength - 3) + ' ...';
      }
      return output;
    }
    return Em.I18n.t('services.service.widgets.list-widget.nothingSelected');
  }.property('val.[]'),

  /**
   * Config-object bound on the template
   * @type {App.StackConfigProperty}
   */
  config: null,

  /**
   * Maximum number of options allowed to select (based on <code>config.valueAttributes.selection_cardinality</code>)
   * @type {number}
   */
  allowedToSelect: 1,

  templateName: require('templates/common/configs/widgets/list_config_widget'),

  willInsertElement: function () {
    this._super();
    this.parseCardinality();
    this.calculateOptions();
    this.calculateInitVal();
  },

  didInsertElement: function () {
    this._super();
    this.addObserver('options.@each.isSelected', this, this.calculateVal);
    this.addObserver('options.@each.isSelected', this, this.checkSelectedItemsCount);
    this.calculateVal();
    this.checkSelectedItemsCount();
    Em.run.next(function () {
      App.tooltip(this.$('[rel="tooltip"]'));
    });
  },

  /**
   * Get list of <code>options</code> basing on <code>config.valueAttributes</code>
   * <code>configOption</code> is used
   * @method calculateOptions
   */
  calculateOptions: function () {
    var valueAttributes = this.get('config.stackConfigProperty.valueAttributes'),
      options = [];
    Em.assert('valueAttributes `entries`, `entry_label` and `entry_descriptions` should have the same length', valueAttributes.entries.length == valueAttributes.entry_labels.length && valueAttributes.entries.length == valueAttributes.entry_descriptions.length);
    valueAttributes.entries.forEach(function (entryValue, indx) {
      options.pushObject(configOption.create({
        value: entryValue,
        label: valueAttributes.entry_labels[indx],
        description: valueAttributes.entry_descriptions[indx]
      }));
    });
    this.set('options', options);
  },

  /**
   * Get initial value for <code>val</code> using calculated earlier <code>options</code>
   * Used on <code>willInsertElement</code> and when user click on "Undo"-button (to restore default value)
   * @method calculateInitVal
   */
  calculateInitVal: function () {
    var config = this.get('config'),
      options = this.get('options'),
      value = config.get('value'),
      self = this;
    if ('string' === Em.typeOf(value)) {
      value = value.split(',');
    }
    options.invoke('setProperties', {isSelected: false, isDisabled: false});
    var val = value.map(function (v) {
      var option = options.findProperty('value', v.trim());
      Em.assert('option with value `%@` is missing for config `%@`'.fmt(v, config.get('name')), option);
      option.setProperties({
        order: self.get('orderCounter'),
        isSelected: true
      });
      self.incrementProperty('orderCounter');
      return option;
    });
    this.set('val', val);
  },

  /**
   * Get config-value basing on selected <code>options</code> sorted by <code>order</code>-field
   * Triggers on each option select/deselect
   * @method calculateVal
   */
  calculateVal: function () {
    var val = this.get('options').filterProperty('isSelected').sortProperty('order');
    this.set('val', val);
    this.set('config.value', val.mapProperty('value').join(','));
  },

  /**
   * If user already selected maximum of allowed options, disable other options
   * If user deselect some option, all disabled options become enabled
   * Triggers on each option select/deselect
   * @method checkSelectedItemsCount
   */
  checkSelectedItemsCount: function () {
    var allowedToSelect = this.get('allowedToSelect'),
      currentlySelected = this.get('options').filterProperty('isSelected').length,
      selectionDisabled = allowedToSelect <= currentlySelected;
    this.get('options').filterProperty('isSelected', false).setEach('isDisabled', selectionDisabled);
  },

  /**
   * Get maximum number of options allowed to select basing on config cardinality value
   * @method parseCardinality
   */
  parseCardinality: function () {
    var cardinality = numberUtils.getCardinalityValue(this.get('config.stackConfigProperty.valueAttributes.selection_cardinality'), true);
    this.set('allowedToSelect', cardinality);
  },

  /**
   * Option click-handler
   * toggle selection for current option and increment <code>orderCounter</code> for proper options selection order
   * @param {{context: Object}} e
   * @returns {boolean} always returns false to avoid list hiding
   */
  toggleOption: function (e) {
    if (e.context.get('isDisabled')) return false;
    var orderCounter = this.get('orderCounter'),
      option = this.get('options').findProperty('value', e.context.get('value'));
    option.set('order', orderCounter);
    option.toggleProperty('isSelected');
    this.incrementProperty('orderCounter');
    return false;
  },

  /**
   * Restore config value
   * @method restoreValue
   */
  restoreValue: function() {
    this._super();
    this.calculateInitVal();
  },

  /**
   * Just a small checkbox-wrapper with improved click-handler
   * Should call <code>parentView.toggleOption</code>
   * User may click on the checkbox or on the link which wraps it, but action in both cases should be the same (<code>toggleOption</code>)
   * @type {Em.Checkbox}
   */
  checkBoxWithoutAction: Em.Checkbox.extend({
    _updateElementValue: function () {
      var option = this.get('parentView.options').findProperty('value', this.get('value'));
      this.get('parentView').toggleOption({context: option});
    }
  })

});
