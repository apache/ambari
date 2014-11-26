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

App.UpgradeVersionBoxView = Em.View.extend({
  /**
   * @type {object|null}
   */
  version: null,

  /**
   * @type {string}
   */
  versionName: function () {
    if (Em.isNone(this.get('version'))) return "";
    return this.get('version.stack') + "-" + this.get('version.version');
  }.property('version.stack', 'version.version'),

  /**
   * @type {string}
   */
  btnClass: 'btn-default',

  /**
   * @type {number}
   */
  hostsCount: 0,

  /**
   * run action by name of method
   * @param event
   * @return {Boolean}
   */
  runAction: function (event) {
    if (typeof this.get('controller')[event.context] === 'function') {
      this.get('controller')[event.context]();
      return true;
    }
    return false;
  }
});
