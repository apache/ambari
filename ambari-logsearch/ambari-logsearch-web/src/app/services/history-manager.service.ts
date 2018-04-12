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

import {Injectable} from '@angular/core';
import 'rxjs/add/operator/distinctUntilChanged';
import 'rxjs/add/operator/takeUntil';
import {TranslateService} from '@ngx-translate/core';
import {SearchBoxParameter, TimeUnitListItem} from '@app/classes/filtering';
import {ListItem} from '@app/classes/list-item';
import {HomogeneousObject} from '@app/classes/object';
import {History} from '@app/classes/models/app-state';
import {Tab} from '@app/classes/models/tab';
import {LogsContainerService} from '@app/services/logs-container.service';
import {UtilsService} from '@app/services/utils.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {TabsService} from '@app/services/storage/tabs.service';

@Injectable()
export class HistoryManagerService {

  /**
   * List of filter parameters which shouldn't affect changes history (related to pagination and sorting)
   * @type {string[]}
   */
  private readonly ignoredParameters: string[] = ['page', 'pageSize', 'auditLogsSorting', 'serviceLogsSorting'];

  /**
   * Maximal number of displayed history items
   * @type {number}
   */
  private readonly maxHistoryItemsCount: number = 25;

  /**
   * Indicates whether there is no changes being applied to filters that are triggered by undo or redo action.
   * Since user can undo or redo several filters changes at once, and they are applied to form controls step-by-step,
   * this flag is needed to avoid recording intermediate items to history.
   * @type {boolean}
   */
  private hasNoPendingUndoOrRedo: boolean = true;

  /**
   * Id of currently active history item.
   * Generally speaking, it isn't id of the latest one because it can be shifted by undo or redo action.
   * @type {number}
   */
  private currentHistoryItemId: number = -1;

  /**
   * Contains i18n labels for filtering form control names
   */
  private controlNameLabels;

  /**
   * Contains i18n labels for time range options
   */
  private timeRangeLabels;

  /**
   * History items for current tab
   * @type {Array}
   */
  activeHistory: ListItem[] = [];

  constructor(
    private translate: TranslateService, private logsContainerService: LogsContainerService, private utils: UtilsService,
    private appState: AppStateService, private tabs: TabsService
  ) {
    // set labels for history list items
    const filters = logsContainerService.filters;
    const controlNames = Object.keys(filters).filter((name: string): boolean => {
        const key = filters[name].label;
        return key && this.ignoredParameters.indexOf(name) === -1;
      });
    const filterLabelKeys = controlNames.map((name: string): string => filters[name].label);
    const timeRangeLabels = filters.timeRange.options.reduce((
          currentArray: string[], group: TimeUnitListItem[]
        ): string[] => {
          return [...currentArray, ...group.map((option: TimeUnitListItem): string => option.label)];
        }, [logsContainerService.customTimeRangeKey]);

    translate.get([
      'filter.include', 'filter.exclude', ...filterLabelKeys, ...timeRangeLabels
    ]).subscribe((translates: object): void => {
      this.controlNameLabels = controlNames.reduce((
        currentObject: HomogeneousObject<string>, name: string
      ): HomogeneousObject<string> => {
        return Object.assign({}, currentObject, {
          [name]: translates[filters[name].label]
        });
      }, {
        include: translates['filter.include'],
        exclude: translates['filter.exclude']
      });
      this.timeRangeLabels = timeRangeLabels.reduce((
        currentObject: HomogeneousObject<string>, key: string
      ): HomogeneousObject<string> => {
        return Object.assign({}, currentObject, {
          [key]: translates[key]
        });
      }, {});
    });

    // set default history state for each tab
    tabs.mapCollection((tab: Tab): Tab => {
      const currentTabAppState = tab.appState || {};
      const nextTabAppState = Object.assign({}, currentTabAppState, {history: this.initialHistory});
      return Object.assign({}, tab, {
        appState: nextTabAppState
      });
    });

    this.logsContainerService.filtersForm.valueChanges
      .filter(() => !this.logsContainerService.filtersFormSyncInProgress.getValue())
      .distinctUntilChanged(this.isHistoryUnchanged)
      .subscribe(this.onFormValueChanges);
  }

  /**
   * List of filtering form control names for active tab
   * @returns {Array}
   */
  private get filterParameters(): string[] {
    return this.logsContainerService.logsTypeMap[this.logsContainerService.activeLogsType].listFilters;
  }

  get initialHistory(): History {
    return Object.assign({}, {
      items: [],
      currentId: -1
    });
  }

  onFormValueChanges = (value): void => {
    if (this.hasNoPendingUndoOrRedo) {
      const defaultState = this.logsContainerService.getFiltersData(this.logsContainerService.activeLogsType);
      const currentHistory = this.activeHistory;
      const previousValue = this.activeHistory.length ? this.activeHistory[0].value.currentValue : defaultState;
      const isUndoOrRedo = value.isUndoOrRedo;
      const previousChangeId = this.currentHistoryItemId;
      if (isUndoOrRedo) {
        this.hasNoPendingUndoOrRedo = false;
        this.logsContainerService.filtersForm.patchValue({
          isUndoOrRedo: false
        });
        this.hasNoPendingUndoOrRedo = true;
      } else {
        this.currentHistoryItemId = currentHistory.length;
      }
      const newItem = {
        value: {
          currentValue: Object.assign({}, value),
          previousValue: Object.assign({}, previousValue),
          changeId: this.currentHistoryItemId,
          previousChangeId,
          isUndoOrRedo
        },
        label: this.getHistoryItemLabel(previousValue, value)
      };
      if (newItem.label) {
        this.activeHistory = [
          newItem,
          ...currentHistory
        ].slice(0, this.maxHistoryItemsCount);
        this.appState.setParameter('history', {
          items: this.activeHistory.slice(),
          currentId: this.currentHistoryItemId
        });
      }
    }
  }

