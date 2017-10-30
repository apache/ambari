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

import {Injectable} from '@angular/core';
import {Response} from '@angular/http';
import {HttpClientService} from '@app/services/http-client.service';
import {FilteringService} from '@app/services/filtering.service';
import {AuditLogsService} from '@app/services/storage/audit-logs.service';
import {AuditLogsFieldsService} from '@app/services/storage/audit-logs-fields.service';
import {ServiceLogsService} from '@app/services/storage/service-logs.service';
import {ServiceLogsFieldsService} from '@app/services/storage/service-logs-fields.service';
import {ServiceLogsHistogramDataService} from '@app/services/storage/service-logs-histogram-data.service';
import {ServiceLogsTruncatedService} from '@app/services/storage/service-logs-truncated.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {TabsService} from '@app/services/storage/tabs.service';
import {ActiveServiceLogEntry} from '@app/classes/active-service-log-entry';
import {Tab} from '@app/classes/models/tab';
import {AuditLogField} from '@app/classes/models/audit-log-field';
import {ServiceLogField} from '@app/classes/models/service-log-field';
import {BarGraph} from '@app/classes/models/bar-graph';

@Injectable()
export class LogsContainerService {

  constructor(private httpClient: HttpClientService, private auditLogsStorage: AuditLogsService, private auditLogsFieldsStorage: AuditLogsFieldsService, private serviceLogsStorage: ServiceLogsService, private serviceLogsFieldsStorage: ServiceLogsFieldsService, private serviceLogsHistogramStorage: ServiceLogsHistogramDataService, private serviceLogsTruncatedStorage: ServiceLogsTruncatedService, private appState: AppStateService, private tabsStorage: TabsService, private filtering: FilteringService) {
    appState.getParameter('activeLog').subscribe((value: ActiveServiceLogEntry | null) => this.activeLog = value);
    appState.getParameter('isServiceLogsFileView').subscribe((value: boolean): void => {
      const activeLog = this.activeLog,
        filtersForm = this.filtering.activeFiltersForm;
      if (value && activeLog) {
        filtersForm.controls.hosts.setValue(activeLog.host_name);
        filtersForm.controls.components.setValue(activeLog.component_name);
      }
      this.isServiceLogsFileView = value;
    });
    appState.getParameter('activeLogsType').subscribe((value: string) => this.activeLogsType = value);
  }

  readonly colors = {
    WARN: '#FF8916',
    ERROR: '#E81D1D',
    FATAL: '#830A0A',
    INFO: '#2577B5',
    DEBUG: '#65E8FF',
    TRACE: '#888',
    UNKNOWN: '#BDBDBD'
  };

  private readonly listFilters = {
    clusters: ['clusters'],
    timeRange: ['to', 'from'],
    components: ['mustBe'],
    levels: ['level'],
    hosts: ['hostList'],
    sorting: ['sortType', 'sortBy'],
    pageSize: ['pageSize'],
    page: ['page'],
    query: ['includeQuery', 'excludeQuery']
  };

  private readonly histogramFilters = {
    clusters: ['clusters'],
    timeRange: ['to', 'from'],
    components: ['mustBe'],
    levels: ['level'],
    hosts: ['hostList'],
    query: ['includeQuery', 'excludeQuery']
  };

  readonly logsTypeMap = {
    auditLogs: {
      logsModel: this.auditLogsStorage,
      fieldsModel: this.auditLogsFieldsStorage
    },
    serviceLogs: {
      logsModel: this.serviceLogsStorage,
      fieldsModel: this.serviceLogsFieldsStorage
    }
  };

  totalCount: number = 0;

  isServiceLogsFileView: boolean = false;

  activeLog: ActiveServiceLogEntry | null = null;

  activeLogsType: string;

