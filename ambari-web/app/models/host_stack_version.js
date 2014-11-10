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
  stack: DS.attr('string'),
  version: DS.attr('string'),
  /**
   * property can have next values:
   *  - INSTALLED
   *  - INSTALLING
   *  - INSTALL_FAILED
   *  - INIT
   */
  status: DS.attr('string'),
  isCurrent: DS.attr('boolean'),
  displayStatus: function() {
    return this.get('status') ?
      Em.I18n.t('hosts.host.stackVersions.status.' + this.get('status').toLowerCase()) :
      Em.I18n.t('common.unknown');
  }.property('status'),
  installEnabled: function () {
    return (this.get('status') === 'INIT' || this.get('status') === 'INSTALL_FAILED');
  }.property('status'),
  installDisabled: Ember.computed.not('installEnabled')
});

App.HostStackVersion.FIXTURES = [
  {
    stack: 'HDP-2.2',
    version: 'HDP-2.2.2',
    status: 'INIT',
    isCurrent: false
  },
  {
    stack: 'HDP-2.2',
    version: 'HDP-2.2.1',
    status: 'INSTALLED',
    isCurrent: true
  },
  {
    stack: 'HDP-2.2',
    version: 'HDP-2.2.3',
    status: 'INSTALLING',
    isCurrent: false
  },
  {
    stack: 'HDP-2.3',
    version: 'HDP-2.3.0',
    status: 'INSTALL_FAILED',
    isCurrent: false
  }
];
