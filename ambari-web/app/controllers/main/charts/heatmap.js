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

App.MainChartsHeatmapController = Em.Controller.extend({
  name: 'mainChartsHeatmapController',
  modelRacks: App.Rack.find(),
  racks: function () {
    var racks = [];
    this.get('modelRacks').forEach(function (rack) {
      racks.push(Em.Object.create({
        name: rack.get('name'),
        hosts: rack.get('hosts'),
        isLoaded: false
      }));
    });
    return racks;
  }.property('modelRacks.@each.isLoaded'),

  allMetrics: function () {
    var metrics = [
      Em.Object.create({
        label: Em.I18n.t('charts.heatmap.category.host'),
        category: 'host',
        items: [
          App.MainChartHeatmapDiskSpaceUsedMetric.create(),
          App.MainChartHeatmapMemoryUsedMetric.create(),
          App.MainChartHeatmapCpuWaitIOMetric.create()
          /*, App.MainChartHeatmapProcessRunMetric.create()*/
        ]
      })
    ];

    if (App.HDFSService.find().get('length')) {
      metrics.push(
        Em.Object.create({
          label: Em.I18n.t('charts.heatmap.category.hdfs'),
          category: 'hdfs',
          items: [
            App.MainChartHeatmapDFSBytesReadMetric.create(),
            App.MainChartHeatmapDFSBytesWrittenMetric.create(),
            App.MainChartHeatmapDFSGCTimeMillisMetric.create(),
            App.MainChartHeatmapDFSMemHeapUsedMetric.create()
          ]
        })
      );
    }

    if (App.MapReduceService.find().get('length')) {
      metrics.push(
        Em.Object.create({
          label: Em.I18n.t('charts.heatmap.category.mapreduce'),
          category: 'mapreduce',
          items: [
            App.MainChartHeatmapMapreduceMapsRunningMetric.create(),
            App.MainChartHeatmapMapreduceReducesRunningMetric.create(),
            App.MainChartHeatmapMapreduceGCTimeMillisMetric.create(),
            App.MainChartHeatmapMapreduceMemHeapUsedMetric.create()
          ]
        })
      );
    }

    if (App.YARNService.find().get('length')) {
      metrics.push(
        Em.Object.create({
          label: Em.I18n.t('charts.heatmap.category.yarn'),
          category: 'yarn',
          items: [
            App.MainChartHeatmapYarnGCTimeMillisMetric.create(),
            App.MainChartHeatmapYarnMemHeapUsedMetric.create(),
            App.MainChartHeatmapYarnResourceUsedMetric.create()
          ]
        })
      );
    }

    if (App.HBaseService.find().get('length')) {
      metrics.push(
        Em.Object.create({
          label: Em.I18n.t('charts.heatmap.category.hbase'),
          category: 'hbase',
          items: [
            App.MainChartHeatmapHbaseReadReqCount.create(),
            App.MainChartHeatmapHbaseWriteReqCount.create(),
            App.MainChartHeatmapHbaseCompactionQueueSize.create(),
            App.MainChartHeatmapHbaseRegions.create(),
            App.MainChartHeatmapHbaseMemStoreSize.create()
          ]
        })
      );
    }
    return metrics;
  }.property(),

  selectedMetric: null,

  inputMaximum: '',

  validation: function () {
    if (this.get('selectedMetric')) {
      if (/^\d+$/.test(this.get('inputMaximum'))) {
        $('#inputMaximum').removeClass('error');
        this.set('selectedMetric.maximumValue', this.get('inputMaximum'));
      } else {
        $('#inputMaximum').addClass('error');
      }
    }
  }.observes('inputMaximum'),

  showHeatMapMetric: function (event) {
    var metricItem = event.context;
    if (metricItem) {
      this.set('selectedMetric', metricItem);
    }
  },

  hostToSlotMap: function () {
    return this.get('selectedMetric.hostToSlotMap');
  }.property('selectedMetric.hostToSlotMap'),

  loadMetrics: function () {
    var selectedMetric = this.get('selectedMetric');
    var hostNames = [];

    if (selectedMetric && this.get('racks').everyProperty('isLoaded', true)) {
      this.get('racks').forEach(function (rack) {
        hostNames = hostNames.concat(rack.hosts.mapProperty('hostName'));
      });
      selectedMetric.refreshHostSlots(hostNames);
    }
    this.set('inputMaximum', this.get('selectedMetric.maximumValue'));
  }.observes('selectedMetric'),

  /**
   * return class name for to be used for containing each rack.
   *
   * @this App.MainChartsHeatmapController
   */
  rackClass: function () {
    var rackCount = this.get('racks.length');
    if (rackCount < 2) {
      return "span12";
    } else if (rackCount == 2) {
      return "span6";
    } else {
      return "span4";
    }
  }.property('racks.length')
});
