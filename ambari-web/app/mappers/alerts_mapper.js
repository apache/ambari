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
    last_wime_warning: "last_time_warning",
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
    var alerts = App.Alert.find();
    var result = [];
    json.alerts.forEach(function (item) {
      if (!alerts.filterProperty('title', item.service_description).length) {
        result.push(this.parseIt(item, this.config));
      }
    }, this);
    App.store.loadMany(this.get('model'), result);

  }
});
