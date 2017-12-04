/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

require('models/repository');

App.WizardConfigureDownloadView = Em.View.extend({

  templateName: require('templates/wizard/configureDownload'),

  proxyAuthOptions: [
    Em.Object.create({ value: 0, label: 'None', selected: true }),
    Em.Object.create({ value: 1, label: 'Option 1', selected: false }),
    Em.Object.create({ value: 2, label: 'Option 2', selected: false })
  ],

  proxyUrlPlaceholder: Em.I18n.t('installer.configureDownload.proxyUrl.placeholder'),
  
  didInsertElement: function () {
    this.get('controller').loadStep();
    
    const selectedProxyAuth = this.get('controller.content.downloadConfig.proxyAuth');
    this.setProxyAuth(selectedProxyAuth, true);
  },

  useRedHatSatelliteChanged: function () {
    this.set('controller.content.downloadConfig.useProxy', false);
    const self = this.useProxyChanged ? this : this.get('parentView'); //parentView is actually just this view, but this function gets called from a sub-view on the template so we have to reference this way
    self.useProxyChanged();
  },

  useProxyChanged: function () {
    this.set('controller.content.downloadConfig.proxyUrl', null);
    const self = this.setProxyAuth ? this : this.get('parentView'); //parentView is actually just this view, but this function gets called from a sub-view on the template so we have to reference this way
    self.setProxyAuth(0);
    this.get('controller').proxySettingsChanged();
  },

  proxyUrlChanged: function () {
    this.get('controller').proxySettingsChanged();
  },

  setProxyAuth: function (value, doNotUpdateController) {
    const optionToSelect = this.get('proxyAuthOptions').filter(option => option.get('value') == value);
    
    if (optionToSelect.length > 0) {
      let proxyAuthOptions = this.get('proxyAuthOptions');
      proxyAuthOptions.forEach(option => {
        if (option.get('value') == value) {
          option.set('selected', true);
        } else {
          option.set('selected', false);
        }
      });

      if (!doNotUpdateController) {
        this.get('controller').setProxyAuth(value);
      }  
    }
  },

  proxyAuthChanged: function (event) {
    const selected = event.target.value;
    this.setProxyAuth(selected);
    this.get('controller').proxySettingsChanged();
  },

  isSubmitDisabled: function () {
    if (this.get('controller.content.downloadConfig.useProxy') && !this.get('controller.content.downloadConfig.proxyTestPassed')) {
      return true;
    }

    return false;
  }.property('controller.content.downloadConfig.useProxy', 'controller.content.downloadConfig.proxyTestPassed')
});
