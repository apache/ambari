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
require('controllers/main/charts/heatmap_metrics/heatmap_metric');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_mapreduce');

describe('App.MainChartHeatmapMapreduceMetrics', function () {

  describe('#metricMapper()', function () {
    var mainChartHeatmapMapreduceMetrics = App.MainChartHeatmapMapreduceMetrics.create();

    it('launch metricMapperWithTransform() method', function () {
      sinon.stub(mainChartHeatmapMapreduceMetrics, 'metricMapperWithTransform', Em.K);
      mainChartHeatmapMapreduceMetrics.set('defaultMetric', 'metric1');
      mainChartHeatmapMapreduceMetrics.set('transformValue', 'value1');

      mainChartHeatmapMapreduceMetrics.metricMapper({'json': 'json'});
      expect(mainChartHeatmapMapreduceMetrics.metricMapperWithTransform.calledWith({'json': 'json'}, 'metric1', 'value1')).to.be.true;
      mainChartHeatmapMapreduceMetrics.metricMapperWithTransform.restore();
    });
  });
});
