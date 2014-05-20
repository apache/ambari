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

'use strict';

window.App = require('config/app');
require('config/router');
require('config/store');
require('translations');

App.reopen({
  /**
   * @type {string}
   */
  name: 'SLIDER',

  /**
   * @type {string}
   */
  version: '1.0.0',

  /**
   * @type {string}
   */
  instance: 'SLIDER_1',

  /**
   * API url for Slider
   * Format:
   *  <code>/api/v1/views/[VIEW_NAME]/versions/[VERSION]/instances/[INSTANCE_NAME]/</code>
   * @type {string}
   */
  urlPrefix: function() {
    return '/api/v1/views/%@1/versions/%@2/instances/%@3/'.fmt(this.get('name'), this.get('version'), this.get('instance'));
  }.property('name', 'version', 'instance')
});

// Load all modules in order automagically. Ember likes things to work this
// way so everything is in the App.* namespace.
var folderOrder = [
    'initializers', 'mixins', 'routes', 'models',
    'views', 'controllers', 'helpers',
    'templates', 'components'
  ];

folderOrder.forEach(function(folder) {
  window.require.list().filter(function(module) {
    return new RegExp('^' + folder + '/').test(module);
  }).forEach(function(module) {
    require(module);
  });
});
