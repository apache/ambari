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
import {HttpModule} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import {StoreModule} from '@ngrx/store';
import {AppStateService, appState} from '@app/services/storage/app-state.service';
import {AuthService} from '@app/services/auth.service';
import {HttpClientService} from '@app/services/http-client.service';

describe('AuthService', () => {

  let successResponse = {
    type: 'default',
    ok: true,
    url: '/',
    status: 200,
    statusText: 'OK',
    bytesLoaded: 100,
    totalBytes: 100,
    headers: null
  };
  let errorResponse = {
    type: 'error',
    ok: false,
    url: '/',
    status: 401,
    statusText: 'ERROR',
    bytesLoaded: 100,
    totalBytes: 100,
    headers: null
  };
  let currentResponse = successResponse;
  let httpServiceStub;
  let authService: AuthService;

  beforeEach(() => {
    // Note: We add delay to help the isLoginInProgress test case.
    httpServiceStub = {
      postFormData: function () {
        return Observable.create(observer => {
          observer.next(currentResponse);
        }).delay(1000);
      },
      post: function () {
        return Observable.create(observer => {
          observer.next(currentResponse);
        }).delay(1000);
      },
      get: function () {
        return Observable.create(observer => {
          observer.next(currentResponse);
        }).delay(1000);
      }
    };
    TestBed.configureTestingModule({
      imports: [
        HttpModule,
        StoreModule.provideStore({
          appState
        })
      ],
      providers: [
        AuthService,
        AppStateService,
        {provide: HttpClientService, useValue: httpServiceStub}
      ]
    });
    authService = TestBed.get(AuthService);
  });

  it('should create service', inject([AuthService], (service: AuthService) => {
    expect(service).toBeTruthy();
  }));

  it('should set the isAuthorized state to true in appState when the login is success', inject(
    [AppStateService],
    (appStateService: AppStateService) => {
      currentResponse = successResponse;
      authService.login('test', 'test')
        .subscribe(() => {
          appStateService.getParameter('isAuthorized').subscribe((value) => {
            expect(value).toBe(true);
          });
        }, (value) => {
          throw value;
        });
    }
  ));


  it('should set the isAuthorized state to false in appState when the login is failed', inject(
    [AppStateService],
    (appStateService: AppStateService) => {
      currentResponse = errorResponse;
      authService.login('test', 'test')
        .subscribe(() => {
          appStateService.getParameter('isAuthorized').subscribe((value) => {
            expect(value).toBe(false);
          });
        });
    }
  ));

  it('should set the isLoginInProgress state to true when the login started.', inject(
    [AppStateService],
    (appStateService: AppStateService) => {
      currentResponse = successResponse;
      authService.login('test', 'test');
      appStateService.getParameter('isLoginInProgress').subscribe((value) => {
        expect(value).toBe(true);
      });
    }
  ));

});
