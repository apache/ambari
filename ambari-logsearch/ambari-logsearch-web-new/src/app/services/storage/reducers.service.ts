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

import {combineReducers} from '@ngrx/store';
import {appSettings} from '@app/services/storage/app-settings.service';
import {appState} from '@app/services/storage/app-state.service';
import {auditLogs} from '@app/services/storage/audit-logs.service';
import {clusters} from '@app/services/storage/clusters.service';
import {components} from '@app/services/storage/components.service';
import {filters} from '@app/services/storage/filters.service';
import {graphs} from '@app/services/storage/graphs.service';
import {nodes} from '@app/services/storage/nodes.service';
import {serviceLogs} from '@app/services/storage/service-logs.service';
import {serviceLogsHistogramData} from '@app/services/storage/service-logs-histogram-data.service';
import {userConfigs} from '@app/services/storage/user-configs.service';

export const reducers = {
  appSettings,
  appState,
  auditLogs,
  serviceLogs,
  serviceLogsHistogramData,
  graphs,
  nodes,
  userConfigs,
  filters,
  clusters,
  components
};

export function reducer(state: any, action: any) {
  return (combineReducers(reducers))(state, action);
}
