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
App.MainServiceInfoHeatmapController = App.MainChartsHeatmapController.extend({
  name: 'mainServiceInfoHeatmapController',
  allMetrics: function () {
    var metrics = [];
    var serviceName = this.get('content.serviceName');
    switch (serviceName) {
      case 'HDFS':
        metrics.pushObjects([
          App.MainChartHeatmapDFSBytesReadMetric.create(),
          App.MainChartHeatmapDFSBytesWrittenMetric.create(),
          App.MainChartHeatmapDFSGCTimeMillisMetric.create(),
          App.MainChartHeatmapDFSMemHeapUsedMetric.create()
        ]);
        break;
      case 'YARN':
        metrics.pushObjects([
          App.MainChartHeatmapYarnGCTimeMillisMetric.create(),
          App.MainChartHeatmapYarnMemHeapUsedMetric.create(),
          App.MainChartHeatmapYarnResourceUsedMetric.create()
        ]);
        break;
      case 'HBASE':
        metrics.pushObjects([
          App.MainChartHeatmapHbaseReadReqCount.create(),
          App.MainChartHeatmapHbaseWriteReqCount.create(),
          App.MainChartHeatmapHbaseCompactionQueueSize.create(),
          App.MainChartHeatmapHbaseRegions.create(),
          App.MainChartHeatmapHbaseMemStoreSize.create()
        ]);
        break;
    }
    return metrics;
  }.property('content.serviceName')
});