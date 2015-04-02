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
var validator = require('utils/validator');
var numericUtils = require('utils/number_utils');

App.WidgetProperty = Ember.Object.extend({

  /**
   * label to be shown for property
   * @type {String}
   */
  label: '',

  /**
   * PORT|METRIC|AGGREGATE
   * @type {String}
   */
  type: '',

  /**
   * config property value
   * @type {*}
   */
  value: null,


  /**
   * input displayType
   * one of 'textFields', 'textArea', 'select' or 'threshold'
   * @type {String}
   */
  displayType: '',


  /**
   * space separated list of css class names to use
   * @type {String}
   */
  classNames: '',

  /**
   * view class according to <code>displayType</code>
   * @type {Em.View}
   */
  viewClass: function () {
    var displayType = this.get('displayType');
    switch (displayType) {
      case 'textField':
        return App.WidgetPropertyTextFieldView;
      case 'threshold':
        return App.WidgetPropertyThresholdView;
      case 'select':
        return App.WidgetPropertySelectView;
      default:
        console.error('Parsing Widget Property: Unable to find viewClass for displayType ', displayType);
    }
  }.property('displayType'),

  /**
   * Define whether property is valid
   * Computed property
   * Should be defined in child class
   * @type {Boolean}
   */
  isValid: function () {
    return true;
  }.property(),

  /**
   * Define whether property is required by user
   * @type {Boolean}
   */
  isRequired: true
});

App.WidgetProperties = {

  WidgetName: App.WidgetProperty.extend({
    name: 'widget_name',
    label: 'Widget Name',
    displayType: 'textField',
    classNames: 'widget-property-text-input'
  }),

  Description: App.WidgetProperty.extend({
    name: 'description',
    label: 'Description',
    displayType: 'textArea',
    classNames: 'widget-property-text-area'
  }),

  Unit: App.WidgetProperty.extend({
    name: 'display-unit',
    label: 'Unit',
    displayType: 'textField',
    classNames: 'widget-property-unit',
    isValid: function () {
      return this.get('isRequired') ? this.get('value') : true;
    }.property('value')
  }),

  GraphType: App.WidgetProperty.extend({
    name: 'graph_type',
    label: 'Graph Type',
    displayType: 'select',
    options: ["LINE", "STACK"]
  }),

  TimeRange: App.WidgetProperty.extend({
    name: 'time_range',
    label: 'Time Range',
    displayType: 'select',
    options: ["Last 1 hour", "Last 2 hours", "Last 4 hours", "Last 12 hours", "Last 24 hours",
    "Last 1 week", "Last 1 month", "Last 1 year"]
  }),


  Threshold: App.WidgetProperty.extend({

    name: 'threshold',
    label: 'Thresholds',

    /**
     * threshold-value
     * @type {string}
     */
    smallValue: '',
    bigValue: '',
    badgeOK: 'OK',
    badgeWarning: 'WARNING',
    badgeCritical: 'CRITICAL',

    displayType: 'threshold',

    classNames: 'widget-property-threshold',

    apiProperty: [],

    init: function () {
      this._super();
    },

    /**
     * Check if <code>smallValue</code> is valid float number
     * @return {boolean}
     */
    isSmallValueValid: function () {
      var value = this.get('smallValue');
      if (!this.get('isRequired') && !this.get('smallValue') && !this.get('bigValue')) {
        return true;
      } else if (!this.get('smallValue')) {
        return false;
      }
      value = ('' + value).trim();
      return validator.isValidFloat(value) && value > 0;
    }.property('smallValue', 'bigValue'),

    /**
     * Check if <code>bigValue</code> is valid float number
     * @return {boolean}
     */
    isBigValueValid: function () {
      var value = this.get('bigValue');
      if (!this.get('isRequired') && !this.get('smallValue') && !this.get('bigValue')) {
        return true;
      } else if (!this.get('bigValue')) {
        return false;
      }
      value = ('' + value).trim();
      return validator.isValidFloat(value) && value > 0;
    }.property('bigValue', 'smallValue'),

    thresholdError: function () {
      if (this.get('isSmallValueValid') && this.get('isBigValueValid')) {
        return Number(this.get('smallValue')) > Number(this.get('bigValue'));
      } else {
        return false;
      }
    }.property('smallValue', 'bigValue', 'isSmallValueValid', 'isBigValueValid'),

    isValid: function () {
      return this.get('isSmallValueValid') && this.get('isBigValueValid') && (!this.get('thresholdError'));
    }.property( 'isSmallValueValid', 'isBigValueValid', 'thresholdError'),

    /**
     * Define whether warning threshold < critical threshold
     * @type {Boolean}
     */
    errorMsg: function () {
      return this.get('thresholdError') ? "Threshold 1 should be smaller than threshold 2" : null;
    }.property('thresholdError')

  })
};

App.WidgetProperties.Thresholds = {

  PercentageThreshold: App.WidgetProperties.Threshold.extend({

    /**
     * Check if <code>smallValue</code> is valid float number
     * @return {boolean}
     */
    isSmallValueValid: function () {
      var value = this.get('smallValue');
      if (!this.get('isRequired') && !this.get('smallValue') && !this.get('bigValue')) {
        return true;
      } else if (!this.get('smallValue')) {
        return false;
      }
      value = ('' + value).trim();
      return validator.isValidFloat(value) && value > 0 && value <=1;
    }.property('smallValue', 'bigValue'),

    /**
     * Check if <code>bigValue</code> is valid float number
     * @return {boolean}
     */
    isBigValueValid: function () {
      var value = this.get('bigValue');
      if (!this.get('isRequired') && !this.get('smallValue') && !this.get('bigValue')) {
        return true;
      } else if (!this.get('bigValue')) {
        return false;
      }
      value = ('' + value).trim();
      return validator.isValidFloat(value) && value > 0 && value <= 1;
    }.property('bigValue', 'smallValue')

  })
}

