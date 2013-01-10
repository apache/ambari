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


App.alertsMapper = App.QuickDataMapper.create({
  model: App.Alert,
  config:{
    $alert_id:'' ,
    title: "service_description",
    service_type: "service_type",
    date: "last_hard_state_change",
    status: "current_state",
    message: "plugin_output",
    host_name: "host_name",
    current_attempt: "current_attempt",
    last_hard_state_change: "last_hard_state_change",
    last_hard_state: "last_hard_state",
    last_time_ok: "last_time_ok",
    last_time_warning: "last_time_warning",
    last_time_unknown: "last_time_unknown",
    last_time_critical: "last_time_critical",
    is_flapping: "is_flapping",
    last_check: "last_check"
  },
  map: function (json) {
    if (!this.get('model')) {
      return;
    }
    if (json.alerts) {
      if (App.Alert.find().content.length > 0) {
        this.update(json);
      } else {
        var result = [];
        json.alerts.forEach(function (item) {
          result.push(this.parseIt(item, this.config));
        }, this);
        App.store.loadMany(this.get('model'), result);
      }
    }
  },
  update: function(json){
    var alertsList = App.Alert.find();
    var titleToAlertMap = {};
    alertsList.forEach(function(alert){
      titleToAlertMap[alert.get('serviceType') + alert.get('title') + alert.get('message')] = alert;
    });
    var newRecords = [];
    json.alerts.forEach(function(item){
      var existAlert = titleToAlertMap[item.service_type + item.service_description + item.plugin_output];
      if (existAlert == null) {
        newRecords.push(this.parseIt(item, this.config));
      } else {
        // update record
        existAlert.set('serviceType', item.service_type);
        existAlert.set('date', DS.attr.transforms.date.from(item.last_hard_state_change));
        existAlert.set('status', item.current_state);
        existAlert.set('message', item.plugin_output);
        existAlert.set('lastHardStateChange', item.last_hard_state_change);
        existAlert.set('lastHardState', item.last_hard_state);
        existAlert.set('lastTimeOk', item.last_time_ok);
        existAlert.set('lastTimeWarning', item.last_time_warning);
        existAlert.set('lastTimeUnknown', item.last_time_unknown);
        existAlert.set('lastTimeCritical', item.last_time_critical);
        existAlert.set('lastCheck', item.last_check);
        existAlert.set('isFlapping', item.is_flapping);
        delete titleToAlertMap[item.service_type + item.service_description + item.plugin_output];
      }
    }, this);
    for ( var e in titleToAlertMap) {
      titleToAlertMap[e].deleteRecord();
    }
    if (newRecords.length > 0) {
      App.store.loadMany(this.get('model'), newRecords); // Add new records
    }
  }
});
