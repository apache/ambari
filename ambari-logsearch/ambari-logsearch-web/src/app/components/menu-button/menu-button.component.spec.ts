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

import {NO_ERRORS_SCHEMA, Injector} from '@angular/core';
import {async, ComponentFixture, TestBed, inject} from '@angular/core/testing';
import {TranslationModules} from '@app/test-config.spec';
import {ServiceInjector} from '@app/classes/service-injector';
import {StoreModule} from '@ngrx/store';
import {AppSettingsService, appSettings} from '@app/services/storage/app-settings.service';
import {AppStateService, appState} from '@app/services/storage/app-state.service';
import {ClustersService, clusters} from '@app/services/storage/clusters.service';
import {ComponentsService, components} from '@app/services/storage/components.service';
import {HostsService, hosts} from '@app/services/storage/hosts.service';
import {AuditLogsService, auditLogs} from '@app/services/storage/audit-logs.service';
import {AuditLogsFieldsService, auditLogsFields} from '@app/services/storage/audit-logs-fields.service';
import {ServiceLogsService, serviceLogs} from '@app/services/storage/service-logs.service';
import {ServiceLogsFieldsService, serviceLogsFields} from '@app/services/storage/service-logs-fields.service';
import {ServiceLogsHistogramDataService, serviceLogsHistogramData} from '@app/services/storage/service-logs-histogram-data.service';
import {ServiceLogsTruncatedService, serviceLogsTruncated} from '@app/services/storage/service-logs-truncated.service';
import {TabsService, tabs} from '@app/services/storage/tabs.service';
import {ComponentActionsService} from '@app/services/component-actions.service';
import {HttpClientService} from '@app/services/http-client.service';
import {LogsContainerService} from '@app/services/logs-container.service';
import {AuthService} from '@app/services/auth.service';

import {MenuButtonComponent} from './menu-button.component';

describe('MenuButtonComponent', () => {
  let component: MenuButtonComponent;
  let fixture: ComponentFixture<MenuButtonComponent>;

  beforeEach(async(() => {
    const httpClient = {
      get: () => {
        return {
          subscribe: () => {
          }
        }
      }
    };
    TestBed.configureTestingModule({
      declarations: [MenuButtonComponent],
      imports: [
        StoreModule.provideStore({
          appSettings,
          appState,
          clusters,
          components,
          hosts,
          auditLogs,
          auditLogsFields,
          serviceLogs,
          serviceLogsFields,
          serviceLogsHistogramData,
          serviceLogsTruncated,
          tabs
        }),
        ...TranslationModules
      ],
      providers: [
        AppSettingsService,
        AppStateService,
        ClustersService,
        ComponentsService,
        HostsService,
        AuditLogsService,
        AuditLogsFieldsService,
        ServiceLogsService,
        ServiceLogsFieldsService,
        ServiceLogsHistogramDataService,
        ServiceLogsTruncatedService,
        TabsService,
        ComponentActionsService,
        {
          provide: HttpClientService,
          useValue: httpClient
        },
        LogsContainerService,
        AuthService
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(inject([Injector], (injector: Injector) => {
    ServiceInjector.injector = injector;
    fixture = TestBed.createComponent(MenuButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  describe('#hasSubItems', () => {
    const cases = [
      {
        subItems: null,
        hasSubItems: false,
        title: 'no sub-items'
      },
      {
        subItems: [],
        hasSubItems: false,
        title: 'empty sub-items array'
      },
      {
        subItems: [
          {
            value: null
          }
        ],
        hasSubItems: true,
        title: 'sub-items present'
      }
    ];

    cases.forEach((test) => {
      it(test.title, () => {
        component.subItems = test.subItems;
        expect(component.hasSubItems).toEqual(test.hasSubItems);
      });
    });
  });

  describe('#hasCaret', () => {
    const cases = [
      {
        subItems: null,
        hideCaret: false,
        hasCaret: false,
        title: 'no sub-items'
      },
      {
        subItems: [],
        hideCaret: false,
        hasCaret: false,
        title: 'empty sub-items array'
      },
      {
        subItems: [
          {
            value: null
          }
        ],
        hideCaret: false,
        hasCaret: true,
        title: 'sub-items present, caret not hidden'
      },
      {
        subItems: [
          {
            value: null
          }
        ],
        hideCaret: true,
        hasCaret: true,
        title: 'sub-items present, caret hidden'
      }
    ];

    cases.forEach((test) => {
      it(test.title, () => {
        component.subItems = test.subItems;
        component.hideCaret = test.hideCaret;
        expect(component.hasSubItems).toEqual(test.hasCaret);
      });
    });
  });
});
