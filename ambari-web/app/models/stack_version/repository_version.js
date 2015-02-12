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

App.RepositoryVersion = DS.Model.extend({
  displayName: DS.attr('string'),
  repositoryVersion: DS.attr('string'),
  upgradePack: DS.attr('string'),
  stackVersionType: DS.attr('string'),
  stackVersionNumber: DS.attr('string'),
  operatingSystems: DS.hasMany('App.OS'),
  stackVersion: DS.belongsTo('App.StackVersion'),
  stack: function () {
    return this.get('stackVersionType') + " " + this.get('stackVersionNumber');
  }.property('stackVersionType', 'stackVersionNumber'),

  /**
   * status used until corresponding stack version get created
   * @type {string}
   */
  defaultStatus: 'INIT',

  /**
   * @type {string}
   */
  status: function () {
    return this.get('stackVersion.state') || this.get('defaultStatus');
  }.property('stackVersion.state', 'defaultStatus'),

  /**
   * @type {Array}
   */
  notInstalledHosts: function () {
    return this.get('stackVersion.notInstalledHosts') || App.get('allHostNames');
  }.property('stackVersion.notInstalledHosts'),

  /**
   * @type {Array}
   */
  installedHosts: function () {
    return this.get('stackVersion.installedHosts') || [];
  }.property('stackVersion.installedHosts'),

  /**
   * @type {Array}
   */
  currentHosts: function () {
    return this.get('stackVersion.currentHosts') || [];
  }.property('stackVersion.currentHosts'),

  /**
   * @type {boolean}
   */
  noInstalledHosts: function () {
    return (this.get('stackVersion')) ? this.get('stackVersion.noInstalledHosts') : true;
  }.property('stackVersion.noInstalledHosts'),

  /**
   * @type {boolean}
   */
  noCurrentHosts: function () {
    return (this.get('stackVersion')) ? this.get('stackVersion.noCurrentHosts') : true;
  }.property('stackVersion.noCurrentHosts'),

  /**
   * @type {boolean}
   */
  noInitHosts: function () {
    return (this.get('stackVersion')) ? this.get('stackVersion.noInitHosts') : false;
  }.property('stackVersion.noInitHosts'),

  /**
   * @type {string}
   */
  noInitHostsTooltip: function () {
    return (this.get('noInitHosts')) ? Em.I18n.t('admin.stackVersions.version.emptyHostsTooltip') : Em.I18n.t('admin.stackVersions.version.hostsTooltip');
  }.property('noInitHosts'),

  /**
   * @type {string}
   */
  noCurrentHostsTooltip: function () {
    return (this.get('noCurrentHosts')) ? Em.I18n.t('admin.stackVersions.version.emptyHostsTooltip') : Em.I18n.t('admin.stackVersions.version.hostsTooltip');
  }.property('noCurrentHosts'),

  /**
   * @type {string}
   */
  noInstalledHostsTooltip: function () {
    return (this.get('noInstalledHosts')) ? Em.I18n.t('admin.stackVersions.version.emptyHostsTooltip') : Em.I18n.t('admin.stackVersions.version.hostsTooltip');
  }.property('noInstalledHosts'),

  /**
   * @type {boolean}
   */
  isVisible: true
});

App.RepositoryVersion.FIXTURES = [];

