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


App.targetClusterMapper = App.QuickDataMapper.create({
  model: App.TargetCluster,
  config: {
    id: 'name',
    name: 'name',
    execute: 'execute',
    workflow: 'workflow',
    write: 'write',
    readonly: 'readonly',
    staging: 'staging',
    working: 'working',
    temp: 'temp'
  },

  map: function (json) {
    var model = this.get('model');
    if (!model) {
      return;
    }

    if (json.items) {
      var result = [];
      var clustersToDelete = model.find().mapProperty('name');
      json.items.forEach(function (item) {
        result.push(this.parseIt(item, this.config));
        clustersToDelete = clustersToDelete.without(item.name);
      }, this);
      clustersToDelete.forEach(function (name) {
        this.deleteRecord(model.find().findProperty('name', name));
      }, this);
      App.store.safeLoadMany(model, result);
    }
  }
});
