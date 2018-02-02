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
    var mpacks = this.get('mpacks');
    var self = this;
    mpacks.forEach(function (mpack) {
      self.downloadMpack(mpack);
    });
  },

  downloadMpack: function (mpack) {
    console.log("downloading mpacks");
    App.ajax.send({
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
          failureMessage = Em.i18n.t('installer.downloadMpacks.failure.default');
      }
      
      this.get('mpacks').findProperty('name', params.name).set('failureMessage', failureMessage);
    }
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

  getRegisteredMpacks: function () {
    return App.ajax.send({
      name: 'mpack.get_registered_mpacks',
      sender: this
    });
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

    if (!this.get('isSubmitDisabled')) {
      //get info about stacks from version definitions and save to Stack model
      this.getRegisteredMpacks().then(mpacks => {
        const stackVersionsRegistered = mpacks.items.map(mpack => this.get('wizardController').createMpackStackVersion
          (
            mpack.version[0].Versions.stack_name,
            mpack.version[0].Versions.stack_version
          )
        );

        $.when(...stackVersionsRegistered).always(() => { //this uses always() because the api call made by createMpackStackVersion will return a 500 error
                                                          //if the stack version has already been registered, but we want to proceed anyway
          App.router.send('next');
        });
      });
    }
  }
});
