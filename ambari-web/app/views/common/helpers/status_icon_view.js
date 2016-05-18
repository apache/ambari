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

App.StatusIconView = Em.View.extend({
  tagName: 'i',

  /**
   * relation map between status and icon class
   * @type {object}
   */
  statusIconMap: {
    'COMPLETED': 'icon-ok completed',
    'WARNING': 'icon-warning-sign',
    'FAILED': 'icon-exclamation-sign failed',
    'HOLDING_FAILED': 'icon-exclamation-sign failed',
    'SKIPPED_FAILED': 'icon-share-alt failed',
    'PENDING': 'icon-cog pending',
    'QUEUED': 'icon-cog queued',
    'IN_PROGRESS': 'icon-cogs in_progress',
    'HOLDING': 'icon-pause',
    'SUSPENDED': 'icon-pause',
    'ABORTED': 'icon-minus aborted',
    'TIMEDOUT': 'icon-time timedout',
    'HOLDING_TIMEDOUT': 'icon-time timedout',
    'SUBITEM_FAILED': 'icon-remove failed'
  },

  classNameBindings: ['iconClass'],
  attributeBindings: ['data-original-title'],

  didInsertElement: function () {
    App.tooltip($(this.get('element')));
  },

  'data-original-title': function() {
    return this.get('content').toCapital();
  }.property('content'),

  /**
   * @type {string}
   */
  iconClass: function () {
    return this.get('statusIconMap')[this.get('content')] || 'icon-question-sign';
  }.property('content')
});