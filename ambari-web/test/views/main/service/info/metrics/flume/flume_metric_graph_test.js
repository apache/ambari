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
require('views/main/service/info/metrics/flume/flume_metric_graph');

describe('App.ChartServiceFlumeMetricGraph', function () {

  var view;

  beforeEach(function () {
    view = App.ChartServiceFlumeMetricGraph.create();
  });

  describe('#getDataForAjaxRequest', function () {

    beforeEach(function () {
      sinon.stub(App, 'get').returns('test');
    });

    afterEach(function () {
      App.get.restore();
    });

    it('should return proper data url', function () {
      view.set('metricItems', ['item1', 'item2']);
      view.set('hostName', 'host1');
      view.set('metricType', 'SINK');
      view.set('metricName', 'm1');
      var result = view.getDataForAjaxRequest();
      expect(result.url).to.equal('test/clusters/test/hosts/host1/host_components/FLUME_HANDLER?fields=metrics/flume/flume/SINK/item1/m1['
        + result.fromSeconds + ',' + result.toSeconds + ',15],metrics/flume/flume/SINK/item2/m1[' + result.fromSeconds + ',' + result.toSeconds + ',15]');
    });

    it('should return flume data array from json', function () {
      var jsonDataMock = {
        "metrics": {
          "flume": {
            "flume": {
              "SINK": {
                "comp1": {
                  "m1": [1, 2, 3]
                },
                "comp2": {
                  "m1": [4, 5, 6]
                },
                "comp3": {
                  "m2": [7, 8, 9]
                }
              }
            }
          }
        }
      };
      view.set('metricType', 'SINK');
      view.set('metricName', 'm1');
      expect(view.getData(jsonDataMock)).to.eql([
        {
          name: 'comp1',
          data: [1, 2, 3]
        },
        {
          name: 'comp2',
          data: [4, 5, 6]
        }
      ]);
    });
  });

});
