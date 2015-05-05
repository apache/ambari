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

  isEditController: function () {
    return this.get('content.controllerName') == 'widgetEditController';
  }.property('content.controllerName'),

  /**
   * @type {string}
   */
  widgetName: '',

  /**
   * @type {string}
   */
  widgetAuthor: '',

  /**
   * @type {boolean}
   */
  isSharedChecked: false,

  /**
   * @type {boolean}
   */
  isSharedCheckboxDisabled: false,

  /**
   * @type {string}
   */
  widgetScope: function () {
    return this.get('isSharedChecked')? 'Cluster': 'User';
  }.property('isSharedChecked'),

  showConfirmationOnSharing: function () {
    var self = this;
    if(this.get('isSharedChecked')) {
      var bodyMessage = Em.Object.create({
        confirmMsg: Em.I18n.t('dashboard.widgets.browser.action.share.confirmation'),
        confirmButton: Em.I18n.t('dashboard.widgets.browser.action.share')
      });
      return App.showConfirmationFeedBackPopup(function (query) {
        self.set('isSharedChecked', true);
      }, bodyMessage, function (query) {
        self.set('isSharedChecked', false);
      });
    }
  },

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
    this.set('widgetAuthor', this.get('content.widgetAuthor'));
    this.set('widgetName', this.get('content.widgetName'));
    this.set('widgetDescription', this.get('content.widgetDescription'));
    this.set('isSharedChecked', this.get('content.widgetScope') == 'CLUSTER');
    // on editing, dont allow changing from shared scope to unshare
    this.set('isSharedCheckboxDisabled', this.get('content.widgetScope') == 'CLUSTER' &&
      this.get('isEditController'));
    this.addObserver('isSharedChecked', this, this.showConfirmationOnSharing);
  },

  isSubmitDisabled: function () {
    return !(this.get('widgetName').trim());
  }.property('widgetName'),

  /**
   * collect all needed data to create new widget
   * @returns {{WidgetInfo: {cluster_name: *, widget_name: *, widget_type: *, description: *, scope: string, metrics: *, values: *, properties: *}}}
   */
  collectWidgetData: function () {
    return {
      WidgetInfo: {
        widget_name: this.get('widgetName'),
        widget_type: this.get('content.widgetType'),
        description: this.get('widgetDescription') || "",
        scope: this.get('widgetScope').toUpperCase(),
        author: this.get('widgetAuthor'),
        "metrics": this.get('widgetMetrics').map(function (metric) {
          return {
            "name": metric.name,
            "service_name": metric.serviceName,
            "component_name": metric.componentName,
            "metric_path": metric.metricPath,
            "host_component_criteria": metric.hostComponentCriteria
          }
        }),
        values: this.get('widgetValues').map(function (value) {
          delete value.computedValue;
          return value;
        }),
        properties: this.get('widgetProperties')
      }
    };
  },

  cancel: function () {
    var controller = App.router.get('widgetEditController');
    controller.cancel();
  },

  complete: function () {
    App.router.send('complete', this.collectWidgetData());
    var editController = App.router.get('widgetEditController');
    editController.finish();
    editController.get('popup').hide();
    var serviceName = editController.get('content.widgetService');
    var service = App.Service.find().findProperty('serviceName', serviceName);
    App.router.transitionTo('main.services.service', service);
  }
});
