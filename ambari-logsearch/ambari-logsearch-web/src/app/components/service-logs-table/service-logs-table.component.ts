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
import {UtilsService} from '@app/services/utils.service';

@Component({
  selector: 'service-logs-table',
  templateUrl: './service-logs-table.component.html',
  styleUrls: ['./service-logs-table.component.less']
})
export class ServiceLogsTableComponent extends LogsTableComponent {

  constructor(private logsContainer: LogsContainerService, private utils: UtilsService) {
    super();
  }

  readonly dateFormat: string = 'dddd, MMMM Do';

  readonly timeFormat: string = 'h:mm:ss A';

  readonly logActions = [
    {
      label: 'logs.copy',
      iconClass: 'fa fa-files-o',
      action: 'copyLog'
    },
    {
      label: 'logs.open',
      iconClass: 'fa fa-external-link',
      action: 'openLog'
    },
    {
      label: 'logs.context',
      iconClass: 'fa fa-crosshairs',
      action: 'openContext'
    }
  ];

  readonly customStyledColumns: string[] = ['level', 'type', 'logtime', 'log_message'];

  private readonly messageFilterParameterName: string = 'log_message';

  private contextMenuElement: HTMLElement;

  private selectedText: string = '';

  get contextMenuItems(): ListItem[] {
    return this.logsContainer.queryContextMenuItems;
  }

  get timeZone(): string {
    return this.logsContainer.timeZone;
  }

  get filters(): any {
    return this.logsContainer.filters;
  }

  get logsTypeMapObject(): object {
    return this.logsContainer.logsTypeMap.serviceLogs;
  }

  get isContextMenuDisplayed(): boolean {
    return Boolean(this.selectedText);
  };

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

  isDifferentDates(dateA, dateB): boolean {
    return this.utils.isDifferentDates(dateA, dateB, this.timeZone);
  }

  openMessageContextMenu(event: MouseEvent): void {
    const selectedText = getSelection().toString();
    if (selectedText) {
      this.contextMenuLeft = event.clientX;
      this.contextMenuTop = event.clientY;
      this.selectedText = selectedText;
      event.preventDefault();
    }
  }

  updateQuery(event: ListItem): void {
    this.logsContainer.queryParameterAdd.next({
      name: this.messageFilterParameterName,
      value: this.selectedText,
      isExclude: event.value
    });
  }

  onContextMenuDismiss(): void {
    this.selectedText = '';
  }

}
