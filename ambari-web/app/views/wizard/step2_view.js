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
  template:Ember.Handlebars.compile('<input type="file" />'),

  change: function (e) {
    self=this;
    if (e.target.files && e.target.files.length == 1) {
      var file = e.target.files[0];
      var reader = new FileReader();

      reader.onload = (function(theFile) {
        return function(e) {
          $('#sshKey').html(e.target.result);
          //$('.sshKey-file-view').html(e.target.result);
          self.set("controller.content.hosts.sshKey", e.target.result);
        };
      })(file);
      reader.readAsText(file);
    }
  }
});

App.WizardStep2View = Em.View.extend({

  templateName: require('templates/wizard/step2'),
  hostNameErr: false,

  didInsertElement: function () {
    $("[rel=popover]").popover({'placement': 'right', 'trigger': 'hover'});
    this.set('hostNameErr', false);
    this.set('controller.hostsError',null);
    this.set('controller.sshKeyError',null);
  },


  onHostNameErr: function () {
    if (this.get('controller.hostNameEmptyError') === false && this.get('controller.hostNameNotRequiredErr') === false && this.get('controller.hostNameErr') === false) {
      this.set('hostNameErr', false);
    } else {
      this.set('hostNameErr', true);
    }
  }.observes('controller.hostNameEmptyError', 'controller.hostNameNotRequiredErr', 'controller.hostNameErr'),

  sshKeyState: function(){
    return this.get("controller.content.hosts.manualInstall");
  }.property("controller.content.hosts.manualInstall"),

  sshKeyClass:function() {
    //alert(this.get("isFileApi"))
    return (this.get("isFileApi")) ? "hide" : "" ;
  }.property("isFileApi"),

  isFileApi: function () {
    return (window.File && window.FileReader && window.FileList) ? true : false ;
  }.property(),

  sshKeyPreviewClass: function() {
    if (this.get('controller.content.hosts.sshKey').trim() != '') {
      if (this.get('controller.content.hosts.manualInstall')) {
        return 'sshKey-file-view disabled';
      } else {
        return 'sshKey-file-view';
      }
    } else {
      return 'hidden';
    }
  }.property('controller.content.hosts.sshKey', 'controller.content.hosts.manualInstall')

});


