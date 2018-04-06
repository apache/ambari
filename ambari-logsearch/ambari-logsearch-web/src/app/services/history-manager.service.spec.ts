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

import {TestBed, inject} from '@angular/core/testing';
import {StoreModule} from '@ngrx/store';
import {AuditLogsService, auditLogs} from '@app/services/storage/audit-logs.service';
import {ServiceLogsService, serviceLogs} from '@app/services/storage/service-logs.service';
import {AuditLogsFieldsService, auditLogsFields} from '@app/services/storage/audit-logs-fields.service';
import {AuditLogsGraphDataService, auditLogsGraphData} from '@app/services/storage/audit-logs-graph-data.service';
import {ServiceLogsFieldsService, serviceLogsFields} from '@app/services/storage/service-logs-fields.service';
import {
  ServiceLogsHistogramDataService, serviceLogsHistogramData
} from '@app/services/storage/service-logs-histogram-data.service';
import {AppSettingsService, appSettings} from '@app/services/storage/app-settings.service';
import {AppStateService, appState} from '@app/services/storage/app-state.service';
import {ClustersService, clusters} from '@app/services/storage/clusters.service';
import {ComponentsService, components} from '@app/services/storage/components.service';
import {HostsService, hosts} from '@app/services/storage/hosts.service';
import {ServiceLogsTruncatedService, serviceLogsTruncated} from '@app/services/storage/service-logs-truncated.service';
import {TabsService, tabs} from '@app/services/storage/tabs.service';
import {LogsContainerService} from '@app/services/logs-container.service';
import {UtilsService} from '@app/services/utils.service';

import {HistoryManagerService} from './history-manager.service';

import {MockHttpRequestModules, TranslationModules} from '@app/test-config.spec';
import {ClusterSelectionService} from '@app/services/storage/cluster-selection.service';
import {RouterTestingModule} from '@angular/router/testing';
import {RoutingUtilsService} from '@app/services/routing-utils.service';
import {LogsFilteringUtilsService} from '@app/services/logs-filtering-utils.service';
import {LogsStateService} from '@app/services/storage/logs-state.service';

describe('HistoryService', () => {
  beforeEach(() => {

    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        ...TranslationModules,
        StoreModule.provideStore({
          auditLogs,
          serviceLogs,
          auditLogsFields,
          auditLogsGraphData,
          serviceLogsFields,
          serviceLogsHistogramData,
          appSettings,
          appState,
          clusters,
          components,
          hosts,
          serviceLogsTruncated,
          tabs
        })
      ],
      providers: [
        ...MockHttpRequestModules,
        HistoryManagerService,
        LogsContainerService,
        UtilsService,
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
        HostsService,
        ServiceLogsTruncatedService,
        TabsService,
        ClusterSelectionService,
        RoutingUtilsService,
        LogsFilteringUtilsService,
        LogsStateService
      ]
    });
  });

  it('should be created', inject([HistoryManagerService], (service: HistoryManagerService) => {
    expect(service).toBeTruthy();
  }));

  describe('#isHistoryUnchanged()', () => {
    const cases = [
      {
        valueA: {
          p0: 'v0',
          p1: ['v1'],
          p2: {
            k2: 'v2'
          }
        },
        valueB: {
          p0: 'v0',
          p1: ['v1'],
          p2: {
            k2: 'v2'
          }
        },
        result: true,
        title: 'no difference'
      },
      {
        valueA: {
          p0: 'v0',
          p1: ['v1'],
          p2: {
            k2: 'v2'
          },
          page: 0
        },
        valueB: {
          p0: 'v0',
          p1: ['v1'],
          p2: {
            k2: 'v2'
          },
          page: 1
        },
        result: true,
        title: 'difference in ignored parameters'
      },
      {
        valueA: {
          p0: 'v0',
          p1: ['v1'],
          p2: {
            k2: 'v2'
          },
          page: 0
        },
        valueB: {
          p0: 'v0',
          p1: ['v3'],
          p2: {
            k2: 'v4'
          },
          page: 1
        },
        result: false,
        title: 'difference in non-ignored parameters'
      }
    ];

    cases.forEach(test => {
      it(test.title, inject([HistoryManagerService], (service: HistoryManagerService) => {
        const isHistoryUnchanged: (valueA: object, valueB: object) => boolean = service['isHistoryUnchanged'];
        expect(isHistoryUnchanged(test.valueA, test.valueB)).toEqual(test.result);
      }));
    });
  });
});
