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
  status: DS.attr('string'),
  criticalHostsCount: DS.attr('number'),
  deadHostsCount: DS.attr('number'),
  liveHostsCount: DS.attr('number'),
  hosts: []
});

App.Rack.FIXTURES = [
  //here example of data
  /*{
    id: 1,
    name: 'Rack-0',
    hosts: ['host01', 'host06', 'host05'],
    status: 'LIVE',
    live_hosts_count: 5,
    critical_hosts_count: 0,
    dead_hosts_count: 2
  }*/
];