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

App.alertNotificationMapper = App.QuickDataMapper.create({
  model: App.AlertNotification,
  config: {
    id: 'AlertTarget.id',
    name: 'AlertTarget.name',
    type: 'AlertTarget.notification_type',
    description: 'AlertTarget.description'
  },

  map: function (json) {
    if (json.items) {
      var result = [];
      var notificationsProperties = {};
      var notificationsAlertStates = {};

      json.items.forEach(function (item) {
        result.push(this.parseIt(item, this.config));
        notificationsProperties[item.AlertTarget.id] = item.AlertTarget.properties;
        notificationsAlertStates[item.AlertTarget.id] = item.AlertTarget.alert_states;
      }, this);

      App.store.loadMany(this.get('model'), result);
      this.setProperties('properties', notificationsProperties);
      this.setProperties('alertStates', notificationsAlertStates);
    }
  },

  /**
   * Set values from <code>propertyMap</code> for <code>propertyName</code> for each record in model
   * @param propertyName
   * @param propertiesMap record_id to value map
   */
  setProperties: function (propertyName, propertiesMap) {
    var modelRecords = this.get('model').find();
    for (var recordId in propertiesMap) {
      if (propertiesMap.hasOwnProperty(recordId)) {
        modelRecords.findProperty('id', +recordId).set(propertyName, propertiesMap[recordId]);
      }
    }
  }
});