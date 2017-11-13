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

import {NO_ERRORS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {TranslationModules} from '@app/test-config.spec';
import {StoreModule} from '@ngrx/store';
import {MomentModule} from 'angular2-moment';
import {MomentTimezoneModule} from 'angular-moment-timezone';
import {AuditLogsService, auditLogs} from '@app/services/storage/audit-logs.service';
import {ServiceLogsService, serviceLogs} from '@app/services/storage/service-logs.service';
import {AppSettingsService, appSettings} from '@app/services/storage/app-settings.service';
import {AppStateService, appState} from '@app/services/storage/app-state.service';
import {ClustersService, clusters} from '@app/services/storage/clusters.service';
import {ComponentsService, components} from '@app/services/storage/components.service';
import {HostsService, hosts} from '@app/services/storage/hosts.service';
import {AuditLogsFieldsService, auditLogsFields} from '@app/services/storage/audit-logs-fields.service';
import {ServiceLogsFieldsService, serviceLogsFields} from '@app/services/storage/service-logs-fields.service';
import {ServiceLogsHistogramDataService, serviceLogsHistogramData} from '@app/services/storage/service-logs-histogram-data.service';
import {ServiceLogsTruncatedService, serviceLogsTruncated} from '@app/services/storage/service-logs-truncated.service';
import {TabsService, tabs} from '@app/services/storage/tabs.service';
import {HttpClientService} from '@app/services/http-client.service';
import {LogsContainerService} from '@app/services/logs-container.service';
import {UtilsService} from '@app/services/utils.service';

import {LogsListComponent} from './logs-list.component';

describe('LogsListComponent', () => {
  let component: LogsListComponent;
  let fixture: ComponentFixture<LogsListComponent>;
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
      declarations: [LogsListComponent],
      imports: [
        StoreModule.provideStore({
          auditLogs,
          serviceLogs,
          appSettings,
          appState,
          clusters,
          components,
          hosts,
          auditLogsFields,
          serviceLogsFields,
          serviceLogsHistogramData,
          serviceLogsTruncated,
          tabs
        }),
        MomentModule,
        MomentTimezoneModule,
        ...TranslationModules
      ],
      providers: [
        {
          provide: HttpClientService,
          useValue: httpClient
        },
        AuditLogsService,
        ServiceLogsService,
        AppSettingsService,
        AppStateService,
        ClustersService,
        ComponentsService,
        HostsService,
        AuditLogsFieldsService,
        ServiceLogsFieldsService,
        ServiceLogsHistogramDataService,
        ServiceLogsTruncatedService,
        TabsService,
        LogsContainerService,
        UtilsService
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LogsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });
});
