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

import {Component, OnDestroy, Input, ViewContainerRef, OnInit} from '@angular/core';
import {FormGroup} from '@angular/forms';
import {Observable} from 'rxjs/Observable';
import {Subject} from 'rxjs/Subject';
import 'rxjs/add/observable/from';
import 'rxjs/add/operator/defaultIfEmpty';
import {FilterCondition, SearchBoxParameter, SearchBoxParameterTriggered} from '@app/classes/filtering';
import {ListItem} from '@app/classes/list-item';
import {HomogeneousObject} from '@app/classes/object';
import {LogsType} from '@app/classes/string';
import {LogsContainerService} from '@app/services/logs-container.service';
import {UtilsService} from '@app/services/utils.service';
import {AppStateService} from '@app/services/storage/app-state.service';
import {Subscription} from 'rxjs/Subscription';

@Component({
  selector: 'filters-panel',
  templateUrl: './filters-panel.component.html',
  styleUrls: ['./filters-panel.component.less']
})
export class FiltersPanelComponent implements OnDestroy, OnInit {

  @Input()
  filtersForm: FormGroup;

  private subscriptions: Subscription[] = [];

  searchBoxItems$: Observable<ListItem[]>;

  searchBoxValueUpdate: Subject<void> = new Subject();

  private isServiceLogsFileView$: Observable<boolean> = this.appState.getParameter('isServiceLogsFileView');

  get containerEl(): Element {
    return this.viewContainerRef.element.nativeElement;
  }

  get filters(): HomogeneousObject<FilterCondition> {
    return this.logsContainer.filters;
  }

  /**
   * Object with options for search box parameter values
   * @returns HomogeneousObject<ListItem[]>
   */
  get options(): HomogeneousObject<ListItem[]> {
    return Object.keys(this.filters).filter((key: string): boolean => {
      const condition = this.filters[key];
      return Boolean(condition.fieldName && condition.options);
    }).reduce((currentValue, currentKey) => {
      const condition = this.filters[currentKey];
      return Object.assign(currentValue, {
        [condition.fieldName]: condition.options
      });
    }, {});
  }

  get queryParameterNameChange(): Subject<SearchBoxParameterTriggered> {
    return this.logsContainer.queryParameterNameChange;
  }

  get queryParameterAdd(): Subject<SearchBoxParameter> {
    return this.logsContainer.queryParameterAdd;
  }

  constructor(private logsContainer: LogsContainerService, public viewContainerRef: ViewContainerRef,
              private utils: UtilsService, private appState: AppStateService) {
  }

  ngOnInit() {
    this.subscriptions.push(this.appState.getParameter('activeLogsType').subscribe(this.onLogsTypeChange));
  }

  ngOnDestroy() {
    this.subscriptions.forEach((subscription: Subscription) => subscription.unsubscribe());
  }

  private onLogsTypeChange = (currentLogsType: LogsType): void => {
    const logsType = this.logsContainer.logsTypeMap[currentLogsType];
    const fieldsModel: any = logsType && logsType.fieldsModel;
    let subType: string;
    let fields: Observable<any>;
    switch (currentLogsType) {
      case 'auditLogs':
        fields = fieldsModel.getParameter(subType ? 'overrides' : 'defaults');
        if (subType) {
          fields = fields.map(items => items && items[subType]);
        }
        break;
      case 'serviceLogs':
        fields = fieldsModel.getAll();
        break;
      default:
        fields = Observable.from([]);
        break;
    }
    this.searchBoxItems$ = fields.defaultIfEmpty([]).map(items => items ? items.filter(field => field.filterable) : [])
      .map(this.utils.logFieldToListItemMapper);
  }

  isFilterConditionDisplayed(key: string): boolean {
    return this.logsContainer.isFilterConditionDisplayed(key);
  }

  updateSearchBoxValue(): void {
    this.searchBoxValueUpdate.next();
  }

  proceedWithExclude(item: string): void {
    this.queryParameterNameChange.next({
      item: {
        value: item
      },
      isExclude: true
    });
  }

}
