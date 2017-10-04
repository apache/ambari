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
  access: {
    displayName: 'logs.accessType',
    isDisplayed: true
  },
  action: {
    displayName: 'logs.action'
  },
  agent: {
    displayName: 'logs.agent'
  },
  agentHost: {
    displayName: 'logs.agentHost'
  },
  authType: {
    displayName: 'logs.authType'
  },
  bundle_id: {
    displayName: 'logs.bundleId'
  },
  case_id: {
    displayName: 'logs.caseId'
  },
  cliIP: {
    displayName: 'logs.clientIp',
    isDisplayed: true
  },
  cliType: {
    displayName: 'logs.clientType'
  },
  cluster: {
    displayName: 'logs.cluster'
  },
  dst: {
    displayName: 'logs.dst'
  },
  evtTime: {
    displayName: 'logs.eventTime',
    isDisplayed: true
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
    displayName: 'logs.level'
  },
  log_message: {
    displayName: 'logs.message'
  },
  logType: {
    displayName: 'logs.logType'
  },
  logfile_line_number: {
    displayName: 'logs.logfileLineNumber'
  },
  logger_name: {
    displayName: 'logs.loggerName'
  },
  logtime: {
    displayName: 'logs.logTime'
  },
  path: {
    displayName: 'logs.path'
  },
  perm: {
    displayName: 'logs.perm'
  },
  policy: {
    displayName: 'logs.policy'
  },
  proxyUsers: {
    displayName: 'logs.proxyUsers'
  },
  reason: {
    displayName: 'logs.reason'
  },
  repo: {
    displayName: 'logs.repo',
    isDisplayed: true
  },
  repoType: {
    displayName: 'logs.repoType'
  },
  req_caller_id: {
    displayName: 'logs.reqCallerId'
  },
  reqContext: {
    displayName: 'logs.reqContext'
  },
  reqData: {
    displayName: 'logs.reqData'
  },
  req_self_id: {
    displayName: 'logs.reqSelfId'
  },
  resType: {
    displayName: 'logs.resType'
  },
  resource: {
    displayName: 'logs.resource',
    isDisplayed: true
  },
  result: {
    displayName: 'logs.result',
    isDisplayed: true
  },
  sess: {
    displayName: 'logs.session'
  },
  text: {
    displayName: 'logs.text'
  },
  type: {
    displayName: 'logs.type'
  },
  ugi: {
    displayName: 'logs.ugi'
  },
  reqUser: {
    displayName: 'logs.user',
    isDisplayed: true
  },
  ws_base_url: {
    displayName: 'logs.baseUrl'
  },
  ws_command: {
    displayName: 'logs.command'
  },
  ws_component: {
    displayName: 'logs.component'
  },
  ws_details: {
    displayName: 'logs.details'
  },
  ws_display_name: {
    displayName: 'logs.displayName'
  },
  ws_os: {
    displayName: 'logs.os'
  },
  ws_repo_id: {
    displayName: 'logs.repoId'
  },
  ws_repo_version: {
    displayName: 'logs.repoVersion'
  },
  ws_repositories: {
    displayName: 'logs.repositories'
  },
  ws_request_id: {
    displayName: 'logs.requestId'
  },
  ws_result_status: {
    displayName: 'logs.resultStatus'
  },
  ws_roles: {
    displayName: 'logs.roles'
  },
  ws_stack_version: {
    displayName: 'logs.stackVersion'
  },
  ws_stack: {
    displayName: 'logs.stack'
  },
  ws_status: {
    displayName: 'logs.status'
  },
  ws_task_id: {
    displayName: 'logs.taskId'
  },
  ws_version_note: {
    displayName: 'logs.versionNote'
  },
  ws_version_number: {
    displayName: 'logs.versionNumber'
  },
  tags: {
    isAvailable: false
  },
  tags_str: {
    isAvailable: false
  },
  seq_num: {
    isAvailable: false
  }
};

export class AuditLogField extends LogField {
  constructor(name: string) {
    super(name);
    const preset = columnsNamesMap[this.name];
    if (preset) {
      Object.assign(this, preset);
    }
  }
}
