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

export default Ember.Component.extend(Ember.I18n.TranslateableProperties, {
  show: function () {
    var self = this;

    this.$('.modal').modal().on('hidden.bs.modal', function () {
      self.sendAction('close');
    });
  }.on('didInsertElement'),

  keyPress: function (e) {
    Ember.run.debounce(this, function () {
      if (e.which === 13) {
        this.send('ok');
      } else if (e.which === 27) {
        this.send('close');
      }
    }, 200);
  },

  setupEvents: function () {
    this.$(document).on('keyup', Ember.$.proxy(this.keyPress, this));
  }.on('didInsertElement'),

  destroyEvents: function () {
    this.$(document).off('keyup', Ember.$.proxy(this.keyPress, this));
  }.on('willDestroyElement'),

  actions: {
    ok: function () {
      this.$('.modal').modal('hide');
      this.sendAction('ok');
    },
    close: function () {
      this.$('.modal').modal('hide');
      this.sendAction('close');
    }
  }
});
