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
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {StoreModule} from '@ngrx/store';
import {MomentModule} from 'angular2-moment';
import {MomentTimezoneModule} from 'angular-moment-timezone';
import {TranslationModules} from '@app/test-config.spec';
import {AuditLogsService, auditLogs} from '@app/services/storage/audit-logs.service';
import {ServiceLogsService, serviceLogs} from '@app/services/storage/service-logs.service';
import {AuditLogsFieldsService, auditLogsFields} from '@app/services/storage/audit-logs-fields.service';
import {ServiceLogsFieldsService, serviceLogsFields} from '@app/services/storage/service-logs-fields.service';
import {ServiceLogsHistogramDataService, serviceLogsHistogramData} from '@app/services/storage/service-logs-histogram-data.service';
import {ServiceLogsTruncatedService, serviceLogsTruncated} from '@app/services/storage/service-logs-truncated.service';
import {AppStateService, appState} from '@app/services/storage/app-state.service';
import {AppSettingsService, appSettings} from '@app/services/storage/app-settings.service';
import {TabsService, tabs} from '@app/services/storage/tabs.service';
import {ClustersService, clusters} from '@app/services/storage/clusters.service';
import {ComponentsService, components} from '@app/services/storage/components.service';
import {HostsService, hosts} from '@app/services/storage/hosts.service';
import {LogsContainerService} from '@app/services/logs-container.service';
import {UtilsService} from '@app/services/utils.service';
import {HttpClientService} from '@app/services/http-client.service';
import {ComponentGeneratorService} from '@app/services/component-generator.service';
import {ComponentActionsService} from '@app/services/component-actions.service';
import {AuthService} from '@app/services/auth.service';
import {PaginationComponent} from '@app/components/pagination/pagination.component';
import {DropdownListComponent} from '@app/components/dropdown-list/dropdown-list.component';

import {ServiceLogsTableComponent} from './service-logs-table.component';

describe('ServiceLogsTableComponent', () => {
  let component: ServiceLogsTableComponent;
  let fixture: ComponentFixture<ServiceLogsTableComponent>;
  const httpClient = {
    get: () => {
      return {
        subscribe: () => {
        }
      };
    }
  };

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        ServiceLogsTableComponent,
        PaginationComponent,
        DropdownListComponent
      ],
      imports: [
        FormsModule,
        ReactiveFormsModule,
        MomentModule,
        MomentTimezoneModule,
        ...TranslationModules,
        StoreModule.provideStore({
          auditLogs,
          serviceLogs,
          auditLogsFields,
          serviceLogsFields,
          serviceLogsHistogramData,
          serviceLogsTruncated,
          appState,
          appSettings,
          tabs,
          clusters,
          components,
          hosts
        })
      ],
      providers: [
        LogsContainerService,
        UtilsService,
        {
          provide: HttpClientService,
          useValue: httpClient
        },
        AuditLogsService,
        ServiceLogsService,
        AuditLogsFieldsService,
        ServiceLogsFieldsService,
        ServiceLogsHistogramDataService,
        ServiceLogsTruncatedService,
        AppStateService,
        AppSettingsService,
        TabsService,
        ClustersService,
        ComponentsService,
        HostsService,
        ComponentGeneratorService,
        ComponentActionsService,
        AuthService
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServiceLogsTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });
});
