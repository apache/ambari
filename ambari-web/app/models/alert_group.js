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

/**
 * Represents an alert-group on the cluster.
 * A alert group is a collection of alert definitions
 *
 * Alert group hierarchy is at 2 levels. For
 * each service there is a 'Default' alert group
 * containing all definitions , this group is read-only
 *
 * User can create new alert group containing alert definitions from
 * any service.
 */
App.AlertGroup = DS.Model.extend({
  id: null,
  name: null,
  description: null,
  default: null,
  definitions: [],
  targets: [],

  displayName: function () {
    var name = this.get('name');
    if (name && name.length > App.config.CONFIG_GROUP_NAME_MAX_LENGTH) {
      var middle = Math.floor(App.config.CONFIG_GROUP_NAME_MAX_LENGTH / 2);
      name = name.substring(0, middle) + "..." + name.substring(name.length-middle);
    }
    return this.get('default') ? (name + ' Default') : name;
  }.property('name', 'default'),

  displayNameDefinitions: function () {
    return this.get('displayName') + ' (' + this.get('definitions.length') + ')';
  }.property('displayName', 'definitions.length')
});
App.AlertGroup.FIXTURES = [];

App.AlertGroupComplex = Ember.Object.extend({
  id: null,
  name: null,
  description: null,
  default: null,
  definitions: [],
  targets: [],

  /**
   * all alert definitions that belong to all services
   */
  alertDefinitionsBinding: 'App.router.manageAlertGroupsController.alertDefinitions',

  displayName: function () {
    var name = this.get('name');
    if (name && name.length > App.config.CONFIG_GROUP_NAME_MAX_LENGTH) {
      var middle = Math.floor(App.config.CONFIG_GROUP_NAME_MAX_LENGTH / 2);
      name = name.substring(0, middle) + "..." + name.substring(name.length-middle);
    }
    return this.get('default') ? (name + ' Default') : name;
  }.property('name', 'default'),

  displayNameDefinitions: function () {
    return this.get('displayName') + ' (' + this.get('definitions.length') + ')';
  }.property('displayName', 'definitions.length'),

  /**
   * Provides alert definitions which are available for inclusion in
   * non-default alert groups.
   */
  availableDefinitions: function () {
    if (this.get('default')) return [];
    var usedDefinitionsMap = {};
    var availableDefinitions = [];
    var sharedDefinitions = this.get('alertDefinitions');

    this.get('definitions').forEach(function (def) {
      usedDefinitionsMap[def.name] = true;
    });
    sharedDefinitions.forEach(function (shared_def) {
      if (!usedDefinitionsMap[shared_def.get('name')]) {
        availableDefinitions.pushObject(shared_def);
      }
    });
    return availableDefinitions;
  }.property('alertDefinitions', 'definitions.@each', 'definitions.length'),

  isAddDefinitionsDisabled: function () {
    return (this.get('default') || this.get('availableDefinitions.length') === 0);
  }.property('availableDefinitions.length')
});


