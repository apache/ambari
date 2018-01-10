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

import {AuditLogsGraphQueryParams} from './audit-logs-graph-query-params';

describe('AuditLogsGraphQueryParams', () => {

  describe('constructor', () => {
    const cases = [
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-01T00:00:00.100Z'
        },
        unit: '+100MILLISECOND',
        title: 'less than 1s'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-01T00:00:01Z'
        },
        unit: '+100MILLISECOND',
        title: '1s'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-01T00:00:20Z'
        },
        unit: '+500MILLISECOND',
        title: 'between 1s and 30s'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-01T00:00:20Z'
        },
        unit: '+500MILLISECOND',
        title: '30s'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-01T00:00:40Z'
        },
        unit: '+2SECOND',
        title: 'between 30s and 1m'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-01T00:01:00Z'
        },
        unit: '+2SECOND',
        title: '1m'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-01T00:20:00Z'
        },
        unit: '+1MINUTE',
        title: 'between 1m and 30m'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-01T00:30:00Z'
        },
        unit: '+2MINUTE',
        title: '30m'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-01T01:00:00Z'
        },
        unit: '+2MINUTE',
        title: 'between 30m and 2h'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-01T02:00:00Z'
        },
        unit: '+5MINUTE',
        title: '2h'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-01T04:00:00Z'
        },
        unit: '+5MINUTE',
        title: 'between 2h and 6h'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-01T06:00:00Z'
        },
        unit: '+10MINUTE',
        title: '6h'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-01T08:00:00Z'
        },
        unit: '+10MINUTE',
        title: 'between 6h and 10h'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-01T10:00:00Z'
        },
        unit: '+10MINUTE',
        title: '10h'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-01T22:00:00Z'
        },
        unit: '+1HOUR',
        title: 'between 10h and 1d'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-02T00:00:00Z'
        },
        unit: '+1HOUR',
        title: '1d'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-10T00:00:00Z'
        },
        unit: '+8HOUR',
        title: 'between 1d and 15d'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-01-16T00:00:00Z'
        },
        unit: '+1DAY',
        title: '15d'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-03-31T00:00:00Z'
        },
        unit: '+1DAY',
        title: 'between 15d and 3M'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-04-01T00:00:00Z'
        },
        unit: '+1DAY',
        title: '3M'
      },
      {
        options: {
          from: '2017-01-01T00:00:00Z',
          to: '2017-05-01T00:00:00Z'
        },
        unit: '+1MONTH',
        title: 'over 3M'
      }
    ];

    cases.forEach(test => {
      it(test.title, () => {
        const paramsObject = new AuditLogsGraphQueryParams(test.options);
        expect(paramsObject.unit).toEqual(test.unit);
      });
    });
  });

});
