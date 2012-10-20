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

App.MainChartsHorizonChartView = App.ChartView.extend({
  containerIdPrefix:'chartHistoricalMetrick',
  classNames:['chart'],
  chartOpened:0,
  chartDrawn:false,
  data:[],
  healthStatusClass:function () {
    return this.get('host.healthStatus') + ' ' + 'health';
  }.property('host.healthStatus'),

  templateName:require('templates/main/charts/horizon/chart'),

  chartClass:function () {
    return this.get('chartOpened') ? "display" : "noDisplay";
  }.property('chartOpened'),

  showChartText:function () {
    return this.t("charts.horizon.chart." + (this.get('chartOpened') ? "hideText" : "showText"));
  }.property('chartOpened'),

  chartContainerSelector:function () {
    return "#" + this.get('chartContainerId');
  }.property('chartContainerId'),

  chartContainerId:function () {
    return this.get('containerIdPrefix') + this.get('host.id');
  }.property('host.id'),

  getNodeChartBlock:function () {
    return $('#' + this.get('elementId') + ' div.chartBlock');
  },

  drawChart:function () {
    this._super();
  }.observes('dataLoaded'),

  loadHorizonInfo:function () {
    var hostInfo = App.HostInfo.create({});
    var data = hostInfo.get('horizonData');
    this.set('data', data);
    this.set('dataLoaded', Math.random()*10);
  },

  usedMetrics: function(){
    var thisW = this;
    var attributes = [];
    $.each(this.get('nodeAttributes'), function(){
      attributes.push(thisW.t('metric.'+this));
    });

    return attributes;

  }.property('nodeAttributes'),

  nodeAttributes: function(){

    console.warn("node attributes:", App.router.get('mainChartsController.metricWidget.chosenMetrics'));

    return App.router.get('mainChartsController.metricWidget.chosenMetrics');
  }.property('App.router.mainChartsController.metricWidget.chosenMetrics'),

  toggleChart:function () {
    var thisChart = this;
    var host = this.get('host');
    if (!this.get('chartOpened')) { // if chart will be opened
      if (!this.get('chartDrawn')) {
        this.drawPlot(); // parent method
        this.set('chartDrawn', 1);
      }

      this.loadHorizonInfo();
      this.addObserver('nodeAttributes', thisChart, 'drawPlot');
      this.addObserver('nodeAttributes', thisChart, 'loadHorizonInfo');
    } else { // if chart will be closed
      this.removeObserver('nodeAttributes', thisChart, 'drawPlot');
      this.removeObserver('nodeAttributes', thisChart, 'loadHorizonInfo');
    }

    this.set('chartOpened', 1 - this.get('chartOpened'));
  }
});