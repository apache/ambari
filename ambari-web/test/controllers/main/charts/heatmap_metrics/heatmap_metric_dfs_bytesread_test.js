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
require('messages');
require('controllers/main/charts/heatmap_metrics/heatmap_metric');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_dfs');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_dfs_bytesread');

describe('App.MainChartHeatmapDFSBytesReadMetric', function () {

  var tests = [
    {i: 0, e: 0},
    {i: 0.5 * 1024* 1024, e: 0.5},
    {i: 1024* 1024, e: 1},
    {i: 10.5 * 1024 * 1024,e: 10.5}
  ];

  describe('#transformValue()', function() {
    var mainChartHeatmapDFSBytesReadMetric = App.MainChartHeatmapDFSBytesReadMetric.create();

    tests.forEach(function(test) {
      it(test.i + ' bytes to ' + test.e + ' MB', function() {
        var r = mainChartHeatmapDFSBytesReadMetric.transformValue(test.i);
        expect(r).to.eql(test.e);
      });
    });

  });

});
