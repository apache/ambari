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

App.MainAdminAccessController = Em.Controller.extend(App.UserPref, {
  name:'mainAdminAccessController',

  /**
   * Show jobs by default
   * @type {bool}
   */
  showJobs: false,

  /**
   * User pref key
   * @type {string}
   */
  persistKey: 'showJobsForNonAdmin',

  /**
   * Handle Save button click event
   */
  save: function() {
    this.postUserPref(this.get('persistKey'), this.get('showJobs'));
  },

  loadShowJobsForUsers: function () {
    var dfd = $.Deferred();
    this.getUserPref(this.get('persistKey')).done(function (value) {
      dfd.resolve(value);
    }).fail(function(value) {
      dfd.resolve(value);
    });
    return dfd.promise();
  },

  getUserPrefSuccessCallback: function (data) {
    this.set('showJobs', data);
    return data;
  },

  getUserPrefErrorCallback: function () {
    if (App.get('isAdmin')) {
      this.postUserPref(this.get('persistKey'), false);
    }
    this.set('showJobs', false);
    return true;
  }

});