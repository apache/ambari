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

describe('App.MainChartHeatmapYarnMetrics', function () {

  var tests = [
    {
      json: {
        "host_components" : [
          {
            "HostRoles" : {
              "host_name" : "dev01.hortonworks.com"
            },
            "metrics" : {
              "jvm" : {
                "memHeapUsedM" : 10
              }
            }
          }
        ]
      },
      result: {'dev01.hortonworks.com': 10},
      m: 'One host_component'
    },
    {
      json: {
        "host_components" : [
          {
            "HostRoles" : {
              "host_name" : "dev01.hortonworks.com"
            },
            "metrics" : {
              "jvm" : {
                "memHeapUsedM" : 10
              }
            }
          },
          {
            "HostRoles" : {
              "host_name" : "dev02.hortonworks.com"
            },
            "metrics" : {
              "jvm" : {
                "memHeapUsedM" : 20
              }
            }
          }
        ]
      },
      result: {'dev01.hortonworks.com': 10, 'dev02.hortonworks.com': 20},
      m: 'Two host_components'
    },
    {
      json: {
        "host_components" : [
          {
            "HostRoles" : {
              "host_name" : "dev01.hortonworks.com"
            },
            "metrics" : {
              "jvm" : {
                "memHeapUsedM" : 10
              }
            }
          },
          {
            "HostRoles" : {
              "host_name" : "dev02.hortonworks.com"
            },
            "metrics" : {
              "jvm" : {

              }
            }
          }
        ]
      },
      result: {'dev01.hortonworks.com': 10},
      m: 'Two host_components, one without metric'
    }
  ];

  describe('#metricMapper()', function() {
    var mainChartHeatmapYarnMetrics = App.MainChartHeatmapYarnMetrics.create();
    mainChartHeatmapYarnMetrics.set('defaultMetric', 'metrics.jvm.memHeapUsedM');

    tests.forEach(function(test) {
      it(test.m, function() {
        var r = mainChartHeatmapYarnMetrics.metricMapper(test.json);
        expect(r).to.eql(test.result);
      });
    });

  });

});
