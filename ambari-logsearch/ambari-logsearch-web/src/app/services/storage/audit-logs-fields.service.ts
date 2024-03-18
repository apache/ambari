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
import {Store} from '@ngrx/store';
import {AppStore, ObjectModelService, getObjectReducer} from '@app/classes/models/store';
import {LogField} from "@app/classes/object";
import {Observable} from "rxjs/Observable";

export const modelName = 'auditLogsFields';

export const enum ResponseRootProperties {
  DEFAULTS = 'defaults',
  OVERRIDES = 'overrides'
};

@Injectable()
export class AuditLogsFieldsService extends ObjectModelService {
  constructor(store: Store<AppStore>) {
    super(modelName, store);
  }

  /**
   * The goal is to return with the proper fieldset for a given group/service and to return with the default fieldset
   * when the group has no overrides.
   * @param {string} group The name of the group/service
   * @returns {Observable<LogField[]>}
   */
  getFieldSetForGroup(group: string): Observable<LogField[]> {
    return Observable.combineLatest(this.getParameter(ResponseRootProperties.DEFAULTS), this.getParameter(ResponseRootProperties.OVERRIDES))
      .map(([defaults, overrides]): LogField[] => {
        return overrides[group] || defaults;
      });
  }

  /**
   * The goal is to update the given fieldset group with the given modifier function. It will map over the selected
   * group. Right now we let to change the defaults fieldset.
   * @param {Function} modifier Called by the map method.
   * @param {string} group The service/group name owner of the fieldset
   */
  mapFieldSetGroup(modifier: Function, group: string) {
    Observable.combineLatest(
      this.getParameter(ResponseRootProperties.DEFAULTS),
      this.getParameter(ResponseRootProperties.OVERRIDES)
    ).first().subscribe(([defaults, overrides]) => {
      const fieldset = (overrides[group] || defaults).map(modifier);
      const payload = group === ResponseRootProperties.DEFAULTS ? fieldset : Object.assign({}, overrides, {
        [group]: fieldset
      });
      this.setParameter(
        group === ResponseRootProperties.DEFAULTS ? ResponseRootProperties.DEFAULTS : ResponseRootProperties.OVERRIDES,
        payload
      );
    });
  }

}

export const auditLogsFields = getObjectReducer(modelName);
