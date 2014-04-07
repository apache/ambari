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

/**
 * Small mixin for processing user preferences
 * Provide methods to save/load some values in <code>persist</code> storage
 * Save available only for admin users!
 * When using this mixin you should redeclare methods:
 * <ul>
 *   <li>getUserPrefSuccessCallback</li>
 *   <li>getUserPrefErrorCallback</li>
 *   <li>postuserPrefSuccessCallback</li>
 *   <li>postuserPrefErrorCallback</li>
 * </ul>
 * @type {Em.Mixin}
 */
App.UserPref = Em.Mixin.create({

  /**
   * Should <code>getUserPref</code> and <code>postUserPref</code> be async
   * @type {bool}
   */
  makeRequestAsync: true,

  /**
   * Additional to request data
   * @type {object}
   */
  additionalData: {},

  /**
   * Get persist value from server with persistKey
   * @param {String} key
   */
  getUserPref: function(key) {
    return App.ajax.send({
      name: 'settings.get.user_pref',
      sender: this,
      data: {
        key: key,
        async: this.get('makeRequestAsync'),
        data: this.get('additionalData')
      },
      success: 'getUserPrefSuccessCallback',
      error: 'getUserPrefErrorCallback'
    });
  },

  /**
   * Should be redeclared in objects that use this mixin
   * @param {*} response
   * @param {Object} request
   * @param {Object} data
   * @returns {*}
   */
  getUserPrefSuccessCallback: function (response, request, data) {},

  /**
   * Should be redeclared in objects that use this mixin
   * @param {Object} request
   * @param {Object} ajaxOptions
   * @param {String} error
   */
  getUserPrefErrorCallback: function (request, ajaxOptions, error) {},

  /**
   * Post persist key/value to server, value is object
   * Only for admin users!
   * @param {String} key
   * @param {Object} value
   */
  postUserPref: function (key, value) {
    if(!App.get('isAdmin')) return;
    var keyValuePair = {};
    keyValuePair[key] = JSON.stringify(value);
    App.ajax.send({
      'name': 'settings.post.user_pref',
      'sender': this,
      'beforeSend': 'postUserPrefBeforeSend',
      'data': {
        'async': this.get('makeRequestAsync'),
        'keyValuePair': keyValuePair
      },
      'success': 'postUserPrefSuccessCallback',
      'error': 'postUserPrefErrorCallback'
    });
  },

  /**
   * Should be redeclared in objects that use this mixin
   * @param {*} response
   * @param {Object} request
   * @param {Object} data
   * @returns {*}
   */
  postUserPrefSuccessCallback: function (response, request, data) {},

  /**
   * Should be redeclared in objects that use this mixin
   * @param {Object} request
   * @param {Object} ajaxOptions
   * @param {String} error
   */
  postUserPrefErrorCallback: function(request, ajaxOptions, error) {},

  /**
   * Little log before post request
   * @param {Object} request
   * @param {Object} ajaxOptions
   * @param {Object} data
   */
  postUserPrefBeforeSend: function(request, ajaxOptions, data){
    console.log('BeforeSend to persist: persistKeyValues', data.keyValuePair);
  }

});
