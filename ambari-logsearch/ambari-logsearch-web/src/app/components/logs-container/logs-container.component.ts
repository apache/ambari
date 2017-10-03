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
import {FilteringService} from '@app/services/filtering.service';
import {LogsContainerService} from '@app/services/logs-container.service';
import {ServiceLogsHistogramDataService} from '@app/services/storage/service-logs-histogram-data.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {AuditLog} from '@app/models/audit-log.model';
import {ServiceLog} from '@app/models/service-log.model';
import {LogField} from '@app/models/log-field.model';
import {ActiveServiceLogEntry} from '@app/classes/active-service-log-entry.class';
import {HistogramOptions} from '@app/classes/histogram-options.class';

@Component({
  selector: 'logs-container',
  templateUrl: './logs-container.component.html',
  styleUrls: ['./logs-container.component.less']
})
export class LogsContainerComponent implements OnInit {

  constructor(private serviceLogsHistogramStorage: ServiceLogsHistogramDataService, private appState: AppStateService, private filtering: FilteringService, private logsContainer: LogsContainerService) {
    serviceLogsHistogramStorage.getAll().subscribe(data => this.histogramData = this.logsContainer.getHistogramData(data));
    appState.getParameter('isServiceLogContextView').subscribe((value: boolean) => this.isServiceLogContextView = value);
  }

  ngOnInit() {
    const fieldsModel = this.logsTypeMapObject.fieldsModel,
      logsModel = this.logsTypeMapObject.logsModel;
    this.appState.getParameter(this.logsTypeMapObject.isSetFlag).subscribe((value: boolean) => this.isLogsSet = value);
    this.availableColumns = fieldsModel.getAll().map(fields => {
      return fields.filter(field => field.isAvailable).map(field => {
        return {
          value: field.name,
          label: field.displayName || field.name,
          isChecked: field.isDisplayed
        };
      });
    });
    fieldsModel.getAll().subscribe(columns => {
      const availableFields = columns.filter(field => field.isAvailable),
        availableNames = availableFields.map(field => field.name);
      if (availableNames.length && !this.isLogsSet) {
        this.logs = logsModel.getAll().map((logs: (AuditLog | ServiceLog)[]): (AuditLog | ServiceLog)[] => {
          return logs.map((log: AuditLog | ServiceLog): AuditLog | ServiceLog => {
            return availableNames.reduce((obj, key) => Object.assign(obj, {
              [key]: log[key]
            }), {});
          });
        });
        this.appState.setParameter(this.logsTypeMapObject.isSetFlag, true);
      }
      this.displayedColumns = columns.filter(column => column.isAvailable && column.isDisplayed);
    });
    this.logsContainer.loadLogs(this.logsType);
    this.filtersForm.valueChanges.subscribe(() => this.logsContainer.loadLogs(this.logsType));
  }

  @Input()
  logsType: string;

  private isLogsSet: boolean = false;

  get logsTypeMapObject(): any {
    return this.logsContainer.logsTypeMap[this.logsType];
  }

  get totalCount(): number {
    return this.logsContainer.totalCount;
  }

  logs: Observable<AuditLog[] | ServiceLog[]>;

  availableColumns: Observable<LogField[]>;

  displayedColumns: any[] = [];

  histogramData: {[key: string]: number};

  readonly histogramOptions: HistogramOptions = {
    keysWithColors: this.logsContainer.colors
  };

  private get filtersForm(): FormGroup {
    return this.filtering.filtersForm;
  }

  get autoRefreshRemainingSeconds(): number {
    return this.filtering.autoRefreshRemainingSeconds;
  }

  get autoRefreshMessageParams(): any {
    return {
      remainingSeconds: this.autoRefreshRemainingSeconds
    };
  }

  isServiceLogContextView: boolean = false;

  get isServiceLogsFileView(): boolean {
    return this.logsContainer.isServiceLogsFileView;
  }

  get activeLog(): ActiveServiceLogEntry | null {
    return this.logsContainer.activeLog;
  }

  setCustomTimeRange(startTime: number, endTime: number): void {
    this.filtering.setCustomTimeRange(startTime, endTime);
  }
}
