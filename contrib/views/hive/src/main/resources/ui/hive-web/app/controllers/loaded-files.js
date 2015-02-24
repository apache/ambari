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
import constants from 'hive/utils/constants';

export default Ember.ArrayController.extend({
  contains: function (obj) {
    if (typeof obj === 'string') {
      return this.findBy('id', obj);
    }

    return this._super(obj);
  },

  loadFile: function (path) {
    var self = this;
    var defer = Ember.RSVP.defer();
    var file = this.contains(path);

    if (file) {
      defer.resolve(file);
    } else {
      this.store.find(constants.namingConventions.file, path).then(function (file) {
        self.pushObject(file);
        defer.resolve(file);
      }, function (err) {
        defer.reject(err);
      });
    }

    return defer.promise;
  },

  reload: function (path) {
    var defer = Ember.RSVP.defer();

    this.store.find(constants.namingConventions.file, path).then(function (file) {
      file.reload().then(function (reloadedFile) {
        defer.resolve(reloadedFile);
      }, function (err) {
        defer.reject(err);
      });
    }, function (err) {
      defer.reject(err);
    });

    return defer.promise;
  }
});
