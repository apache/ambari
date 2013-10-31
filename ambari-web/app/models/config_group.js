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
 * Represents a configuration-group on the cluster. 
 * A configuration-group is a collection of hosts
 * on which a collection of configurations are applied.
 * 
 * Configuration group hierarchy is at 2 levels. For 
 * each service there is a 'Default' configuration group
 * containing all hosts not belonging to any group of that
 * service. 
 * 
 * A default configuration group has child configuration
 * groups which contain configuration overrides (deltas)
 * for a bunch of hosts. This allows different configurations 
 * for different hosts in a heterogeneous cluster environment.
 */
App.ConfigGroup = Ember.Object.extend({
  id: DS.attr('number'),
  name: DS.attr('string'),
  description: DS.attr('string'),
  isDefault: DS.attr('boolean'),
  
  /**
   * Parent configuration group for this group.
   * When {@link #isDefault} is true, this value is <code>null</code>
   * When {@link #isDefault} is false, this represents the configuration
   * deltas that are applied on the default.
   */
  parentConfigGroup: DS.belongsTo('App.ConfigGroup'),
  
  /**
   * Children configuration groups for this group.
   * When {@link #isDefault} is false, this value is <code>null</code>
   * When {@link #isDefault} is true, this represents the various
   * configuration groups that override the default.
   */
  childConfigGroups: DS.hasMany('App.ConfigGroup'),
  
  /**
   * Service for which this configuration-group 
   * is applicable.
   */
  service: DS.belongsTo('App.Service'),
  
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
   * 
   */
  hosts: DS.hasMany('App.Host'),
  
  /**
   * Provides hosts which are available for inclusion in 
   * non-default configuration groups. 
   */
  availableHosts: function() {
    
  }.property('isDefault', 'parentConfigGroup', 'childConfigGroups'),
  
  /**
   * Collection of (site, tag) pairs representing properties.
   * 
   * When {#isDefault} is true, this represents the 
   * default cluster configurations for that service.
   * 
   * When {#isDefault} is false, this represents the
   * configuration overrides on top of the cluster default for the
   * hosts identified by 'hosts'.
   */
  configSiteTags: DS.hasMany('App.ConfigSiteTag')
});