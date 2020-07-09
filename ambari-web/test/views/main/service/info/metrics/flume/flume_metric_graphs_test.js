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
require('views/main/service/info/metrics/flume/flume_metric_graphs');

describe('App.MainServiceInfoFlumeGraphsView', function () {

  var view;

  beforeEach(function () {
    view = App.MainServiceInfoFlumeGraphsView.create();
  });

  describe('#didInsertElement()', function () {

    beforeEach(function () {
      sinon.stub(view, 'loadMetrics');
    });

    it('should execute loadMetrics function', function () {
      view.didInsertElement();
      expect(view.loadMetrics.calledOnce).to.be.true;
    });
  });

  describe('#loadMetrics()', function () {

    it('should return empty array', function () {
      expect(view.loadMetrics()).to.eql([]);
    });

    it('should not send ajax request', function () {
      view.loadMetrics();
      expect(App.ajax.send.called).to.be.false;
    });

    it('should send ajax request', function () {
      view.set('viewData', {
        metricType: 'SINK',
        agent: Em.Object.create({
          hostName: 'host1'
        })
      });
      view.loadMetrics();
      expect(App.ajax.send.called).to.be.true;
    });
  });

  describe('#onLoadMetricsSuccess', function () {

    beforeEach(function () {
      sinon.stub(App.ChartServiceFlumeMetricGraph, 'extend', function(data) { return data });
    });

    afterEach(function () {
      App.ChartServiceFlumeMetricGraph.extend.restore();
    });

    it('should return empty array in array', function () {
      view.set('viewData', {
        metricType: 'SINK',
        agent: Em.Object.create({
          hostName: 'host1'
        })
      });
      view.onLoadMetricsSuccess(null);
      expect(view.get('serviceMetricGraphs')).to.eql([[]]);
    });

    it('should set serviceMetricGraphs property', function () {
      var dataMock = {
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
      view.set('viewData', {
        metricType: 'SINK',
        agent: Em.Object.create({
          hostName: 'host1'
        })
      });
      view.onLoadMetricsSuccess(dataMock);
      expect(view.get('serviceMetricGraphs')).to.eql([
        [
          {
            metricType: 'SINK',
            metricName: 'm1',
            hostName: 'host1',
            metricItems: ['comp1', 'comp2', 'comp3']
          },
          {
            metricType: 'SINK',
            metricName: 'm2',
            hostName: 'host1',
            metricItems: ['comp1', 'comp2', 'comp3']
          }
        ]
      ])
    });

    it('should set serviceMetricGraphs property', function () {
      var dataMock = {
        "metrics": {
          "flume": {
            "flume": {
              "SINK": {
                "comp1": {
                  "m1": [1, 2, 3]
                },
                "comp2": {
                  "m2": [1, 2, 3]
                },
                "comp3": {
                  "m3": [1, 2, 3]
                }
              }
            }
          }
        }
      };
      view.set('viewData', {
        metricType: 'SINK',
        agent: Em.Object.create({
          hostName: 'host1'
        })
      });
      view.onLoadMetricsSuccess(dataMock);
      expect(view.get('serviceMetricGraphs')).to.eql([
        [
          {
            metricType: 'SINK',
            metricName: 'm1',
            hostName: 'host1',
            metricItems: ['comp1', 'comp2', 'comp3']
          },
          {
            metricType: 'SINK',
            metricName: 'm2',
            hostName: 'host1',
            metricItems: ['comp1', 'comp2', 'comp3']
          }
          ],
        [
          {
            metricType: 'SINK',
            metricName: 'm3',
            hostName: 'host1',
            metricItems: ['comp1', 'comp2', 'comp3']
          }
        ]
      ])
    });
  });

});
