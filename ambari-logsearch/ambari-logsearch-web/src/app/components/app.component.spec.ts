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

import {CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {TestBed, async} from '@angular/core/testing';
import {StoreModule} from '@ngrx/store';
import {TranslationModules} from '@app/test-config.spec';
import {AppStateService, appState} from '@app/services/storage/app-state.service';
import {HttpClientService} from '@app/services/http-client.service';

import {AppComponent} from './app.component';

describe('AppComponent', () => {
  beforeEach(async(() => {
    const httpClient = {
      get: () => {
        return {
          subscribe: () => {}
        }
      }
    };
    TestBed.configureTestingModule({
      declarations: [AppComponent],
      imports: [
        StoreModule.provideStore({
          appState
        }),
        ...TranslationModules
      ],
      providers: [
        AppStateService,
        {
          provide: HttpClientService,
          useValue: httpClient
        }
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();
  }));

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  });
});
