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

import {Component, OnInit, ElementRef, ViewChild, HostListener, Input, OnDestroy, ChangeDetectorRef} from '@angular/core';
import {FormGroup} from '@angular/forms';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/skipWhile';
import 'rxjs/add/operator/skip';
import {LogsContainerService} from '@app/services/logs-container.service';
import {ServiceLogsHistogramDataService} from '@app/services/storage/service-logs-histogram-data.service';
import {AuditLogsGraphDataService} from '@app/services/storage/audit-logs-graph-data.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {TabsService} from '@app/services/storage/tabs.service';
import {AuditLog} from '@app/classes/models/audit-log';
import {ServiceLog} from '@app/classes/models/service-log';
import {Tab} from '@app/classes/models/tab';
import {BarGraph} from '@app/classes/models/bar-graph';
import {ActiveServiceLogEntry} from '@app/classes/active-service-log-entry';
import {ListItem} from '@app/classes/list-item';
import {HomogeneousObject, LogLevelObject} from '@app/classes/object';
import {LogsType, LogLevel} from '@app/classes/string';
import {FiltersPanelComponent} from '@app/components/filters-panel/filters-panel.component';
import {Subscription} from 'rxjs/Subscription';
import {LogsFilteringUtilsService} from '@app/services/logs-filtering-utils.service';
import {ActivatedRoute, Router} from '@angular/router';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {LogsStateService} from '@app/services/storage/logs-state.service';

@Component({
  selector: 'logs-container',
  templateUrl: './logs-container.component.html',
  styleUrls: ['./logs-container.component.less']
})
export class LogsContainerComponent implements OnInit, OnDestroy {

  private isFilterPanelFixedPostioned: boolean = false;

  tabs: Observable<Tab[]> = this.tabsStorage.getAll().map((tabs: Tab[]) => {
    return tabs.map((tab: Tab) => {
      const queryParams = this.logsFilteringUtilsService.getQueryParamsFromActiveFilter(
        tab.activeFilters, tab.appState.activeLogsType
      );
      return Object.assign({}, tab, {queryParams});
    });
  });

  private logsType: LogsType;

  serviceLogsHistogramData: HomogeneousObject<HomogeneousObject<number>>;

  auditLogsGraphData: HomogeneousObject<HomogeneousObject<number>>;

  serviceLogsHistogramColors: HomogeneousObject<string> = this.logsContainerService.logLevels.reduce((
    currentObject: HomogeneousObject<string>, level: LogLevelObject
  ): HomogeneousObject<string> => {
    return Object.assign({}, currentObject, {
      [level.name]: level.color
    });
  }, {});

  isServiceLogContextView: boolean = false;

  @ViewChild('container') containerRef: ElementRef;
  @ViewChild('filtersPanel') filtersPanelRef: FiltersPanelComponent;

  @Input()
  routerPath: string[] = ['/logs'];

  private subscriptions: Subscription[] = [];
  private queryParamsSyncInProgress: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  private isServiceLogsFileView$: Observable<boolean> = this.appState.getParameter('isServiceLogsFileView');

  constructor(
    private appState: AppStateService,
    private tabsStorage: TabsService,
    private logsContainerService: LogsContainerService,
    private logsFilteringUtilsService: LogsFilteringUtilsService,
    private serviceLogsHistogramStorage: ServiceLogsHistogramDataService,
    private auditLogsGraphStorage: AuditLogsGraphDataService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private logsStateService: LogsStateService
  ) {}

  ngOnInit() {
    this.logsContainerService.loadColumnsNames();
    this.subscriptions.push(
      this.appState.getParameter('activeLogsType').subscribe((value: LogsType) => this.logsType = value)
    );
    this.subscriptions.push(
      this.serviceLogsHistogramStorage.getAll().subscribe((data: BarGraph[]): void => {
        this.serviceLogsHistogramData = this.logsContainerService.getGraphData(data, this.logsContainerService.logLevels.map((
          level: LogLevelObject
        ): LogLevel => {
          return level.name;
        }));
      })
    );
    this.subscriptions.push(
      this.auditLogsGraphStorage.getAll().subscribe((data: BarGraph[]): void => {
        this.auditLogsGraphData = this.logsContainerService.getGraphData(data);
      })
    );
    this.subscriptions.push(
      this.appState.getParameter('isServiceLogContextView').subscribe((value: boolean): void => {
        this.isServiceLogContextView = value;
      })
    );

    // Sync from form to query params on form values change
    this.subscriptions.push(
      this.filtersForm.valueChanges
        .filter(() => !this.logsContainerService.filtersFormSyncInProgress.getValue()).subscribe(this.onFiltersFormChange)
    );

    this.subscriptions.push(
      this.activatedRoute.params.map((params: {[key: string]: any}) => params && params.activeTab)
        .subscribe(this.onActiveTabParamChange)
    );

    this.subscriptions.push(
      this.activatedRoute.queryParams.filter(() => !this.queryParamsSyncInProgress.getValue()).subscribe(this.onQueryParamsChange)
    );
    if (!this.activatedRoute.queryParams || !Object.keys(this.activatedRoute.queryParams).length) {
      this.syncFiltersToQueryParams(this.filtersForm.value);
    }

    this.subscriptions.push(
      this.logsStateService.getParameter('activeTabId').skip(1).subscribe(this.onActiveTabSwitched)
    );

  }

  ngOnDestroy() {
    this.subscriptions.forEach((subscription: Subscription) => subscription.unsubscribe());
  }

  @HostListener('window:scroll', ['$event'])
  onWindowScroll(): void {
    this.setFixedPositionValue();
  }

