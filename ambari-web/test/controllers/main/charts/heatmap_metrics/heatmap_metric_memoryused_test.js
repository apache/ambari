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
require('controllers/main/charts/heatmap_metrics/heatmap_metric_memoryused');

describe('App.MainChartHeatmapMemoryUsedMetric', function () {

  var tests = [
    {
      json:{
        "items" : [
          {
            "Hosts" : {
              "host_name" : "dev01.hortonworks.com"
            },
            "metrics" : {
              "memory" : {
                "mem_buffers" : 109888.0,
                "mem_cached" : 1965624.0,
                "mem_free" : 261632.0,
                "mem_shared" : 0.0,
                "mem_total" : 6123776.0,
                "swap_free" : 4126820.0,
                "swap_total" : 4128760.0
              }
            }
          }
        ]
      },
      m: 'One host',
      e: {'dev01.hortonworks.com': '63.6'}
    },
    {
      json:{
        "items" : [
          {
            "Hosts" : {
              "host_name" : "dev01.hortonworks.com"
            },
            "metrics" : {
              "memory" : {
                "mem_buffers" : 109888.0,
                "mem_cached" : 1965624.0,
                "mem_free" : 261632.0,
                "mem_shared" : 0.0,
                "mem_total" : 6123776.0,
                "swap_free" : 4126820.0,
                "swap_total" : 4128760.0
              }
            }
          },
          {
            "Hosts" : {
              "host_name" : "dev02.hortonworks.com"
            },
            "metrics" : {
              "memory" : {
                "mem_buffers" : 109888.0,
                "mem_cached" : 1965624.0,
                "mem_free" : 261632.0,
                "mem_shared" : 0.0,
                "mem_total" : 6123776.0,
                "swap_free" : 4126820.0,
                "swap_total" : 4128760.0
              }
            }
          }
        ]
      },
      m: 'Two hosts',
      e: {'dev01.hortonworks.com': '63.6', 'dev02.hortonworks.com': '63.6'}
    },
    {
      json:{
        "items" : [
          {
            "Hosts" : {
              "host_name" : "dev01.hortonworks.com"
            },
            "metrics" : {
              "memory" : {
                "mem_buffers" : 109888.0,
                "mem_cached" : 1965624.0,
                "mem_free" : 261632.0,
                "mem_shared" : 0.0,
                "mem_total" : 6123776.0,
                "swap_free" : 4126820.0,
                "swap_total" : 4128760.0
              }
            }
          },
          {
            "Hosts" : {
              "host_name" : "dev02.hortonworks.com"
            },
            "metrics" : {

            }
          }
        ]
      },
      m: 'Two hosts, One without metric',
      e: {'dev01.hortonworks.com': '63.6'}
    }
  ];

  describe('#metricMapper()', function() {
    var mainChartHeatmapMemoryUsedMetric = App.MainChartHeatmapMemoryUsedMetric.create();

    tests.forEach(function(test) {
      it(test.m, function() {
        var r = mainChartHeatmapMemoryUsedMetric.metricMapper(test.json);
        expect(r).to.eql(test.e);
      });
    });

  });

});
