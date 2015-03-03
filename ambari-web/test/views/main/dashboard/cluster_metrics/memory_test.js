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

require('views/main/dashboard/cluster_metrics/memory');

describe('App.ChartClusterMetricsMemory', function () {

  var view;

  beforeEach(function () {
    view = App.ChartClusterMetricsMemory.create();
  });

  describe('#transformToSeries', function () {

    var cases = [
        {
          isAmbariMetricsInstalled: true,
          seriesData: [
            [
              {
                x: 1000000000,
                y: 262144
              },
              {
                x: 1000001000,
                y: 524288
              }
            ],
            [
              {
                x: 1100000000,
                y: 1048576
              },
              {
                x: 1100001000,
                y: 2097152
              }
            ]
          ],
          title: 'Ambari Metrics is installed'
        },
        {
          isAmbariMetricsInstalled: false,
          isAmbariMetricsAvailable: true,
          isGangliaInstalled: false,
          seriesData: [
            [
              {
                x: 1000000000,
                y: 262144
              },
              {
                x: 1000001000,
                y: 524288
              }
            ],
            [
              {
                x: 1100000000,
                y: 1048576
              },
              {
                x: 1100001000,
                y: 2097152
              }
            ]
          ],
          title: 'Ganglia is not installed, Ambari Metrics is available'
        },
        {
          isAmbariMetricsInstalled: false,
          isAmbariMetricsAvailable: true,
          isGangliaInstalled: true,
          seriesData: [
            [
              {
                x: 1000000000,
                y: 256
              },
              {
                x: 1000001000,
                y: 512
              }
            ],
            [
              {
                x: 1100000000,
                y: 1024
              },
              {
                x: 1100001000,
                y: 2048
              }
            ]
          ],
          title: 'Ganglia is installed, Ambari Metrics is available'
        },
        {
          isAmbariMetricsInstalled: false,
          isAmbariMetricsAvailable: false,
          isGangliaInstalled: true,
          seriesData: [
            [
              {
                x: 1000000000,
                y: 256
              },
              {
                x: 1000001000,
                y: 512
              }
            ],
            [
              {
                x: 1100000000,
                y: 1024
              },
              {
                x: 1100001000,
                y: 2048
              }
            ]
          ],
          title: 'Ganglia is installed, Ambari Metrics is not available'
        },
        {
          isAmbariMetricsInstalled: false,
          isAmbariMetricsAvailable: false,
          isGangliaInstalled: false,
          seriesData: [
            [
              {
                x: 1000000000,
                y: 256
              },
              {
                x: 1000001000,
                y: 512
              }
            ],
            [
              {
                x: 1100000000,
                y: 1024
              },
              {
                x: 1100001000,
                y: 2048
              }
            ]
          ],
          title: 'Ganglia is not installed, Ambari Metrics is not available'
        }
      ],
      jsonData = {
        metrics: {
          memory: {
            Buffer: [
              [256, 1000000000],
              [512, 1000001000]
            ],
            Total: [
              [1024, 1100000000],
              [2048, 1100001000]
            ]
          }
        }
      },
      names = ['Buffer', 'Total'];

    afterEach(function () {
      App.StackService.find.restore();
      App.Service.find.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        var stackServices = [],
          services = [];
        if (item.isAmbariMetricsAvailable) {
          stackServices.push({
            serviceName: 'AMBARI_METRICS'
          });
        }
        if (item.isAmbariMetricsInstalled) {
          services.push({
            serviceName: 'AMBARI_METRICS'
          });
        }
        if (item.isGangliaInstalled) {
          services.push({
            serviceName: 'GANGLIA'
          });
        }
        sinon.stub(App.StackService, 'find').returns(stackServices);
        sinon.stub(App.Service, 'find').returns(services);
        var series = view.transformToSeries(jsonData);
        expect(series.mapProperty('name')).to.eql(names);
        expect(series.mapProperty('data')).to.eql(item.seriesData);
      });
    });

  });

});
