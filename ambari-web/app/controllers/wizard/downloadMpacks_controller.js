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
require('./wizardStep_controller');

App.WizardDownloadMpacksController = App.WizardStepController.extend({

  name: 'wizardDownloadMpacksController',

  stepName: 'downloadMpacks',

  mpacks: [],

  addMpacks: function () {
    const selectedMpacks = this.get('content.selectedMpacks');

    selectedMpacks.forEach(mpack => {
      this.get('mpacks').pushObject(Em.Object.create({
        name: mpack.name,
        version: mpack.version,
        displayName: mpack.displayName,
        url: mpack.downloadUrl,
        inProgress: true,
        failed: false,
        succeeded: false,
        failureMessage: null
      }));
    }, this);
  },

  registerMpacks: function () {
    const mpacks = this.get('mpacks');
    const self = this;
    mpacks.forEach(function (mpack) {
     self.downloadMpack(mpack).then(self.loadMpackInfo.bind(self));
    });
  },

  downloadMpack: function (mpack) {
    console.log("downloading mpacks");
    return App.ajax.send({
      name: 'mpack.download_by_url',
      sender: this,
      data: {
        name: mpack.name,
        url: mpack.url
      },
      success: 'downloadMpackSuccess',
      error: 'downloadMpackError',
    });
  },

  downloadMpackSuccess: function (data, opt, params) {
    this.get('mpacks').findProperty('name', params.name).set('succeeded', true);
    this.get('mpacks').findProperty('name', params.name).set('failed', false);
    this.get('mpacks').findProperty('name', params.name).set('inProgress', false);
  },

  downloadMpackError: function (request, ajaxOptions, error, opt, params) {
    if(request.status == 409) {
      this.downloadMpackSuccess(request, opt, params);
    } else {
      this.get('mpacks').findProperty('name', params.name).set('succeeded', false);
      this.get('mpacks').findProperty('name', params.name).set('failed', true);
      this.get('mpacks').findProperty('name', params.name).set('inProgress', false);
      
      let failureMessage;
      switch (request.status) {
        case 400:
        case 500:
          failureMessage = request.statusText;
          break;  
        default:
          failureMessage = Em.I18n.t('installer.downloadMpacks.failure.default');
      }
      
      this.get('mpacks').findProperty('name', params.name).set('failureMessage', failureMessage);
    }
  },

  loadMpackInfo: function (data) {
    App.ajax.send({
      name: 'mpack.get_registered_mpack',
      sender: this,
      data: {
        id: data.resources[0].MpackInfo.id
      }
    }).then(mpackInfo => this.get('content.registeredMpacks').push(mpackInfo));
  },

  retryDownload: function (event) {
    const mpack = event.context;
    
    if (mpack.get('failed')) {
      mpack.set('inProgress', true);
      mpack.set('succeeded', false);
      mpack.set('failed', false);
      this.downloadMpack(mpack);
    }  
  },

  showError: function (event) {
    const mpack = event.context;
    
    if (mpack.get('failed')) {
      const error = mpack.get('failureMessage');
      
      App.ModalPopup.show({
        header: `${Em.I18n.t('common.download')} ${Em.I18n.t('common.failed')}`,
        primary: Em.I18n.t('common.close'),
        secondary: false,
        body: error
      });
    }
  },

  isSubmitDisabled: function () {
    const mpacks = this.get('mpacks');
    return App.get('router.btnClickInProgress')
      || (this.get('wizardController.errors') && this.get('wizardController.errors').length > 0)
      || mpacks.filterProperty('succeeded', false).length > 0;
  }.property('mpacks.@each.succeeded', 'App.router.btnClickInProgress', 'wizardController.errors'),

  submit: function () {
    if (App.get('router.nextBtnClickInProgress')) {
      return;
    }

    //TODO: mpacks - For now, hard coding this to use the name and version of the first stack/mpack that successfully registered. 
    //We need to get rid of the concept of "selected stack".
    const selectedStack = this.get('mpacks').findProperty('succeeded');
    if (selectedStack) {
      this.set('content.selectedStack', { name: selectedStack.get('name'), version: selectedStack.get('version') });
    }

    App.router.send('next');
  }
});