  /**
   * List of changes that can be undone
   * @returns {ListItem[]}
   */
  get undoItems(): ListItem[] {
    const allItems = this.activeHistory;
    const startIndex = allItems.findIndex((item: ListItem): boolean => {
        return item.value.changeId === this.currentHistoryItemId && !item.value.isUndoOrRedo;
      });
    let endIndex = allItems.slice(startIndex + 1).findIndex((item: ListItem): boolean => item.value.isUndoOrRedo);
    let items = [];
    if (startIndex > -1) {
      if (endIndex === -1) {
        endIndex = allItems.length;
      }
      items = allItems.slice(startIndex, startIndex + endIndex + 1);
    }
    return items;
  }

  /**
   * List of changes that can be redone
   * @returns {ListItem[]}
   */
  get redoItems(): ListItem[] {
    const allItems = this.activeHistory.slice().reverse();
    let startIndex = allItems.findIndex((item: ListItem): boolean => {
        return item.value.previousChangeId === this.currentHistoryItemId && !item.value.isUndoOrRedo;
      }),
      endIndex = allItems.slice(startIndex + 1).findIndex((item: ListItem): boolean => item.value.isUndoOrRedo);
    if (startIndex === -1) {
      startIndex = allItems.length;
    }
    if (endIndex === -1) {
      endIndex = allItems.length;
    }
    return allItems.slice(startIndex, endIndex + startIndex + 1);
  }

  /**
   * Indicates whether there are no filtering form changes that should be tracked
   * (all except the ones related to pagination and sorting)
   * @param {object} valueA
   * @param {object} valueB
   * @returns {boolean}
   */
  private isHistoryUnchanged = (valueA: object, valueB: object): boolean => {
    const objectA = Object.assign({}, valueA),
      objectB = Object.assign({}, valueB);
    this.ignoredParameters.forEach((controlName: string): void => {
      delete objectA[controlName];
      delete objectB[controlName];
    });
    return this.utils.isEqual(objectA, objectB);
  }

  /**
   * Get label for certain form control change
   * @param {string} controlName
   * @param {any} selection
   * @returns {string}
   */
  private getItemValueString(controlName: string, selection: any): string {
    switch (controlName) {
      case 'timeRange':
        return `${this.controlNameLabels[controlName]}: ${this.timeRangeLabels[selection.label]}`;
      case 'query':
        const includes = selection.filter((item: SearchBoxParameter): boolean => {
            return !item.isExclude;
          }).map((item: SearchBoxParameter): string => `${item.name}: ${item.value}`).join(', ');
        const excludes = selection.filter((item: SearchBoxParameter): boolean => {
            return item.isExclude;
          }).map((item: SearchBoxParameter): string => `${item.name}: ${item.value}`).join(', ');
        const includesString = includes.length ? `${this.controlNameLabels.include}: ${includes}` : '';
        const excludesString = excludes.length ? `${this.controlNameLabels.exclude}: ${excludes}` : '';
        return `${includesString} ${excludesString}`;
      default:
        const values = selection.map((option: ListItem) => option.value).join(', ');
        return `${this.controlNameLabels[controlName]}: ${values}`;
    }
  }

  /**
   * Get label for history list item (i.e., difference with the previous one)
   * @param {object} previousFormValue
   * @param {object} currentFormValue
   * @returns {string}
   */
  private getHistoryItemLabel(previousFormValue: object, currentFormValue: object): string {
    return this.filterParameters.reduce(
      (currentResult: string, currentName: string): string => {
        const currentValue = currentFormValue[currentName];
        if (this.ignoredParameters.indexOf(currentName) > -1
          || this.utils.isEqual(previousFormValue[currentName], currentValue)) {
          return currentResult;
        } else {
          const currentLabel = this.getItemValueString(currentName, currentValue);
          return `${currentResult} ${currentLabel}`;
        }
      }, ''
    );
  }

  /**
   * Handle undo or redo action correctly
   * @param {object} value
   */
  private handleUndoOrRedo(value: object): void {
    const filtersForm = this.logsContainerService.filtersForm;
    this.hasNoPendingUndoOrRedo = false;
    this.logsContainerService.filtersFormSyncInProgress.next(true);
    this.filterParameters.filter(controlName => this.ignoredParameters.indexOf(controlName) === -1)
      .forEach((controlName: string): void => {
        filtersForm.controls[controlName].setValue(value[controlName], {
          emitEvent: false,
          onlySelf: true
        });
      });
    this.logsContainerService.filtersFormSyncInProgress.next(false);
    this.hasNoPendingUndoOrRedo = true;
    filtersForm.controls.isUndoOrRedo.setValue(true);
  }

  undo(item: ListItem): void {
    if (item) {
      this.hasNoPendingUndoOrRedo = false;
      this.currentHistoryItemId = item.value.previousChangeId;
      this.handleUndoOrRedo(item.value.previousValue);
    }
  }

  redo(item: ListItem): void {
    if (item) {
      this.hasNoPendingUndoOrRedo = false;
      this.currentHistoryItemId = item.value.changeId;
      this.handleUndoOrRedo(item.value.currentValue);
    }
  }

}
