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
import {LogsContainerService} from '@app/services/logs-container.service';
import {ServiceLogsHistogramDataService} from '@app/services/storage/service-logs-histogram-data.service';
import {AuditLogsGraphDataService} from '@app/services/storage/audit-logs-graph-data.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {TabsService} from '@app/services/storage/tabs.service';
import {AuditLog} from '@app/classes/models/audit-log';
import {ServiceLog} from '@app/classes/models/service-log';
import {LogTypeTab} from '@app/classes/models/log-type-tab';
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

  private isFilterPanelFixedPostioned = false;

  tabs: Observable<LogTypeTab[]> = this.tabsStorage.getAll().map((tabs: LogTypeTab[]) => {
    return tabs.map((tab: LogTypeTab) => {
      const params = this.logsFilteringUtilsService.getParamsFromActiveFilter(
        tab.activeFilters, tab.appState.activeLogsType
      );
      return Object.assign({}, tab, {params});
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

  isServiceLogContextView = false;

  private activeTabId$: BehaviorSubject<string> = new BehaviorSubject(
    this.router.routerState.snapshot.root.firstChild && this.router.routerState.snapshot.root.firstChild.params.activeTab
  );

  @ViewChild('container') containerRef: ElementRef;
  @ViewChild('filtersPanel') filtersPanelRef: FiltersPanelComponent;

  @Input()
  routerPath: string[] = ['/logs'];

  private subscriptions: Subscription[] = [];
  private paramsSyncInProgress: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

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
    // set te logsType when the activeLogsType state has changed
    this.subscriptions.push(
      this.appState.getParameter('activeLogsType').subscribe((value: LogsType) => this.logsType = value)
    );
    // set the hhistogramm data
    this.subscriptions.push(
      this.serviceLogsHistogramStorage.getAll().subscribe((data: BarGraph[]): void => {
        this.serviceLogsHistogramData = this.logsContainerService.getGraphData(data, this.logsContainerService.logLevels.map((
          level: LogLevelObject
        ): LogLevel => {
          return level.name;
        }));
      })
    );
    // audit graph data set
    this.subscriptions.push(
      this.auditLogsGraphStorage.getAll().subscribe((data: BarGraph[]): void => {
        this.auditLogsGraphData = this.logsContainerService.getGraphData(data);
      })
    );
    // service log context flag subscription
    this.subscriptions.push(
      this.appState.getParameter('isServiceLogContextView').subscribe((value: boolean): void => {
        this.isServiceLogContextView = value;
      })
    );

    this.activatedRoute.params.first().map(params => params.activeTab).subscribe((tabId) => {
      this.logsContainerService.setActiveTabById(tabId);
    });

    //// SYNC BETWEEN PARAMS AND FORM
    // sync to filters form when the query params changed (only when there is no other way sync)
    this.subscriptions.push(
      this.activatedRoute.params.filter(() => !this.paramsSyncInProgress.getValue())
        .subscribe(this.onParamsChange)
    );
    // Sync from form to params on form values change
    this.subscriptions.push(
      this.filtersForm.valueChanges
        .filter(() => !this.logsContainerService.filtersFormSyncInProgress.getValue())
        .subscribe(this.onFiltersFormChange)
    );
    //// SYNC BETWEEN PARAMS AND FORM END

    //// TAB CHANGE
    // when the activeTabId$ behaviour subject change, this depends on the params' changes
    this.subscriptions.push(
      this.activeTabId$.distinctUntilChanged().subscribe(this.onActiveTabIdChange)
    );

    // set the position of the filter panel depending on the scroll height: so it is fixed when it would be out from the screen
    this.subscriptions.push(
      Observable.fromEvent(window, 'scroll').debounceTime(10).subscribe(this.setFixedPositionValue)
    );

  }

  ngOnDestroy() {
    this.subscriptions.forEach((subscription: Subscription) => subscription.unsubscribe());
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
  get totalEventsFoundMessageParams(): {totalCount: number} {
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
  private onActiveTabIdChange = (tabId: string): void => {
    this.logsContainerService.setActiveTabById(tabId);
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
  private paramsSyncStart = (): void => {
    this.paramsSyncInProgress.next(true);
  }
  /**
   * Turn off the 'query params in sync' flag
   */
  private paramsSyncStop = (): void => {
    this.paramsSyncInProgress.next(false);
  }

  /**
   * The goal is to make the app always bookmarkable.
   * @param filters
   */
  private syncFiltersToParams(filters): void {
    const params = this.logsFilteringUtilsService.getParamsFromActiveFilter(
      filters, this.logsContainerService.activeLogsType
    );
    this.paramsSyncStart(); // turn on the 'sync in progress' flag
    this.router.navigate([params], { relativeTo: this.activatedRoute })
      .then(this.paramsSyncStop, this.paramsSyncStop) // turn off the 'sync in progress' flag
      .catch(this.paramsSyncStop); // turn off the 'sync in progress' flag
  }

  /**
   * This will call the LogsContainerService to reset the filter form with the given values.
   * It will add default values where it is missing from the object.
   * @param values {[key: string]: any} The new values for the filter form
   */
  private resetFiltersForm(values: {[key: string]: any}): void {
    if (Object.keys(values).length) {
      this.logsContainerService.resetFiltersForms({
        ...this.logsFilteringUtilsService.defaultFilterSelections,
        ...values
      });
    }
  }

  /**
   * It will request the LogsContainerService to store the given filters to the given tab
   * in order to apply these filters when there is no filter params in the URL.
   * @param filters {[key: string]: any} The values for the filters form
   * @param tabId string The tab where it should be stored (in activeFilters property)
   */
  private syncFilterToTabStore(filters: {[key: string]: any}, tabId: string): void {
    this.logsContainerService.syncFiltersToTabFilters(filters, tabId);
  }

  /**
   * Handle the filters' form changes and sync it to the query parameters
   * @param values The new filter values. This is the raw value of the form group
   */
  private onFiltersFormChange = (filters): void => {
    this.syncFiltersToParams(filters);
  }

  private onParamsChange = (params: {[key: string]: any}) => {
    const {activeTab, ...filtersParams} = params;
    this.tabsStorage.findInCollection((tab: LogTypeTab) => tab.id === params.activeTab)
      .first()
      .subscribe((tab) => {
        if (tab) {
          const filtersFromParams: {[key: string]: any} = this.logsFilteringUtilsService.getFilterFromParams(
            filtersParams,
            tab.appState.activeLogsType
          );
          // we dont't have to reset the form with the new values when there is tab changes
          // because the onActiveTabIdChange will call the setActiveTabById on LogsContainerService
          // which will reset the form to the tab's activeFilters prop.
          // If we do reset wvery time then the form will be reseted twice with every tab changes... not a big deal anyway
          if (this.activeTabId$.getValue() === activeTab) {
            this.resetFiltersForm(filtersFromParams);
          }
          this.syncFilterToTabStore(filtersFromParams, activeTab);
          this.activeTabId$.next(activeTab);
        }
      });
  }

  //
  // SECTION END: FILTER SYNCHRONIZATION
  //

  /**
   * The goal is to set the fixed position of the filter panel when it is scrolled to the top. So that the panel
   * can be always visible for the user.
   */
  private setFixedPositionValue = (): void => {
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

  onSwitchTab(activeTab: LogTypeTab): void {
    this.logsContainerService.switchTab(activeTab);
  }

  onCloseTab(activeTab: LogTypeTab, newActiveTab: LogTypeTab): void {
    const activateNewTab: boolean = activeTab.isActive;
    this.tabsStorage.deleteObjectInstance(activeTab);
    if (activateNewTab && newActiveTab) {
      this.router.navigate(['/logs', ...this.logsFilteringUtilsService.getNavigationForTab(newActiveTab)]);
    }
  }
}
