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

/**
 * Here will be stored slave functions related to components
 * @type {Object}
 */

var App = require('app');
module.exports = {

  /**
   * This needs to be done because mapper functions like App.stackServiceComponentMapper.map(data) does not override
   * but unions the instances. So on re-navigation if the stack is switched and this function is not called then union of
   * StackServiceComponent of both the stacks will be mapped to the model.
   */
  clearStackModel: function() {
    if (App.StackServiceComponent.find().get('content').length) {
      App.StackServiceComponent.find().set('content', []);
    }
  },

  /**
   * Format and load info about components to StackServiceComponent model.
   *
   * @method loadStackServiceComponentModel
   * @param data {object} response from server
   * @return {object} formatted info about components
   */
  loadStackServiceComponentModel: function(data) {
    this.clearStackModel();
    var serviceComponents = {items: []};
    data.items.forEach(function(item){
      item.serviceComponents.forEach(function(_serviceComponent){
        var stackServiceComponents =  _serviceComponent.StackServiceComponents;
        var serviceComponent = {
          component_name: stackServiceComponents.component_name,
          service_name: stackServiceComponents.service_name,
          component_category: stackServiceComponents.component_category,
          is_master: stackServiceComponents.is_master,
          is_client: stackServiceComponents.is_client,
          stack_name: stackServiceComponents.stack_name,
          stack_version: stackServiceComponents.stack_version
        };
        serviceComponents.items.pushObject(serviceComponent);
      }, this);
    }, this);
    App.stackServiceComponentMapper.map(serviceComponents);
    App.handleStackDependedComponents();
    return serviceComponents;
  }
};
