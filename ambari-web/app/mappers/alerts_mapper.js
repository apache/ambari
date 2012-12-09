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

//todo: refactor it
App.alertsMapper = App.ServerDataMapper.create({
  map: function (json) {
    if (json.alerts) {
      $.each(json.alerts, function (i, _alert) {
        var alert = App.store.createRecord(App.Alert, {
          alertId: _alert.service_description,
          title: _alert.service_description,
          serviceType: _alert.service_type,
          date: new Date(_alert.last_hard_state_change * 1000),
          status: _alert.current_state,
          message: _alert.plugin_output
        });
      });
    }
  }
});
