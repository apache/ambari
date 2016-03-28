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

var heatmap = require('utils/heatmap');

describe('heatmap utils', function () {

  describe('mappers', function () {

    var mappers;

    beforeEach(function () {
      mappers = Em.Object.create(heatmap.mappers);
    });

    describe('#metricMapperWithTransform', function () {

      var cases = [
        {
          hostComponents: null,
          hostToValueMap: {},
          title: 'no host components data'
        },
        {
          hostComponents: [null, null],
          metricName: 'm0',
          hostToValueMap: {},
          title: 'host components data is absent'
        },
        {
          hostComponents: [{}, {}],
          metricName: 'm1',
          hostToValueMap: {},
          title: 'provided metric data is absent'
        },
        {
          hostComponents: [{}, {}],
          metricName: 'm2.m3',
          hostToValueMap: {},
          title: 'provided metrics data is absent'
        },
        {
          hostComponents: [
            null,
            {},
            {
              m4: 1,
              HostRoles: {
                host_name: 'h0'
              }
            },
            {
              m4: 1.5,
              HostRoles: {
                host_name: 'h1'
              }
            },
            {
              m4: 1.60,
              HostRoles: {
                host_name: 'h2'
              }
            },
            {
              m4: 1.72,
              HostRoles: {
                host_name: 'h3'
              }
            },
            {
              m4: 1.85,
              HostRoles: {
                host_name: 'h4'
              }
            },
            {
              m4: 1.97,
              HostRoles: {
                host_name: 'h5'
              }
            }
          ],
          metricName: 'm4',
          hostToValueMap: {
            h0: '1.0',
            h1: '1.5',
            h2: '1.6',
            h3: '1.7',
            h4: '1.9',
            h5: '2.0'
          },
          title: 'no transform function'
        },
        {
          hostComponents: [
            {
              m5: 100,
              HostRoles: {
                host_name: 'h6'
              }
            }
          ],
          metricName: 'm5',
          transformValueFunction: Math.sqrt,
          hostToValueMap: {
            h6: '10.0'
          },
          title: 'transform function provided'
        }
      ];

      cases.forEach(function (item) {

        it(item.title, function () {
          expect(mappers.metricMapperWithTransform({
            host_components: item.hostComponents
          }, item.metricName, item.transformValueFunction)).to.eql(item.hostToValueMap);
        });

      });

    });

  });

});
