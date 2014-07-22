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

App.serviceConfigVersionsMapper = App.QuickDataMapper.create({
  model: App.ServiceConfigVersion,
  config: {
    service_name: 'servicename',
    version: "serviceconfigversion",
    create_time: 'createtime',
    applied_time: 'appliedtime',
    author: 'author',
    notes: 'notes'
  },
  map: function (json) {
    var result = [];

    if (json && json.items) {
      json.items.forEach(function (item) {
        var parsedItem = this.parseIt(item, this.get('config'));
        parsedItem.id = parsedItem.service_name + '_' + parsedItem.version;
        result.push(parsedItem);
      }, this);

      App.store.loadMany(this.get('model'), result);
    }
  }
});
