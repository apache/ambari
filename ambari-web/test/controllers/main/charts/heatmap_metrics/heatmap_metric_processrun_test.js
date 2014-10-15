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
require('controllers/main/charts/heatmap_metrics/heatmap_metric_processrun');

describe('App.MainChartHeatmapProcessRunMetric', function () {

  var tests = [
    {
      json: {
        "items" : [
          {
            "Hosts" : {
              "host_name" : "dev01.hortonworks.com"
            },
            "metrics" : {
              "process" : {
                "proc_run" : 0.0
              }
            }
          }
        ]
      },
      m: 'One host',
      result: {'dev01.hortonworks.com': '0.0'}
    },
    {
      json: {
        "items" : [
          {
            "Hosts" : {
              "host_name" : "dev01.hortonworks.com"
            },
            "metrics" : {
              "process" : {
                "proc_run" : 0.1
              }
            }
          },
          {
            "Hosts" : {
              "host_name" : "dev02.hortonworks.com"
            },
            "metrics" : {
              "process" : {
                "proc_run" : 0.46
              }
            }
          }
        ]
      },
      m: 'Two hosts',
      result: {'dev01.hortonworks.com': '0.1', 'dev02.hortonworks.com': '0.5'}
    },
    {
      json: {
        "items" : [
          {
            "Hosts" : {
              "host_name" : "dev01.hortonworks.com"
            },
            "metrics" : {
              "process" : {
                "proc_run" : 0.99
              }
            }
          },
          {
            "Hosts" : {
              "host_name" : "dev02.hortonworks.com"
            },
            "metrics" : {
              "process" : {
              }
            }
          }
        ]
      },
      m: 'Two hosts, One without metric',
      result: {'dev01.hortonworks.com': '1.0'}
    }
  ];

  describe('#metricMapper()', function() {
    var mainChartHeatmapProcessRunMetric = App.MainChartHeatmapProcessRunMetric.create();

    tests.forEach(function(test) {
      it(test.m, function() {
        var r = mainChartHeatmapProcessRunMetric.metricMapper(test.json);
        expect(r).to.eql(test.result);
      });
    });
  });
});
