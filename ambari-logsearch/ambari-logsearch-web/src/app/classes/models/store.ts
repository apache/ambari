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

import {ReflectiveInjector} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {Store, Action} from '@ngrx/store';
import {AppSettings} from '@app/classes/models/app-settings';
import {AppState} from '@app/classes/models/app-state';
import {AuditLog} from '@app/classes/models/audit-log';
import {ServiceLog} from '@app/classes/models/service-log';
import {BarGraph} from '@app/classes/models/bar-graph';
import {Graph} from '@app/classes/models/graph';
import {NodeItem} from '@app/classes/models/node-item';
import {UserConfig} from '@app/classes/models/user-config';
import {LogTypeTab} from '@app/classes/models/log-type-tab';
import {LogField} from '@app/classes/object';
import {UtilsService} from '@app/services/utils.service';
import {NotificationInterface} from '@modules/shared/interfaces/notification.interface';
import {LogsState} from '@app/classes/models/logs-state';
import { DataAvaibilityStatesModel } from '@app/modules/app-load/models/data-availability-state.model';

const storeActions = {
    'ARRAY.ADD': 'ADD',
    'ARRAY.ADD.START': 'ADD_TO_START',
    'ARRAY.ADD.UNIQUE': 'ADD_UNIQUE',
    'ARRAY.DELETE.PRIMITIVE': 'DELETE_PRIMITIVE',
    'ARRAY.DELETE.OBJECT': 'DELETE_OBJECT',
    'ARRAY.CLEAR': 'CLEAR',
    'ARRAY.MAP': 'MAP',

    'OBJECT.SET': 'SET'
  },
  provider = ReflectiveInjector.resolve([UtilsService]),
  injector = ReflectiveInjector.fromResolvedProviders(provider),
  utils = injector.get(UtilsService);

export interface AppStore {
  appSettings: AppSettings;
  appState: AppState;
  auditLogs: AuditLog[];
  auditLogsGraphData: BarGraph[];
  serviceLogs: ServiceLog[];
  serviceLogsHistogramData: BarGraph[];
  serviceLogsTruncated: ServiceLog[];
  graphs: Graph[];
  hosts: NodeItem[];
  userConfigs: UserConfig[];
  clusters: string[];
  components: NodeItem[];
  serviceLogsFields: LogField[];
  auditLogsFields: LogField[];
  tabs: LogTypeTab[];
  notifications: NotificationInterface[];
  logsState: LogsState;
  dataAvailabilityStates: DataAvaibilityStatesModel;
}

export class ModelService {

  protected modelName: string;

  protected store: Store<AppStore>;

  constructor(modelName: string, store: Store<AppStore>) {
    this.modelName = modelName;
    this.store = store;
  }

  getAll(): Observable<any> {
    return this.store.select(this.modelName);
  }

}

export class CollectionModelService extends ModelService {

  addInstance(instance: any): void {
    this.addInstances([instance]);
  }

  addInstances(instances: any[]): void {
    this.store.dispatch({
      type: `${storeActions['ARRAY.ADD']}_${this.modelName}`,
      payload: instances
    });
  }

  addInstancesToStart(instances: any[]): void {
    this.store.dispatch({
      type: `${storeActions['ARRAY.ADD.START']}_${this.modelName}`,
      payload: instances
    });
  }

  addUniqueInstances(instances: any[]): void {
    this.store.dispatch({
      type: `${storeActions['ARRAY.ADD.UNIQUE']}_${this.modelName}`,
      payload: instances
    });
  }

  deleteObjectInstance(instance: any): void {
    this.store.dispatch({
      type: `${storeActions['ARRAY.DELETE.OBJECT']}_${this.modelName}`,
      payload: instance
    });
  }

  deletePrimitiveInstance(instance: any): void {
    this.store.dispatch({
      type: `${storeActions['ARRAY.DELETE.PRIMITIVE']}_${this.modelName}`,
      payload: instance
    });
  }

  clear(): void {
    this.store.dispatch({
      type: `${storeActions['ARRAY.CLEAR']}_${this.modelName}`
    });
  }

  mapCollection(modifier: (item: any) => any): void {
    this.store.dispatch({
      type: `${storeActions['ARRAY.MAP']}_${this.modelName}`,
      payload: {
        modifier: modifier
      }
    });
  }

  findInCollection(findFunction): Observable<any> {
    return this.getAll().map((result: any[]): any => result.find(findFunction));
  }

  filterCollection(filterFunction): Observable<any[]> {
    return this.getAll().map((result: any[]): any[] => result.filter(filterFunction));
  }

}

export class ObjectModelService extends ModelService {

  getParameter(key: string): Observable<any> {
    return this.store.select(this.modelName, key);
  }

  setParameter(key: string, value: any): void {
    this.setParameters({
      [key]: value
    });
  }

  setParameters(params: any): void {
    this.store.dispatch({
      type: `${storeActions['OBJECT.SET']}_${this.modelName}`,
      payload: params
    });
  }

}

export function getCollectionReducer(modelName: string, defaultState: any = []): any {
  return (state: any = defaultState, action: Action) => {
    switch (action.type) {
      case `${storeActions['ARRAY.ADD']}_${modelName}`:
        return [...state, ...action.payload];
      case `${storeActions['ARRAY.ADD.START']}_${modelName}`:
        return [...action.payload, ...state];
      case `${storeActions['ARRAY.ADD.UNIQUE']}_${modelName}`:
        return utils.pushUniqueValues(state.slice(), action.payload);
      case `${storeActions['ARRAY.DELETE.OBJECT']}_${modelName}`:
        return state.filter(instance => instance.id !== action.payload.id);
      case `${storeActions['ARRAY.DELETE.PRIMITIVE']}_${modelName}`:
        return state.filter(item => item !== action.payload);
      case `${storeActions['ARRAY.CLEAR']}_${modelName}`:
        return [];
      case `${storeActions['ARRAY.MAP']}_${modelName}`:
        return state.map(action.payload.modifier);
      default:
        return state;
    }
  };
}

export function getObjectReducer(modelName: string, defaultState: any = {}) {
  return (state: any = defaultState, action: Action): any => {
    switch (action.type) {
      case `${storeActions['OBJECT.SET']}_${modelName}`:
        return Object.assign({}, state, action.payload);
      default:
        return state;
    }
  };
}
