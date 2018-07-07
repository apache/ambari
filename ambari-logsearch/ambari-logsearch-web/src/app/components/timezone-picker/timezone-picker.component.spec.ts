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
import {StoreModule} from '@ngrx/store';
import {AppSettingsService, appSettings} from '@app/services/storage/app-settings.service';
import {AppStateService, appState} from '@app/services/storage/app-state.service';
import {ClustersService, clusters} from '@app/services/storage/clusters.service';
import {ComponentsService, components} from '@app/services/storage/components.service';
import {HostsService, hosts} from '@app/services/storage/hosts.service';
import {AuditLogsService, auditLogs} from '@app/services/storage/audit-logs.service';
import {AuditLogsFieldsService, auditLogsFields} from '@app/services/storage/audit-logs-fields.service';
import {AuditLogsGraphDataService, auditLogsGraphData} from '@app/services/storage/audit-logs-graph-data.service';
import {ServiceLogsService, serviceLogs} from '@app/services/storage/service-logs.service';
import {ServiceLogsFieldsService, serviceLogsFields} from '@app/services/storage/service-logs-fields.service';
import {
  ServiceLogsHistogramDataService, serviceLogsHistogramData
} from '@app/services/storage/service-logs-histogram-data.service';
import {ServiceLogsTruncatedService, serviceLogsTruncated} from '@app/services/storage/service-logs-truncated.service';
import {TabsService, tabs} from '@app/services/storage/tabs.service';
import {HttpClientService} from '@app/services/http-client.service';
import {LogsContainerService} from '@app/services/logs-container.service';
import {UserSettingsService} from '@app/services/user-settings.service';
import {UtilsService} from '@app/services/utils.service';
import {AuthService} from '@app/services/auth.service';
import {TimeZoneAbbrPipe} from '@app/pipes/timezone-abbr.pipe';
import {ModalComponent} from '@app/modules/shared/components/modal/modal.component';

import {MockHttpRequestModules, TranslationModules} from '@app/test-config.spec';

import {TimeZonePickerComponent} from './timezone-picker.component';
import {ClusterSelectionService} from '@app/services/storage/cluster-selection.service';
import {RouterTestingModule} from '@angular/router/testing';
import {LogsStateService} from '@app/services/storage/logs-state.service';
import {RoutingUtilsService} from '@app/services/routing-utils.service';
import {LogsFilteringUtilsService} from '@app/services/logs-filtering-utils.service';
import {NotificationsService} from 'angular2-notifications/src/notifications.service';
import {NotificationService} from '@modules/shared/services/notification.service';

describe('TimeZonePickerComponent', () => {
  let component: TimeZonePickerComponent;
  let fixture: ComponentFixture<TimeZonePickerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        TimeZonePickerComponent,
        ModalComponent,
        TimeZoneAbbrPipe
      ],
      imports: [
        RouterTestingModule,
        StoreModule.provideStore({
          appSettings,
          appState,
          clusters,
          components,
          hosts,
          auditLogs,
          auditLogsFields,
          auditLogsGraphData,
          serviceLogs,
          serviceLogsFields,
          serviceLogsHistogramData,
          serviceLogsTruncated,
          tabs
        }),
        ...TranslationModules
      ],
      providers: [
        ...MockHttpRequestModules,
        AppSettingsService,
        AppStateService,
        ClustersService,
        ComponentsService,
        HostsService,
        AuditLogsService,
        AuditLogsFieldsService,
        AuditLogsGraphDataService,
        ServiceLogsService,
        ServiceLogsFieldsService,
        ServiceLogsHistogramDataService,
        ServiceLogsTruncatedService,
        TabsService,
        LogsContainerService,
        AuthService,
        UserSettingsService,
        UtilsService,
        ClusterSelectionService,
        RoutingUtilsService,
        LogsFilteringUtilsService,
        LogsStateService,
        NotificationsService,
        NotificationService
      ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TimeZonePickerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });
});
