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
  tagName: 'li',
  classNameBindings: ['pullRight:pull-right', 'active'],
  pullRight: Ember.computed.readOnly('tab.pullRight'),
  active: Ember.computed.alias('tab.active'),

  shouldTransition: Ember.computed('tab.transition', function() {
    if(!Ember.isEmpty(this.get('tab.transition'))) {
      return this.get('tab.transition');
    } else {
      return true;
    }
  }),


  actions : {
    selected() {
      this.get('tabs').setEach('active', false);
      this.set('active', true);
      this.sendAction('activate', this.get('tab.link'));
    }
  }
});
