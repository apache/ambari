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
import STATUS from '../configs/service-check-status';

export default Ember.Component.extend({
  classNames: ['col-md-12', 'alert'],
  classNameBindings: ['alertType'],

  errorExpanded: false,

  alertType: Ember.computed('status', function() {
    const status = this.get('status');
    return status === STATUS.notStarted ? 'alert-info' :
      status === STATUS.started ? 'alert-info' :
        status === STATUS.completed ? 'alert-success' :
          status === STATUS.errored ? 'alert-danger' : '';
  }),

  iconName: Ember.computed('status', function() {
    const status = this.get('status');
    let iconName = status === STATUS.notStarted ? 'stop' :
      status === STATUS.started ? 'location-arrow' :
      status === STATUS.completed ? 'check' :
      status === STATUS.errored ? 'times' : '';
    return iconName;
  }),

  actions: {
    toggleError() {
      this.toggleProperty('errorExpanded');
    }
  }
});
