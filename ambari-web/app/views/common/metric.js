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

/**
 * use: {{view App.MetricFilteringWidget controllerBinding="App.router.mainChartsController"}}
 * set controller.metric field with metric value
 * widget assign itself to controller like metricWidget (controller.get('metricWidget'))
 * @type {*}
 */
App.MetricFilteringWidget = Em.View.extend({
  metrics:[],
  itemView:Em.View.extend({
    tagName:'li',
    classNameBindings:['disabled'],
    disabled:function () {
      return this.get('isActive') ? "disabled" : false;
    }.property('isActive'),
    isActive:function () {
      return this.get('metric.value') == this.get('widget.controller.metric');
    }.property('widget.controller.metric'),
    template:Em.Handlebars.compile('<a {{action activate view.metric.value target="view.widget" }}>{{view.metric.label}}</a>')
  }),

  metricsConfig:[
    { label:Em.I18n.t('metric.default'), value:null},
    { label:Em.I18n.t('metric.cpu'), value:'cpu'},
    { label:Em.I18n.t('metric.memory'), value:'memory'},
    { label:Em.I18n.t('metric.network'), value:'network'},
    { label:Em.I18n.t('metric.io'), value:'io'}
  ],

  allMetrics:function () {
    var values = [];
    $.each(this.get('metricsConfig'), function () {
      if (this.value) {
        values.push(this.value);
      }
    });
    return values;
  }.property(),

  init:function () {
    this._super();
    var thisW = this;
    var controller = this.get('controller');
    controller.set('metricWidget', thisW);

    this.get('itemView').reopen({
      widget:thisW
    });

    // preparing metric objects
    this.get('metricsConfig').forEach(function (config) {
      config['widget'] = thisW;
      thisW.get('metrics').push(Em.Object.create(config))
    });

  },

  /**
   * write active metric to binded controller
   * @param event
   */
  activate:function (event) {
    var selected = event.context;
    var controller = this.get('controller');
    controller.set('metric', selected);
  },

  templateName:require('templates/common/metric')
})