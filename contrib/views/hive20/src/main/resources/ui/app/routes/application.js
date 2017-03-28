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
import tabs from '../configs/top-level-tabs';
import ENV from 'ui/config/environment';

export default Ember.Route.extend({
  serviceCheck: Ember.inject.service(),
  ldapAuth: Ember.inject.service(),


  init() {
    this.get('ldapAuth').on('ask-password', this.askPassword.bind(this));
    this.get('ldapAuth').on('password-provided', this.passwordProvided.bind(this));
    return this._super(...arguments);
  },

  beforeModel() {
    if (ENV.APP.SHOULD_PERFORM_SERVICE_CHECK && !this.get('serviceCheck.checkCompleted')) {
      this.transitionTo('service-check');
    }
  },

  setupController: function (controller, model) {
    this._super(controller, model);
    controller.set('tabs', tabs);
  },

  askPassword() {
    this.set('ldapAuth.passwordRequired', true);
    this.transitionTo('password');
  },

  passwordProvided() {
    this.refresh();
  }

});
