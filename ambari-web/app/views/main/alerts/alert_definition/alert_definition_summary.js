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

App.AlertDefinitionSummary = Em.View.extend({

  templateName: require('templates/main/alerts/alert_definition/alert_definition_summary'),

  didInsertElement: function() {
    this.stateObserver();
  },

  hostCount: 0,
  states: [],

  stateObserver: function () {
    var order = this.get('content.order'),
      summary = this.get('content.summary'),
      shortState = this.get('content.shortState');

    var hostCnt = 0;
    order.forEach(function (state) {
      hostCnt += summary[state] ? summary[state].count + summary[state].maintenanceCount : 0;
    });
    var states = [];
    if (hostCnt) {
      order.forEach(function (state) {
        if (summary[state]) {
          if (summary[state].count) {
            states.push({
              'shortStateWithCounter': shortState[state] + (summary[state].count > 1 ? ' (' + summary[state].count + ')' : ''),
              'isMaintenance': false,
              'stateClass': 'alert-state-' + state
            });
          }
          if (summary[state].maintenanceCount) {
            states.push({
              'shortStateWithCounter': shortState[state] + (summary[state].maintenanceCount > 1 ? ' (' + summary[state].maintenanceCount + ')' : ''),
              'isMaintenance': true,
              'stateClass': 'alert-state-PENDING'
            });
          }
        }
      }, this);
    }
    this.set('hostCount', hostCnt);
    this.set('states', states);
  }.observes('content.summary')

});