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
  classNames:['metric-filtering-widget'],
  /**
   * metrics
   */
  metrics:[
    Em.Object.create({ label:Em.I18n.t('metric.default'), value:null}),
    Em.Object.create({ label:Em.I18n.t('metric.cpu'), value:'cpu'}),
    Em.Object.create({ label:Em.I18n.t('metric.memory'), value:'memory'}),
    Em.Object.create({ label:Em.I18n.t('metric.network'), value:'network'}),
    Em.Object.create({ label:Em.I18n.t('metric.io'), value:'io'})
  ],
  /**
   * chosen metric value
   */
  chosenMetric:null,
  chosenMoreMetric: null,

  showMore:0, // toggle more metrics indicator

  /**
   * return array of chosen metrics
   */
  chosenMetrics:function () {
    return this.get('chosenMetric') ? [this.get('chosenMetric')] : this.get('defaultMetrics');
  }.property('chosenMetric'),

  /**
   * metric item view
   */
  itemView:Em.View.extend({
    tagName:'li',
    classNameBindings:['disabled'],
    disabled:function () {
      return this.get('isActive') ? "disabled" : false;
    }.property('isActive'),
    isActive:function () {
      return this.get('metric.value') == this.get('widget.chosenMetric');
    }.property('widget.chosenMetric'),
    template:Em.Handlebars.compile('<a {{action activate view.metric.value target="view.widget" href="true" }}>{{unbound view.metric.label}}</a>')
  }),

  moreItemView: function(){
    return this.get('itemView').extend({});
  }.property(),

  moreMetrics:[
    Em.Object.create({ label:Em.I18n.t('metric.more.cpu'), code:'cpu', items:[] }),
    Em.Object.create({ label:Em.I18n.t('metric.more.memory'), code:'memory',
      items:[
        Em.Object.create({label:Em.I18n.t('metric.more.memory.swapFree'), value:'swap_free'}),
        Em.Object.create({label:Em.I18n.t('metric.more.memory.memCached'), value:'cpu'})
      ]
    })
  ],

  /**
   * return default selected metrics (currently - all)
   */
  defaultMetrics:function () {
    var values = [];
    $.each(this.get('metrics'), function () {
      if (this.value) {
        values.push(this.value);
      }
    });
    return values;
  }.property(),

  bindToController:function () {
    var thisW = this;
    var controller = this.get('controller');
    controller.set('metricWidget', thisW);
  },

  toggleMore:function () {
    this.set('showMore', 1 - this.get('showMore'));
  },

  /**
   * assign this widget to controller, prepare items by metricsConfig
   */
  init:function () {
    this._super();
    this.bindToController();
  },

  /**
   * write active metric to widget
   * @param event
   */
  activate:function (event) {
    this.set('chosenMetric', event.context);
  },

  templateName:require('templates/common/metric')
})