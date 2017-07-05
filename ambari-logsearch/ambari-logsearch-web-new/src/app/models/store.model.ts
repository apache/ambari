/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http; //www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Observable} from 'rxjs/Observable';
import {Store, Action} from '@ngrx/store';
import {AppSettings} from '@app/models/app-settings.model';
import {AppState} from '@app/models/app-state.model';
import {AuditLog} from '@app/models/audit-log.model';
import {ServiceLog} from '@app/models/service-log.model';
import {BarGraph} from '@app/models/bar-graph.model';
import {Graph} from '@app/models/graph.model';
import {Node} from '@app/models/node.model';
import {UserConfig} from '@app/models/user-config.model';
import {Filter} from '@app/models/filter.model';

export const storeActions = {
  ADD: 'ADD',
  DELETE: 'DELETE',
  CLEAR: 'CLEAR',
  SET: 'SET'
};

export interface AppStore {
  appSettings: AppSettings;
  appState: AppState;
  auditLogs: AuditLog[];
  serviceLogs: ServiceLog[];
  barGraphs: BarGraph[];
  graphs: Graph[];
  nodes: Node[];
  userConfigs: UserConfig[];
  filters: Filter[];
}

export class ModelService {

  constructor(modelName: string, store: Store<AppStore>) {
    this.modelName = modelName;
    this.store = store;
  }

  protected modelName: string;

  protected store: Store<AppStore>;

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
      type: storeActions.ADD,
      payload: instances
    });
  }

  deleteInstance(instance: any): void {
    this.store.dispatch({
      type: storeActions.DELETE,
      payload: instance
    });
  }

  clear(): void {
    this.store.dispatch({
      type: storeActions.CLEAR
    });
  }

}

export class ObjectModelService extends ModelService {

  getParameter(key: string): Observable<any> {
    return this.store.select(this.modelName, key);
  }

  setParameter(key: string, value: any): void {
    let payload = {};
    payload[key] = value;
    this.setParameters(payload);
  }

  setParameters(params: any): void {
    this.store.dispatch({
      type: storeActions.SET,
      payload: params
    });
  }

}

export function collectionReducer(state: any[] = [], action: Action): any {
  switch (action.type) {
    case storeActions.ADD:
      return [...state, ...action.payload];
    case storeActions.DELETE:
      return state.filter(instance => {
        return instance.id !== action.payload.id;
      });
    case storeActions.CLEAR:
      return [];
    default:
      return state;
  }
}

export function objectReducer(state: any = {}, action: Action): any {
  switch (action.type) {
    case storeActions.SET:
      return Object.assign({}, state, action.payload);
    default:
      return state;
  }
}
