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

import {Component} from '@angular/core';
import {ListItem} from '@app/classes/list-item';
import {LogsTableComponent} from '@app/classes/components/logs-table/logs-table-component';
import {LogsContainerService} from '@app/services/logs-container.service';

@Component({
  selector: 'audit-logs-table',
  templateUrl: './audit-logs-table.component.html',
  styleUrls: ['./audit-logs-table.component.less']
})
export class AuditLogsTableComponent extends LogsTableComponent {

  constructor(private logsContainer: LogsContainerService) {
    super();
  }

  readonly customProcessedColumns: string[] = ['evtTime'];

  readonly timeFormat: string = 'YYYY-MM-DD HH:mm:ss,SSS';

  get logsTypeMapObject(): object {
    return this.logsContainer.logsTypeMap.auditLogs;
  }

  get filters(): any {
    return this.logsContainer.filters;
  }

  get timeZone(): string {
    return this.logsContainer.timeZone;
  }

  getColumnByName(name: string): ListItem | undefined {
    return this.columns.find((column: ListItem): boolean => column.value === name);
  }

}
