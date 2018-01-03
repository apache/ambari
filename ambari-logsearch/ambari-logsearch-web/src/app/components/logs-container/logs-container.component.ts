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

import {Component, ElementRef, ViewChild, HostListener} from '@angular/core';
import {FormGroup} from '@angular/forms';
import {Observable} from 'rxjs/Observable';
import {LogsContainerService} from '@app/services/logs-container.service';
import {ServiceLogsHistogramDataService} from '@app/services/storage/service-logs-histogram-data.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {TabsService} from '@app/services/storage/tabs.service';
import {AuditLog} from '@app/classes/models/audit-log';
import {ServiceLog} from '@app/classes/models/service-log';
import {Tab} from '@app/classes/models/tab';
import {BarGraph} from '@app/classes/models/bar-graph';
import {ActiveServiceLogEntry} from '@app/classes/active-service-log-entry';
import {ListItem} from '@app/classes/list-item';
import {HomogeneousObject} from '@app/classes/object';
import {LogsType} from '@app/classes/string';
import {FiltersPanelComponent} from "@app/components/filters-panel/filters-panel.component";

@Component({
  selector: 'logs-container',
  templateUrl: './logs-container.component.html',
  styleUrls: ['./logs-container.component.less']
})
export class LogsContainerComponent {

  constructor(
    private serviceLogsHistogramStorage: ServiceLogsHistogramDataService, private appState: AppStateService,
    private tabsStorage: TabsService, private logsContainer: LogsContainerService
  ) {
    this.logsContainer.loadColumnsNames();
    appState.getParameter('activeLogsType').subscribe((value: LogsType) => this.logsType = value);
    serviceLogsHistogramStorage.getAll().subscribe((data: BarGraph[]): void => {
      this.histogramData = this.logsContainer.getHistogramData(data);
    });
    appState.getParameter('isServiceLogContextView').subscribe((value: boolean) => this.isServiceLogContextView = value);
  }

  @ViewChild('container') containerRef: ElementRef;
  @ViewChild('filtersPanel') filtersPanelRef: FiltersPanelComponent;

  @HostListener("window:scroll", ['$event'])
  onWindowScroll(): void {
    this.setFixedPositionValue();
  }

  private isFilterPanelFixedPostioned: boolean = false;

  tabs: Observable<Tab[]> = this.tabsStorage.getAll();

  get filtersForm(): FormGroup {
    return this.logsContainer.filtersForm;
  };

  private logsType: LogsType;

  get totalCount(): number {
    return this.logsContainer.totalCount;
  }

  histogramData: HomogeneousObject<HomogeneousObject<number>>;

  get serviceLogsHistogramColors(): HomogeneousObject<string> {
    return this.logsContainer.colors;
  }

  get autoRefreshRemainingSeconds(): number {
    return this.logsContainer.autoRefreshRemainingSeconds;
  }

  get autoRefreshMessageParams(): object {
    return {
      remainingSeconds: this.autoRefreshRemainingSeconds
    };
  }

  /**
   * The goal is to provide the single source for the parameters of 'xyz events found' message.
   * @returns {Object}
   */
  get totalEventsFoundMessageParams(): object {
    return {
      totalCount: this.totalCount
    };
  }

  isServiceLogContextView: boolean = false;

  get isServiceLogsFileView(): boolean {
    return this.logsContainer.isServiceLogsFileView;
  }

  get activeLog(): ActiveServiceLogEntry | null {
    return this.logsContainer.activeLog;
  }

  get auditLogs(): Observable<AuditLog[]> {
    return this.logsContainer.auditLogs;
  }

  get auditLogsColumns(): Observable<ListItem[]> {
    return this.logsContainer.auditLogsColumns;
  }

  get serviceLogs(): Observable<ServiceLog[]> {
    return this.logsContainer.serviceLogs;
  }

  get serviceLogsColumns(): Observable<ListItem[]> {
    return this.logsContainer.serviceLogsColumns;
  }

  /**
   * The goal is to set the fixed position of the filter panel when it is scrolled to the top. So that the panel
   * can be always visible for the user.
   */
  private setFixedPositionValue(): void {
    const el:Element = this.containerRef.nativeElement;
    const top:number = el.getBoundingClientRect().top;
    const valueBefore: boolean = this.isFilterPanelFixedPostioned;
    if (valueBefore != (top <= 0)) {
      const fpEl:Element = this.filtersPanelRef.containerEl;
      this.isFilterPanelFixedPostioned = top <= 0;
      const filtersPanelHeight: number = fpEl.getBoundingClientRect().height;
      const containerPaddingTop: number = parseFloat(window.getComputedStyle(el).paddingTop);
      const htmlEl:HTMLElement = this.containerRef.nativeElement;
      if (this.isFilterPanelFixedPostioned) {
        htmlEl.style.paddingTop = (containerPaddingTop + filtersPanelHeight) + 'px';
      } else {
        htmlEl.style.paddingTop = (containerPaddingTop - filtersPanelHeight) + 'px';
      }
    }
  }

  setCustomTimeRange(startTime: number, endTime: number): void {
    this.logsContainer.setCustomTimeRange(startTime, endTime);
  }

  onSwitchTab(activeTab: Tab): void {
    this.logsContainer.switchTab(activeTab);
  }

  onCloseTab(activeTab: Tab, newActiveTab: Tab): void {
    this.tabsStorage.deleteObjectInstance(activeTab);
    if (newActiveTab) {
      this.onSwitchTab(newActiveTab);
    }
  }
}
