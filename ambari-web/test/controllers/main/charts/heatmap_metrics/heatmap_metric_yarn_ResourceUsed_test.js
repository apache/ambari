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
require('controllers/main/charts/heatmap_metrics/heatmap_metric_yarn');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_yarn_ResourceUsed');

describe('App.MainChartHeatmapYarnResourceUsedMetric', function () {

  var mainChartHeatmapYarnResourceUsedMetric = App.MainChartHeatmapYarnResourceUsedMetric.create({});

  describe('#metricMapper', function () {
    var tests = [
      {
        m: 'Correct JSON #1',
        i: {
          ServiceComponentInfo: {
            cluster_name: "c1",
            component_name: "NODEMANAGER",
            service_name: "YARN"
          },
          host_components: [{
            HostRoles: {
              cluster_name: "c1",
              component_name: "NODEMANAGER",
              host_name: "host1"
            },
            metrics: {
              yarn: {
                AllocatedGB: 0,
                AvailableGB: 2
              }
            }
          }]
        },
        e: {
          length: 1,
          val: '0.0',
          host: 'host1'
        }
      },
      {
        m: 'Correct JSON #2',
        i: {
          ServiceComponentInfo: {
            cluster_name: "c1",
            component_name: "NODEMANAGER",
            service_name: "YARN"
          },
          host_components: [{
            HostRoles: {
              cluster_name: "c1",
              component_name: "NODEMANAGER",
              host_name: "host1"
            },
            metrics: {
              yarn: {
                AllocatedGB: 1,
                AvailableGB: 2
              }
            }
          }]
        },
        e: {
          length: 1,
          val: '33.3',
          host: 'host1'
        }
      },
      {
        m: 'Correct JSON #3',
        i: {
          ServiceComponentInfo: {
            cluster_name: "c1",
            component_name: "NODEMANAGER",
            service_name: "YARN"
          },
          host_components: [{
            HostRoles: {
              cluster_name: "c1",
              component_name: "NODEMANAGER",
              host_name: "host1"
            },
            metrics: {
              yarn: {
                AllocatedGB: 0,
                AvailableGB: 0
              }
            }
          }]
        },
        e: {
          length: 1,
          val: 'Unknown',
          host: 'host1'
        }
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function () {
        var result = mainChartHeatmapYarnResourceUsedMetric.metricMapper(test.i),
          length = Em.keys(result).length;
        expect(length).to.equal(test.e.length);
        if (test.e.host) {
          expect(result.hasOwnProperty(test.e.host)).to.equal(true);
          expect(result[test.e.host]).to.equal(test.e.val);
        }
      });
    });
  });

});
