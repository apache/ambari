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

App.statusMapper = App.QuickDataMapper.create({

  config:{
    id:'ServiceInfo.service_name',
    work_status:'ServiceInfo.state'
  },

  config3:{
    id:'id',
    work_status:'HostRoles.state',
    desired_status: 'HostRoles.desired_state'
  },

  map:function (json) {
    var start = new Date().getTime();
    console.log('in status mapper');

    if (json.items) {
      var result = {};
      json.items.forEach(function (item) {
        item = this.parseIt(item, this.config);
        result[item.id] = item;
      }, this);

      var services = App.Service.find();
      services.forEach(function(service) {
        var item = result[service.get('id')];
        if (item) {
          service.set('workStatus', item.work_status);
        }
      });

      //host_components
      result = {};
      json.items.forEach(function (item) {
        item.components.forEach(function (component) {
          component.host_components.forEach(function (host_component) {
            host_component.id = host_component.HostRoles.component_name + "_" + host_component.HostRoles.host_name;
            result[host_component.id] = this.parseIt(host_component, this.config3);
          }, this)
        }, this)
      }, this);

      // console.profile("App.statusMapper.map() profile");

      var hostComponents = App.HostComponent.find();

      hostComponents.forEach(function(hostComponent) {
        var item = result[hostComponent.get('id')];
        if (item) {
         hostComponent.set('workStatus', item.work_status);
        }
      });

      // console.profileEnd();

      console.log('out status mapper.  Took ' + (new Date().getTime() - start) + 'ms');
    }
  }
});
