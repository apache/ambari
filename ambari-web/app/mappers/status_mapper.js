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

  config2:{
    id:'ServiceComponentInfo.component_name',
    work_status:'host_components[0].HostRoles.state'
  },

  config3:{
    id:'id',
    work_status:'HostRoles.state'
  },

  map:function (json) {

    if (json.items) {
      var result = [];
      json.items.forEach(function (item) {

        item.host_components = [];
        item.components.forEach(function (component) {
          component.host_components.forEach(function (host_component) {
            host_component.id = host_component.HostRoles.component_name + "_" + host_component.HostRoles.host_name;
            item.host_components.push(host_component.id);
          }, this)
        }, this);

        result.push(this.parseIt(item, this.config));
      }, this);

      //console.log(result)
      var services = App.Service.find();
      result.forEach(function(item){
        var service = services.findProperty('id', item.id);
        if(service){
          service.set('workStatus', item.work_status);
        }
      })

      result = [];
      json.items.forEach(function (item) {
        item.components.forEach(function (component) {
          result.push(this.parseIt(component, this.config2));
        }, this)
      }, this);

      //console.log(result)
      var components = App.Component.find();
      result.forEach(function(item){
        var component = components.findProperty('id', item.id);
        if(component){
          component.set('workStatus', item.work_status);
        }
      })

      result = [];
      json.items.forEach(function (item) {
        item.components.forEach(function (component) {
          component.host_components.forEach(function (host_component) {
            result.push(this.parseIt(host_component, this.config3));
          }, this)
        }, this)
      }, this);

      //console.log(result)
      var hostComponents = App.HostComponent.find();
      result.forEach(function(item){
        var hostComponent = hostComponents.findProperty('id', item.id);
        if(hostComponent){
          hostComponent.set('workStatus', item.work_status);
        }
      })
    }
  }
});
