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

import DS from 'ember-data';
import Ember from 'ember';

export default DS.RESTAdapter.extend({
  namespace: Ember.computed(function() {
    var parts = window.location.pathname.match(/\/[^\/]*/g);
    var view = parts[1];
    var version = '/versions' + parts[2];
    var instance = parts[3];
    if (parts.length === 4) { // version is not present
      instance = parts[2];
      version = '';
    }
    return 'api/v1/views' + view + version + '/instances' + instance + '/resources/files/fileops';
  }),

  headers: {
    'X-Requested-By': 'ambari'
  }
});
