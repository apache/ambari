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

var LZString = require('utils/lz-string');
/**
 * Persist storage managing utils. It helps to put and get values from persisted storage
 * by api (/api/v1/persist).
 * @type {Object}
 */
module.exports = {
  /**
   * Get item from persist storage by key and optional path.
   *
   * @param  {String} key persist key to get e.g 'CLUSTER_STATUS', will fetch data from
   * /api/v1/persist/CLUSTER_STATUS
   * @param  {String} [path=null] Em.get compatible attributes path
   * @return {$.Deferred}
   */
  get: function(key, path) {
    var dfd = $.Deferred();
    App.ajax.send({
      name: 'persist.get',
      sender: this,
      data: {
        deferred: dfd,
        key: key || '',
        path: path
      },
      success: 'getSuccessCallback',
      error: 'getErrorCallback'
    });
    return dfd.promise();
  },

  getSuccessCallback: function(data, xhr, params) {
    var extracted, response = data;
    try {
      response = JSON.parse(response);
    } catch(e) { }
    if (Em.isEmpty(data)) {
      params.deferred.resolve(null);
      return;
    }
    if (typeof response === 'string') {
      extracted = JSON.parse(LZString.decompressFromBase64(response));
      params.deferred.resolve(params.path ? Em.get(extracted, params.path) : extracted);
    } else {
      params.deferred.resolve(response);
    }
  },

  getErrorCallback: function(request, ajaxOptions, error, opt, params) {
    params.deferred.reject({
      request: request,
      error: error
    });
  },

  /**
   * Update key value.
   *
   * @param  {String} key
   * @param  {Object} value value to save
   * @return {$.Deferred}
   */
  put: function(key, value) {
    var kv = {};
    kv[key] = !Em.isEmpty(value) ? LZString.compressToBase64(JSON.stringify(value)) : '';
    return App.ajax.send({
      name: 'persist.put',
      sender: this,
      data: {
        keyValuePair: kv
      },
      success: 'putSuccessCallback',
      error: 'putErrorCallback'
    });
  },

  putSuccessCallback: function() {},
  putErrorCallback: function() {},

  remove: function(key) {
    return this.put(key, '');
  }
}
