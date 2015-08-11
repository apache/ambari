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
 * THIS IS NOT USED FOR NOW
 * FOR CONFIG GROUPS WE ARE USING OLD MODELS AND LOGIC
 */

var App = require('app');

App.ServiceConfigGroup = DS.Model.extend({
  /**
   * unique id generated as <code>serviceName<code><code>configGroupId<code>
   * in case default configGroup <code>serviceName<code><code>0<code>
   * @property {string}
   */
  id: DS.attr('string'),

  /**
   * original id for config group that is get from server
   * for default groups "-1"
   * @property {number}
   */
  configGroupId: DS.attr('number'),

  name: DS.attr('string'),
  serviceName: DS.attr('string'),
  description: DS.attr('string'),
  hostNames: DS.attr('array'),
  configVersions: DS.hasMany('App.ConfigVersion'),
  service: DS.belongsTo('App.Service'),
  desiredConfigs: DS.attr('array'),

  /**
   * all hosts that belong to cluster
   */
  clusterHostsBinding: 'App.router.manageConfigGroupsController.clusterHosts',

  /**
   * Hosts on which this configuration-group
   * is to be applied. For a service, a host can
   * belong to only one non-default configuration-group.
   *
   * When {#isDefault} is false, this contains hosts
   * for which the overrides will apply.
   *
   * When {#isDefault} is true, this value is empty, as
   * it dynamically reflects hosts not belonging to other
   * non-default groups.
   * @type {Array}
   */
  hosts: function() {
    return this.get('hostNames');
  }.property('hostNames'),

  /**
   * defines if group is default
   * @type {boolean}
   */
  isDefault: function() {
    return this.get('configGroupId') == "-1";
  }.property('configGroupId'),

  /**
   * list of group names that shows which config
   * groups should be updated as dependent when current is changed
   * @type App.ServiceConfigGroup[]
   */
  dependentConfigGroups: DS.attr('object', {defaultValue: {}}),

  /**
   * Parent configuration group for this group.
   * When {@link #isDefault} is true, this value is <code>null</code>
   * When {@link #isDefault} is false, this represents the configuration
   * deltas that are applied on the default.
   */
  parentConfigGroup: DS.belongsTo('App.ServiceConfigGroup'),

  /**
   * Children configuration groups for this group.
   * When {@link #isDefault} is false, this value is <code>null</code>
   * When {@link #isDefault} is true, this represents the various
   * configuration groups that override the default.
   */
  childConfigGroups: DS.hasMany('App.ServiceConfigGroup'),

  hash: DS.attr('string'),
  /**
   * Provides a display friendly name. This includes trimming
   * names to a certain length.
   */
  displayName: function () {
    var name = this.get('name');
    if (name && name.length>App.config.CONFIG_GROUP_NAME_MAX_LENGTH) {
      var middle = Math.floor(App.config.CONFIG_GROUP_NAME_MAX_LENGTH / 2);
      name = name.substring(0, middle) + "..." + name.substring(name.length-middle);
    }
    return name;
  }.property('name'),

  /**
   *
   */
  displayNameHosts: function () {
    return this.get('displayName') + ' (' + this.get('hostNames.length') + ')';
  }.property('displayName', 'hostNames.length'),

  /**
   * Provides hosts which are available for inclusion in
   * non-default configuration groups.
   */
  availableHosts: function () {
    if (this.get('isDefault')) return [];
    var unusedHostsMap = {};
    var availableHosts = [];
    var sharedHosts = this.get('clusterHosts');
    // parentConfigGroup.hosts(hosts from default group) - are available hosts, which don't belong to any group
    this.get('parentConfigGroup.hosts').forEach(function (hostName) {
      unusedHostsMap[hostName] = true;
    });
    sharedHosts.forEach(function (host) {
      if (unusedHostsMap[host.get('id')]) {
        availableHosts.pushObject(Ember.Object.create({
          selected: false,
          host: host,
          hostComponentNames: host.get('hostComponents').mapProperty('componentName')
        }));
      }
    });
    return availableHosts;
  }.property('isDefault', 'parentConfigGroup', 'childConfigGroups', 'parentConfigGroup.hosts.@each', 'clusterHosts'),

  isAddHostsDisabled: function () {
    return (this.get('isDefault') || this.get('availableHosts.length') === 0);
  }.property('availableHosts.length'),

  /**
   * @type {Array}
   */
  properties: [],

  propertiesList: function () {
    var result = '';
    this.get('properties').forEach(function (item) {
      result += item.name + " : " + item.value + '<br/>';
    }, this);
    return result;
  }.property('properties.length')
});

App.ServiceConfigGroup.FIXTURES = [];
