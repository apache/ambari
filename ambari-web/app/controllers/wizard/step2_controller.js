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
var validator = require('utils/validator');

App.WizardStep2Controller = Em.Controller.extend({
  name: 'wizardStep2Controller',
  hostNameArr: [],
  isPattern: false,
  bootRequestId:  null,
  hasSubmitted: false,

  hostNames: function () {
    return this.get('content.installOptions.hostNames');
  }.property('content.installOptions.hostNames'),

  manualInstall: function () {
    return this.get('content.installOptions.manualInstall');
  }.property('content.installOptions.manualInstall'),

  sshKey: function () {
    return this.get('content.installOptions.sshKey');
  }.property('content.installOptions.sshKey'),

  installType: function () {
    return this.get('manualInstall') ? 'manualDriven' : 'ambariDriven';
  }.property('manualInstall'),

  isHostNameValid: function (hostname) {
    // disabling hostname validation as we don't want to be too restrictive and disallow
    // user's hostnames
    // return validator.isHostname(hostname) && (!(/^\-/.test(hostname) || /\-$/.test(hostname)));
    return true;
  },
  /**
   * set not installed hosts to the hostNameArr
   */
  updateHostNameArr: function(){
    this.hostNameArr = this.get('hostNames').trim().split(new RegExp("\\s+", "g"));
    this.patternExpression();
    var installedHostNames = App.Host.find().mapProperty('hostName');
    var tempArr = [];
    for (i = 0; i < this.hostNameArr.length; i++) {
      if (!installedHostNames.contains(this.hostNameArr[i])) {
        tempArr.push(this.hostNameArr[i]);
      }
    }
    this.set('hostNameArr', tempArr);
  },
  /**
   * validate host names
   * @return {Boolean}
   */
  isAllHostNamesValid: function () {
    var self = this;
    var result = true;
    this.updateHostNameArr();

    this.hostNameArr.forEach(function(hostName){
      if (!self.isHostNameValid(hostName)) {
        result = false;
      }
    });

    return result;
  },

  hostsError: null,
  /**
   * set hostsError if host names don't pass validation
   */
  checkHostError: function () {
    if (this.get('hostNames').trim() === '') {
      this.set('hostsError', Em.I18n.t('installer.step2.hostName.error.required'));
    }
    else {
      if (this.isAllHostNamesValid() === false) {
        this.set('hostsError', Em.I18n.t('installer.step2.hostName.error.invalid'));
      }
      else {
        this.set('hostsError', null);
      }
    }
  },

  checkHostAfterSubmitHandler: function() {
    if (this.get('hasSubmitted')) {
      this.checkHostError();
    }
  }.observes('hasSubmitted', 'hostNames'),

  sshKeyError: function () {
    if (this.get('hasSubmitted') && this.get('manualInstall') === false && this.get('sshKey').trim() === '') {
      return Em.I18n.t('installer.step2.sshKey.error.required');
    }
    return null;
  }.property('sshKey', 'manualInstall', 'hasSubmitted'),

  /**
   * Get host info, which will be saved in parent controller
   */
  getHostInfo: function () {

    var hostNameArr = this.get('hostNameArr');
    var hostInfo = {};
    for (var i = 0; i < hostNameArr.length; i++) {
      hostInfo[hostNameArr[i]] = {
        name: hostNameArr[i],
        installType: this.get('installType'),
        bootStatus: 'PENDING'
      };
    }

    return hostInfo;
  },

  /**
   * Used to set sshKey from FileUploader
   * @param sshKey
   */
  setSshKey: function(sshKey){
    this.set("content.installOptions.sshKey", sshKey);
  },

  /**
   * Onclick handler for <code>next button</code>. Do all UI work except data saving.
   * This work is doing by router.
   * @return {Boolean}
   */
  evaluateStep: function () {
    console.log('TRACE: Entering controller:WizardStep2:evaluateStep function');

    if (this.get('isSubmitDisabled')) {
      return false;
    }

    this.set('hasSubmitted', true);

    this.checkHostError();
    if (this.get('hostsError')) {
      return false;
    }

    if (this.get('sshKeyError')) {
      return false;
    }

    this.updateHostNameArr();

    if (!this.hostNameArr.length) {
      this.set('hostsError', Em.I18n.t('installer.step2.hostName.error.required'));
      return false;
    }

    if(this.isPattern)
    {
      this.hostNamePatternPopup(this.hostNameArr);
      return false;
    }

    this.proceedNext();
  },
  /**
   * check is there a pattern expression in host name textarea
   * push hosts that match pattern in hostNamesArr
   */
  patternExpression: function(){
    this.isPattern = false;
    var self = this;
    var hostNames = [];
    $.each(this.hostNameArr, function(e,a){
      var start, end, extra = {0:""};
      if(/\[\d*\-\d*\]/.test(a)){
        start=a.match(/\[\d*/);
        end=a.match(/\-\d*]/);

        start=start[0].substr(1);
        end=end[0].substr(1);

        if(parseInt(start) <= parseInt(end) && parseInt(start) >= 0){
          self.isPattern = true;

          if(start[0] == "0" && start.length > 1) {
            extra = start.match(/0*/);
          }

          for (var i = parseInt(start); i < parseInt(end) + 1; i++) {
            hostNames.push(a.replace(/\[\d*\-\d*\]/,extra[0].substring(1,1+extra[0].length-i.toString().length)+i))
          }

        }else{
          hostNames.push(a);
        }
      }else{
        hostNames.push(a);
      }
    });
    this.hostNameArr =  hostNames;
  },
  /**
   * launch hosts to bootstrap
   * and save already registered hosts
   * @return {Boolean}
   */
  proceedNext: function(){
    if (this.get('manualInstall') === true) {
      this.manualInstallPopup();
      return false;
    }

    var bootStrapData = JSON.stringify({'verbose': true, 'sshKey': this.get('sshKey'), hosts: this.get('hostNameArr')});

    if (App.skipBootstrap) {
      this.saveHosts();
      return true;
    }

    var requestId = App.router.get(this.get('content.controllerName')).launchBootstrap(bootStrapData);
    if (requestId == '0') {
      var controller = App.router.get(App.clusterStatus.wizardControllerName);
      controller.registerErrPopup('Information', 'Host Registration is currently in progress.  Please try again later.');
    } else if (requestId) {
      this.set('content.installOptions.bootRequestId', requestId);
      this.saveHosts();
    }
  },
  /**
   * show popup with hosts generated by pattern
   * @param hostNames
   */
  hostNamePatternPopup: function (hostNames) {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('installer.step2.hostName.pattern.header'),
      onPrimary: function () {
        self.proceedNext();
        this.hide();
      },
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile(['{{#each host in view.hostNames}}<p>{{host}}</p>{{/each}}'].join('\n')),
        hostNames: hostNames
      })
    });
  },
  /**
   * show notify that installation is manual
   * save hosts
   */
  manualInstallPopup: function () {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('installer.step2.manualInstall.popup.header'),
      onPrimary: function () {
        this.hide();
        self.saveHosts();
      },
      bodyClass: Ember.View.extend({
        templateName: require('templates/wizard/step2ManualInstallPopup')
      })
    });
  },

  isSubmitDisabled: function () {
    return (this.get('hostsError') || this.get('sshKeyError'));
  }.property('hostsError', 'sshKeyError'),

  saveHosts: function(){
    this.set('content.hosts', this.getHostInfo());
    App.router.send('next');
  }

});
