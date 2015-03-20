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

App.TimeIntervalSpinnerView = App.ConfigWidgetView.extend({
  templateName: require('templates/common/configs/widgets/time_interval_spinner'),
  classNames: ['spinner-input-widget'],

  /**
   * @property isValid
   * @type {Boolean}
   */
  isValid: true,

  /**
   * @property disabled
   * @type {Boolean}
   */
  disabled: false,

  /**
   * @property valueIsChanged
   * @type {Boolean}
   */
  valueIsChanged: false,

  /**
   * Default property value in widget format.
   *
   * @property defaultValue
   * @type {Object[]}
   */
  defaultValue: null,

  /**
   * Maximum property value in widget format.
   *
   * @property maxValue
   * @type {Object[]}
   */
  maxValue: null,

  /**
   * Minimum property value in widget format.
   *
   * @property minValue
   * @type {Object[]}
   */
  minValue: null,

  /**
   * Map with units lookup used for convertation.
   *
   * @property timeConvertMap
   * @type {Object}
   */
  timeConvertMap: {
    milliseconds: [],
    seconds: [1000],
    minutes: [60, 1000],
    hours: [60, 60, 1000],
    days: [24, 60, 60, 1000]
  },

  /**
   * Map with maximum value per unit.
   *
   * @property timeMaxValueOverflow
   * @type {Object}
   */
  timeMaxValueOverflow: {
    milliseconds: 999,
    seconds: 59,
    minutes: 59,
    hours: 23,
    days: 365
  },

  didInsertElement: function () {
    this.prepareContent();
    this._super();
  },

  /**
   * Content setter.
   * Affects to view attributes:
   *  @see propertyUnit
   *  @see defaultValue
   *  @see minValue
   *  @see maxValue
   *       content
   */
  prepareContent: function() {
    var self = this;
    var property = this.get('config');
    var propertyUnit = property.get('stackConfigProperty.valueAttributes').unit;

    Em.run.once(function() {
      self.set('propertyUnit', propertyUnit);
      self.set('minValue', self.generateWidgetValue(property.get('stackConfigProperty.valueAttributes.minimum')));
      self.set('maxValue', self.generateWidgetValue(property.get('stackConfigProperty.valueAttributes.maximum')));
      self.set('content', self.generateWidgetValue(property.get('value')));
    });
  },

  /**
   * Generate formatted value for widget.
   *
   * @param {String|Number} value
   * @returns {Object[]}
   */
  generateWidgetValue: function(value) {
    var property = this.get('config');
    var widgetUnits = property.get('stackConfigProperty.widget.units.firstObject.unit');
    var propertyUnit = property.get('stackConfigProperty.valueAttributes').unit;
    return this.convertToWidgetUnits(value, propertyUnit, widgetUnits);
  },
  /**
   * Convert property value to widget format units.
   *
   * @param {String|Number} input - time to convert
   * @param {String} inputUnitType - type of input value e.g. "milliseconds"
   * @param {String|String[]} desiredUnits - units to convert input value e.g. ['days', 'hours', 'minutes']
   *   or 'days,hours,minutes'
   * @return {Object[]} - converted values according to desiredUnits order. Returns object
   *   contains unit, value, label, minValue, maxValue, invertOnOverflow attributes according to desiredUnits array
   *   For example:
   *   <code>
   *     [
   *       {unit: 'days', label: 'Days', value: 2, minValue: 0, maxValue: 23},
   *       {unit: 'hours', label: 'Hours', value: 3, minValue: 0, maxValue: 59}
   *     ]
   *   </code>
   */
  convertToWidgetUnits: function(input, inputUnitType, desiredUnits) {
    var self = this;
    var time = parseInt(input);
    var msUnitMap = this.generateUnitsTable(desiredUnits, inputUnitType);
    if (typeof desiredUnits == 'string') {
      desiredUnits = desiredUnits.split(',');
    }

    return desiredUnits.map(function(item) {
      var unitMs = msUnitMap[item];
      var unitValue = Math.floor(time/unitMs);
      time = time - unitValue*unitMs;

      return {
        label: Em.I18n.t('common.' + item),
        unit: item,
        value: unitValue,
        minValue: 0,
        maxValue: Em.get(self, 'timeMaxValueOverflow.' + item),
        invertOnOverflow: true
      };
    });
  },

  /**
   * Convert widget value to config property format.
   *
   * @param {Object[]} widgetValue - formatted value for widget
   * @param {String} propertyUnit - config property unit to convert
   * @return {Number}
   */
  convertToPropertyUnit: function(widgetValue, propertyUnit) {
    var widgetUnitNames = widgetValue.mapProperty('unit');
    var msUnitMap = this.generateUnitsTable(widgetUnitNames, propertyUnit);
    return widgetUnitNames.map(function(item) {
      return parseInt(Em.get(widgetValue.findProperty('unit', item), 'value')) * msUnitMap[item];
    }).reduce(Em.sum);
  },

  /**
   * Generate convertion map with specified unit.
   *
   * @param {String|String[]} units - units to convert
   * @param {String} convertationUnit - unit factor name e.g 'milliseconds', 'minutes', 'seconds', etc.
   */
  generateUnitsTable: function(units, convertationUnit) {
    var msUnitMap = $.extend({}, this.get('timeConvertMap'));
    if (typeof units == 'string') {
      units = units.split(',');
    }
    units.forEach(function(unit) {
      var keys = Em.keys(msUnitMap);
      // check the convertion level
      var distance = keys.indexOf(unit) - keys.indexOf(convertationUnit);
      var convertPath = msUnitMap[unit].slice(0, distance);
      // set unit value of the property it always 1
      if (distance === 0) {
        msUnitMap[unit] = 1;
      }
      if (convertPath.length) {
        // reduce convert path values to value
        msUnitMap[unit] = convertPath.reduce(function(p,c) {
          return p * c;
        });
      }
    });

    return msUnitMap;
  },

  /**
   * Subscribe for value changes.
   */
  valueObserver: function() {
    if (!this.get('content')) return;
    var self = this;
    Em.run.once(function() {
      self.checkModified();
      self.checkErrors();
      self.setConfigValue();
    });
  }.observes('content.@each.value'),

  /**
   * Check for property modification.
   */
  checkModified: function() {
    this.set('valueIsChanged', this.convertToPropertyUnit(this.get('content'), this.get('propertyUnit')) != parseInt(this.get('config.defaultValue')));
  },

  /**
   * Check for validation errors like minimum or maximum required value.
   */
  checkErrors: function() {
    var convertedValue = this.convertToPropertyUnit(this.get('content'), this.get('propertyUnit'));
    var errorMessage = false;
    if (convertedValue < parseInt(this.get('config.stackConfigProperty.valueAttributes.minimum'))) {
      errorMessage = Em.I18n.t('number.validate.lessThanMinumum').format(this.dateToText(this.get('minValue')));
    }
    else if (convertedValue > parseInt(this.get('config.stackConfigProperty.valueAttributes.maximum'))) {
      errorMessage = Em.I18n.t('number.validate.moreThanMaximum').format(this.dateToText(this.get('maxValue')));
    }
    this.set('isValid', !errorMessage);
    this.set('errorMessage', errorMessage);
  },

  /**
   * set appropriate attribute for configProperty model
   */
  setConfigValue: function() {
    this.set('config.value', this.convertToPropertyUnit(this.get('content'), this.get('propertyUnit')));
  },

  /**
   * Convert value to readable format using widget value.
   *
   * @param {Object[]} widgetFormatValue - value formatted for widget @see convertToWidgetUnits
   * @return {String}
   */
  dateToText: function(widgetFormatValue) {
    return widgetFormatValue.map(function(item) {
      if (Em.get(item, 'value') > 0) {
        return Em.get(item, 'value') + ' ' + Em.get(item, 'label');
      }
      else {
        return null;
      }
    }).compact().join(' ');
  },

  /**
   * Restore value to default.
   */
  restoreValue: function() {
    this._super();
    this.set('content', this.generateWidgetValue(this.get('config.defaultValue')));
  }
});
