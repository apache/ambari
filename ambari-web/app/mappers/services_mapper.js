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
/*
App.servicesMapper = App.ServerDataMapper.create({
  map: function (json) {
    if (json.items) {
      json.items.forEach(function (data) {
        if (data.ServiceInfo) {
          var service = App.store.createRecord(App.Service, {
            id: data.ServiceInfo.service_name.toLowerCase(),
            serviceName: data.ServiceInfo.service_name.toLowerCase()
          });

          if (data.components) {
            var components = service.get('components');
            $.each(data.components, function (i, _component) {
              if (_component.ServiceComponentInfo) {
                var component = App.store.createRecord(App.ServiceComponent, {
                  id: _component.ServiceComponentInfo.component_name,
                  componentName: _component.ServiceComponentInfo.component_name
                });
                components.pushObject(component);

                if (_component.host_components) {
                  var hostComponents = component.get('hostComponents');
                  $.each(_component.host_components, function (j, _hostComponent) {
                    var _hostRoles = _hostComponent.HostRoles;
                    var hostComponent = App.store.createRecord(App.HostComponent, {
                      hostComponentId: _hostRoles.component_name + ":" + _hostRoles.host_name,
                      host: App.Host.find(_hostRoles.host_name),
                      hostName: _hostRoles.host_name,
                      state: _hostRoles.state
                    });
                    hostComponents.pushObject(hostComponent);
                  });
                }
              }
            });
          }
        }
      });
    }
  }
});
*/
App.servicesMapper = App.QuickDataMapper.create({
  config : {
    id : 'ServiceInfo.service_name',
    service_name : 'ServiceInfo.service_name',
    components_key : 'components',
    components : {
        id : 'ServiceComponentInfo.component_name',
        component_name : 'ServiceComponentInfo.component_name',
        //service_id : 'ServiceComponentInfo.service_name',
        service_name : 'ServiceComponentInfo.service_name',
        state: 'host_components[0].HostRoles.state',
        host_name: 'host_components[0].HostRoles.host_name'
      }
  },
  map:function(json){
    if(json.items){
      var result = [];
      json.items.forEach(function(item){
        result.push(this.parseIt(item, this.config));
      }, this)

      App.store.loadMany(App.Service1, result);
    }
  },
  parseIt : function(data, config){
    var result = {};
    for(var i in config){
      if(i.substr(-4) !== '_key' && typeof config[i] == 'string'){
        result[i] = this.getJsonProperty(data, config[i]);
      } else if(typeof config[i] == 'object'){
        result[i] = [];
        var _data = data[config[i+'_key']];
        var l = _data.length;
        for(var index = 0; index<l; index++){
          result[i].push(this.parseIt(_data[index], config[i]));
        }
      }
    }
    return result;
  },
  getJsonProperty:function(json, path){
    var pathArr = path.split('.');
    var current = json;
    while(pathArr.length){
      if(pathArr[0].substr(-1) == ']'){
        var index = parseInt(pathArr[0].substr(-2,1));
        var attr = pathArr[0].substr(0, pathArr[0].length-3);
        current = current[attr][index];
      } else {
        current = current[pathArr[0]];
      }
      pathArr.splice(0,1);
    }
    return current;
  }
});
