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
  }.property('sinks')
});

App.FlumeService.FIXTURES = [ {
  version: '1.4.0',
  agents: [ 0, 1 ]
} ];
App.FlumeAgent.FIXTURES = [ {
  id: 0,
  name: 'a1_dev01',
  status: 'INSTALLED',
  host: 'dev01',
  channels: 'c1,c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2'
}, {
  id: 1,
  name: 'a2_dev01',
  status: 'STARTED',
  host: 'dev01',
  channels: 'c2',
  sources: 'so1,so2,so3,so4',
  sinks: 'si1,si2,si3,si4'
} ];
