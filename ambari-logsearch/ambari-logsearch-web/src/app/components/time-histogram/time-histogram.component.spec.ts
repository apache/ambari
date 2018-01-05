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

import {Injector} from '@angular/core';
import {async, ComponentFixture, TestBed, inject} from '@angular/core/testing';
import {StoreModule} from '@ngrx/store';
import {AppSettingsService, appSettings} from '@app/services/storage/app-settings.service';
import {TranslationModules} from '@app/test-config.spec';
import {MomentModule} from 'angular2-moment';
import {MomentTimezoneModule} from 'angular-moment-timezone';
import {ServiceInjector} from '@app/classes/service-injector';
import {TimeZoneAbbrPipe} from '@app/pipes/timezone-abbr.pipe';
import {GraphLegendComponent} from '@app/components/graph-legend/graph-legend.component';
import {GraphLegendItemComponent} from '@app/components/graph-legend-item/graph-legend-item.component';
import {GraphTooltipComponent} from '@app/components/graph-tooltip/graph-tooltip.component';

import {ServiceLogsHistogramDataService} from '@app/services/storage/service-logs-histogram-data.service';
import {TimeHistogramComponent} from './time-histogram.component';
import {LogsContainerService} from '@app/services/logs-container.service';
import {HttpClientService} from '@app/services/http-client.service';
import {UtilsService} from '@app/services/utils.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {AuditLogsService} from '@app/services/storage/audit-logs.service';
import {AuditLogsFieldsService} from '@app/services/storage/audit-logs-fields.service';
import {ServiceLogsService} from '@app/services/storage/service-logs.service';
import {ServiceLogsFieldsService} from '@app/services/storage/service-logs-fields.service';
import {ServiceLogsTruncatedService} from '@app/services/storage/service-logs-truncated.service';
import {TabsService} from '@app/services/storage/tabs.service';
import {ClustersService} from '@app/services/storage/clusters.service';
import {ComponentsService} from '@app/services/storage/components.service';
import {HostsService} from '@app/services/storage/hosts.service';
import {HomogeneousObject} from '@app/classes/object';

describe('TimeHistogramComponent', () => {
  let component: TimeHistogramComponent;
  let fixture: ComponentFixture<TimeHistogramComponent>;
  let histogramData: any;
  let colors: HomogeneousObject<string>;

  beforeEach(async(() => {
    const httpClient = {
      get: () => {
        return {
          subscribe: () => {}
        }
      }
    };
    histogramData = {
      1512476481940: {
        FATAL: 0,
        ERROR: 1000,
        WARN: 700,
        INFO: 0,
        DEBUG: 0,
        TRACE: 0,
        UNKNOWN: 0
      },
      1512472881940: {
        FATAL: 0,
        ERROR: 2000,
        WARN: 900,
        INFO: 0,
        DEBUG: 0,
        TRACE: 0,
        UNKNOWN: 0
      }
    };
    colors = {
      FATAL: '#830A0A',
      ERROR: '#E81D1D',
      WARN: '#FF8916',
      INFO: '#2577B5',
      DEBUG: '#65E8FF',
      TRACE: '#888',
      UNKNOWN: '#BDBDBD'
    };
    TestBed.configureTestingModule({
      declarations: [
        TimeHistogramComponent,
        GraphLegendComponent,
        GraphLegendItemComponent,
        GraphTooltipComponent,
        TimeZoneAbbrPipe
      ],
      imports: [
        StoreModule.provideStore({
          appSettings
        }),
        ...TranslationModules,
        MomentModule,
        MomentTimezoneModule
      ],
      providers: [
        AppSettingsService,
        ServiceLogsHistogramDataService,
        LogsContainerService,
        {
          provide: HttpClientService,
          useValue: httpClient
        },
        UtilsService,
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
        HostsService
      ]
    })
      .compileComponents();
  }));

  beforeEach(inject([Injector], (injector: Injector) => {
    ServiceInjector.injector = injector;
    fixture = TestBed.createComponent(TimeHistogramComponent);
    component = fixture.componentInstance;
    component.colors = colors;
    component.svgId = 'HistogramSvg';
    component.data = histogramData;
    fixture.detectChanges();
  }));

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  const getTimeGapTestCases = [{
    startDate: new Date(2017, 0, 1),
    endDate: new Date(2017, 0, 8),
    expected: {
      unit: 'week',
      value: 1,
      label: 'histogram.gap.week'
    }
  }, {
    startDate: new Date(2017, 0, 1),
    endDate: new Date(2017, 0, 2),
    expected: {
      unit: 'day',
      value: 1,
      label: 'histogram.gap.day'
    }
  }, {
    startDate: new Date(2017, 0, 1, 1),
    endDate: new Date(2017, 0, 1, 2),
    expected: {
      unit: 'hour',
      value: 1,
      label: 'histogram.gap.hour'
    }
  }, {
    startDate: new Date(2017, 0, 1, 1, 1),
    endDate: new Date(2017, 0, 1, 1, 2),
    expected: {
      unit: 'minute',
      value: 1,
      label: 'histogram.gap.minute'
    }
  }, {
    startDate: new Date(2017, 0, 1, 1, 1, 1),
    endDate: new Date(2017, 0, 1, 1, 1, 11),
    expected: {
      unit: 'second',
      value: 10,
      label: 'histogram.gap.seconds'
    }
  }];

  getTimeGapTestCases.forEach((test) => {
    it(`should the getTimeGap return with the proper time gap obj for ${test.expected.value} ${test.expected.unit} difference`, () => {
      const getTimeGap: (startDate: Date, endDate: Date) => {value: number, unit: string} = component['getTimeGap'];
      const gap = getTimeGap(test.startDate, test.endDate);
      expect(gap).toEqual(test.expected);
    });
  });

});
