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
require('views/main/host/metrics/cpu');

describe('App.ChartHostMetricsCPU', function () {

  var view;

  beforeEach(function () {
    view = App.ChartHostMetricsCPU.create();
  });

  describe('#colorForSeries()', function() {

    it('should return color', function() {
      var series = {name: 'CPU Idle'};
      expect(view.colorForSeries(series)).to.equal('#CFECEC');
    });

    it('should return null', function() {
      var series = {name: 'CPU'};
      expect(view.colorForSeries(series)).to.equal(null);
    });
  });

  describe('#getData()', function() {

    it('should return data array with all metrics', function () {
      var jsonData = {
        "metrics": {
          "cpu": {
            "cpu_wio": [[15.5, 1352706465], [15.966666667, 1352706480]],
            "cpu_aidle": [[1.0, 1352706465], [1.1666666667, 1352706480]],
            "cpu_system": [[3.2, 1352706465], [3.4333333333, 1352706480]],
            "cpu_nice": [[0.0, 1352706465], [0.0, 1352706480]],
            "cpu_idle": [[80.3, 1352706465], [79.433333333, 1352706480]]
          }
        }
      };
      var expectedResult = [
        {name: 'CPU I/O Idle', data: [[15.5, 1352706465], [15.966666667, 1352706480]]},
        {name: 'CPU Boot Idle', data: [[1.0, 1352706465], [1.1666666667, 1352706480]]},
        {name: 'CPU System', data: [[3.2, 1352706465], [3.4333333333, 1352706480]]},
        {name: 'CPU Nice', data: [[0.0, 1352706465], [0.0, 1352706480]]},
        {name: 'CPU Idle', data: [[80.3, 1352706465], [79.433333333, 1352706480]]}
      ];
      expect(view.getData(jsonData)).to.eql(expectedResult);
    });

    it('should not return metrics without data', function () {
      var jsonData = {
        "metrics": {
          "cpu": {
            "cpu_nice": null,
            "cpu_idle": [[80.3, 1352706465], [79.433333333, 1352706480]]
          }
        }
      };
      var expectedResult = [{name: 'CPU Idle', data: [[80.3, 1352706465], [79.433333333, 1352706480]]}];
      expect(view.getData(jsonData)).to.eql(expectedResult);
    });

    it('should return empty array', function () {
      var jsonData = null;
      expect(view.getData(jsonData)).to.be.empty;
    });
  });

});
