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
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ClusterFilterComponent } from './cluster-filter.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {
  getCommonTestingBedConfiguration, MockHttpRequestModules,
  TranslationModules
} from '@app/test-config.spec';
import {FilterDropdownComponent} from '@modules/shared/components/filter-dropdown/filter-dropdown.component';
import {DropdownListComponent} from '@modules/shared/components/dropdown-list/dropdown-list.component';
import {ClusterSelectionService} from '@app/services/storage/cluster-selection.service';
import {RoutingUtilsService} from '@app/services/routing-utils.service';
import {StoreModule} from '@ngrx/store';
import {auditLogs, AuditLogsService} from '@app/services/storage/audit-logs.service';
import {serviceLogsTruncated, ServiceLogsTruncatedService} from '@app/services/storage/service-logs-truncated.service';
import {components, ComponentsService} from '@app/services/storage/components.service';
import {UtilsService} from '@app/services/utils.service';
import {MomentTimezoneModule} from 'angular-moment-timezone';
import {tabs, TabsService} from '@app/services/storage/tabs.service';
import {serviceLogs, ServiceLogsService} from '@app/services/storage/service-logs.service';
import {hosts, HostsService} from '@app/services/storage/hosts.service';
import {MomentModule} from 'angular2-moment';
import {auditLogsGraphData, AuditLogsGraphDataService} from '@app/services/storage/audit-logs-graph-data.service';
import {serviceLogsHistogramData, ServiceLogsHistogramDataService} from '@app/services/storage/service-logs-histogram-data.service';
import {clusters, ClustersService} from '@app/services/storage/clusters.service';
import {auditLogsFields, AuditLogsFieldsService} from '@app/services/storage/audit-logs-fields.service';
import {appSettings, AppSettingsService} from '@app/services/storage/app-settings.service';
import {appState, AppStateService} from '@app/services/storage/app-state.service';
import {serviceLogsFields, ServiceLogsFieldsService} from '@app/services/storage/service-logs-fields.service';
import {LogsContainerService} from '@app/services/logs-container.service';
import {RouterTestingModule} from '@angular/router/testing';
import {LogsStateService} from '@app/services/storage/logs-state.service';
import {LogsFilteringUtilsService} from '@app/services/logs-filtering-utils.service';

describe('ClusterFilterComponent', () => {
  let component: ClusterFilterComponent;
  let fixture: ComponentFixture<ClusterFilterComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule(getCommonTestingBedConfiguration({
      declarations: [
        FilterDropdownComponent,
        DropdownListComponent,
        ClusterFilterComponent
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
        })
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
        ClusterSelectionService,
        RoutingUtilsService,
        LogsFilteringUtilsService,
        LogsStateService
      ]
    }))
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ClusterFilterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
