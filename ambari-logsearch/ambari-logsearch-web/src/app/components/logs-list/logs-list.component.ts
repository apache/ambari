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
import {AppStateService} from '@app/services/storage/app-state.service';
import {FilteringService} from '@app/services/filtering.service';
import {UtilsService} from '@app/services/utils.service';
import {AuditLog} from '@app/models/audit-log.model';
import {ServiceLog} from '@app/models/service-log.model';

@Component({
  selector: 'logs-list',
  templateUrl: './logs-list.component.html',
  styleUrls: ['./logs-list.component.less']
})
export class LogsListComponent implements AfterViewInit {

  constructor(private filtering: FilteringService, private utils: UtilsService, private appState: AppStateService) {
    appState.getParameter('isServiceLogsFileView').subscribe((value: boolean) => this.isServiceLogsFileView = value);
  }

  ngAfterViewInit() {
    this.contextMenuElement = this.contextMenu.nativeElement;
  }

  @Input()
  logs: (AuditLog| ServiceLog)[] = [];

  @Input()
  totalCount: number = 0;

  @Input()
  displayedColumns: any[] = [];

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
    return this.filtering.timeZone;
  }

  get filters(): any {
    return this.filtering.filters;
  }
  
  get filtersForm(): FormGroup {
    return this.filtering.filtersForm;
  }

  isServiceLogsFileView: boolean = false;

  isDifferentDates(dateA, dateB): boolean {
    return this.utils.isDifferentDates(dateA, dateB, this.timeZone);
  }

  isColumnDisplayed(key: string): boolean {
    return this.displayedColumns.some(column => column.name === key);
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
    this.filtering.queryParameterAdd.next({
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
