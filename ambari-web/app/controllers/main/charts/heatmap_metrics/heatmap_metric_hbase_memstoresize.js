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
 *
 */
App.MainChartHeatmapHbaseMemStoreSize = App.MainChartHeatmapHbaseMetrics.extend({
  name: Em.I18n.t('charts.heatmap.metrics.HbaseRegionServerMemStoreSize'),
  maximumValue: function() {
    return App.get('isHadoop2Stack') ? 100*1024*1024 : 100;
  }.property('App.isHadoop2Stack'),
  defaultMetric: function() {
    return App.get('isHadoop2Stack') ? 'metrics.hbase.regionserver.memstoreSize' : 'metrics.hbase.regionserver.memstoreSizeMB';
  }.property('App.isHadoop2Stack'),
  units: function() {
    return App.get('isHadoop2Stack') ? 'B' : 'MB';
  }.property('App.isHadoop2Stack'),
  slotDefinitionLabelSuffix: function() {
    return App.get('isHadoop2Stack') ? 'B' : 'MB';
  }.property('App.isHadoop2Stack')
});