/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
import 'rxjs/add/operator/map';
import {HttpClientService} from '@app/services/http-client.service';
import {ServiceLogsService} from '@app/services/storage/service-logs.service';
import {FilteringService} from '@app/services/filtering.service';

@Component({
  selector: 'logs-list',
  templateUrl: './logs-list.component.html',
  styleUrls: ['./logs-list.component.less']
})
export class LogsListComponent implements OnInit {

  constructor(private httpClient: HttpClientService, private serviceLogsStorage: ServiceLogsService, private filtering: FilteringService) {
  }

  ngOnInit() {
    this.loadLogs();
    this.filtersForm.valueChanges.subscribe(() => {
      this.loadLogs();
    });
  }

  @Input()
  private logsArrayId: string;

  totalCount: number = 0;

  timeFormat: string = 'DD/MM/YYYY HH:mm:ss';

  private readonly usedFilters = {
    clusters: ['clusters'],
    text: ['iMessage'],
    timeRange: ['end_time', 'start_time'],
    components: ['component_name'],
    levels: ['level'],
    sorting: ['sortType', 'sortBy'],
    pageSize: ['pageSize'],
    page: ['page']
  };

  logs = this.serviceLogsStorage.getAll().map(logs => logs.map(log => {
    return {
      type: log.type,
      level: log.level,
      className: log.level.toLowerCase(),
      message: log.log_message,
      time: log.logtime
    };
  }));

  get timeZone(): string {
    return this.filtering.timeZone;
  }

  get filters(): any {
    return this.filtering.filters;
  }
  
  get filtersForm(): FormGroup {
    return this.filtering.filtersForm;
  }

  private loadLogs(): void {
    this.httpClient.get(this.logsArrayId, this.getParams()).subscribe(response => {
      const jsonResponse = response.json();
      this.serviceLogsStorage.clear();
      if (jsonResponse) {
        const logs = jsonResponse.logList,
          count = jsonResponse.totalCount || 0;
        if (logs) {
          const logs = response.json().logList;
          this.serviceLogsStorage.addInstances(logs);
        }
        this.totalCount = count;
      }
    });
  }

  private getParams(): any {
    let params = {};
    Object.keys(this.usedFilters).forEach(key => {
      const inputValue = this.filtersForm.getRawValue()[key],
        paramNames = this.usedFilters[key];
      paramNames.forEach(paramName => {
        let value;
        const valueGetter = this.filtering.valueGetters[paramName];
        if (valueGetter) {
          if (paramName === 'start_time') {
            value = valueGetter(inputValue, params['end_time']);
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
