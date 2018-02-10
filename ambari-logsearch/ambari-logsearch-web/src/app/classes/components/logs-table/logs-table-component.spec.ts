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

import {LogsTableComponent} from './logs-table-component';

describe('LogsTableComponent', () => {
  let component;

  beforeEach(() => {
    component = new LogsTableComponent();
  });

  describe('#isColumnDisplayed()', () => {
    const cases = [
      {
        name: 'v1',
        result: true,
        title: 'column is displayed'
      },
      {
        name: 'l1',
        result: false,
        title: 'column is not displayed'
      }
    ];

    beforeEach(() => {
      component.displayedColumns = [
        {
          label: 'l0',
          value: 'v0'
        },
        {
          label: 'l1',
          value: 'v1'
        }
      ];
    });

    cases.forEach(test => {
      it(test.title, () => {
        expect(component.isColumnDisplayed(test.name)).toEqual(test.result);
      });
    });
  });
});