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
import {CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {StoreModule} from '@ngrx/store';
import {AppSettingsService, appSettings} from '@app/services/storage/app-settings.service';
import {ClustersService, clusters} from '@app/services/storage/clusters.service';
import {ComponentsService, components} from '@app/services/storage/components.service';
import {ServiceLogsService, serviceLogs} from '@app/services/storage/service-logs.service';
import {ServiceLogsHistogramDataService, serviceLogsHistogramData} from '@app/services/storage/service-logs-histogram-data.service';
import {HttpClientService} from '@app/services/http-client.service';
import {FilteringService} from '@app/services/filtering.service';
import {UtilsService} from '@app/services/utils.service';

import {LogsContainerComponent} from './logs-container.component';

describe('LogsContainerComponent', () => {
  const httpClient = {
    get: () => {
      return {
        subscribe: () => {
        }
      };
    }
  };
  let component: LogsContainerComponent;
  let fixture: ComponentFixture<LogsContainerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [LogsContainerComponent],
      imports: [
        StoreModule.provideStore({
          appSettings,
          clusters,
          components,
          serviceLogs,
          serviceLogsHistogramData
        })
      ],
      providers: [
        {
          provide: HttpClientService,
          useValue: httpClient
        },
        AppSettingsService,
        ClustersService,
        ComponentsService,
        ServiceLogsService,
        ServiceLogsHistogramDataService,
        FilteringService,
        UtilsService
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LogsContainerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });
});
