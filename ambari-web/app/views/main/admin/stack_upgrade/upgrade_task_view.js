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

App.upgradeTaskView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/upgrade_task'),

  /**
   * @type {object|null}
   */
  task: null,

  statusIconMap: {
    'COMPLETED': 'icon-ok',
    'WARNING': 'icon-warning-sign',
    'FAILED': 'icon-warning-sign',
    'PENDING': 'icon-cog'
  },

  /**
   * @type {boolean}
   */
  isManualDone: false,

  /**
   * @type {string}
   */
  iconClass: function () {
    return this.get('statusIconMap')[this.get('content.status')] || 'icon-question-sign';
  }.property('content.status'),

  /**
   * @type {boolean}
   */
  isFailed: function () {
    return this.get('content.status') === 'FAILED';
  }.property('content.status'),

  /**
   * @type {boolean}
   */
  isManualOpened: function () {
    //TODO modify logic according to actual API
    return this.get('content.status') === 'IN_PROGRESS' && this.get('content.type') === 'manual'
  }.property('content.status', 'content.type')
});
