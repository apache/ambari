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

export default Ember.Controller.extend({
  serviceCheck: Ember.inject.service(),
  hdfsError: null,
  userHomeError: null,
  atsError: null,
  hiveError: null,

  reset() {
    this.set('hdfsError');
    this.set('userHomeError');
    this.set('atsError');
    this.set('hiveError');

  },

  progressStyle: Ember.computed('serviceCheck.percentCompleted', function() {
    let percentCompleted = this.get('serviceCheck.percentCompleted');
    return `width: ${percentCompleted}%;`;
  }),

  hasError: Ember.computed('hdfsError', 'userHomeError', 'atsError', 'hiveError', function() {
    return !(Ember.isEmpty(this.get('hdfsError')) &&
      Ember.isEmpty(this.get('userHomeError')) &&
      Ember.isEmpty(this.get('atsError')) &&
      Ember.isEmpty(this.get('hiveError')));
  }),

  transitioner: Ember.observer('serviceCheck.transitionToApplication', function() {
    if(this.get('serviceCheck.transitionToApplication')) {
      this.transitionToRoute('application');
    }
  }),

  init() {
    this._super(...arguments);
  }
});
