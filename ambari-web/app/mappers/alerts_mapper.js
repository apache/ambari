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


var previousAlertsResponse = [];
var stringUtils = require('utils/string_utils');

/**
 * Fields, which are not displayed and used only to compute date property, listed below:
 * last_hard_state_change
 * last_hard_state
 * last_time_ok
 * last_time_warning
 * last_time_unknown
 * last_time_critical
 */
App.alertsMapper = App.QuickDataMapper.create({
  model: App.Alert,
  config: {
    id: 'id',
    title: "service_description",
    service_type: "service_type",
    last_time: "last_time",
    status: "current_state",
    message: "plugin_output",
    host_name: "host_name",
    current_attempt: "current_attempt",
    is_flapping: "is_flapping",
    last_check: "last_check"
  },
  map: function (json) {
    console.time('App.alertsMapper execution time');
    if (json && json.items && json.items.length > 0 && json.items[0].HostRoles && json.items[0].HostRoles.nagios_alerts) {
      if (json.items[0].HostRoles.nagios_alerts.alerts.length === 0) {
        //Clear Alerts model when NAGIOS stopped or doesn't have alerts anymore
        App.Alert.find().clear();
        console.log("NAGIOS stopped: all alerts deleted");
        return;
      }
      var alerts = json.items[0].HostRoles.nagios_alerts.alerts;
      var alertsMap = {};
      var addAlerts = [];
      var mutableFields = ['last_time', 'status', 'message', 'current_attempt', 'is_flapping', 'last_check'];

      alerts.forEach(function (item) {
        //id consists of combination of serviceType, title and hostName
        item.id = item.service_type + item.service_description + item.host_name;
        item.last_time = this.computeLastTime(item);
        var parsedItem = this.parseIt(item, this.config);
        alertsMap[item.id] = parsedItem;
        if (!previousAlertsResponse[item.id]) {
          addAlerts.push(parsedItem);
        }
      }, this);

      this.get('model').find().forEach(function (alertRecord) {
        if (alertRecord) {
          var existAlert = alertsMap[alertRecord.get('id')];
          if (existAlert) {
            existAlert = this.getDiscrepancies(existAlert, previousAlertsResponse[alertRecord.get('id')], mutableFields);
            if (existAlert) {
              for (var i in existAlert) {
                alertRecord.set(stringUtils.underScoreToCamelCase(i), existAlert[i]);
              }
            }
          } else {
            this.deleteRecord(alertRecord);
          }
        }
      }, this);

      if (addAlerts.length > 0) {
        App.store.loadMany(this.get('model'), addAlerts);
      }
      previousAlertsResponse = alertsMap;
    }
    console.timeEnd('App.alertsMapper execution time');
  },
  computeLastTime: function (item) {
    var dateMap = {
      '0': 'last_time_ok',
      '1': 'last_time_warning',
      '2': 'last_time_critical',
      '3': 'last_time_unknown'
    };
    if (item.current_state && item.last_hard_state && item.current_state != item.last_hard_state) {
      return item[dateMap[item.current_state]] || item['last_hard_state_change'];
    } else {
      return item['last_hard_state_change'];
    }
  }
});
