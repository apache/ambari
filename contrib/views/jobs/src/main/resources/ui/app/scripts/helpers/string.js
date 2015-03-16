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

App.Helpers.string = {

  /**
   * Determines whether string end within another string.
   *
   * @method endsWith
   * @param {string} str string
   * @param {string} suffix substring for search
   * @return {boolean}
   */
  endsWith: function (str, suffix) {
    return str.indexOf(suffix, this.length - suffix.length) !== -1;
  },

  /**
   * Determines whether string start within another string.
   *
   * @method startsWith
   * @param {string} str string
   * @param {string} prefix substring for search
   * @return {boolean}
   */
  startsWith: function (str, prefix) {
    return str.indexOf(prefix) == 0;
  },

  getCamelCase: function(name) {
    if (name != null) {
      return name.toLowerCase().replace(/(\b\w)/g, function(f) {
        return f.toUpperCase();
      })
    }
    return name;
  },

  /**
   * Finds the value in an object where this string is a key.
   * Optionally, the index of the key can be provided where the
   * value of the nth key in the hierarchy is returned.
   *
   * Example:
   *  var tofind = 'smart';
   *  var person = {'name': 'Bob Bob', 'smart': 'no', 'age': '28', 'personality': {'smart': 'yes', 'funny': 'yes', 'emotion': 'happy'} };
   *  findIn(tofind, person); // 'no'
   *  findIn(tofind, person, 0); // 'no'
   *  findIn(tofind, person, 1); // 'yes'
   *  findIn(tofind, person, 2); // null
   *
   *  @method findIn
   *  @param s {string}
   *  @param multi {object}
   *  @param index {number} Occurrence count of this key
   *  @param _foundValues {array}
   *  @return {*} Value of key at given index
   */
  findIn: function(s, multi, index, _foundValues) {
    if (!index) {
      index = 0;
    }
    if (!_foundValues) {
      _foundValues = [];
    }
    multi = multi || '';
    var value = null,
      str = s.valueOf();
    if (typeof multi == 'object') {
      for ( var key in multi) {
        if (value != null) {
          break;
        }
        if (key == str) {
          _foundValues.push(multi[key]);
        }
        if (_foundValues.length - 1 == index) {
          // Found the value
          return _foundValues[index];
        }
        if (typeof multi[key] == 'object') {
          value = value || this.findIn(s, multi[key], index, _foundValues);
        }
      }
    }
    return value;
  },

  /**
   * Convert spaces to underscores
   * @method convertSpacesToUnderscores
   * @param {string} str
   * @returns {string}
   */
  convertSpacesToUnderscores: function (str) {
    return Em.isNone(str) ? '' : str.replace(' ', '_');
  }

};
