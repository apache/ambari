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

  deepEqual: function() {
    var i, l, leftChain, rightChain;
    var values = arguments;
    function compare2Objects (x, y) {
      var p;
      if (isNaN(x) && isNaN(y) && typeof x === 'number' && typeof y === 'number') {
        return true;
      }

      if (x === y) {
        return true;
      }

      if ((typeof x === 'function' && typeof y === 'function') ||
        (x instanceof Date && y instanceof Date) ||
        (x instanceof RegExp && y instanceof RegExp) ||
        (x instanceof String && y instanceof String) ||
        (x instanceof Number && y instanceof Number)) {
         return x.toString() === y.toString();
      }

      if (!(x instanceof Object && y instanceof Object)) {
        return false;
      }

      if (x.isPrototypeOf(y) || y.isPrototypeOf(x)) {
        return false;
      }

      if (x.constructor !== y.constructor) {
        return false;
      }

      if (x.prototype !== y.prototype) {
        return false;
      }

      if (leftChain.indexOf(x) > -1 || rightChain.indexOf(y) > -1) {
        return false;
      }

      for (p in y) {
        if (y.hasOwnProperty(p) !== x.hasOwnProperty(p)) {
            return false;
        }
        else if (typeof y[p] !== typeof x[p]) {
            return false;
        }
      }

      for (p in x) {
        if (y.hasOwnProperty(p) !== x.hasOwnProperty(p)) {
          return false;
        }
        else if (typeof y[p] !== typeof x[p]) {
          return false;
        }
        switch (typeof (x[p])) {
          case 'object':
          case 'function':
            leftChain.push(x);
            rightChain.push(y);
            if (!compare2Objects (x[p], y[p])) {
                return false;
            }
            leftChain.pop();
            rightChain.pop();
            break;
          default:
            if (x[p] !== y[p]) {
                return false;
            }
            break;
        }
      }

      return true;
    }

    if (arguments.length < 1) {
      return true;
    }

    for (i = 1, l = arguments.length; i < l; i++) {
      leftChain = [];
      rightChain = [];
      if (!compare2Objects(arguments[0], arguments[i])) {
        return false;
      }
    }

    return true;
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
