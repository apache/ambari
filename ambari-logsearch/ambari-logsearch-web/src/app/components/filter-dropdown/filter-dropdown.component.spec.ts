/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
import {StoreModule} from '@ngrx/store';
import {ServiceInjector} from '@app/classes/service-injector';
import {AppSettingsService, appSettings} from '@app/services/storage/app-settings.service';
import {AppStateService, appState} from '@app/services/storage/app-state.service';
import {AuditLogsService, auditLogs} from '@app/services/storage/audit-logs.service';
import {AuditLogsFieldsService, auditLogsFields} from '@app/services/storage/audit-logs-fields.service';
import {ServiceLogsService, serviceLogs} from '@app/services/storage/service-logs.service';
import {ServiceLogsFieldsService, serviceLogsFields} from '@app/services/storage/service-logs-fields.service';
import {
  ServiceLogsHistogramDataService, serviceLogsHistogramData
} from '@app/services/storage/service-logs-histogram-data.service';
import {ServiceLogsTruncatedService, serviceLogsTruncated} from '@app/services/storage/service-logs-truncated.service';
import {TabsService, tabs} from '@app/services/storage/tabs.service';
import {ClustersService, clusters} from '@app/services/storage/clusters.service';
import {ComponentsService, components} from '@app/services/storage/components.service';
import {HostsService, hosts} from '@app/services/storage/hosts.service';
import {UtilsService} from '@app/services/utils.service';
import {ComponentActionsService} from '@app/services/component-actions.service';
import {LogsContainerService} from '@app/services/logs-container.service';
import {HttpClientService} from '@app/services/http-client.service';
import {AuthService} from '@app/services/auth.service';

import {FilterDropdownComponent} from './filter-dropdown.component';

describe('FilterDropdownComponent', () => {
  let component: FilterDropdownComponent;
  let fixture: ComponentFixture<FilterDropdownComponent>;
  const filtering = {
    filters: {
      f: {
        options: [
          {
            value: 'v0',
            label: 'l0'
          },
          {
            value: 'v1',
            label: 'l1'
          }
        ]
      }
    }
  };

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
      declarations: [FilterDropdownComponent],
      imports: [
        StoreModule.provideStore({
          appSettings,
          appState,
          auditLogs,
          auditLogsFields,
          serviceLogs,
          serviceLogsFields,
          serviceLogsHistogramData,
          serviceLogsTruncated,
          tabs,
          clusters,
          components,
          hosts
        }),
        ...TranslationModules
      ],
      providers: [
        AppSettingsService,
        AppStateService,
        AuditLogsService,
        AuditLogsFieldsService,
        ServiceLogsService,
        ServiceLogsFieldsService,
        ServiceLogsHistogramDataService,
        ServiceLogsTruncatedService,
        TabsService,
        ClustersService,
        ComponentsService,
        HostsService,
        {
          provide: LogsContainerService,
          useValue: filtering
        },
        UtilsService,
        ComponentActionsService,
        LogsContainerService,
        {
          provide: HttpClientService,
          useValue: httpClient
        },
        AuthService
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(inject([Injector], (injector: Injector) => {
    ServiceInjector.injector = injector;
    fixture = TestBed.createComponent(FilterDropdownComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

});
