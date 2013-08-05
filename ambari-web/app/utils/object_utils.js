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

  recursiveKeysCount: function(obj) {
    if (!(obj instanceof Object)) {
      return null;
    }

    function r(obj) {
      var count = 0;
      for (var k in obj) {
        if (obj.hasOwnProperty(k)) {
          if (obj[k] instanceof Object) {
            count += 1 + r(obj[k]);
          }
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
    function r(obj, indx) {
      var str = '';
      for (var k in obj) {
        if (obj.hasOwnProperty(k)) {
          if (obj[k] instanceof Object) {
            var spaces = (new Array(indx + 1).join('&nbsp;'));
            var bull = (indx != 0 ? '&bull; ' : ' '); // empty for "root" element
            str += spaces + bull + k + '<br />' + r(obj[k], indx + 1);
          }
        }
      }
      return str;
    }
    return r(obj, 0);
  },
  
  /**
   * Gets value of property path.
   * 
   * @param propertyPath
   *          Format is 'a.b.c'
   * @return Returns <code>undefined</code> when path does not exist.
   */
  getProperty: function (object, propertyPath) {
    var props = propertyPath.split('.');
    for ( var c = 0; c < props.length - 1 && object; c++) {
      object = object[props[c]];
      if (object === null) {
        break;
      }
    }
    if (object != null) {
      return object[props[props.length - 1]];
    }
    return undefined;
  }

};
