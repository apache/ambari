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

import {Component, OnChanges, SimpleChanges, Input} from '@angular/core';
import {FormGroup} from '@angular/forms';
import {Observable} from 'rxjs/Observable';
import {Subject} from 'rxjs/Subject';
import 'rxjs/add/observable/from';
import {FilterCondition, SearchBoxParameter, SearchBoxParameterTriggered} from '@app/classes/filtering';
import {ListItem} from '@app/classes/list-item';
import {LogsType} from '@app/classes/string';
import {CommonEntry} from '@app/classes/models/common-entry';
import {LogsContainerService} from '@app/services/logs-container.service';

@Component({
  selector: 'filters-panel',
  templateUrl: './filters-panel.component.html',
  styleUrls: ['./filters-panel.component.less']
})
export class FiltersPanelComponent implements OnChanges {

  constructor(private logsContainer: LogsContainerService) {
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.hasOwnProperty('logsType')) {
      let result;
      switch (changes.logsType.currentValue) {
        case 'auditLogs':
          result = this.logsContainer.auditLogsColumns;
          break;
        case 'serviceLogs':
          result = this.logsContainer.serviceLogsColumns;
          break;
        default:
          result = Observable.from([]);
          break;
      }
      this.searchBoxItems = result;
    }
  }

  @Input()
  filtersForm: FormGroup;

  @Input()
  logsType: LogsType;

  searchBoxItems: Observable<ListItem[]>;

  get searchBoxItemsTranslated(): CommonEntry[] {
    switch (this.logsType) {
      case 'auditLogs':
        return this.logsContainer.auditLogsColumnsTranslated;
      case 'serviceLogs':
        return this.logsContainer.serviceLogsColumnsTranslated;
      default:
        return [];
    }
  }

  get filters(): {[key: string]: FilterCondition} {
    return this.logsContainer.filters;
  }

  /**
   * Object with options for search box parameter values
   * @returns {[key: string]: CommonEntry[]}
   */
  get options(): {[key: string]: CommonEntry[]} {
    return Object.keys(this.filters).filter((key: string): boolean => {
      const condition = this.filters[key];
      return Boolean(condition.fieldName && condition.options);
    }).reduce((currentValue, currentKey) => {
      const condition = this.filters[currentKey];
      return Object.assign(currentValue, {
        [condition.fieldName]: condition.options.map((option: ListItem): CommonEntry => {
          return {
            name: option.value,
            value: option.value
          }
        })
      });
    }, {});
  }

  get queryParameterNameChange(): Subject<SearchBoxParameterTriggered> {
    return this.logsContainer.queryParameterNameChange;
  }

  get queryParameterAdd(): Subject<SearchBoxParameter> {
    return this.logsContainer.queryParameterAdd;
  }

  get captureSeconds(): number {
    return this.logsContainer.captureSeconds;
  }

  searchBoxValueUpdate: Subject<void> = new Subject();

  isFilterConditionDisplayed(key: string): boolean {
    return this.logsContainer.isFilterConditionDisplayed(key);
  }

  updateSearchBoxValue(): void {
    this.searchBoxValueUpdate.next();
  }

}
