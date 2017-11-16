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

import {TestBed, inject} from '@angular/core/testing';

import {UtilsService} from './utils.service';

describe('UtilsService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UtilsService]
    });
  });

  it('should create service', inject([UtilsService], (service: UtilsService) => {
    expect(service).toBeTruthy();
  }));

  describe('#isEqual()', () => {
    const cases = [
      {
        valueA: 1,
        valueB: 1,
        result: true,
        title: 'same numbers'
      },
      {
        valueA: 1,
        valueB: 2,
        result: false,
        title: 'different numbers'
      },
      {
        valueA: 'a',
        valueB: 'a',
        result: true,
        title: 'same strings'
      },
      {
        valueA: 'a',
        valueB: 'b',
        result: false,
        title: 'different strings'
      },
      {
        valueA: '1',
        valueB: 1,
        result: false,
        title: 'different types'
      },
      {
        valueA: true,
        valueB: true,
        result: true,
        title: 'same booleans'
      },
      {
        valueA: false,
        valueB: true,
        result: false,
        title: 'different booleans'
      },
      {
        valueA: {},
        valueB: {},
        result: true,
        title: 'empty objects'
      },
      {
        valueA: {
          p0: 'v0'
        },
        valueB: {
          p0: 'v0'
        },
        result: true,
        title: 'same objects'
      },
      {
        valueA: {
          p0: 'v0'
        },
        valueB: {
          p0: 'v1'
        },
        result: false,
        title: 'different objects'
      },
      {
        valueA: {
          p0: {
            p1: 'v1'
          }
        },
        valueB: {
          p0: {
            p1: 'v1'
          }
        },
        result: true,
        title: 'same objects in depth'
      },
      {
        valueA: {
          p0: {
            p1: 'v1'
          }
        },
        valueB: {
          p0: {
            p1: 'v2'
          }
        },
        result: false,
        title: 'different objects in depth'
      },
      {
        valueA: [],
        valueB: [],
        result: true,
        title: 'empty arrays'
      },
      {
        valueA: [1, 'a'],
        valueB: [1, 'a'],
        result: true,
        title: 'same arrays'
      },
      {
        valueA: [1, 'a'],
        valueB: [1, 'b'],
        result: false,
        title: 'different arrays'
      },
      {
        valueA: [1, 1],
        valueB: [1, 1, 1],
        result: false,
        title: 'arrays of different length'
      },
      {
        valueA: [{}],
        valueB: [{}],
        result: true,
        title: 'arrays of empty objects'
      },
      {
        valueA: [
          {
            p0: 'v0'
          }
        ],
        valueB: [
          {
            p0: 'v0'
          }
        ],
        result: true,
        title: 'arrays of same objects'
      },
      {
        valueA: [
          {
            p0: 'v0'
          }
        ],
        valueB: [
          {
            p0: 'v1'
          }
        ],
        result: false,
        title: 'arrays of different objects'
      },
      {
        valueA: function() {},
        valueB: function() {},
        result: true,
        title: 'same functions'
      },
      {
        valueA: function(a) {
          return a;
        },
        valueB: function(b) {
          return !b;
        },
        result: false,
        title: 'different functions'
      },
      {
        valueA: new Date(1),
        valueB: new Date(1),
        result: true,
        title: 'same dates'
      },
      {
        valueA: new Date(1),
        valueB: new Date(2),
        result: false,
        title: 'different dates'
      },
      {
        valueA: new RegExp('a'),
        valueB: new RegExp('a'),
        result: true,
        title: 'same regexps'
      },
      {
        valueA: new RegExp('a', 'i'),
        valueB: new RegExp('a', 'g'),
        result: false,
        title: 'same regexps with different flags'
      },
      {
        valueA: new RegExp('a'),
        valueB: new RegExp('b'),
        result: false,
        title: 'different regexps'
      },
      {
        valueA: new Number(1),
        valueB: new Number(1),
        result: true,
        title: 'same number objects'
      },
      {
        valueA: new Number(1),
        valueB: new Number(2),
        result: false,
        title: 'different number objects'
      },
      {
        valueA: new String('a'),
        valueB: new String('a'),
        result: true,
        title: 'same string objects'
      },
      {
        valueA: new String('a'),
        valueB: new String('b'),
        result: false,
        title: 'different string objects'
      },
      {
        valueA: new Boolean(true),
        valueB: new Boolean(true),
        result: true,
        title: 'same boolean objects'
      },
      {
        valueA: new Boolean(true),
        valueB: new Boolean(false),
        result: false,
        title: 'different boolean objects'
      },
      {
        valueA: null,
        valueB: null,
        result: true,
        title: 'null values'
      },
      {
        valueA: undefined,
        valueB: undefined,
        result: true,
        title: 'undefined values'
      },
      {
        valueA: undefined,
        valueB: null,
        result: false,
        title: 'undefined vs null'
      }
    ];

    cases.forEach(test => {
      describe(test.title, () => {
        it('equality', inject([UtilsService], (service: UtilsService) => {
          expect(service.isEqual(test.valueA, test.valueB)).toEqual(test.result);
        }));
        it('symmetry', inject([UtilsService], (service: UtilsService) => {
          expect(service.isEqual(test.valueA, test.valueB)).toEqual(service.isEqual(test.valueB, test.valueA));
        }));
      });
    });
  });
});
