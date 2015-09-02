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

App.StackVersion = DS.Model.extend({
  clusterName: DS.attr('string'),
  stack: DS.attr('string'),
  version: DS.attr('string'),
  name: function() {
    return this.get('stack') + " " + this.get('version');
  }.property('stack', 'version'),
  state: DS.attr('string'),
  repositoryVersion: DS.belongsTo('App.RepositoryVersion'),
  notInstalledHosts: DS.attr('array'),
  installingHosts: DS.attr('array'),
  installedHosts: DS.attr('array'),
  installFailedHosts: DS.attr('array'),
  outOfSyncHosts: DS.attr('array'),
  upgradingHosts: DS.attr('array'),
  upgradedHosts: DS.attr('array'),
  upgradeFailedHosts: DS.attr('array'),
  currentHosts: DS.attr('array'),

  noInstalledHosts:  function() {
    return this.get('installedHosts.length') == 0;
  }.property('installedHosts.length'),

  noCurrentHosts: function() {
    return this.get('currentHosts.length') == 0;
  }.property('currentHosts.length'),

  noInitHosts: function() {
    return this.get('notInstalledHosts.length') == 0;
  }.property('notInstalledHosts.length'),

  isCurrent: function() {
    return this.get('state') === 'CURRENT';
  }.property('state')
});

App.StackVersion.FIXTURES = [];

