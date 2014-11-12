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

var App = require('app');

App.rootServiceMapper = App.QuickDataMapper.create({
  root_service_model: App.RootService,
  root_service_component_model: App.RootServiceComponents,

  configRootService: {
    id: 'service_name',
    service_name: 'service_name',
    components_key: 'components',
    components_type: 'array',
    components: {
      item: 'id'
    }
  },

  configRootServiceComponents: {
    id: 'component_name',
    component_name: 'component_name',
    component_version: 'component_version',
    server_clock: 'server_clock',
    service_name: 'service_name',
    properties: 'properties'
  },

  map: function (data) {
    var rootServiceModel = this.get('root_service_model');
    var rootServiceComponentModel = this.get('root_service_component_model');
    var rootService = {}, rootServiceComponents = [];
    rootService.id = rootService.service_name = data.RootService.service_name;
    data.components.forEach(function (item) {
      item.RootServiceComponents.id = item.RootServiceComponents.component_name;
      rootServiceComponents.push(this.parseIt(item.RootServiceComponents, this.configRootServiceComponents));
    }, this);
    rootService.components =  rootServiceComponents;
    App.store.commit();
    App.store.loadMany(rootServiceComponentModel, rootServiceComponents);
    App.store.load(rootServiceModel, this.parseIt(rootService, this.configRootService));
  }
});
