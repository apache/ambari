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
  /**
   * Config for displaying more hosts
   * if oldHosts.length more than config.count that configuration will be applied
   */
  hostDisplayConfig: [
    {
      count: 0,
      delimitery: '<br/>',
      popupDelimitery: '<br />'
    },
    {
      count: 10,
      delimitery: ', ',
      popupDelimitery: '<br />'
    },
    {
      count: 50,
      delimitery: ', ',
      popupDelimitery: ', '
    }
  ],
  showMoreHosts: function () {
    var self = this;
    App.ModalPopup.show({
      header: "Hosts are already part of the cluster and will be ignored",
      body: self.get('hostsInfo.oldHostNamesMore'),
      encodeBody: false,
      onPrimary: function () {
        this.hide();
      },
      secondary: null
    });
  },
  loadHostsInfo: function(){

    var hostsInfo = Em.Object.create();

    var oldHostNames = App.Host.find().getEach('id');
    var k = 10;

    var usedConfig = false;
    this.get('hostDisplayConfig').forEach(function (config) {
      if (oldHostNames.length > config.count) {
        usedConfig = config;
      }
    });

    k = usedConfig.count ? usedConfig.count : oldHostNames.length;
    var displayedHostNames = oldHostNames.slice(0, k);
    hostsInfo.set('oldHostNames', displayedHostNames.join(usedConfig.delimitery));
    if (usedConfig.count) {
      var moreHostNames = oldHostNames.slice(k + 1);
      hostsInfo.set('oldHostNamesMore', moreHostNames.join(usedConfig.popupDelimitery));
      hostsInfo.set('showMoreHostsText', "...and %@ more".fmt(moreHostNames.length));
    }

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

  sshKeyClass:function() {
    return (this.get("isFileApi")) ? "hide" : "" ;
  }.property("isFileApi"),

  isFileApi: function () {
    return (window.File && window.FileReader && window.FileList) ? true : false ;
  }.property(),

  sshKeyPreviewClass: function() {
    if (this.get('controller.content.installOptions.sshKey').trim() != '') {
      if (this.get('controller.content.installOptions.manualInstall')) {
        return 'sshKey-file-view disabled';
      } else {
        return 'sshKey-file-view';
      }
    } else {
      return 'hidden';
    }
  }.property('controller.content.installOptions.sshKey', 'controller.content.installOptions.manualInstall'),

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
  }.observes('controller.content.installOptions.useSsh')

});


