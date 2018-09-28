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

import {Component, Input, ElementRef} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import {LogsContainerService} from '@app/services/logs-container.service';
import {ServiceLogsTruncatedService} from '@app/services/storage/service-logs-truncated.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {ServiceLog} from '@app/classes/models/service-log';
import {ServiceLogContextEntry} from '@app/classes/service-log-context-entry';

@Component({
  selector: 'log-context',
  templateUrl: './log-context.component.html',
  styleUrls: ['./log-context.component.less']
})
export class LogContextComponent {

  @Input()
  id: string;

  @Input()
  hostName: string;

  @Input()
  componentName: string;

  readonly currentLogClassName: string = 'alert-warning'; // TODO implement custom class name with actual styles

  firstEntryId: string;

  lastEntryId: string;

  logs: Observable<ServiceLogContextEntry[]> = this.serviceLogsTruncatedStorage.getAll()
    .map((logs: ServiceLog[]): ServiceLogContextEntry[] => {
      if (logs.length) {
        this.firstEntryId = logs[0].id;
        this.lastEntryId = logs[logs.length - 1].id;
      }
      return logs.map((log: ServiceLog): ServiceLogContextEntry => {
        return {
          id: log.id,
          time: log.logtime,
          level: log.level,
          message: log.log_message,
          fileName: log.file,
          lineNumber: log.line_number
        };
      });
    });

  constructor(
    private element: ElementRef,
    private logsContainer: LogsContainerService,
    private serviceLogsTruncatedStorage: ServiceLogsTruncatedService,
    private appState: AppStateService) {}

  closeLogContext(): void {
    this.appState.setParameters({
      isServiceLogContextView: false,
      activeLog: null
    });
    this.serviceLogsTruncatedStorage.clear();
    this.firstEntryId = '';
    this.lastEntryId = '';
  }

  scrollToCurrentEntry() {
    this.element.nativeElement.getElementsByClassName(this.currentLogClassName).item(0).scrollIntoView();
  }

  loadBefore(): void {
    this.logsContainer.loadLogContext(this.firstEntryId, this.hostName, this.componentName, 'before');
  }

  loadAfter(): void {
    this.logsContainer.loadLogContext(this.lastEntryId, this.hostName, this.componentName, 'after');
  }

}