  loadLogs = (logsType: string = this.activeLogsType): void => {
    this.httpClient.get(logsType, this.getParams('listFilters')).subscribe((response: Response): void => {
      const jsonResponse = response.json(),
        model = this.logsTypeMap[logsType].logsModel;
      model.clear();
      if (jsonResponse) {
        const logs = jsonResponse.logList,
          count = jsonResponse.totalCount || 0;
        if (logs) {
          model.addInstances(logs);
        }
        this.totalCount = count;
      }
    });
    if (logsType === 'serviceLogs') {
      // TODO rewrite to implement conditional data loading for service logs histogram or audit logs graph
      this.httpClient.get('serviceLogsHistogram', this.getParams('histogramFilters')).subscribe((response: Response): void => {
        const jsonResponse = response.json();
        this.serviceLogsHistogramStorage.clear();
        if (jsonResponse) {
          const histogramData = jsonResponse.graphData;
          if (histogramData) {
            this.serviceLogsHistogramStorage.addInstances(histogramData);
          }
        }
      });
    }
  }

  loadLogContext(id: string, hostName: string, componentName: string, scrollType: 'before' | 'after' | '' = ''): void {
    const params = {
      id: id,
      host_name: hostName,
      component_name: componentName,
      scrollType: scrollType
    };
    this.httpClient.get('serviceLogsTruncated', params).subscribe((response: Response): void => {
      const jsonResponse = response.json();
      if (!scrollType) {
        this.serviceLogsTruncatedStorage.clear();
      }
      if (jsonResponse) {
        const logs = jsonResponse.logList;
        if (logs) {
          if (scrollType === 'before') {
            this.serviceLogsTruncatedStorage.addInstancesToStart(logs);
          } else {
            this.serviceLogsTruncatedStorage.addInstances(logs);
          }
          if (!scrollType) {
            this.appState.setParameters({
              isServiceLogContextView: true,
              activeLog: params
            });
          }
        }
      }
    });
  }

  private getParams(filtersMapName: string): any {
    let params = {};
    Object.keys(this[filtersMapName]).forEach((key: string): void => {
      const inputValue = this.filtering.activeFiltersForm.getRawValue()[key],
        paramNames = this[filtersMapName][key];
      paramNames.forEach(paramName => {
        let value;
        const valueGetter = this.filtering.valueGetters[paramName];
        if (valueGetter) {
          if (paramName === 'from') {
            value = valueGetter(inputValue, params['to']);
          } else {
            value = valueGetter(inputValue);
          }
        } else {
          value = inputValue;
        }
        if (value != null && value !== '') {
          params[paramName] = value;
        }
      });
    }, this);
    return params;
  }

  getHistogramData(data: BarGraph[]): {[key: string]: number} {
    let histogramData = {};
    data.forEach(type => {
      const name = type.name;
      type.dataCount.forEach(entry => {
        const timeStamp = new Date(entry.name).valueOf();
        if (!histogramData[timeStamp]) {
          let initialValue = {};
          Object.keys(this.colors).forEach(key => initialValue[key] = 0);
          histogramData[timeStamp] = initialValue;
        }
        histogramData[timeStamp][name] = Number(entry.value);
      });
    });
    return histogramData;
  }

  loadColumnsNames(): void {
    this.httpClient.get('serviceLogsFields').subscribe((response: Response): void => {
      const jsonResponse = response.json();
      if (jsonResponse) {
        this.serviceLogsFieldsStorage.addInstances(this.getColumnsArray(jsonResponse, ServiceLogField));
      }
    });
    this.httpClient.get('auditLogsFields').subscribe((response: Response): void => {
      const jsonResponse = response.json();
      if (jsonResponse) {
        this.auditLogsFieldsStorage.addInstances(this.getColumnsArray(jsonResponse, AuditLogField));
      }
    });
  }

  private getColumnsArray(keysObject: any, fieldClass: any): any[] {
    return Object.keys(keysObject).map((key: string): {fieldClass} => new fieldClass(key));
  }

  switchTab(activeTab: Tab): void {
    this.appState.setParameters(activeTab.appState);
    this.tabsStorage.mapCollection((tab: Tab): Tab => Object.assign({}, tab, {
      isActive: tab.id === activeTab.id
    }));
    this.loadLogs();
  }

}
