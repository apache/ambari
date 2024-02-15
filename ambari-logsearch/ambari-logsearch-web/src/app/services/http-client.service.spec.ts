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
import {HttpModule, Request} from '@angular/http';
import {StoreModule} from '@ngrx/store';
import {AppStateService, appState} from '@app/services/storage/app-state.service';
import {HttpClientService} from './http-client.service';

describe('HttpClientService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpModule,
        StoreModule.provideStore({
          appState
        })
      ],
      providers: [
        HttpClientService,
        AppStateService
      ]
    });
  });

  it('should create service', inject([HttpClientService], (service: HttpClientService) => {
    expect(service).toBeTruthy();
  }));

  describe('#generateUrlString()', () => {
    it('should generate URL from presets', inject([HttpClientService], (service: HttpClientService) => {
      expect(service['generateUrlString']('status')).toEqual('api/v1/status');
    }));

    it('should return explicit URL', inject([HttpClientService], (service: HttpClientService) => {
      expect(service['generateUrlString']('login')).toEqual('login');
    }));
  });

  describe('#generateUrl()', () => {
    it('string parameter', inject([HttpClientService], (service: HttpClientService) => {
      expect(service['generateUrl']('status')).toEqual('api/v1/status');
    }));

    it('request object parameter', inject([HttpClientService], (service: HttpClientService) => {
      let request = new Request({
        url: 'status'
      });
      expect(service['generateUrl'](request)['url']).toEqual('api/v1/status');
    }));
  });
});
