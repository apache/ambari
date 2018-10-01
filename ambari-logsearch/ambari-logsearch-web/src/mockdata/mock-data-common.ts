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

import * as moment from 'moment';
import * as randomize from 'randomatic';

export const clusters: string[] = ['cl0', 'cl1', 'cl2'];

export const hosts: string[] = ['c64001', 'c64002', 'c64003'];

export const services: string[] = ['hdfs', 'ambari'];

export const users: string[] = ['hdfs', 'admin', 'user'];

export const components = [
  'ambari_agent',
  'hdfs_secondarynamenode',
  'infra_solr',
  'logsearch_app',
  'logsearch_feeder'
];

export const levels = [
  'INFO',
  'WARN',
  'ERROR',
  'FATAL',
  'DEBUG'
];

export function ucFirst(str) {
  return str.charAt(0).toUpperCase() + str.slice(1);
}

export function getRandomInt(max) {
  return Math.floor(Math.random() * Math.floor(max));
}

export function getRandomElement(list: Array<any>) {
  return list[getRandomInt(list.length)];
}

export function generatePath(
  c: number = 3,
  addComponent: boolean | string = true,
  addService: boolean | string = false,
  folderNameMaxLength: number = 12
): string {
  let path = '/var/log';
  if (addService) {
    path += ('/' + (addService === true ? getRandomElement(services) : addService));
    c -= 1;
  }
  if (addComponent) {
    path += ('/' + (addComponent === true ? getRandomElement(components) : addComponent));
    c -= 1;
  }
  for (let i = 0; i < c; i += 1) {
    path += ('/' + randomize('Aa0?', getRandomInt(folderNameMaxLength), {chars: '-_'}));
  }
  return path;
}

export function generateServiceLog(defaults?: {[key:string]: any}) {
  const component = (defaults && defaults.type) || getRandomElement(components);
  const host = (defaults && defaults.host) || getRandomElement(hosts);
  return Object.assign({
    'id': randomize('a0', 32, {chars: '-'}),
    'bundle_id': null,
    'case_id': null,
    'cluster': getRandomElement(clusters),
    'seq_num': randomize('0', 5),
    'log_message': randomize('a0?a0', getRandomInt(1000), {chars: ' \n'}),
    'logfile_line_number': randomize('0', 4),
    'event_dur_ms': null,
    'file': randomize('a0?a0', 16, {chars: '-_'}) + '.java',
    'type': component,
    'event_count': getRandomInt(1000),
    'event_md5': randomize('a0', 32),
    'message_md5': randomize('a0', 32),
    '_ttl_': `-${getRandomInt(30)}DAYS`,
    '_expire_at_': 1518188622956,
    '_version_': randomize('0', 20),
    '_router_field_': null,
    'level': getRandomElement(levels),
    'line_number': getRandomInt(999),
    'logtime': moment().subtract(getRandomInt(14), 'days').valueOf(),
    'ip': `${getRandomInt(255)}.${getRandomInt(255)}.${getRandomInt(255)}.${getRandomInt(255)}`,
    'path': generatePath(3, component) + '.json',
    'host': host + '.ambari.apache.org',
    'group': host + '.ambari.apache.org'
  }, defaults || {});
}

export function generateAuditLog(defaults?: {[key: string]: any}) {
  const component: string = (defaults && defaults.component)  || getRandomElement(components); // meta default
  const service: string = (defaults && defaults.repo)  || getRandomElement(services);
  const time = moment().subtract(getRandomInt(14), 'days');
  return Object.assign({
    policy: 'policy',
    reason: randomize('aA', {
      length: getRandomInt(50),
      chars: ' .:'
    }),
    result: 0,
    text: randomize('aA', {
      length: getRandomInt(50),
      chars: ' .:'
    }),
    tags: [component],
    resource: '/' + component,
    sess: '0',
    access: '0',
    logType: ucFirst(service) + 'Audit',
    tags_str: component,
    resType: 'agent',
    reqUser: 'admin',
    reqData: 'data',
    repoType: 1,
    repo: service,
    proxyUsers: ['admin'],
    evtTime: time.valueOf(),
    enforcer: service + '-acl',
    reqContext: service,
    cliType: getRandomElement(['GET', 'POST']),
    cliIP: '192.168.0.1',
    agent: 'agent',
    agentHost: 'localhost',
    action: 'SERVICE_CHECK',
    type: service + '-audit',
    _version_: 2,
    id: 'id0',
    file: component + '.log',
    seq_num: 3,
    bundle_id: 'b0',
    case_id: 'c0',
    log_message: `User(${getRandomElement(users)}), Operation(SERVICE_CHECK)`,
    logfile_line_number: 4,
    message_md5: randomize('a0', 20),
    cluster: getRandomElement(clusters),
    event_count: getRandomInt(100),
    event_md5: randomize('0', 20),
    event_dur_ms: getRandomInt(900),
    _ttl_: '+7DAYS',
    _expire_at_: time.format(),
    _router_field_: getRandomInt(20)
  }, defaults || {});
}

export function generateDataCount(from, to, unit, gap) {
  let current = moment(from);
  const end = moment(to);
  const data = [];
  while (current.isBefore(end)) {
    data.push({
      name: current.toISOString(),
      value: getRandomInt(9000)
    });
    current = current.add(gap, unit);
  }
  return data;
}

export function generateGraphData(from, to, unit, gap) {
  return levels.map((level) => {
    return {
      dataCount: generateDataCount(from, to, unit, gap),
      name: level
    };
  });
}
