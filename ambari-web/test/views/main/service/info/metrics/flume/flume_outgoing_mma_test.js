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
require('views/main/service/info/metrics/flume/flume_outgoing_mma');

describe('App.ChartServiceMetricsFlume_OutgoingMMA', function () {

  var view;

  beforeEach(function () {
    view = App.ChartServiceMetricsFlume_OutgoingMMA.create();
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
                  EventTakeSuccessCount: {
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
                  EventTakeSuccessCount: {
                    rate: {
                      avg: [
                        [0, 1445472000],
                        [1, 1445472015]
                      ],
                      max: [
                        [2, 1445472000],
                        [3, 1445472015]
                      ],
                      min: [
                        [4, 1445472000],
                        [5, 1445472015]
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
            name: Em.I18n.t('services.service.info.metrics.flume.outgoing_mma').format('avg'),
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
          },
          {
            name: Em.I18n.t('services.service.info.metrics.flume.outgoing_mma').format('max'),
            data: [
              {
                x: 1445472000,
                y: 2
              },
              {
                x: 1445472015,
                y: 3
              }
            ]
          },
          {
            name: Em.I18n.t('services.service.info.metrics.flume.outgoing_mma').format('min'),
            data: [
              {
                x: 1445472000,
                y: 4
              },
              {
                x: 1445472015,
                y: 5
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
