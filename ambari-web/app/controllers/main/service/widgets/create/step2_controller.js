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

App.WidgetWizardStep2Controller = Em.Controller.extend({
  name: "widgetWizardStep2Controller",

  widgetProperties: [],
  widgetMetrics: {},
  widgetValues: {},

  //TODO: Following computed property needs to be implemented. Next button should be enabled when there is no validation error and all required fields are filled
  isSubmitDisabled: function () {
    return this.get('widgetProperties').someProperty('isValid', false);
  }.property('widgetProperties.@each.isValid'),

  /*
   * Generate the thresholds, unit, time range.etc object based on the widget type selected in previous step.
   */
  renderProperties: function () {
    var widgetType = this.get('content.widgetType');
    this.set("widgetProperties", {});
    var widgetProperties = App.WidgetType.find().findProperty('name', widgetType).get('properties');
    var properties = [];
    switch (widgetType) {
      case 'GAUGE':
        properties = this.renderGaugeProperties(widgetProperties);
        break;
      case 'NUMBER':
        properties = this.renderNumberProperties(widgetProperties);
        break;
      case 'GRAPH':
        properties = this.renderGraphProperties(widgetProperties);
        break;
      case 'TEMPLATE':
        properties = this.renderTemplateProperties(widgetProperties);
        break;
      default:
        console.error('Incorrect Widget Type: ', widgetType);
    }
    this.set('widgetProperties', properties);
  },

  /**
   * Render properties for gauge-type widget
   * @method renderGaugeProperties
   * @returns {App.WidgetProperties[]}
   */
  renderGaugeProperties: function (widgetProperties) {
    var result = [];
    result = result.concat([
      App.WidgetProperties.Thresholds.PercentageThreshold.create({
        smallValue: '0.7',
        bigValue: '0.9',
        isRequired: true
      })
    ]);
    return result;
  },

  /**
   * Render properties for number-type widget
   * @method renderNumberProperties
   * @returns {App.WidgetProperties[]}
   */
  renderNumberProperties: function (widgetProperties) {
    var result = [];

    result = result.concat([
      App.WidgetProperties.Threshold.create({
        smallValue: '10',
        bigValue: '20',
        isRequired: false
      }),
      App.WidgetProperties.Unit.create({
        value: 'MB',
        isRequired: false
      })
    ]);
    return result;
  },

  /**
   * Render properties for template-type widget
   * @method renderTemplateProperties
   * @returns {App.WidgetProperties[]}
   */
  renderTemplateProperties: function (widgetProperties) {
    var result = [];
    result = result.concat([
      App.WidgetProperties.Unit.create({
        value: 'MB',
        isRequired: false
      })
    ]);
    return result;
  },

  /**
   * Render properties for graph-type widget
   * @method renderGraphProperties
   * @returns {App.WidgetProperties[]}
   */
  renderGraphProperties: function (widgetProperties) {
    var result = [];
    result = result.concat([
      App.WidgetProperties.GraphType.create({
        value: 'LINE',
        isRequired: true
      }),
      App.WidgetProperties.TimeRange.create({
        value: 'Last 1 hour',
        isRequired: true
      }),
      App.WidgetProperties.Unit.create({
        value: 'MB',
        isRequired: false
      })
    ]);
    return result;
  },

  next: function () {
    if (!this.get('isSubmitDisabled')) {
      App.router.send('next');
    }
  }
});

