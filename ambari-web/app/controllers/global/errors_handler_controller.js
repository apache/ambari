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

App.ErrorsHandlerController = Em.Controller.extend(App.UserPref, {
  init: function () {
    var oldError = window.onerror || Em.K;
    var self = this;
    window.onerror = function (err, url, lineNumber, colNumber, Err) {
      oldError.call(this, err, url, lineNumber, colNumber, Err);
      var ls = localStorage.getObject('errors') || {};
      if(Object.keys(localStorage.getObject('errors')).length > 25) {
        delete ls[Object.keys(ls).sort()[0]];
      }
      var key = new Date().getTime();
      var val = {
        file: url,
        line: lineNumber,
        col: colNumber,
        error: err,
        stackTrace: Em.get(Err || {}, 'stack')
      };
      ls[key] = val;
      localStorage.setObject('errors', ls);
      self.postUserPref(key, val);
    };
    return this._super();
  }
});
