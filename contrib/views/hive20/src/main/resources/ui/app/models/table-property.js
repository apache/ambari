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

export default Ember.Object.extend({
  key: '',
  value: '',


  hasError: Ember.computed('errors', function() { return this.get('errors.length') !== 0; }),
  errors: [],

  keyError: Ember.computed('errors.@each', function() {
    return this.get('errors').findBy('type', 'key');
  }),

  valueError: Ember.computed('errors.@each', function() {
    return this.get('errors').findBy('type', 'value');
  }),


  // Control the UI
  editing: false,


  validate() {
    this.set('errors', []);
    if (Ember.isEmpty(this.get('key'))) {
      this.get('errors').pushObject({type: 'key', error: "Name cannot be empty"});
    }

    if(Ember.isEmpty(this.get('value'))) {
      this.get('errors').pushObject({type: 'value', error: "Value cannot be empty"});
    }

    return this.get('errors.length') === 0;
  }
});
