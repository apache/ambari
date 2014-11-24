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

var App = require('app');

App.PigModalComponent = Ember.Component.extend({
  didClose:'removeModal',
  size:'',
  large:function () {
    return this.get('size') =='lg';
  }.property('size'),
  small:function () {
    return this.get('size') =='sm';
  }.property('size'),
  layoutName:'modal/modalLayout',
  actions: {
    ok: function() {
      this.$('.modal').modal('hide');
      this.sendAction('ok');
    },
    cancel:function () {
      this.$('.modal').modal('hide');
      this.sendAction('close');
    },
    option:function () {
      this.$('.modal').modal('hide');
      this.sendAction('option');
    }
  },
  keyUp:function (e) {
    if (e.keyCode == 27) {
      this.sendAction('close');
    }
  },
  show: function() {
    var modal = this.$('.modal').modal();
    modal.off('hidden.bs.modal');
    modal.off('shown.bs.modal');
    modal.on('shown.bs.modal',function () {
      this.find('input').first().focus();
    }.bind(modal));
    modal.on('hidden.bs.modal', function() {
      this.sendAction('didClose');
    }.bind(this));
  }.on('didInsertElement')
});
