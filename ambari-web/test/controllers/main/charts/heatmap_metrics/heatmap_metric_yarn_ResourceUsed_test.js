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
          "ServiceComponentInfo" : {
            "rm_metrics" : {
              "cluster" : {
                "nodeManagers" : "[{\"HostName\":\"dev01.hortonworks.com\",\"Rack\":\"/default-rack\",\"State\":\"RUNNING\",\"NodeId\":\"dev01.hortonworks.com:45454\",\"NodeHTTPAddress\":\"dev01.hortonworks.com:8042\",\"LastHealthUpdate\":1375869232870,\"HealthReport\":\"\",\"NumContainers\":0,\"UsedMemoryMB\":10,\"AvailableMemoryMB\":100}]"
              }
            }
          }
        },
        e: {
          length: 1,
          val: '10.0',
          host: 'dev01.hortonworks.com'
        }
      },
      {
        m: 'Correct JSON #2',
        i: {
          "ServiceComponentInfo" : {
            "rm_metrics" : {
              "cluster" : {
                "nodeManagers" : "[{\"HostName\":\"dev01.hortonworks.com\",\"Rack\":\"/default-rack\",\"State\":\"RUNNING\",\"NodeId\":\"dev01.hortonworks.com:45454\",\"NodeHTTPAddress\":\"dev01.hortonworks.com:8042\",\"LastHealthUpdate\":1375869232870,\"HealthReport\":\"\",\"NumContainers\":0,\"UsedMemoryMB\":0,\"AvailableMemoryMB\":100}]"
              }
            }
          }
        },
        e: {
          length: 1,
          val: '0.0',
          host: 'dev01.hortonworks.com'
        }
      },
      {
        m: 'JSON without "cluster"',
        i: {
          "ServiceComponentInfo" : {
            "rm_metrics" : {
            }
          }
        },
        e: {
          length: 0,
          val: null,
          host: null
        }
      },
      {
        m: 'JSON without "nodeManagers"',
        i: {
          "ServiceComponentInfo" : {
            "rm_metrics" : {
              "cluster" : {
              }
            }
          }
        },
        e: {
          length: 0,
          val: null,
          host: null
        }
      },
      {
        m: 'Correct JSON #3 (with two nodeManagers)',
        i: {
          "ServiceComponentInfo" : {
            "rm_metrics" : {
              "cluster" : {
                "nodeManagers" : "[{\"HostName\":\"dev01.hortonworks.com\",\"Rack\":\"/default-rack\",\"State\":\"RUNNING\",\"NodeId\":\"dev01.hortonworks.com:45454\",\"NodeHTTPAddress\":\"dev01.hortonworks.com:8042\",\"LastHealthUpdate\":1375869232870,\"HealthReport\":\"\",\"NumContainers\":0,\"UsedMemoryMB\":0,\"AvailableMemoryMB\":100}, {\"HostName\":\"dev02.hortonworks.com\",\"Rack\":\"/default-rack\",\"State\":\"RUNNING\",\"NodeId\":\"dev02.hortonworks.com:45454\",\"NodeHTTPAddress\":\"dev01.hortonworks.com:8042\",\"LastHealthUpdate\":1375869232870,\"HealthReport\":\"\",\"NumContainers\":0,\"UsedMemoryMB\":100,\"AvailableMemoryMB\":100}]"
              }
            }
          }
        },
        e: {
          length: 2,
          val: '100.0',
          host: 'dev02.hortonworks.com'
        }
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function () {
        var result = mainChartHeatmapYarnResourceUsedMetric.metricMapper(test.i);
        var length = 0;
        for(var p in result) {
          if (result.hasOwnProperty(p)) {
            length++;
          }
        }
        expect(length).to.equal(test.e.length);
        if (test.e.host) {
          expect(result.hasOwnProperty(test.e.host)).to.equal(true);
          expect(result[test.e.host]).to.equal(test.e.val);
        }
      });
    });
  });

});
