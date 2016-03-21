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

import Ember from 'ember';

/* globals moment */

export default Ember.Object.create({
  isInteger: function (x) {
    return !isNaN(x);
  },

  isDate: function (date) {
    return moment(date).isValid();
  },

  regexes: {
    allUppercase: /^[^a-z]*$/,
    whitespaces: /^(\s*).*$/,
    digits: /^\d+$/,
    name: /\w+/ig,
    dotPath: /[a-z.]+/i,
    setSetting: /^set\s+[\w-.]+(\s+|\s?)=(\s+|\s?)[\w-.]+(\s+|\s?);/gim
  },

  validationValues: {
    bool: [
      Ember.Object.create({ value: 'true' }),
      Ember.Object.create({ value: 'false' })
    ],

    execEngine: [
      Ember.Object.create({ value: 'tez' }),
      Ember.Object.create({ value: 'mr' })
    ]
  },

  insensitiveCompare: function (sourceString) {
    var args = Array.prototype.slice.call(arguments, 1);

    if (!sourceString) {
      return false;
    }

    return !!args.find(function (arg) {
      return sourceString.match(new RegExp('^' + arg + '$', 'i'));
    });
  },

  insensitiveContains: function (sourceString, destString) {
    return sourceString.toLowerCase().indexOf(destString.toLowerCase()) > -1;
  },

  convertToArray : function (inputObj) {
    var array = [];

    for (var key in inputObj) {
      if (inputObj.hasOwnProperty(key)) {
        array.pushObject({
          name: key,
          value: inputObj[key]
        });
      }
    }
    return array;
  },

  /**
   * Convert number of seconds into time object HH MM SS
   *
   * @param integer secs Number of seconds to convert
   * @return object
  */
  secondsToHHMMSS: function (secs)
   {
    var hours = Math.floor(secs / (60 * 60));

    var divisor_for_minutes = secs % (60 * 60);
    var minutes = Math.floor(divisor_for_minutes / 60);

    var divisor_for_seconds = divisor_for_minutes % 60;
    var seconds = Math.ceil(divisor_for_seconds);

    var obj = {
      "h": hours,
      "m": minutes,
      "s": seconds
    };
    return  ((obj.h > 0) ? obj.h + ' hr ' : '') + ((obj.m > 0) ? obj.m + ' min ' : '') + ((obj.s >= 0) ? obj.m + ' sec ' : '');
  }

});
