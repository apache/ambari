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

App.WizardTextField = Ember.TextField.extend({
  disabled: function(){
    return !this.get('controller.content.installOptions.isJavaHome');
  }.property('controller.content.installOptions.isJavaHome'),
  click: function(){
    return false;
  }
})

App.WizardStep2View = Em.View.extend({

  templateName: require('templates/wizard/step2'),
  hostNameErr: false,
  hostsInfo: null,

  didInsertElement: function () {
    $("[rel=popover]").popover({'placement': 'right', 'trigger': 'hover'});
    this.set('hostNameErr', false);
    this.set('controller.hostsError',null);
    this.set('controller.sshKeyError',null);
    this.loadHostsInfo();
  },

  loadHostsInfo: function(){
    var hostsInfo = Em.Object.create();
    this.set('hostsInfo', hostsInfo);
  },

  onHostNameErr: function () {
    if (this.get('controller.hostNameEmptyError') === false && this.get('controller.hostNameNotRequiredErr') === false && this.get('controller.hostNameErr') === false) {
      this.set('hostNameErr', false);
    } else {
      this.set('hostNameErr', true);
    }
  }.observes('controller.hostNameEmptyError', 'controller.hostNameNotRequiredErr', 'controller.hostNameErr'),

  sshKeyState: function(){
    return this.get("controller.content.installOptions.manualInstall");
  }.property("controller.content.installOptions.manualInstall"),

  isFileApi: function () {
    return (window.File && window.FileReader && window.FileList) ? true : false ;
  }.property(),

  manualInstallPopup: function(){
    if(!this.get('controller.content.installOptions.useSsh')){
      App.ModalPopup.show({
        header: "Warning",
        body: Em.I18n.t('installer.step2.manualInstall.info'),
        encodeBody: false,
        onPrimary: function () {
          this.hide();
        },
        secondary: null
      });
    }
    this.set('controller.content.installOptions.manualInstall', !this.get('controller.content.installOptions.useSsh'));
  }.observes('controller.content.installOptions.useSsh'),

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
  })
});


