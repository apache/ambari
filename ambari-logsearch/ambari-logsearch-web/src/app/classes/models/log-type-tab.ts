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

import {HomogeneousObject} from '@app/classes/object';

export interface LogTypeTab {
  id: string;
  isActive?: boolean;
  isCloseable?: boolean;
  label: string;
  activeFilters?: object;
  appState?: HomogeneousObject<any>;
}

export const initialTabs: LogTypeTab[] = [
  {
    id: 'serviceLogs',
    isActive: true,
    label: 'common.serviceLogs',
    activeFilters: null,
    appState: {
      activeLogsType: 'serviceLogs',
      isServiceLogsFileView: false
    }
  },
  {
    id: 'auditLogs',
    isActive: false,
    label: 'common.auditLogs',
    activeFilters: null,
    appState: {
      activeLogsType: 'auditLogs',
      isServiceLogsFileView: false
    }
  }
];
