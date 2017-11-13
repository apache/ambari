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

import {Component, AfterViewInit, Input, ViewChild, ElementRef} from '@angular/core';
import {FormGroup} from '@angular/forms';
import 'rxjs/add/operator/map';
import {LogsContainerService} from '@app/services/logs-container.service';
import {UtilsService} from '@app/services/utils.service';
import {AuditLog} from '@app/classes/models/audit-log';
import {ServiceLog} from '@app/classes/models/service-log';
import {LogField} from '@app/classes/models/log-field';

@Component({
  selector: 'logs-list',
  templateUrl: './logs-list.component.html',
  styleUrls: ['./logs-list.component.less']
})
export class LogsListComponent implements AfterViewInit {

  constructor(private logsContainer: LogsContainerService, private utils: UtilsService) {
  }

  ngAfterViewInit() {
    if (this.contextMenu) {
      this.contextMenuElement = this.contextMenu.nativeElement;
    }
  }

  @Input()
  logs: (AuditLog| ServiceLog)[] = [];

  @Input()
  totalCount: number = 0;

  @Input()
  displayedColumns: LogField[] = [];

  @Input()
  isServiceLogsFileView: boolean = false;

  @Input()
  filtersForm: FormGroup;

  @ViewChild('contextmenu', {
    read: ElementRef
  })
  contextMenu: ElementRef;

  private contextMenuElement: HTMLElement;

  private selectedText: string = '';

  private readonly messageFilterParameterName = 'log_message';

  readonly customStyledColumns = ['level', 'type', 'logtime', 'log_message'];

  readonly contextMenuItems = [
    {
      label: 'logs.addToQuery',
      iconClass: 'fa fa-search-plus',
      value: false // 'isExclude' is false
    },
    {
      label: 'logs.excludeFromQuery',
      iconClass: 'fa fa-search-minus',
      value: true // 'isExclude' is true
    }
  ];

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

  readonly dateFormat: string = 'dddd, MMMM Do';

  readonly timeFormat: string = 'h:mm:ss A';

  get timeZone(): string {
    return this.logsContainer.timeZone;
  }

  get filters(): any {
    return this.logsContainer.filters;
  }

  isDifferentDates(dateA, dateB): boolean {
    return this.utils.isDifferentDates(dateA, dateB, this.timeZone);
  }

  isColumnDisplayed(key: string): boolean {
    return this.displayedColumns.some((column: LogField): boolean  => column.name === key);
  }

  openMessageContextMenu(event: MouseEvent): void {
    const selectedText = getSelection().toString();
    if (selectedText) {
      let contextMenuStyle = this.contextMenuElement.style;
      Object.assign(contextMenuStyle, {
        left: `${event.clientX}px`,
        top: `${event.clientY}px`,
        display: 'block'
      });
      this.selectedText = selectedText;
      document.body.addEventListener('click', this.dismissContextMenu);
      event.preventDefault();
    }
  }

  updateQuery(event: any) {
    this.logsContainer.queryParameterAdd.next({
      name: this.messageFilterParameterName,
      value: this.selectedText,
      isExclude: event.value
    });
  }

  private dismissContextMenu = (): void => {
    this.selectedText = '';
    this.contextMenuElement.style.display = 'none';
    document.body.removeEventListener('click', this.dismissContextMenu);
  }

}
