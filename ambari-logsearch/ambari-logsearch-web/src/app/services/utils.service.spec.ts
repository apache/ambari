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

  describe('#updateMultiSelectValue()', () => {
    const cases = [
      {
        currentValue: '',
        value: 'v0',
        isChecked: true,
        result: 'v0',
        title: 'check; no checked items before'
      },
      {
        currentValue: 'v1,v2',
        value: 'v3',
        isChecked: true,
        result: 'v1,v2,v3',
        title: 'check'
      },
      {
        currentValue: 'v4,v5',
        value: 'v4',
        isChecked: false,
        result: 'v5',
        title: 'uncheck'
      },
      {
        currentValue: 'v6,v7',
        value: 'v6',
        isChecked: true,
        result: 'v6,v7',
        title: 'avoid repeating check action'
      },
      {
        currentValue: 'v8,v9',
        value: 'v10',
        isChecked: false,
        result: 'v8,v9',
        title: 'avoid repeating uncheck action'
      },
      {
        currentValue: 'v11',
        value: 'v11',
        isChecked: false,
        result: '',
        title: 'uncheck last item'
      }
    ];

    cases.forEach(test => {
      it(test.title, inject([UtilsService], (service: UtilsService) => {
        expect(service.updateMultiSelectValue(test.currentValue, test.value, test.isChecked)).toEqual(test.result);
      }));
    });
  });
});
