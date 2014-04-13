/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.FlumeService = App.Service.extend({
  version: DS.attr('string'),
  agents: DS.hasMany('App.FlumeAgent')
});

App.FlumeAgent = DS.Model.extend({
  name: DS.attr('string'),
  /**
   * Status of agent. One of 'STARTED', 'INSTALLED', 'UNKNOWN'.
   */
  status: DS.attr('string'),
  host: DS.belongsTo('App.Host'),

  /**
   * A comma separated list of channels.
   */
  channels: DS.attr('string'),

  /**
   * A comma separated list of sources.
   */
  sources: DS.attr('string'),

  /**
   * A comma separated list of sinks.
   */
  sinks: DS.attr('string'),

  channelsCount: function() {
    var channels = this.get('channels');
    if (!channels) {
      return 0;
    } else {
      return channels.split(',').length;
    }
  }.property('channels'),

  sourcesCount: function() {
    var sources = this.get('sources');
    if (!sources) {
      return 0;
    } else {
      return sources.split(',').length;
    }
  }.property('sources'),

  sinksCount: function() {
    var sinks = this.get('sinks');
    if (!sinks) {
      return 0;
    } else {
      return sinks.split(',').length;
    }
  }.property('sinks'),

  healthClass : function() {
    switch (this.get('status')) {
    case 'STARTED':
    case 'STARTING':
      return App.healthIconClassGreen;
      break;
    case 'INSTALLED':
    case 'STOPPING':
      return App.healthIconClassRed;
      break;
    case 'UNKNOWN':
    default:
      return App.healthIconClassYellow;
      break;
    }
  }.property('status')
});

App.FlumeAgent.FIXTURES = [ {
  id: 1,
  name: 'a1_dev01',
  status: 'INSTALLED',
  host: 'dev01.hortonworks.com',
  channels: 'c1,c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2'
}, {
  id: 2,
  name: 'a2_dev01',
  status: 'STARTED',
  host: 'dev01.hortonworks.com',
  channels: 'c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2,si3,si4'
}, {
  id: 3,
  name: 'a3_dev01',
  status: 'UNKNOWN',
  host: 'dev01.hortonworks.com',
  channels: 'c1, c4',
  sources: 'so1',
  sinks: 'si1,si2'
}, {
  id: 4,
  name: 'a4_dev01',
  status: 'UNKNOWN',
  host: 'dev01.hortonworks.com',
  channels: 'c1,c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2'
}, {
  id: 5,
  name: 'a5_dev01',
  status: 'STARTED',
  host: 'dev01.hortonworks.com',
  channels: 'c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2,si3,si4'
}, {
  id: 6,
  name: 'a6_dev01',
  status: 'STARTED',
  host: 'dev01.hortonworks.com',
  channels: 'c1, c4',
  sources: 'so1',
  sinks: 'si1,si2'
}, {
  id: 7,
  name: 'a7_dev01',
  status: 'INSTALLED',
  host: 'dev01.hortonworks.com',
  channels: 'c1,c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2'
}, {
  id: 8,
  name: 'a8_dev01',
  status: 'STARTED',
  host: 'dev01.hortonworks.com',
  channels: 'c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2,si3,si4'
}, {
  id: 9,
  name: 'a9_dev01',
  status: 'UNKNOWN',
  host: 'dev01.hortonworks.com',
  channels: 'c1, c4',
  sources: 'so1',
  sinks: 'si1,si2'
}, {
  id: 10,
  name: 'a10_dev01',
  status: 'UNKNOWN',
  host: 'dev01.hortonworks.com',
  channels: 'c1,c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2'
}, {
  id: 11,
  name: 'a11_dev01',
  status: 'STARTED',
  host: 'dev01.hortonworks.com',
  channels: 'c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2,si3,si4'
}, {
  id: 12,
  name: 'a12_dev01',
  status: 'STARTED',
  host: 'dev01.hortonworks.com',
  channels: 'c1, c4',
  sources: 'so1',
  sinks: 'si1,si2'
}, {
  id: 13,
  name: 'a13_dev01',
  status: 'INSTALLED',
  host: 'dev01.hortonworks.com',
  channels: 'c1,c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2'
}, {
  id: 14,
  name: 'a14_dev01',
  status: 'STARTED',
  host: 'dev01.hortonworks.com',
  channels: 'c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2,si3,si4'
}, {
  id: 15,
  name: 'a15_dev01',
  status: 'UNKNOWN',
  host: 'dev01.hortonworks.com',
  channels: 'c1, c4',
  sources: 'so1',
  sinks: 'si1,si2'
}, {
  id: 16,
  name: 'a16_dev01',
  status: 'UNKNOWN',
  host: 'dev01.hortonworks.com',
  channels: 'c1,c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2'
}, {
  id: 17,
  name: 'a17_dev01',
  status: 'STARTED',
  host: 'dev01.hortonworks.com',
  channels: 'c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2,si3,si4'
}, {
  id: 18,
  name: 'a18_dev01',
  status: 'STARTED',
  host: 'dev01.hortonworks.com',
  channels: 'c1, c4',
  sources: 'so1',
  sinks: 'si1,si2'
}, {
  id: 19,
  name: 'a19_dev01',
  status: 'INSTALLED',
  host: 'dev01.hortonworks.com',
  channels: 'c1,c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2'
}, {
  id: 20,
  name: 'a20_dev01',
  status: 'STARTED',
  host: 'dev01.hortonworks.com',
  channels: 'c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2,si3,si4'
}, {
  id: 21,
  name: 'a21_dev01',
  status: 'UNKNOWN',
  host: 'dev01.hortonworks.com',
  channels: 'c1, c4',
  sources: 'so1',
  sinks: 'si1,si2'
}, {
  id: 22,
  name: 'a22_dev01',
  status: 'UNKNOWN',
  host: 'dev01.hortonworks.com',
  channels: 'c1,c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2'
}, {
  id: 23,
  name: 'a23_dev01',
  status: 'STARTED',
  host: 'dev01.hortonworks.com',
  channels: 'c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2,si3,si4'
}, {
  id: 24,
  name: 'a24_dev01',
  status: 'STARTED',
  host: 'dev01.hortonworks.com',
  channels: 'c1, c4',
  sources: 'so1',
  sinks: 'si1,si2'
} ];
App.FlumeService.FIXTURES = [ {
  id: 0,
  version: '1.4.0',
  service_name: 'FLUME',
  agents: [ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 ]
} ];
