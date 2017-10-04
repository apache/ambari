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

import {LogField} from '@app/classes/models/log-field';

const columnsNamesMap = {
  log_message: {
    displayName: 'logs.message',
    isDisplayed: true
  },
  bundle_id: {
    displayName: 'logs.bundleId'
  },
  case_id: {
    displayName: 'logs.caseId'
  },
  cluster: {
    displayName: 'logs.cluster'
  },
  event_count: {
    displayName: 'logs.eventCount'
  },
  file: {
    displayName: 'logs.file'
  },
  host: {
    displayName: 'logs.host'
  },
  id: {
    displayName: 'logs.id'
  },
  ip: {
    displayName: 'logs.ip'
  },
  level: {
    displayName: 'logs.level',
    isDisplayed: true
  },
  line_number: {
    displayName: 'logs.lineNumber'
  },
  logtype: {
    displayName: 'logs.logType'
  },
  logfile_line_number: {
    displayName: 'logs.logfileLineNumber'
  },
  logger_name: {
    displayName: 'logs.loggerName'
  },
  logtime: {
    isDisplayed: true
  },
  method: {
    displayName: 'logs.method'
  },
  path: {
    displayName: 'logs.path'
  },
  rowtype: {
    displayName: 'logs.rowType'
  },
  thread_name: {
    displayName: 'logs.threadName'
  },
  type: {
    displayName: 'logs.type',
    isDisplayed: true
  },
  tags: {
    isAvailable: false
  },
  text: {
    isAvailable: false
  },
  message: {
    isAvailable: false
  },
  seq_num: {
    isAvailable: false
  }
};

export class ServiceLogField extends LogField {
  constructor(name: string) {
    super(name);
    const preset = columnsNamesMap[this.name];
    if (preset) {
      Object.assign(this, preset);
    }
  }
}
