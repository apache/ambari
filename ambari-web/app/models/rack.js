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


var App = require('app');

App.Rack = DS.Model.extend({
  name: DS.attr('string'),
  hosts: DS.hasMany('App.Host'),
  status: DS.attr('string'),
  liveHostsCount: DS.attr('number'),
  criticalHostsCount: DS.attr('number'),
  deadHostsCount: DS.attr('number')
});

App.Rack.FIXTURES = [
  {
    id: 1,
    name: 'Rack-0',
    hosts: ['host01', 'host06', 'host05'],
    status: 'LIVE',
    live_hosts_count: 5,
    critical_hosts_count: 0,
    dead_hosts_count: 2
  },
  {
    id: 2,
    name: 'Rack-1',
    hosts: ['host04', 'host02', 'host03'],
    status: 'LIVE',
    live_hosts_count: 2,
    critical_hosts_count: 0,
    dead_hosts_count: 1
  },
  {
    id: 3,
    name: 'Rack-2',
    hosts: [1, 2, 3, 4, 8, 9, 10],
    status: 'LIVE',
    live_hosts_count: 5,
    critical_hosts_count: 0,
    dead_hosts_count: 2
  },
  {
    id: 4,
    name: 'Rack-3',
    hosts: [5, 6, 7],
    status: 'LIVE',
    live_hosts_count: 2,
    critical_hosts_count: 0,
    dead_hosts_count: 1
  },
  {
    id: 5,
    name: 'Rack-4',
    hosts: [1, 2, 3, 4, 8, 9, 10],
    status: 'LIVE',
    live_hosts_count: 5,
    critical_hosts_count: 0,
    dead_hosts_count: 2
  },
  {
    id: 6,
    name: 'Rack-5',
    hosts: [5, 6, 7],
    status: 'LIVE',
    live_hosts_count: 2,
    critical_hosts_count: 0,
    dead_hosts_count: 1
  }
];