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
  id: DS.attr('string'),
  name: DS.attr('string'),
  version: DS.attr('string'),
  installedHosts: DS.attr('array'),
  currentHosts: DS.attr('array'),
  operatingSystems: DS.hasMany('App.OS'),
  hostStackVersions: DS.hasMany('App.HostStackVersion'),

  setStatusHosts: function(){
    Em.run.once(this, 'updateStatus');
  }.observes('hostStackVersions.@each.status'),

  updateStatus: function() {
    var current = [];
    var installed = [];
    var upgradeFailed = [];
    var upgrading = [];
    var init = [];
    var notInstalled = [];
    this.get('hostStackVersions').forEach(function(hv) {
      switch(hv.get('status')) {
        case 'CURRENT':
          current.push(hv);
        case 'INSTALLED':
          installed.push(hv);
          break;
        case 'INIT':
          init.push(hv);
          notInstalled.push(hv);
          break;
        case 'UPGRADE_FAILED':
          upgradeFailed.push(hv);
          notInstalled.push(hv);
          break;
        case 'UPGRADING':
          upgrading.push(hv);
          break;
      }
    });
    this.set('currentHostStacks', current);
    this.set('installedHostStacks', installed);
    this.set('notInstalledHostStacks', notInstalled);
    this.set('upgradeFailedHostStacks', upgradeFailed);
    this.set('initHostStacks', init);
    this.set('upgradingHostStacks', upgrading);
  },

  currentHostStacks: [],

  installedHostStacks: [],

  upgradeFailedHostStacks: [],

  upgradingHostStacks: [],

  initHostStacks: [],

  notInstalledHostStacks: [],

  noInstalledHosts:  function() {
    return this.get('installedHosts.length') == 0;
  }.property('installedHosts.length'),

  noCurrentHosts: function() {
    return this.get('currentHosts.length') == 0;
  }.property('currentHosts.length')
});

App.StackVersion.FIXTURES = [];

