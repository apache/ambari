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

var stringUtils = require('utils/string_utils');

module.exports = {

  isChild: function(obj)
  {
    for (var k in obj) {
      if (obj.hasOwnProperty(k)) {
        if (obj[k] instanceof Object) {
          return false;
        }
      }
    }
    return true;
  },

  recursiveKeysCount: function(obj) {
    if (!(obj instanceof Object)) {
      return null;
    }
    var self = this;
    function r(obj) {
      var count = 0;
      for (var k in obj) {
        if(self.isChild(obj[k])){
          count++;
        } else {
          count += r(obj[k]);
        }
      }
      return count;
    }
    return r(obj);
  },

  recursiveTree: function(obj) {
    if (!(obj instanceof Object)) {
      return null;
    }
    var self = this;
    function r(obj,parent) {
      var leaf = '';
      for (var k in obj) {
        if(self.isChild(obj[k])){
          leaf += k + ' ('+parent+')' + '<br/>';
        } else {
          leaf += r(obj[k],parent +'/' + k);
        }
      }
      return leaf;
    }
    return r(obj,'');
  }
};
