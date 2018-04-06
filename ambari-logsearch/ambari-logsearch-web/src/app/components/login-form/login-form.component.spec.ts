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

import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {TranslationModules} from '@app/test-config.spec';
import {StoreModule} from '@ngrx/store';
import {AppStateService, appState} from '@app/services/storage/app-state.service';
import {HttpClientService} from '@app/services/http-client.service';
import {AuthService} from '@app/services/auth.service';

import {LoginFormComponent} from './login-form.component';
import {RouterTestingModule} from '@angular/router/testing';

describe('LoginFormComponent', () => {
  let component: LoginFormComponent;
  let fixture: ComponentFixture<LoginFormComponent>;

  const authMock = {
    isError: false
  };
  const httpClient = {
    isAuthorized: true,
    postFormData: () => {
      return {
        subscribe: (success: () => void, error: () => void) => {
          authMock.isError ? error() : success();
        }
      };
    }
  };

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [LoginFormComponent],
      imports: [
        RouterTestingModule,
        FormsModule,
        ...TranslationModules,
        StoreModule.provideStore({
          appState
        })
      ],
      providers: [
        AppStateService,
        {
          provide: HttpClientService,
          useValue: httpClient
        },
        AuthService
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LoginFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  describe('#login()', () => {
    const cases = [
      {
        isError: true,
        isLoginAlertDisplayed: true,
        isAuthorized: false,
        title: 'login failure'
      },
      {
        isError: false,
        isLoginAlertDisplayed: false,
        isAuthorized: true,
        title: 'login success'
      }
    ];

    cases.forEach(test => {
      describe(test.title, () => {
        beforeEach(() => {
          authMock.isError = test.isError;
          component.login();
        });

        it('isLoginAlertDisplayed', () => {
          expect(component.isLoginAlertDisplayed).toEqual(test.isLoginAlertDisplayed);
        });

      });
    });

  });
});
