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

App.ServiceConfigGroup = DS.Model.extend({
  /**
   * unique id generated as <code>serviceName<code><code>configGroupId<code>
   * in case default configGroup <code>serviceName<code><code>0<code>
   * @property {string}
   */
  id: DS.attr('string'),

  /**
   * original id for config group that is get from server
   * for default groups -1
   * @property {number}
   */
  configGroupId: DS.attr('number'),

  name: DS.attr('string'),
  serviceName: DS.attr('string'),
  description: DS.attr('string'),
  hosts: DS.attr('array'),
  configVersions: DS.hasMany('App.ConfigVersion'),
  service: DS.belongsTo('App.Service'),
  desiredConfigs: DS.attr('array', {defaultValue: []}),

  /**
   * this flag is used for installed services' config groups
   * if user make changes to them - mark this flag to true
   * @default [false]
   */
  isForUpdate: DS.attr('boolean', {defaultValue: false}),

  /**
   * mark config groups for installed services
   * @default [false]
   */
  isForInstalledService: DS.attr('boolean', {defaultValue: false}),

  /**
   * all hosts that belong to cluster
   */
  clusterHostsBinding: 'App.router.manageConfigGroupsController.clusterHosts',

  /**
   * defines if group is default
   * @type {boolean}
   */
  isDefault: Em.computed.equal('configGroupId', -1),

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
    return App.config.truncateGroupName(this.get('name'));
  }.property('name'),

  /**
   *
   */
  displayNameHosts: Em.computed.format('{0} ({1})', 'displayName', 'hosts.length'),

  /**
   * Provides hosts which are available for inclusion in
   * non-default configuration groups.
   * @type {Array}
   */
  availableHosts: function () {
    if (this.get('isDefault')) return [];
    var unusedHostsMap = this.get('parentConfigGroup.hosts').toWickMap();
    var availableHosts = [];
    var sharedHosts = this.get('clusterHosts');
    // parentConfigGroup.hosts(hosts from default group) - are available hosts, which don't belong to any group
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

  /**
   * @type {boolean}
   */
  isAddHostsDisabled: Em.computed.or('isDefault', '!availableHosts.length'),

  /**
   * @type {Array}
   */
  properties: DS.attr('array', {defaultValue: []}),

  /**
   * @type {string}
   */
  propertiesList: function () {
    var result = '';

    if (Array.isArray(this.get('properties'))) {
      this.get('properties').forEach(function (item) {
        result += item.name + " : " + item.value + '<br/>';
      }, this);
    }
    return result;
  }.property('properties.length')
});

App.ServiceConfigGroup.FIXTURES = [];

App.ServiceConfigGroup.getParentConfigGroupId = function(serviceName) {
  return App.ServiceConfigGroup.groupId(serviceName, 'Default');
};

App.ServiceConfigGroup.groupId = function(serviceName, groupName) {
  return serviceName + "_" + groupName;
};
