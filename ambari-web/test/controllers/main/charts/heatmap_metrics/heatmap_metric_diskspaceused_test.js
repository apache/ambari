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
require('controllers/main/charts/heatmap_metrics/heatmap_metric_diskspaceused');

describe('App.MainChartHeatmapDiskSpaceUsedMetric', function () {

  var tests = Em.A([
    {
      json:{
        "items" : [
          {
            "Hosts" : {
              "host_name" : "dev01.hortonworks.com"
            },
            "metrics" : {
              "disk" : {
                "disk_free" : 89.973,
                "disk_total" : 101.515
              }
            }
          }
        ]
      },
      m: 'One host',
      e: {'dev01.hortonworks.com': 11.37}
    },
    {
      json:{
        "items" : [
          {
            "Hosts" : {
              "host_name" : "dev01.hortonworks.com"
            },
            "metrics" : {
              "disk" : {
                "disk_free" : 89.973,
                "disk_total" : 101.515
              }
            }
          },
          {
            "Hosts" : {
              "host_name" : "dev02.hortonworks.com"
            },
            "metrics" : {
              "disk" : {
                "disk_free" : 89.973,
                "disk_total" : 101.515
              }
            }
          }
        ]
      },
      m: 'Two hosts',
      e: {'dev01.hortonworks.com': 11.37, 'dev02.hortonworks.com': 11.37}
    },
    {
      json:{
        "items" : [
          {
            "Hosts" : {
              "host_name" : "dev01.hortonworks.com"
            },
            "metrics" : {
              "disk" : {
                "disk_free" : 89.973,
                "disk_total" : 101.515
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
      e: {'dev01.hortonworks.com': 11.37}
    }
  ]);

  describe('#metricMapper()', function() {
    var mainChartHeatmapDiskSpaceUsedMetric = App.MainChartHeatmapDiskSpaceUsedMetric.create();

    tests.forEach(function(test) {
      it(test.m, function() {
        var r = mainChartHeatmapDiskSpaceUsedMetric.metricMapper(test.json);
        expect(r).to.eql(test.e);
      });
    });

  });

});
