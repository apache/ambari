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

var stringUtils = require('utils/string_utils');

App.alertGroupsMapper = App.QuickDataMapper.create({

  model: App.AlertGroup,

  config: {
    id: 'AlertGroup.id',
    name: 'AlertGroup.name',
    default: 'AlertGroup.default',
    definitions: 'AlertGroup.definitions',
    targets: 'AlertGroup.targets'
  },

  map: function (json) {
    if (json && json.items) {
      var alertGroups = [];
      var self = this;
      json.items.forEach (function(item) {
       alertGroups.push(self.parseIt(item, self.get('config')));
      }, this);
      App.store.loadMany(this.get('model'), alertGroups);
    }
  }
});
