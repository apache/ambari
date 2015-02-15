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

var App = require('app');

/**
 * @class
 * 
 * This is a view for showing HBase Cluster Requests
 * 
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartServiceMetricsFlume_ChannelSizeMMA = App.ChartLinearTimeView.extend({
  id: "service-metrics-flume-channel-size-mma",
  title: Em.I18n.t('services.service.info.metrics.flume.channelSizeMMA'),
  renderer: 'line',
  ajaxIndex: 'service.metrics.flume.channel_size_for_all',
  yAxisFormatter: App.ChartLinearTimeView.CreateRateFormatter('',
    App.ChartLinearTimeView.DefaultFormatter),

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    var self = this;

    if (Em.get(jsonData, "metrics.flume.flume.CHANNEL.ChannelSize.rate")) {
      for ( var cname in jsonData.metrics.flume.flume.CHANNEL.ChannelSize.rate) {
        if(cname != "sum"){
          var seriesName = Em.I18n.t('services.service.info.metrics.flume.channelType').format(cname);
          var seriesData = jsonData.metrics.flume.flume.CHANNEL.ChannelSize.rate[cname];
          if (seriesData) {
            seriesArray.push(self.transformData(seriesData, seriesName));
          }
        }
      }
    }
    return seriesArray;
  },

  colorForSeries: function (series) {
    if (Em.I18n.t('services.service.info.metrics.flume.channelType').format("avg") == series.name){
      return '#0066b3';
    }else if (Em.I18n.t('services.service.info.metrics.flume.channelType').format("min") == series.name){
      return '#00CC00';
    }else if (Em.I18n.t('services.service.info.metrics.flume.channelType').format("max") == series.name){
      return '#FF8000';
    }
    return null;
  }
});
