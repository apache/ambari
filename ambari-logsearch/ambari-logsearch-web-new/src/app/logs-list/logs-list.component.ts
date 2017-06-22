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
    this.filtering.filteringSubject.subscribe(this.loadLogs.bind(this));
  }

  ngOnInit() {
    this.loadLogs();
  }

  @Input()
  private logsArrayId: string;

  private readonly usedFilters = {
    clusters: ['clusters'],
    text: ['iMessage'],
    timeRange: ['end_time', 'start_time'],
    components: ['component_name'],
    levels: ['level']
  };

  private logs = this.serviceLogsStorage.getInstances().map(logs => {
    return logs.map(log => {
      return {
        type: log.type,
        level: log.level,
        className: log.level.toLowerCase(),
        message: log.log_message,
        time: new Date(log.logtime).toLocaleDateString() + ' ' + new Date(log.logtime).toLocaleTimeString() // TODO use moment with custom time zone
      }
    });
  });

  private loadLogs(): void {
    this.httpClient.get(this.logsArrayId, this.getParams()).subscribe(response => {
      this.serviceLogsStorage.clear();
      const logs = response.json().logList;
      this.serviceLogsStorage.addInstances(logs);
    });
  }

  private getParams(): any {
    let params = {};
    Object.keys(this.usedFilters).forEach(key => {
      const inputFilter = this.filtering.filters[key],
        inputValue = inputFilter.selectedValue,
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
        if (value) {
          params[paramName] = value;
        }
      });
    }, this);
    return params;
  }

}
