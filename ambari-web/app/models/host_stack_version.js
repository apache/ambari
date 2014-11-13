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

App.HostStackVersion = DS.Model.extend({
  stackName: DS.attr('string'),
  stack: DS.belongsTo('App.StackVersion'),
  version: DS.attr('string'),
  /**
   * possible property value defined at App.HostStackVersion.statusDefinition
   */
  status: DS.attr('string'),
  host: DS.belongsTo('App.Host'),
  hostName: DS.attr('string'),
  isCurrent: function () {
    return this.get('status') === 'CURRENT'
  }.property('status'),
  displayStatus: function() {
    return App.HostStackVersion.formatStatus(this.get('status'));
  }.property('status'),
  installEnabled: function () {
    return (this.get('status') === 'INIT' || this.get('status') === 'INSTALL_FAILED');
  }.property('status'),
  installDisabled: Ember.computed.not('installEnabled')
});

App.HostStackVersion.FIXTURES = [];

/**
 * definition of possible statuses of Stack Version
 * @type {Array}
 */
App.HostStackVersion.statusDefinition = [
  "INSTALLED",
  "INSTALLING",
  "INSTALL_FAILED",
  "INIT",
  "CURRENT"
];

/**
 * translate status to label
 * @param status
 * @return {string}
 */
App.HostStackVersion.formatStatus = function (status) {
  return status ?
    Em.I18n.t('hosts.host.stackVersions.status.' + status.toLowerCase()) :
    Em.I18n.t('common.unknown');
};
