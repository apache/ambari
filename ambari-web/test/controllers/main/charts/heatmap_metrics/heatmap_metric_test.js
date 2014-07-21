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

describe('MainChartHeatmapMetric', function () {

  var mainChartHeatmapMetric = App.MainChartHeatmapMetric.create({});

  describe('#formatLegendNumber', function () {
    var tests = [
      {m:'undefined to undefined',i:undefined,e:undefined},
      {m:'0 to 0',i:0,e:0},
      {m:'1 to 1',i:1,e:1},
      {m:'1.23 to 1.2',i:1.23,e:1.2}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(mainChartHeatmapMetric.formatLegendNumber(test.i)).to.equal(test.e);
      });
    });
    it('NaN to NaN' + ' ', function () {
      expect(isNaN(mainChartHeatmapMetric.formatLegendNumber(NaN))).to.equal(true);
    });
  });

  describe('#refreshHostSlots', function() {
    beforeEach(function() {
      App.set('apiPrefix', '/api/v1');
      App.set('clusterName', 'tdk');
      sinon.stub(App, 'get', function(k) {
        if ('testMode' === k) return false;
        return Em.get(App, k);
      });
      sinon.spy($, 'ajax');
    });

    afterEach(function() {
      $.ajax.restore();
      App.get.restore();
    });

    mainChartHeatmapMetric  = App.MainChartHeatmapMetric.create({});
    mainChartHeatmapMetric.set('ajaxIndex', 'hosts.metrics.host_component');
    mainChartHeatmapMetric.set('ajaxData', {
      serviceName: 'SERVICE',
      componentName: 'COMPONENT'
    });
    mainChartHeatmapMetric.set('defaultMetric', 'default.metric');

    it('Should load proper URL', function() {
      mainChartHeatmapMetric.refreshHostSlots();
      expect($.ajax.args[0][0].url.endsWith('/api/v1/clusters/tdk/services/SERVICE/components/COMPONENT?fields=host_components/default/metric')).to.equal(true);
    });

  });

});
