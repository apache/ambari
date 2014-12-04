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
  agents: DS.hasMany('App.FlumeAgent'),
  flumeHandlersTotal: DS.attr('number')
});

App.FlumeAgent = DS.Model.extend({
  /**
   * ID of a flume agent will be of the format
   * '<agent-name>-<host-name>'
   */
  id: DS.attr('string'),
  name: DS.attr('string'),
  /**
   * Status of agent. One of 'RUNNING', 'NOT_RUNNING', 'UNKNOWN'.
   */
  status: DS.attr('string'),
  host: DS.belongsTo('App.Host'),
  hostName: DS.attr('string'),

  channelsCount: DS.attr('number'),
  sourcesCount: DS.attr('number'),
  sinksCount: DS.attr('number'),

  healthClass : function() {
    switch (this.get('status')) {
    case 'RUNNING':
      return App.healthIconClassGreen;
      break;
    case 'NOT_RUNNING':
      return App.healthIconClassRed;
      break;
    case 'UNKNOWN':
    default:
      return App.healthIconClassYellow;
      break;
    }
  }.property('status'),

  displayStatus : function() {
    switch (this.get('status')) {
      case 'RUNNING':
        return "Running";
        break;
      case 'NOT_RUNNING':
        return "Stopped";
        break;
      case 'UNKNOWN':
      default:
        return "Unknown";
        break;
    }
  }.property('status')

});

App.FlumeAgent.FIXTURES = [];
App.FlumeService.FIXTURES = [];
