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

    var jsonData = {
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
      seriesData = [
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
      names = ['Buffer', 'Total'],
      title = 'should transform data to series';

      it(title, function () {
        var series = view.transformToSeries(jsonData);
        expect(series.mapProperty('name')).to.eql(names);
        expect(series.mapProperty('data')).to.eql(seriesData);
    });

  });

});
