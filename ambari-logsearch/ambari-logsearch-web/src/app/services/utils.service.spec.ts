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

  describe('#isEmptyObject()', () => {
    const cases = [
      {
        obj: {},
        result: true,
        title: 'empty object'
      },
      {
        obj: {
          p: 'v'
        },
        result: false,
        title: 'not empty object'
      },
      {
        obj: null,
        result: false,
        title: 'null'
      },
      {
        obj: undefined,
        result: false,
        title: 'undefined'
      },
      {
        obj: '',
        result: false,
        title: 'empty string'
      },
      {
        obj: 0,
        result: false,
        title: 'zero'
      },
      {
        obj: false,
        result: false,
        title: 'false'
      },
      {
        obj: NaN,
        result: false,
        title: 'NaN'
      },
      {
        obj: [],
        result: false,
        title: 'empty array'
      },
      {
        obj: '123',
        result: false,
        title: 'not empty primitive'
      }
    ];

    cases.forEach(test => {
      it(test.title, inject([UtilsService], (service: UtilsService) => {
        expect(service.isEmptyObject(test.obj)).toEqual(test.result);
      }));
    });
  });

  describe('#getMaxNumberInObject()', () => {
    const cases = [
      {
        obj: {
          a: 1,
          b: -1,
          c: 0
        },
        max: 1,
        title: 'basic case'
      },
      {
        obj: {
          a: 1
        },
        max: 1,
        title: 'single-item object'
      },
      {
        obj: {
          a: -Infinity,
          b: 0,
          c: 1
        },
        max: 1,
        title: 'object with -Infinity'
      },
      {
        obj: {
          a: Infinity,
          b: 0,
          c: 1
        },
        max: Infinity,
        title: 'object with Infinity'
      },
      {
        obj: {
          a: NaN,
          b: 0,
          c: 1
        },
        max: 1,
        title: 'object with NaN'
      }
    ];

    cases.forEach(test => {
      it(test.title, inject([UtilsService], (service: UtilsService) => {
        expect(service.getMaxNumberInObject(test.obj)).toEqual(test.max);
      }));
    });
  });

  describe('#getListItemFromString()', () => {
    it('should convert string to ListItem', inject([UtilsService], (service: UtilsService) => {
      expect(service.getListItemFromString('customName')).toEqual({
        label: 'customName',
        value: 'customName'
      });
    }));
  });

  describe('#getListItemFromNode()', () => {
    it('should convert NodeItem to ListItem', inject([UtilsService], (service: UtilsService) => {
      expect(service.getListItemFromNode({
        name: 'customName',
        value: '1',
        isParent: true,
        isRoot: true
      })).toEqual({
        label: 'customName (1)',
        value: 'customName'
      });
    }));
  });

  describe('#pushUniqueValues()', () => {
    const cases = [
      {
        source: [1, 2, 3],
        itemsToPush: [2, 4, 5, 1],
        compareFunction: undefined,
        result: [1, 2, 3, 4, 5],
        title: 'primitives array'
      },
      {
        source: [
          {
            p0: 'v0'
          },
          {
            p1: 'v1'
          },
          {
            p2: 'v2'
          }
        ],
        itemsToPush: [
          {
            p3: 'v3'
          },
          {
            p2: 'v2'
          },
          {
            p2: 'v3'
          },
          {
            p4: 'v4'
          }
        ],
        compareFunction: undefined,
        result: [
          {
            p0: 'v0'
          },
          {
            p1: 'v1'
          },
          {
            p2: 'v2'
          },
          {
            p3: 'v3'
          },
          {
            p2: 'v3'
          },
          {
            p4: 'v4'
          }
        ],
        title: 'objects array'
      },
      {
        source: [
          {
            id: 0,
            value: 'v0'
          },
          {
            id: 1,
            value: 'v1'
          },
          {
            id: 2,
            value: 'v2'
          }
        ],
        itemsToPush: [
          {
            id: 3,
            value: 'v3'
          },
          {
            id: 4,
            value: 'v4'
          },
          {
            id: 0,
            value: 'v5'
          },
          {
            id: 1,
            value: 'v6'
          }
        ],
        compareFunction: (itemA: any, itemB: any): boolean => itemA.id === itemB.id,
        result: [
          {
            id: 0,
            value: 'v0'
          },
          {
            id: 1,
            value: 'v1'
          },
          {
            id: 2,
            value: 'v2'
          },
          {
            id: 3,
            value: 'v3'
          },
          {
            id: 4,
            value: 'v4'
          }
        ],
        title: 'custom comparison function'
      }
    ];

    cases.forEach(test => {
      it(test.title, inject([UtilsService], (service: UtilsService) => {
        expect(service.pushUniqueValues(test.source, test.itemsToPush, test.compareFunction)).toEqual(test.result);
      }));
    });
  });

});
