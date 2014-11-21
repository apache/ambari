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

App.AlertConfigProperty = Ember.Object.extend({

  /**
   * label to be shown for config property
   * @type {String}
   */
  label: '',

  /**
   * config property value
   * @type {*}
   */
  value: null,

  /**
   * property value cache to realise undo function
   * @type {*}
   */
  previousValue: null,

  /**
   * define either input is disabled or enabled
   * @type {Boolean}
   */
  isDisabled: false,

  /**
   * options that Select list will have
   * @type {Array}
   */
  options: [],

  /**
   * input displayType
   * one of 'textFields', 'textArea', 'select' or 'threshold'
   * @type {String}
   */
  displayType: '',

  /**
   * unit to be shown with value
   * @type {String}
   */
  unit: null,

  /**
   * space separated list of css class names to use
   * @type {String}
   */
  classNames: '',

  /**
   * name or names of properties related to config
   * may be either string for one property or array of strings for multiple properties
   * if this property is array, then <code>apiFormattedValue</code> should also be an array
   * example: <code>apiProperty[0]</code> relates to <code>apiFormattedValue[0]</code>
   * @type {String|Array}
   */
  apiProperty: '',

  /**
   * for some metrics properties may be set true or false
   * depending on what property is related to (JMX or Ganglia)
   */
  isJMXMetric: null,

  /**
   * value converted to appropriate format for sending to server
   * should be computed property
   * should be defined in child class
   * @type {*}
   */
  apiFormattedValue: function () {
    return this.get('value');
  }.property('value'),

  /**
   * define if property was changed by user
   * @type {Boolean}
   */
  wasChanged: function () {
    return this.get('previousValue') !== null && this.get('value') !== this.get('previousValue');
  }.property('value', 'previousValue'),

  /**
   * view class according to <code>displayType</code>
   * @type {Em.View}
   */
  viewClass: function () {
    var displayType = this.get('displayType');
    switch (displayType) {
      case 'textField':
        return App.AlertConfigTextFieldView;
      case 'textArea':
        return App.AlertConfigTextAreaView;
      case 'select':
        return App.AlertConfigSelectView;
      case 'threshold':
        return App.AlertConfigThresholdView;
      default:
        console.error('Unable to find viewClass for displayType ', displayType);
    }
  }.property('displayType')

});

App.AlertConfigProperties = {

  AlertName: App.AlertConfigProperty.extend({
    label: 'Alert Name',
    displayType: 'textField',
    classNames: 'alert-text-input',
    apiProperty: 'name'
  }),
  AlertNameSelected: App.AlertConfigProperty.extend({
    label: 'Alert Name',
    displayType: 'select',
    apiProperty: 'name'
  }),
  Service: App.AlertConfigProperty.extend({
    label: 'Service',
    displayType: 'select',
    apiProperty: 'service_name',
    apiFormattedValue: function () {
      return App.StackService.find().findProperty('displayName', this.get('value')).get('serviceName');
    }.property('value')
  }),
  Component: App.AlertConfigProperty.extend({
    label: 'Component',
    displayType: 'select',
    apiProperty: 'component_name',
    apiFormattedValue: function () {
      return App.StackServiceComponent.find().findProperty('displayName', this.get('value')).get('componentName');
    }.property('value')
  }),
  Scope: App.AlertConfigProperty.extend({
    label: 'Scope',
    options: ['Any', 'Host', 'Service'],
    displayType: 'select',
    apiProperty: 'scope',
    apiFormattedValue: function () {
      return this.get('value').toUpperCase();
    }.property('value')
  }),
  Description: App.AlertConfigProperty.extend({
    label: 'Description',
    displayType: 'textArea',
    classNames: 'alert-config-text-area',
    // todo: check value after API will be provided
    apiProperty: 'description'
  }),
  Interval: App.AlertConfigProperty.extend({
    label: 'Interval',
    displayType: 'textField',
    unit: 'Second',
    classNames: 'alert-interval-input',
    apiProperty: 'interval'
  }),
  Thresholds: App.AlertConfigProperty.extend({
    label: 'Thresholds',
    displayType: 'threshold',
    classNames: 'alert-thresholds-input',
    from: '',
    to: '',
    value: '',

    setFromTo: function () {
      this.set('doNotChangeValue', true);
      this.set('from', this.get('value').split('-')[0]);
      this.set('to', this.get('value').split('-')[1]);
      this.set('doNotChangeValue', false);
    }.observes('value'),

    setValue: function () {
      if (!this.get('doNotChangeValue')) {
        this.set('value', this.get('from') + '-' + this.get('to'));
      }
    }.observes('from', 'to'),

    // flag for providing correct from, to and value recomputing
    doNotChangeValue: false,

    apiProperty: [
      'source.reporting.warning.value',
      'source.reporting.critical.value'
    ],
    apiFormattedValue: function () {
      return [
          +this.get('from'),
          +this.get('to')
      ]
    }.property('from', 'to')
  }),
  URI: App.AlertConfigProperty.extend({
    label: 'URI',
    displayType: 'textField',
    classNames: 'alert-text-input',
    apiProperty: 'source.uri'
  }),
  URIExtended: App.AlertConfigProperty.extend({
    label: 'URI',
    displayType: 'textArea',
    classNames: 'alert-config-text-area',
    apiProperty: 'source.uri',
    apiFormattedValue: function () {
      var result = {};
      try {
        result = JSON.parse(this.get('value'));
      } catch (e) {
        console.error('Wrong format of URI');
      }
      return result;
    }.property('value')
  }),
  DefaultPort: App.AlertConfigProperty.extend({
    label: 'Default Port',
    displayType: 'textField',
    classNames: 'alert-port-input',
    apiProperty: 'source.default_port'
  }),
  Path: App.AlertConfigProperty.extend({
    label: 'Path',
    displayType: 'textField',
    classNames: 'alert-text-input',
    apiProperty: 'source.path'
  }),
  Metrics: App.AlertConfigProperty.extend({
    label: 'JMX/Ganglia Metrics',
    displayType: 'textArea',
    classNames: 'alert-config-text-area',
    apiProperty: function () {
      return this.get('isJMXMetric') ? 'source.jmx.property_list' : 'source.ganglia.property_list'
    }.property('isJMXMetric'),
    apiFormattedValue: function () {
      return this.get('value').split(',\n');
    }.property('value')
  }),
  FormatString: App.AlertConfigProperty.extend({
    label: 'Format String',
    displayType: 'textArea',
    classNames: 'alert-config-text-area',
    apiProperty: function () {
      return this.get('isJMXMetric') ? 'source.jmx.value' : 'source.ganglia.value'
    }.property('isJMXMetric')
  })

};