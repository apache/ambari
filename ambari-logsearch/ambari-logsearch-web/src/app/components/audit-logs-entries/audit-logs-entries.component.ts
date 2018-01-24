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
import {GraphEmittedEvent} from '@app/classes/graph';
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

  /**
   * 'left' CSS property value for context menu dropdown
   * @type {number}
   */
  contextMenuLeft: number = 0;

  /**
   * 'top' CSS property value for context menu dropdown
   * @type {number}
   */
  contextMenuTop: number = 0;

  readonly usersGraphTitleParams = {
    number: this.logsContainer.topUsersCount
  };

  readonly resourcesGraphTitleParams = {
    number: this.logsContainer.topResourcesCount
  };

  private readonly resourceFilterParameterName: string = 'resource';

  /**
   * Text for filtering be resource type (set from Y axis tick of Resources chart)
   * @type {string}
   */
  private selectedResource: string = '';

  get topResourcesGraphData(): HomogeneousObject<HomogeneousObject<number>> {
    return this.logsContainer.topResourcesGraphData;
  }

  get topUsersGraphData(): HomogeneousObject<HomogeneousObject<number>> {
    return this.logsContainer.topUsersGraphData;
  }

  get isContextMenuDisplayed(): boolean {
    return Boolean(this.selectedResource);
  }

  get contextMenuItems(): ListItem[] {
    return this.logsContainer.queryContextMenuItems;
  }

  setActiveTab(tab: Tab): void {
    this.activeTab = tab.id;
  }

  showContextMenu(event: GraphEmittedEvent<MouseEvent>): void {
    this.contextMenuLeft = event.nativeEvent.clientX;
    this.contextMenuTop = event.nativeEvent.clientY;
    this.selectedResource = event.tick;
  }

  updateQuery(event: ListItem): void {
    this.logsContainer.queryParameterAdd.next({
      name: this.resourceFilterParameterName,
      value: this.selectedResource,
      isExclude: event.value
    });
  }

  onContextMenuDismiss(): void {
    this.selectedResource = '';
  }

}
