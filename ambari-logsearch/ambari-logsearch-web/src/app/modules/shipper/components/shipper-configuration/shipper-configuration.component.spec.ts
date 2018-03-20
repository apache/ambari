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

import {ShipperConfigurationComponent} from './shipper-configuration.component';
import {StoreModule} from '@ngrx/store';
import {auditLogs, AuditLogsService} from '@app/services/storage/audit-logs.service';
import {serviceLogsTruncated, ServiceLogsTruncatedService} from '@app/services/storage/service-logs-truncated.service';
import {components, ComponentsService} from '@app/services/storage/components.service';
import {UtilsService} from '@app/services/utils.service';
import {tabs, TabsService} from '@app/services/storage/tabs.service';
import {serviceLogs, ServiceLogsService} from '@app/services/storage/service-logs.service';
import {hosts, HostsService} from '@app/services/storage/hosts.service';
import {MockHttpRequestModules, TranslationModules} from '@app/test-config.spec';
import {ComponentGeneratorService} from '@app/services/component-generator.service';
import {auditLogsGraphData, AuditLogsGraphDataService} from '@app/services/storage/audit-logs-graph-data.service';
import {serviceLogsHistogramData, ServiceLogsHistogramDataService} from '@app/services/storage/service-logs-histogram-data.service';
import {clusters, ClustersService} from '@app/services/storage/clusters.service';
import {AuditLogsFieldsService, auditLogsFields} from '@app/services/storage/audit-logs-fields.service';
import {appSettings, AppSettingsService} from '@app/services/storage/app-settings.service';
import {appState, AppStateService} from '@app/services/storage/app-state.service';
import {ClusterSelectionService} from '@app/services/storage/cluster-selection.service';
import {serviceLogsFields, ServiceLogsFieldsService} from '@app/services/storage/service-logs-fields.service';
import {LogsContainerService} from '@app/services/logs-container.service';
import {ShipperRoutingModule} from '@modules/shipper/shipper-routing.module';
import {ShipperClusterServiceListComponent} from '@modules/shipper/components/shipper-cluster-service-list/shipper-cluster-service-list.component';
import {ShipperServiceConfigurationFormComponent} from '@modules/shipper/components/shipper-service-configuration-form/shipper-service-configuration-form.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {TypeaheadModule} from 'ngx-bootstrap';
import {DisableControlDirective} from '@modules/shared/directives/disable-control.directive';
import {ModalComponent} from '@modules/shared/components/modal/modal.component';
import {RouterTestingModule} from '@angular/router/testing';
import {ShipperClusterServiceListService} from '@modules/shipper/services/shipper-cluster-service-list.service';
import {ShipperConfigurationService} from '@modules/shipper/services/shipper-configuration.service';
import {NotificationService} from '@modules/shared/services/notification.service';
import {NotificationsService} from 'angular2-notifications/src/notifications.service';

describe('ShipperConfigurationComponent', () => {
  let component: ShipperConfigurationComponent;
  let fixture: ComponentFixture<ShipperConfigurationComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        ShipperRoutingModule,
        StoreModule.provideStore({
          hosts,
          auditLogs,
          serviceLogs,
          auditLogsGraphData,
          auditLogsFields,
          serviceLogsFields,
          serviceLogsHistogramData,
          appSettings,
          appState,
          clusters,
          components,
          serviceLogsTruncated,
          tabs
        }),
        ...TranslationModules,
        FormsModule,
        ReactiveFormsModule,
        TypeaheadModule.forRoot()
      ],
      providers: [
        ...MockHttpRequestModules,
        ComponentGeneratorService,
        LogsContainerService,
        UtilsService,
        HostsService,
        AuditLogsService,
        ServiceLogsService,
        AuditLogsFieldsService,
        AuditLogsGraphDataService,
        ServiceLogsFieldsService,
        ServiceLogsHistogramDataService,
        AppSettingsService,
        AppStateService,
        ClustersService,
        ComponentsService,
        ServiceLogsTruncatedService,
        TabsService,
        ComponentGeneratorService,
        ClusterSelectionService,
        ShipperClusterServiceListService,
        ShipperConfigurationService,
        NotificationsService,
        NotificationService
      ],
      declarations: [
        ShipperConfigurationComponent,
        ShipperClusterServiceListComponent,
        ShipperServiceConfigurationFormComponent,
        DisableControlDirective,
        ModalComponent
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ShipperConfigurationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
