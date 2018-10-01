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
import {TooltipModule} from 'ngx-bootstrap';
import {MockHttpRequestModules, TranslationModules} from '@app/test-config.spec';
import {AuditLogsService, auditLogs} from '@app/services/storage/audit-logs.service';
import {ServiceLogsService, serviceLogs} from '@app/services/storage/service-logs.service';
import {AuditLogsFieldsService, auditLogsFields} from '@app/services/storage/audit-logs-fields.service';
import {AuditLogsGraphDataService, auditLogsGraphData} from '@app/services/storage/audit-logs-graph-data.service';
import {ServiceLogsFieldsService, serviceLogsFields} from '@app/services/storage/service-logs-fields.service';
import {
  ServiceLogsHistogramDataService, serviceLogsHistogramData
} from '@app/services/storage/service-logs-histogram-data.service';
import {ServiceLogsTruncatedService, serviceLogsTruncated} from '@app/services/storage/service-logs-truncated.service';
import {AppStateService, appState} from '@app/services/storage/app-state.service';
import {AppSettingsService, appSettings} from '@app/services/storage/app-settings.service';
import {TabsService, tabs} from '@app/services/storage/tabs.service';
import {ClustersService, clusters} from '@app/services/storage/clusters.service';
import {ComponentsService, components} from '@app/services/storage/components.service';
import {HostsService, hosts} from '@app/services/storage/hosts.service';
import {LogsContainerService} from '@app/services/logs-container.service';
import {UtilsService} from '@app/services/utils.service';
import {ComponentGeneratorService} from '@app/services/component-generator.service';
import {AuthService} from '@app/services/auth.service';
import {PaginationComponent} from '@app/components/pagination/pagination.component';
import {DropdownListComponent} from '@modules/shared/components/dropdown-list/dropdown-list.component';

import {ServiceLogsTableComponent, ListLayout} from './service-logs-table.component';
import {ComponentLabelPipe} from "@app/pipes/component-label";
import {ClusterSelectionService} from '@app/services/storage/cluster-selection.service';
import {LogsStateService} from '@app/services/storage/logs-state.service';
import {RoutingUtilsService} from '@app/services/routing-utils.service';
import {LogsFilteringUtilsService} from '@app/services/logs-filtering-utils.service';
import {RouterTestingModule} from '@angular/router/testing';
import {NotificationsService} from 'angular2-notifications/src/notifications.service';
import {NotificationService} from '@modules/shared/services/notification.service';

describe('ServiceLogsTableComponent', () => {
  let component: ServiceLogsTableComponent;
  let fixture: ComponentFixture<ServiceLogsTableComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        ServiceLogsTableComponent,
        PaginationComponent,
        DropdownListComponent,
        ComponentLabelPipe
      ],
      imports: [
        RouterTestingModule,
        FormsModule,
        ReactiveFormsModule,
        MomentModule,
        MomentTimezoneModule,
        ...TranslationModules,
        StoreModule.provideStore({
          auditLogs,
          serviceLogs,
          auditLogsFields,
          auditLogsGraphData,
          serviceLogsFields,
          serviceLogsHistogramData,
          serviceLogsTruncated,
          appState,
          appSettings,
          tabs,
          clusters,
          components,
          hosts
        }),
        TooltipModule.forRoot()
      ],
      providers: [
        ...MockHttpRequestModules,
        LogsContainerService,
        UtilsService,
        AuditLogsService,
        ServiceLogsService,
        AuditLogsFieldsService,
        AuditLogsGraphDataService,
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
        AuthService,
        ClusterSelectionService,
        RoutingUtilsService,
        LogsFilteringUtilsService,
        LogsStateService,
        NotificationsService,
        NotificationService
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

  it('should change the layout to TABLE', () => {
    component.setLayout(ListLayout.Table);
    expect(component.layout).toEqual(ListLayout.Table);
  });

  it('should change the layout to FLEX', () => {
    component.setLayout(ListLayout.Flex);
    expect(component.layout).toEqual(ListLayout.Flex);
  });

});
