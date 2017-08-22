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

import {Component, OnInit, Input} from '@angular/core';
import {FormGroup} from '@angular/forms';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import {HttpClientService} from '@app/services/http-client.service';
import {FilteringService} from '@app/services/filtering.service';
import {AuditLogsService} from '@app/services/storage/audit-logs.service';
import {AuditLogsFieldsService} from '@app/services/storage/audit-logs-fields.service';
import {ServiceLogsService} from '@app/services/storage/service-logs.service';
import {ServiceLogsFieldsService} from '@app/services/storage/service-logs-fields.service';
import {ServiceLogsHistogramDataService} from '@app/services/storage/service-logs-histogram-data.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {AuditLog} from '@app/models/audit-log.model';
import {ServiceLog} from '@app/models/service-log.model';
import {LogField} from '@app/models/log-field.model';

@Component({
  selector: 'logs-container',
  templateUrl: './logs-container.component.html',
  styleUrls: ['./logs-container.component.less']
})
export class LogsContainerComponent implements OnInit {

  constructor(private httpClient: HttpClientService, private auditLogsStorage: AuditLogsService, private auditLogsFieldsStorage: AuditLogsFieldsService, private serviceLogsStorage: ServiceLogsService, private serviceLogsFieldsStorage: ServiceLogsFieldsService, private serviceLogsHistogramStorage: ServiceLogsHistogramDataService, private appState: AppStateService, private filtering: FilteringService) {
    serviceLogsHistogramStorage.getAll().subscribe(data => {
      let histogramData = {};
      data.forEach(type => {
        const name = type.name;
        type.dataCount.forEach(entry => {
          const timeStamp = new Date(entry.name).valueOf();
          if (!histogramData[timeStamp]) {
            let initialValue = {};
            Object.keys(this.histogramOptions.keysWithColors).forEach(key => initialValue[key] = 0);
            histogramData[timeStamp] = initialValue;
          }
          histogramData[timeStamp][name] = Number(entry.value);
        });
      });
      this.histogramData = histogramData;
    });
  }

  ngOnInit() {
    this.logsTypeMapObject = this.logsTypeMap[this.logsType];
    this.appState.getParameter(this.logsTypeMapObject.isSetFlag).subscribe(value => this.isLogsSet = value);
    this.availableColumns = this[this.logsTypeMapObject.fieldsModel].getAll().map(fields => {
      const availableFields = fields.filter(field => field.isAvailable),
        availableNames = availableFields.map(field => field.name);
      if (availableNames.length && !this.isLogsSet) {
        this.logs = this[this.logsTypeMapObject.logsModel].getAll().map(logs => logs.map(log => {
          let logObject = availableNames.reduce((obj, key) => Object.assign(obj, {
            [key]: log[key]
          }), {});
          if (logObject.level) {
            logObject.className = logObject.level.toLowerCase();
          }
          return logObject;
        }));
        this.appState.setParameter(this.logsTypeMapObject.isSetFlag, true);
      }
      return availableFields.map(field => {
        return {
          value: field.name,
          label: field.displayName || field.name,
          isChecked: field.isDisplayed
        };
      });
    });
    this[this.logsTypeMapObject.fieldsModel].getAll().subscribe(columns => {
      this.displayedColumns = columns.filter(column => column.isAvailable && column.isDisplayed);
    });
    this.loadLogs();
    this.filtersForm.valueChanges.subscribe(() => {
      this.loadLogs();
    });
  }

  @Input()
  private logsType: string;

  private isLogsSet: boolean = false;

  logsTypeMapObject: any;

  totalCount: number = 0;

  private readonly listFilters = {
    clusters: ['clusters'],
    text: ['iMessage'],
    timeRange: ['end_time', 'start_time'],
    components: ['mustBe'],
    levels: ['level'],
    sorting: ['sortType', 'sortBy'],
    pageSize: ['pageSize'],
    page: ['page']
  };

  private readonly histogramFilters = {
    clusters: ['clusters'],
    text: ['iMessage'],
    timeRange: ['to', 'from'],
    components: ['mustBe'],
    levels: ['level']
  };

  private readonly logsTypeMap = {
    auditLogs: {
      logsModel: 'auditLogsStorage',
      fieldsModel: 'auditLogsFieldsStorage',
      isSetFlag: 'isAuditLogsSet'
    },
    serviceLogs: {
      logsModel: 'serviceLogsStorage',
      fieldsModel: 'serviceLogsFieldsStorage',
      isSetFlag: 'isServiceLogsSet'
    }
  };

  logs: Observable<AuditLog[] | ServiceLog[]>;

  availableColumns: Observable<LogField[]>;

  displayedColumns: any[] = [];

  histogramData: any;

  readonly histogramOptions = {
    keysWithColors: {
      WARN: '#FF8916',
      ERROR: '#E81D1D',
      FATAL: '#830A0A',
      INFO: '#2577B5',
      DEBUG: '#65E8FF',
      TRACE: '#888',
      UNKNOWN: '#BDBDBD'
    }
  };

  private get filtersForm(): FormGroup {
    return this.filtering.filtersForm;
  }

  private loadLogs(): void {
    this.httpClient.get(this.logsType, this.getParams('listFilters')).subscribe(response => {
      const jsonResponse = response.json();
      this.serviceLogsStorage.clear();
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
      const inputValue = this.filtersForm.getRawValue()[key],
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

}
