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

App.alertDefinitionSummaryMapper = App.QuickDataMapper.create({

  config: {},

  map: function(data) {
    if (!data.alerts_summary_grouped) return;
    var alertDefinitions = App.AlertDefinition.getAllDefinitions();
    data.alerts_summary_grouped.forEach(function(alertDefinitionSummary) {
      var alertDefinition = alertDefinitions.findProperty('id', alertDefinitionSummary.definition_id);
      if (alertDefinition) {
        var summary = {},
          timestamp = 0;
        Em.keys(alertDefinitionSummary.summary).forEach(function(status) {
          summary[status] = alertDefinitionSummary.summary[status].count;
          if (alertDefinitionSummary.summary[status].original_timestamp > timestamp) {
            timestamp = alertDefinitionSummary.summary[status].original_timestamp;
          }
        });
        alertDefinition.setProperties({
          summary: summary,
          lastTriggered: parseInt(timestamp)
        });
      }
    });
  }
});
