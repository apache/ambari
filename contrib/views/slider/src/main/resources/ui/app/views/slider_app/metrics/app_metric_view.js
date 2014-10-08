/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * @class
 *
 * This is a view for showing cluster CPU metrics
 *
 * @extends App.ChartView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.AppMetricView = App.ChartView.extend(App.RunPeriodically, {

  app: null,

  metricName: null,

  id: function () {
    return 'graph_' + this.get('app.id') + this.get('metricName');
  }.property('app.id', 'metricName'),

  title: function () {
    return this.get('metricName').humanize();
  }.property('metricName'),

  yAxisFormatter: App.ChartView.DefaultFormatter,

  renderer: 'line',

  ajaxIndex: 'metrics',

  getDataForAjaxRequest: function () {
    return {
      id: this.get('app.id'),
      metric: this.get('metricName')
    };
  },

  transformToSeries: function (jsonData) {
    var self = this,
      seriesArray = [],
      metricName = this.get('metricName'),
      metrics = Ember.get(jsonData, 'metrics');
    if (!Ember.isNone(metrics)) {
      Ember.keys(metrics).forEach(function () {
        var seriesData = metrics[metricName];
        if (seriesData) {
          var s = self.transformData(seriesData, metricName);
          seriesArray.push(s);
        }
      });
    }
    this.get('parentView.graphs').findBy('metricName', metricName).set('dataExists', !!seriesArray.length);
    return seriesArray;
  },

  loadDataErrorCallback: function() {
    this.set('isReady', true);
    this.get('parentView.graphs').findBy('metricName', this.get('metricName')).set('dataExists', false);
  }

});