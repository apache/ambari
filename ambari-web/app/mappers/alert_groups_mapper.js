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

/**
 * Mapper for <code>App.AlertGroup</code>
 * Save general information
 * Use <code>App.cache['previousAlertGroupsMap']</code> to store map alertDefinitions-alertGroups. This map is used
 * in the <code>App.AlertDefinitionsMapper</code> to correctly link alertDefinitions and alertGroups
 */
App.alertGroupsMapper = App.QuickDataMapper.create({

  model: App.AlertGroup,

  config: {
    id: 'AlertGroup.id',
    name: 'AlertGroup.name',
    default: 'AlertGroup.default',
    targets: 'AlertGroup.targets'
  },

  /**
   * Map for alertGroup's alertDefinitions
   * Store alertDefinitions to alertGroup-properties basing on alertDefinitionType
   * Format: key - alertDefinitionType, value - alertGroup-property where alertDefinition should be saved
   * @type {object}
   */
  typesMap: {
    PORT: 'port_alert_definitions',
    METRIC: 'metrics_alert_definitions',
    WEB: 'web_alert_definitions',
    AGGREGATE: 'aggregate_alert_definitions',
    SCRIPT: 'script_alert_definitions'
  },

  map: function (json) {
    if (!Em.isNone(json, 'items')) {

      var alertGroups = [],
        self = this,
        typesMap = this.get('typesMap'),
        /**
         * AlertGroups-map for <code>App.AlertDefinitionsMappers</code>
         * Format:
         * <code>
         *   {
         *    alert_definition1_id: [alert_group1_id, alert_group2_id],
         *    alert_definition2_id: [alert_group3_id, alert_group1_id],
         *    ...
         *   }
         * </code>
         * @type {object}
         */
        alertDefinitionsGroupsMap = {};

      json.items.forEach(function(item) {
        var group = self.parseIt(item, self.get('config'));
        Em.keys(typesMap).forEach(function(k) {
          group[typesMap[k]] = [];
        });
        if (item.AlertGroup.definitions) {
          item.AlertGroup.definitions.forEach(function(definition) {
              var type = typesMap[definition.source_type];
            if (!group[type].contains(definition.id)) {
              group[type].push(definition.id);
            }
            if (Em.isNone(alertDefinitionsGroupsMap[definition.id])) {
              alertDefinitionsGroupsMap[definition.id] = [];
            }
            alertDefinitionsGroupsMap[definition.id].push(group.id);
          });
        }
        alertGroups.push(group);
      }, this);

      App.cache['previousAlertGroupsMap'] = alertDefinitionsGroupsMap;
      App.store.loadMany(this.get('model'), alertGroups);
      App.store.commit();
    }
  }
});
