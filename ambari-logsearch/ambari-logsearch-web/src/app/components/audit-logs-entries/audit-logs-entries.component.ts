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

import {Component, Input} from '@angular/core';
import {FormGroup} from '@angular/forms';
import {ListItem} from '@app/classes/list-item';
import {HomogeneousObject} from '@app/classes/object';
import {AuditLog} from '@app/classes/models/audit-log';
import {Tab} from '@app/classes/models/tab';
import {LogsContainerService} from '@app/services/logs-container.service';

@Component({
  selector: 'audit-logs-entries',
  templateUrl: './audit-logs-entries.component.html'
})
export class AuditLogsEntriesComponent {

  constructor(private logsContainer: LogsContainerService) {
  }

  @Input()
  logs: AuditLog[] = [];

  @Input()
  columns: ListItem[] = [];

  @Input()
  filtersForm: FormGroup;

  @Input()
  totalCount: number = 0;

  tabs: Tab[] = [
    {
      id: 'summary',
      isActive: true,
      label: 'common.summary'
    },
    {
      id: 'logs',
      isActive: false,
      label: 'common.logs'
    }
  ];

  /**
   * Id of currently active tab (Summary or Logs)
   * @type {string}
   */
  activeTab: string = 'summary';

  readonly usersGraphTitleParams = {
    number: this.logsContainer.topUsersCount
  };

  readonly resourcesGraphTitleParams = {
    number: this.logsContainer.topResourcesCount
  };

  get topResourcesGraphData(): HomogeneousObject<HomogeneousObject<number>> {
    return this.logsContainer.topResourcesGraphData;
  }

  get topUsersGraphData(): HomogeneousObject<HomogeneousObject<number>> {
    return this.logsContainer.topUsersGraphData;
  }

  setActiveTab(tab: Tab): void {
    this.activeTab = tab.id;
  }

}
