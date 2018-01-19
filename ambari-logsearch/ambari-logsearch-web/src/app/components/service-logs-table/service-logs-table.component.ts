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

import {Component, AfterViewInit, AfterViewChecked, ViewChild, ElementRef, Input, ChangeDetectorRef} from '@angular/core';

import {ListItem} from '@app/classes/list-item';
import {LogsTableComponent} from '@app/classes/components/logs-table/logs-table-component';
import {LogsContainerService} from '@app/services/logs-container.service';
import {UtilsService} from '@app/services/utils.service';

export enum ListLayout {
  Table = 'TABLE',
  Flex = 'FLEX'
}

@Component({
  selector: 'service-logs-table',
  templateUrl: './service-logs-table.component.html',
  styleUrls: ['./service-logs-table.component.less']
})
export class ServiceLogsTableComponent extends LogsTableComponent implements AfterViewChecked {

  constructor(
    private logsContainer: LogsContainerService,
    private utils: UtilsService,
    private cdRef:ChangeDetectorRef
  ) {
    super();
  }

  ngAfterViewChecked() {
    this.checkListLayout();
    this.cdRef.detectChanges();
  }

  /**
   * The element reference is used to check if the table is broken or not.
   */
  @ViewChild('tableListEl', {
    read: ElementRef
  })
  private tableListElRef: ElementRef;

  /**
   * The element reference is used to check if the table is broken or not.
   */
  @ViewChild('tableWrapperEl', {
    read: ElementRef
  })
  private tableWrapperElRef: ElementRef;

  /**
   * We only show the labels in flex layout when this property is TRUE.
   * @type {boolean}
   */
  @Input()
  showLabels: boolean = false;

  /**
   * The minimum width for the log message column. It is used when we check if the layout is broken or not.
   * @type {number}
   */
  @Input()
  logMessageColumnMinWidth: number = 175;

  /**
   * We use this property in the broken table layout check process when the log message is displayed.
   * @type {string}
   */
  @Input()
  logMessageColumnCssSelector: string = 'tbody tr td.log-message';

  /**
   * Set the layout for the list.
   * It can be:
   * 'TABLE': good for comparison, but it is not useful whe the user wants to display too much fields
   * 'FLEX': flexible layout (with flex box) is good for display lot of column or display the log list on a relative
   * narrow display.
   * @type {Layout}
   */
  @Input()
  layout: ListLayout = ListLayout.Table;

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

  readonly customStyledColumns: string[] = ['level', 'type', 'logtime', 'log_message', 'path'];

  get contextMenuItems(): ListItem[] {
    return this.logsContainer.queryContextMenuItems;
  }

  private readonly messageFilterParameterName: string = 'log_message';

  /**
   * The goal is to show or hide the context menu on right click.
   * @type {boolean}
   */
  private isContextMenuDisplayed: boolean = false;

  /**
   * 'left' CSS property value for context menu dropdown
   * @type {number}
   */
  private contextMenuLeft: number = 0;

  /**
   * 'top' CSS property value for context menu dropdown
   * @type {number}
   */
  private contextMenuTop:number = 0;

  private selectedText: string = '';


  /**
   * This is a private flag to store the table layout check result. It is used to show user notifications about
   * non-visible information.
   * @type {boolean}
   */
  private tooManyColumnsSelected: boolean = false;

  get timeZone(): string {
    return this.logsContainer.timeZone;
  }

  get filters(): any {
    return this.logsContainer.filters;
  }

  get logsTypeMapObject(): object {
    return this.logsContainer.logsTypeMap.serviceLogs;
  }

  isDifferentDates(dateA, dateB): boolean {
    return this.utils.isDifferentDates(dateA, dateB, this.timeZone);
  }

  openMessageContextMenu(event: MouseEvent): void {
    const selectedText = getSelection().toString();
    if (selectedText) {
      this.isContextMenuDisplayed = true;
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

  /**
   * Handle the event when the contextual menu component hide itself.
   */
  private onContextMenuDismiss = (): void => {
    this.isContextMenuDisplayed = false;
    this.selectedText = '';
  };

  /**
   * The goal is to check if the log message column is readable or not. Doing this by checking if it is displayed or not
   * and by checking the current width and comparing with the minimum configured width.
   * @returns {boolean}
   */
  isLogMessageVisible(): boolean {
    let visible:boolean = this.isColumnDisplayed('log_message');
    if (this.logs.length && visible && this.layout === ListLayout.Table) {
      const tableElement: HTMLElement = this.tableListElRef.nativeElement;
      const lastTdElement = (tableElement && <HTMLElement>tableElement.querySelectorAll(this.logMessageColumnCssSelector)[0]) || undefined;
      const minWidth = parseFloat(window.getComputedStyle(lastTdElement).minWidth) || this.logMessageColumnMinWidth;
      const lastTdElementInfo = lastTdElement.getBoundingClientRect();
      visible = lastTdElementInfo.width >= minWidth;
    }
    return visible;
  }

  /**
   * Check if the log list (table) fits its container. The goal is to decide if the layout is broken or not.
   * @returns {boolean}
   */
  isLogListFitToTheContainer(): boolean {
    let result = this.layout === ListLayout.Flex;
    if (!result) {
      const tableElement: HTMLElement = this.tableListElRef.nativeElement;
      const tableElementInfo = tableElement.getBoundingClientRect();
      const wrapperElement: HTMLElement = this.tableWrapperElRef.nativeElement;
      const wrapperElementInfo = wrapperElement.getBoundingClientRect();
      result = wrapperElementInfo.width >= tableElementInfo.width;
    }
    return result;
  }

  /**
   * The goal of this function is to check either the log message column is readable if displayed or the all table
   * columns are visible otherwise.
   */
  private checkListLayout(): void {
    this.tooManyColumnsSelected = this.isColumnDisplayed('log_message') ? !this.isLogMessageVisible() : !this.isLogListFitToTheContainer();
  }

  /**
   * The goal is to enable the layout change to the user so that he/she can decide which view is more readable.
   * @param {Layout} layout
   */
  public setLayout(layout: ListLayout): void {
    this.layout = layout;
  }

  /**
   * Find the label for the given field in the @columns ListItem array
   * @param {string} field
   * @returns {string}
   */
  private getLabelForField(field: string): string {
    const column: ListItem = this.columns.find(column => column.value === field);
    return column && column.label;
  }

  /**
   * Toggle the true/false value of the showLabels property. The goal is to show/hide the labels in the flex box layout,
   * so that the user can decide if he/she wants to see the labels and lost some space.
   */
  private toggleShowLabels(): void {
    this.showLabels = !this.showLabels;
  }

}
