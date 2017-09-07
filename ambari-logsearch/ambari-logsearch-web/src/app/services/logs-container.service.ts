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
import {HttpClientService} from '@app/services/http-client.service';
import {FilteringService} from '@app/services/filtering.service';
import {AuditLogsService} from '@app/services/storage/audit-logs.service';
import {AuditLogsFieldsService} from '@app/services/storage/audit-logs-fields.service';
import {ServiceLogsService} from '@app/services/storage/service-logs.service';
import {ServiceLogsFieldsService} from '@app/services/storage/service-logs-fields.service';
import {ServiceLogsHistogramDataService} from '@app/services/storage/service-logs-histogram-data.service';

@Injectable()
export class LogsContainerService {

  constructor(private httpClient: HttpClientService, private auditLogsStorage: AuditLogsService, private auditLogsFieldsStorage: AuditLogsFieldsService, private serviceLogsStorage: ServiceLogsService, private serviceLogsFieldsStorage: ServiceLogsFieldsService, private serviceLogsHistogramStorage: ServiceLogsHistogramDataService, private filtering: FilteringService) {
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
    text: ['iMessage'],
    timeRange: ['end_time', 'start_time'],
    components: ['mustBe'],
    levels: ['level'],
    hosts: ['host_name'],
    sorting: ['sortType', 'sortBy'],
    pageSize: ['pageSize'],
    page: ['page'],
    query: ['includeQuery', 'excludeQuery']
  };

  private readonly histogramFilters = {
    clusters: ['clusters'],
    text: ['iMessage'],
    timeRange: ['to', 'from'],
    components: ['mustBe'],
    levels: ['level'],
    query: ['includeQuery', 'excludeQuery']
  };

  readonly logsTypeMap = {
    auditLogs: {
      logsModel: this.auditLogsStorage,
      fieldsModel: this.auditLogsFieldsStorage,
      isSetFlag: 'isAuditLogsSet'
    },
    serviceLogs: {
      logsModel: this.serviceLogsStorage,
      fieldsModel: this.serviceLogsFieldsStorage,
      isSetFlag: 'isServiceLogsSet'
    }
  };

  totalCount: number = 0;

  loadLogs(logsType: string): void {
    this.httpClient.get(logsType, this.getParams('listFilters')).subscribe(response => {
      const jsonResponse = response.json();
      this.logsTypeMap[logsType].logsModel.clear();
      if (jsonResponse) {
        const logs = jsonResponse.logList,
          count = jsonResponse.totalCount || 0;
        if (logs) {
          this.serviceLogsStorage.addInstances(logs);
        }
        this.totalCount = count;
      }
    });
    this.httpClient.get('serviceLogsHistogram', this.getParams('histogramFilters')).subscribe(response => {
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

  private getParams(filtersMapName: string): any {
    let params = {};
    Object.keys(this[filtersMapName]).forEach(key => {
      const inputValue = this.filtering.filtersForm.getRawValue()[key],
        paramNames = this[filtersMapName][key];
      paramNames.forEach(paramName => {
        let value;
        const valueGetter = this.filtering.valueGetters[paramName];
        if (valueGetter) {
          if (paramName === 'start_time') {
            value = valueGetter(inputValue, params['end_time']);
          } else if (paramName === 'from') {
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

  getHistogramData(data: any[]): any {
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

}
