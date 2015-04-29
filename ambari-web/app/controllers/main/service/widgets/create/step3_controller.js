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

App.WidgetWizardStep3Controller = Em.Controller.extend({
  name: "widgetWizardStep3Controller",

  /**
   * @type {Array}
   */
  scopes: [
    Em.Object.create({
      name: 'User',
      checked: false
    }),
    Em.Object.create({
      name: 'Cluster',
      checked: false
    })
  ],

  /**
   * @type {string}
   */
  widgetName: '',

  /**
   * @type {string}
   */
  widgetAuthor: '',

  /**
   * @type {string}
   */
  widgetScope: function () {
    return this.get('scopes').findProperty('checked');
  }.property('scopes.@each.checked'),

  /**
   * @type {string}
   */
  widgetDescription: '',

  /**
   * actual values of properties in API format
   * @type {object}
   */
  widgetProperties: {},

  /**
   * @type {Array}
   */
  widgetValues: [],

  /**
   * @type {Array}
   */
  widgetMetrics: [],

  /**
   * restore widget data set on 2nd step
   */
  initPreviewData: function () {
    this.set('widgetProperties', this.get('content.widgetProperties'));
    this.set('widgetValues', this.get('content.widgetValues'));
    this.set('widgetMetrics', this.get('content.widgetMetrics'));
    this.set('widgetAuthor', App.router.get('loginName'));
    this.set('widgetName', this.get('content.widgetName'));
    this.set('widgetDescription', this.get('content.widgetDescription'));
    this.get('scopes').forEach(function (scope) {
      scope.set('checked', scope.get('name').toUpperCase() == this.get('content.widgetScope'));
    }, this);
    //if no scope selected, choose User by default
    if (!this.get('scopes').someProperty('checked')) {
      this.get('scopes').findProperty('name', 'User').set('checked', true);
    }
  },

  //TODO: Following computed property needs to be implemented. Next button should be enabled when there is no validation error and all required fields are filled
  isSubmitDisabled: function () {
    return !(this.get('widgetName') && this.get('widgetDescription'));
  }.property('widgetName', 'widgetDescription'),

  /**
   * collect all needed data to create new widget
   * @returns {{WidgetInfo: {cluster_name: *, widget_name: *, widget_type: *, description: *, scope: string, metrics: *, values: *, properties: *}}}
   */
  collectWidgetData: function () {
    return {
      WidgetInfo: {
        widget_name: this.get('widgetName'),
        widget_type: this.get('content.widgetType'),
        description: this.get('widgetDescription'),
        scope: this.get('widgetScope.name').toUpperCase(),
        "metrics": this.get('widgetMetrics').map(function (metric) {
          return {
            "name": metric.name,
            "service_name": metric.serviceName,
            "component_name": metric.componentName,
            "metric_path": metric.metricPath,
            "host_component_criteria": metric.hostComponentCriteria
          }
        }),
        values: this.get('widgetValues'),
        properties: this.get('widgetProperties')
      }
    };
  },

  complete: function () {
    App.router.send('complete', this.collectWidgetData());
  }
});
