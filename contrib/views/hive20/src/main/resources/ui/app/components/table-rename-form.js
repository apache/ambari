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

export default Ember.Component.extend({
  classNames: ['form-horizontal'],

  errorCleaner: Ember.observer('newTableName', function() {
    if(this.get('error')) {
      this.clearError();
    }
  }),

  validate() {
    if(Ember.isEmpty(this.get('newTableName'))) {
      this.setError("Table name cannot be empty");
      return false;
    }
    if(this.get('newTableName') === this.get('table.table')) {
      this.setError("New table name cannot be same as the old table name");
      return false;
    }
    return true;
  },

  setError(message) {
    this.set('error', true);
    this.set('errorMessage', message);
  },

  clearError() {
    this.set('error');
    this.set('errorMessage');
  },

  actions: {
    rename() {
      if(this.validate()) {
        this.sendAction('rename', this.get('newTableName'));
      }
    },

    cancel() {
      this.sendAction('cancel');
    }
  }
});
