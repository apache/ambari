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
  }
});