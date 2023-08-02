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
require('views/main/dashboard/cluster_metrics/cpu');

describe('App.ChartClusterMetricsCPU', function () {

  var view;

  beforeEach(function() {
    view = App.ChartClusterMetricsCPU.create({
      seriesTemplate: {
        path: 'metrics.cpu'
      }
    });
  });

  describe('#getData()', function() {

    it('should return data array with all metrics', function () {
      var jsonData = {
        "href": "http://ambari/api/clusters/vmc?fields=metrics/cpu[1352702257,1352705857,15]",
        "metrics": {
          "cpu": {
            "User": [[15.5, 1352706465], [15.966666667, 1352706480]],
            "Wait": [[1.0, 1352706465], [1.1666666667, 1352706480]],
            "System": [[3.2, 1352706465], [3.4333333333, 1352706480]],
            "Nice": [[0.0, 1352706465], [0.0, 1352706480]],
            "Idle": [[80.3, 1352706465], [79.433333333, 1352706480]]
          }
        },
        "Clusters": {
          "cluster_name": "vmc",
          "version": "HDP-1.2.0"
        }
      };
      var expectedResult = [
        {name: 'User', data: [[15.5, 1352706465], [15.966666667, 1352706480]]},
        {name: 'Wait', data: [[1.0, 1352706465], [1.1666666667, 1352706480]]},
        {name: 'System', data: [[3.2, 1352706465], [3.4333333333, 1352706480]]},
        {name: 'Nice', data: [[0.0, 1352706465], [0.0, 1352706480]]},
        {name: 'Idle', data: [[80.3, 1352706465], [79.433333333, 1352706480]]},
      ];
      expect(view.getData(jsonData)).to.eql(expectedResult);
    });

    it('should return data array with "Idle" metric as last', function () {
      var jsonData = {
        "href": "http://ambari/api/clusters/vmc?fields=metrics/cpu[1352702257,1352705857,15]",
        "metrics": {
          "cpu": {
            "Idle": [[80.3, 1352706465], [79.433333333, 1352706480]],
            "User": [[15.5, 1352706465], [15.966666667, 1352706480]],
            "Wait": [[1.0, 1352706465], [1.1666666667, 1352706480]],
            "System": [[3.2, 1352706465], [3.4333333333, 1352706480]],
            "Nice": [[0.0, 1352706465], [0.0, 1352706480]]
          }
        },
        "Clusters": {
          "cluster_name": "vmc",
          "version": "HDP-1.2.0"
        }
      };
      var expectedResult = [
        {name: 'User', data: [[15.5, 1352706465], [15.966666667, 1352706480]]},
        {name: 'Wait', data: [[1.0, 1352706465], [1.1666666667, 1352706480]]},
        {name: 'System', data: [[3.2, 1352706465], [3.4333333333, 1352706480]]},
        {name: 'Nice', data: [[0.0, 1352706465], [0.0, 1352706480]]},
        {name: 'Idle', data: [[80.3, 1352706465], [79.433333333, 1352706480]]},
      ];
      expect(view.getData(jsonData)).to.eql(expectedResult);
    });

    it('should return empty array', function () {
      var jsonData = null;
      expect(view.getData(jsonData)).to.be.empty;
    });
  });

  describe('#colorForSeries()', function() {
    var series = {name: 'Idle'};
    it('should return color', function() {
      expect(view.colorForSeries(series)).to.equal('#CFECEC');
    });
  });

  describe('#colorForSeries()', function() {
    var series = {name: 'Stopped'};
    it('should return null', function() {
      expect(view.colorForSeries(series)).to.equal(null);
    });
  });

});