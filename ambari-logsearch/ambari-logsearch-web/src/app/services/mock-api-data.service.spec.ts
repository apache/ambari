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
import {MockApiDataService} from './mock-api-data.service';

describe('MockApiDataService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [MockApiDataService]
    });
  });

  it('should create service', inject([MockApiDataService], (service: MockApiDataService) => {
    expect(service).toBeTruthy();
  }));

  describe('#parseUrl()', () => {
    const cases = [
      {
        url: 'root',
        base: '/',
        collectionName: 'root',
        query: '',
        title: 'one-level depth url, no query params'
      },
      {
        url: 'root?param0=1&param1=2',
        base: '/',
        collectionName: 'root',
        query: 'param0=1&param1=2',
        title: 'one-level depth url with query params'
      },
      {
        url: 'root/resources/collection',
        base: 'root/resources/',
        collectionName: 'collection',
        query: '',
        title: 'more than one-level depth url, no query params'
      },
      {
        url: 'root/resources/collection?param0=1&param1=2',
        base: 'root/resources/',
        collectionName: 'collection',
        query: 'param0=1&param1=2',
        title: 'more than one-level depth url with query params'
      }
    ];

    cases.forEach(test => {
      describe(test.title, () => {
        it('base', inject([MockApiDataService], (service: MockApiDataService) => {
          expect(service.parseUrl(test.url).base).toEqual(test.base);
        }));

        it('collectionName', inject([MockApiDataService], (service: MockApiDataService) => {
          expect(service.parseUrl(test.url).collectionName).toEqual(test.collectionName);
        }));

        it('query', inject([MockApiDataService], (service: MockApiDataService) => {
          expect(service.parseUrl(test.url).query.toString()).toEqual(test.query);
        }));
      });
    });
  });
});
