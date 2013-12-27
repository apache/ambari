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

App.WizardStep0View = Em.View.extend({

  tagName: "form", //todo: why form?
  attributeBindings: ['autocomplete'],
  autocomplete: 'off',
  templateName: require('templates/wizard/step0'),

  //todo: create property for placeholder(go to template)

  didInsertElement: function () {
    App.popover($("[rel=popover]"), {'placement': 'right', 'trigger': 'hover'});
    this.get('controller').loadStep();
  },

  //todo: rename it to class or write comments
  onError: function () {
    return this.get('controller.clusterNameError') !== '';
  }.property('controller.clusterNameError')

});

App.WizardStep0ViewClusterNameInput = Em.TextField.extend({
  keyPress: function(event) {
    if (event.keyCode == 13) {
      this.get('parentView.controller').submit();
      return false;
    }
  }
});
