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

App.stackServiceComponentMapper = App.QuickDataMapper.create({
  model: App.StackServiceComponent,
  config: {
    id: 'component_name',
    component_name: 'component_name',
    service_name: 'service_name',
    component_category: 'component_category',
    is_master: 'is_master',
    is_client: 'is_client',
    stack_name: 'stack_name',
    stack_version: 'stack_version'
  },

  map: function (json) {
    var model = this.get('model');
    if (json.items) {
      var result = [];
      json.items.forEach(function (item) {
        result.push(this.parseIt(item, this.config));
      }, this);
      App.store.loadMany(model, result);
    }
  }
});