  get filtersForm(): FormGroup {
    return this.logsContainerService.filtersForm;
  };

  get totalCount(): number {
    return this.logsContainerService.totalCount;
  }

  get autoRefreshRemainingSeconds(): number {
    return this.logsContainerService.autoRefreshRemainingSeconds;
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

  get isServiceLogsFileView(): boolean {
    return this.logsContainerService.isServiceLogsFileView;
  }

  get activeLog(): ActiveServiceLogEntry | null {
    return this.logsContainerService.activeLog;
  }

  get auditLogs(): Observable<AuditLog[]> {
    return this.logsContainerService.auditLogs;
  }

  get auditLogsColumns(): Observable<ListItem[]> {
    return this.logsContainerService.auditLogsColumns;
  }

  get serviceLogs(): Observable<ServiceLog[]> {
    return this.logsContainerService.serviceLogs;
  }

  get serviceLogsColumns(): Observable<ListItem[]> {
    return this.logsContainerService.serviceLogsColumns;
  }

  //
  // SECTION: TABS
  //

  /**
   * Set the active params in the store corresponding to the URL param (activeTab)
   * @param {string} tabId The 'activeTab' segment of the URL (eg.: #/logs/serviceLogs where the serviceLogs is the activeTab parameter)
   */
  private onActiveTabParamChange = (tabId: string): void => {
    this.logsContainerService.setActiveTabById(tabId);
  }

  private onActiveTabSwitched = (tabId: string): void => {
    this.tabsStorage.findInCollection((tab: Tab) => tab.id === tabId).first().subscribe((tab: Tab) => {
      if (tab) {
        this.syncFiltersToQueryParams(tab.activeFilters);
      }
    });
  }

  //
  // SECTION END: TABS
  //

  //
  // SECTION: FILTER SYNCHRONIZATION
  //

  /**
   * Turn on the 'query params in sync' flag, so that the query to form sync don't run.
   * So when we actualize the query params to reflect the filters form values we have to turn of the back sync (query params change to form)
   */
  private queryParamsSyncStart = (): void => {
    this.queryParamsSyncInProgress.next(true);
  }
  /**
   * Turn off the 'query params in sync' flag
   */
  private queryParamsSyncStop = (): void => {
    this.queryParamsSyncInProgress.next(false);
  }

  /**
   * The goal is to make the app always bookmarkable.
   * @param filters
   */
  private syncFiltersToQueryParams(filters): void {
    const queryParams = this.logsFilteringUtilsService.getQueryParamsFromActiveFilter(
      filters, this.logsContainerService.activeLogsType
    );
    this.queryParamsSyncStart(); // turn on the 'sync in progress' flag
    this.router.navigate([], { relativeTo: this.activatedRoute, queryParams: queryParams })
      .then(this.queryParamsSyncStop, this.queryParamsSyncStop) // turn off the 'sync in progress' flag
      .catch(this.queryParamsSyncStop); // turn off the 'sync in progress' flag
  }

  private syncQueryParamsToFiltersForms(queryParams): void {
    if (Object.keys(queryParams).length) {
      const filtersFromQueryParams = this.logsFilteringUtilsService.getFilterFromQueryParams(
        queryParams, this.logsContainerService.activeLogsType
      );
      this.logsContainerService.syncFiltersToFiltersForms(filtersFromQueryParams);
    }
  }

  /**
   * Handle the filters' form changes and sync it to the query parameters
   * @param values The new filter values. This is the raw value of the form group
   */
  private onFiltersFormChange = (filters): void => {
    this.syncFiltersToQueryParams(filters);
  }

  private onQueryParamsChange = (queryParams: {[key: string]: any}) => {
    this.syncQueryParamsToFiltersForms(queryParams);
  }

  //
  // SECTION END: FILTER SYNCHRONIZATION
  //

  /**
   * The goal is to set the fixed position of the filter panel when it is scrolled to the top. So that the panel
   * can be always visible for the user.
   */
  private setFixedPositionValue(): void {
    const el: Element = this.containerRef.nativeElement;
    const top: number = el.getBoundingClientRect().top;
    const valueBefore: boolean = this.isFilterPanelFixedPostioned;
    if (valueBefore !== (top <= 0)) {
      const fpEl: Element = this.filtersPanelRef.containerEl;
      this.isFilterPanelFixedPostioned = top <= 0;
      const filtersPanelHeight: number = fpEl.getBoundingClientRect().height;
      const containerPaddingTop: number = parseFloat(window.getComputedStyle(el).paddingTop);
      const htmlEl: HTMLElement = this.containerRef.nativeElement;
      if (this.isFilterPanelFixedPostioned) {
        htmlEl.style.paddingTop = (containerPaddingTop + filtersPanelHeight) + 'px';
      } else {
        htmlEl.style.paddingTop = (containerPaddingTop - filtersPanelHeight) + 'px';
      }
    }
  }

  setCustomTimeRange(startTime: number, endTime: number): void {
    this.logsContainerService.setCustomTimeRange(startTime, endTime);
  }

  onSwitchTab(activeTab: Tab): void {
    this.logsContainerService.switchTab(activeTab);
  }

  onCloseTab(activeTab: Tab, newActiveTab: Tab): void {
    const activateNewTab: boolean = activeTab.isActive;
    this.tabsStorage.deleteObjectInstance(activeTab);
    if (activateNewTab && newActiveTab) {
      this.router.navigate(['/logs', newActiveTab.id], {
        queryParams: this.logsFilteringUtilsService.getQueryParamsFromActiveFilter(
          newActiveTab.activeFilters, newActiveTab.appState.activeLogsType
        )
      });
    }
  }
}
