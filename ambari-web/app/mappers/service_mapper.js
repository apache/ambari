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

App.serviceMapper = App.QuickDataMapper.create({
  map: function (json) {
    console.time("App.serviceMapper execution time");

    json.items.forEach(function (service) {
      var cachedService = App.cache['services'].findProperty('ServiceInfo.service_name', service.ServiceInfo.service_name);
      if (cachedService) {
        // restore service workStatus
        App.Service.find(cachedService.ServiceInfo.service_name).set('workStatus', service.ServiceInfo.state);
        cachedService.ServiceInfo.state = service.ServiceInfo.state;
        cachedService.ServiceInfo.passive_state = service.ServiceInfo.maintenance_state;

        //check whether Nagios installed and started
        if (service.alerts) {
          cachedService.ServiceInfo.critical_alerts_count = service.alerts.summary.CRITICAL + service.alerts.summary.WARNING;
        }
      } else {
        var serviceData = {
          ServiceInfo: {
            service_name: service.ServiceInfo.service_name,
            state: service.ServiceInfo.state,
            passive_state: service.ServiceInfo.maintenance_state
          },
          host_components: [],
          components: []
        };

        //check whether Nagios installed and started
        if (service.alerts) {
          serviceData.ServiceInfo.critical_alerts_count = service.alerts.summary.CRITICAL + service.alerts.summary.WARNING;
        }
        App.cache['services'].push(serviceData);
      }
    });

    console.timeEnd("App.serviceMapper execution time");
  }
});
