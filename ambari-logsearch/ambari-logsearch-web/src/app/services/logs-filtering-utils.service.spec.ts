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
import { TestBed, inject } from '@angular/core/testing';

import { LogsFilteringUtilsService } from './logs-filtering-utils.service';
import {StoreModule} from '@ngrx/store';
import {serviceLogsTruncated} from '@app/services/storage/service-logs-truncated.service';
import {RouterTestingModule} from '@angular/router/testing';
import {serviceLogs} from '@app/services/storage/service-logs.service';
import {hosts} from '@app/services/storage/hosts.service';
import {auditLogsFields} from '@app/services/storage/audit-logs-fields.service';
import {auditLogsGraphData} from '@app/services/storage/audit-logs-graph-data.service';
import {TranslationModules} from '@app/test-config.spec';
import {tabs} from '@app/services/storage/tabs.service';
import {serviceLogsHistogramData} from '@app/services/storage/service-logs-histogram-data.service';
import {clusters} from '@app/services/storage/clusters.service';
import {auditLogs} from '@app/services/storage/audit-logs.service';
import {appState} from '@app/services/storage/app-state.service';
import {components} from '@app/services/storage/components.service';
import {serviceLogsFields} from '@app/services/storage/service-logs-fields.service';
import {appSettings} from '@app/services/storage/app-settings.service';
import {UtilsService} from '@app/services/utils.service';

describe('LogsFilteringUtilsService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
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
        }),
        ...TranslationModules
      ],
      providers: [
        LogsFilteringUtilsService,
        UtilsService
      ]
    });
  });

  it('should be created', inject([LogsFilteringUtilsService], (service: LogsFilteringUtilsService) => {
    expect(service).toBeTruthy();
  }));
});
