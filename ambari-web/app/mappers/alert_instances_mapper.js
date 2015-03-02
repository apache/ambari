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

App.alertInstanceMapper = App.QuickDataMapper.create({

  model : App.AlertInstance,

  config : {
    id: 'Alert.id',
    label: 'Alert.label',
    definition_name: 'Alert.definition_name',
    definition_id: 'Alert.definition_id',
    service_id: 'Alert.service_name',
    service_name: 'Alert.service_name',
    component_name: 'Alert.component_name',
    host_id: 'Alert.host_name',
    host_name: 'Alert.host_name',
    scope: 'Alert.scope',
    original_timestamp: 'Alert.original_timestamp',
    latest_timestamp: 'Alert.latest_timestamp',
    maintenance_state: 'Alert.maintenance_state',
    instance: 'Alert.instance',
    state: 'Alert.state',
    text: 'Alert.text'
  },

  map: function(json, skipDelete) {
    if (json.items) {
      var alertInstances = (skipDelete) ? this.mapWithoutDelete(json) : this.mapAndDelete(json);

      App.store.loadMany(this.get('model'), alertInstances);
    }
  },

  /**
   * method that used when we not on alert definition state
   * in this case we need to delete alerts that is not critical and not warning
   * @param json
   * @returns {Array}
   */
  mapAndDelete: function(json) {
    var self = this,
      alertInstances = [],
      model = this.get('model'),
      alertsToDelete = model.find().mapProperty('id');

    json.items.forEach(function (item) {
      var alert = this.parseIt(item, this.get('config'));
      alertInstances.push(alert);
      alertsToDelete = alertsToDelete.without(alert.id);
    }, this);


    alertsToDelete.forEach(function(alertId) {
      var item = model.find(alertId);
      self.deleteRecord(item);
    });

    return alertInstances;
  },

  /**
   * this method is used on alert definition page
   * @param json
   * @returns {Array}
   */
  mapWithoutDelete: function(json) {
    var alertInstances = [];
    json.items.forEach(function (item) {
      var alert = this.parseIt(item, this.get('config'));
      alertInstances.push(alert);
    }, this);
    return alertInstances;
  }

});
