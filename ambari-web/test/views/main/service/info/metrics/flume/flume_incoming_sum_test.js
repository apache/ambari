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
require('views/main/service/info/metrics/flume/flume_incoming_sum');

describe('App.ChartServiceMetricsFlume_IncommingSum', function () {

  var view;

  beforeEach(function () {
    view = App.ChartServiceMetricsFlume_IncommingSum.create();
  });

  describe('#transformToSeries', function () {

    var cases = [
      {
        data: {},
        seriesArray: [],
        title: 'empty response'
      },
      {
        data: {
          metrics: {}
        },
        seriesArray: [],
        title: 'invalid response'
      },
      {
        data: {
          metrics: {
            flume: {
              flume: {
                CHANNEL: {
                  EventPutSuccessCount: {
                    rate: null
                  }
                }
              }
            }
          }
        },
        seriesArray: [],
        title: 'empty data'
      },
      {
        data: {
          metrics: {
            flume: {
              flume: {
                CHANNEL: {
                  EventPutSuccessCount: {
                    rate: {
                      sum: [
                        [0, 1445472000],
                        [1, 1445472015]
                      ]
                    }
                  }
                }
              }
            }
          }
        },
        seriesArray: [
          {
            name: Em.I18n.t('services.service.info.metrics.flume.incoming.sum'),
            data: [
              {
                x: 1445472000,
                y: 0
              },
              {
                x: 1445472015,
                y: 1
              }
            ]
          }
        ],
        title: 'valid data'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(view.transformToSeries(item.data)).to.eql(item.seriesArray);
      });
    });

  });

});
