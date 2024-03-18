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

import { Component, Input, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { DataStateStoreKeys, baseDataKeys } from '@app/modules/app-load/services/app-load.service';
import { Observable } from 'rxjs/Observable';
import { DataAvailabilityValues } from '@app/classes/string';
import { DataAvailabilityStatesStore } from '@app/modules/app-load/stores/data-availability-state.store';

export interface DataAvaibilityObject {
  storeKey: DataStateStoreKeys;
  avaibility: DataAvailabilityValues;
};

@Component({
  selector: 'data-loading-indicator',
  templateUrl: './data-loading-indicator.component.html',
  styleUrls: ['./data-loading-indicator.component.less']
})
export class DataLoadingIndicatorComponent implements OnDestroy {

  @Input()
  keysToWatch: DataStateStoreKeys[] = baseDataKeys;

  private destroy$: Subject<boolean> = new Subject<boolean>();

  private currentWatchedDataStates$: Observable<{[key: string]: DataAvailabilityValues}> = this.dataAvailabilityStatesStore.getAll()
    .map((dataStates: {[key: string]: DataAvailabilityValues}): {[key: string]: DataAvailabilityValues} => {
      return Object.keys(dataStates || {})
        .filter((dataStateKey: DataStateStoreKeys) => this.keysToWatch.indexOf(dataStateKey) > -1)
        .reduce((watchedStates, key) => Object.assign({}, watchedStates, {
          [key]: dataStates[key]
        }), {});
    });
  currentWatchedDataStatesAsArray$: Observable<DataAvaibilityObject[]> = this.currentWatchedDataStates$.map((dataStates) => {
    return Object.keys(dataStates).reduce((statesArray, key): DataAvaibilityObject[] => {
      return [
        ...statesArray,
        {
          storeKey: key,
          avaibility: dataStates[key]
        }
      ];
    }, []);
  });
  dataLoadingProgress$: Observable<number> = this.currentWatchedDataStates$.map((dataStates): number => {
    const keys: string[] = Object.keys(dataStates);
    const total: number = keys.length;
    const totalAvailable: number = keys.filter(
      (key: string) => dataStates[key] === DataAvailabilityValues.AVAILABLE
    ).length;
    return totalAvailable / total;
  });
  hasDataStateError$: Observable<boolean> = this.currentWatchedDataStates$.map((dataStates): boolean => {
    return Object.keys(dataStates).reduce((hasError: boolean, key) => {
      return hasError || dataStates[key] === DataAvailabilityValues.ERROR;
    }, false);
  });

  constructor(
    private dataAvailabilityStatesStore: DataAvailabilityStatesStore
  ) {}

  ngOnDestroy() {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }
}
