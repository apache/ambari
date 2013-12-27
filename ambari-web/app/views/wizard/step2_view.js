/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * 'License'); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


var App = require('app');

App.SshKeyFileUploader = Ember.View.extend({
  //TODO: rewrite it using tagName and attribute binding
  //TODO: rewrite it as independent component and place it somewhere in utils
  // alternative is to move it to App.WizardStep2View
  template:Ember.Handlebars.compile('<input type="file" {{bindAttr disabled="view.disabled"}} />'),
  classNames: ['ssh-key-input-indentation'],

  change: function (e) {
    var self=this;
    if (e.target.files && e.target.files.length == 1) {
      var file = e.target.files[0];
      var reader = new FileReader();

      reader.onload = (function(theFile) {
        return function(e) {
          $('#sshKey').html(e.target.result);
          self.get("controller").setSshKey(e.target.result);
        };
      })(file);
      reader.readAsText(file);
    }
  }
});

App.WizardStep2View = Em.View.extend({

  templateName: require('templates/wizard/step2'),

  didInsertElement: function () {
    //TODO: move it to separate function in Ember.View using reopenClass
    App.popover($("[rel=popover]"), {'placement': 'right', 'trigger': 'hover'});

    //todo: move them to conroller
    this.set('controller.hostsError',null);
    this.set('controller.sshKeyError',null);
  },

  sshKeyState: function(){
    return this.get("controller.content.installOptions.manualInstall");
  }.property("controller.content.installOptions.manualInstall"),

  //TODO: incupsulate it inside of App.SshKeyFileUploader
  isFileApi: function () {
    return (window.File && window.FileReader && window.FileList) ? true : false ;
  }.property(),

  //TODO: replace next 2 properties with new one used in both places
  providingSSHKeyRadioButton: Ember.Checkbox.extend({
    tagName: 'input',
    attributeBindings: ['type', 'checked'],
    checked: function () {
      return this.get('controller.content.installOptions.useSsh');
    }.property('controller.content.installOptions.useSsh'),
    type: 'radio',

    click: function () {
      this.set('controller.content.installOptions.useSsh', true);
      this.set('controller.content.installOptions.manualInstall', false);
    }
  }),

  manualRegistrationRadioButton: Ember.Checkbox.extend({
    tagName: 'input',
    attributeBindings: ['type', 'checked'],
    checked: function () {
      return this.get('controller.content.installOptions.manualInstall');
    }.property('controller.content.installOptions.manualInstall'),
    type: 'radio',

    click: function () {
      this.set('controller.content.installOptions.manualInstall', true);
      this.set('controller.content.installOptions.useSsh', false);
    }
  }),

  textFieldView: Ember.TextField.extend({
    disabled: function(){
      return !this.get('isEnabled');
    }.property('isEnabled')
  })
});


