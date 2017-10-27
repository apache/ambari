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

App.WizardDownloadProductsController = Em.Controller.extend({

  name: 'wizardDownloadProductsController',

  mpacks: [],

  addMpacks: function () {
    const selectedMpacks = this.get('content.selectedMpacks') || this.get('wizardController').getDBProperty('selectedMpacks');

    selectedMpacks.forEach(mpack => {
      this.get('mpacks').pushObject(Em.Object.create({
        name: mpack.name,
        displayName: mpack.displayName || mpack.name, //TODO: remove default when displayName is available
        url: mpack.url,
        inProgress: true,
        failed: false,
        success: false
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
    console.dir("Mpack " + params.name + " download completed with success code " + data.status);
    this.get('mpacks').findProperty('name', params.name).set('inProgress', false);
    this.get('mpacks').findProperty('name', params.name).set('success', true);
  },

  downloadMpackError: function (request, ajaxOptions, error, opt, params) {
    if(request.status == 409) {
      this.downloadMpackSuccess(request, opt, params);
    } else {
      console.dir("Mpack " + params.name + " download failed with error code " + request.status);
      this.get('mpacks').findProperty('name', params.name).set('inProgress', false);
      this.get('mpacks').findProperty('name', params.name).set('failed', true);
    }
  },

  retryDownload: function (event) {
    var mpack = event.context;
    mpack.set('inProgress', true);
    mpack.set('failed', false);
    this.downloadMpack(mpack);
  },

  getRegisteredMpacks: function () {
    return App.ajax.send({
      name: 'mpack.get_registered_mpacks',
      sender: this
    });
  },

  isSubmitDisabled: function () {
    const mpacks = this.get('mpacks');
    return mpacks.filterProperty('success', false).length > 0 || App.get('router.btnClickInProgress');
  }.property('mpacks.@each.success', 'App.router.btnClickInProgress'),

  /**
   * Onclick handler for <code>Next</code> button.
   * Disable 'Next' button while it is already under process. (using Router's property 'nextBtnClickInProgress')
   * @method submit
   */
  submit: function () {
    const self = this;

    if (App.get('router.nextBtnClickInProgress')) {
      return;
    }

    if (!this.get('isSubmitDisabled')) {
      //TODO: mpacks
      //get info about stacks from version definitions and save to Stack model
      this.getRegisteredMpacks().then(mpacks => {
        const stackVersionsRegistered = mpacks.items.map(mpack => this.get('wizardController').createMpackStackVersion
          (
            mpack.MpackInfo.stack_name || "HDP", //TODO: mpacks - remove fallback when stack info is included in API response
            mpack.MpackInfo.stack_version || "3.0.0" //TODO: mpacks - remove fallback when stack info is included in API response
          )
        );

        //var versionData = installerController.getSelectedRepoVersionData(); //This would be used to post a VDF xml for a local repo (I think), but do we still need to do this when we will just be using mpacks?
        $.when(...stackVersionsRegistered).always(() => { //this uses always() because the api call made by createMpackStackVersion will return a 500 error
                                                          //if the stack version has already been registered, but we want to proceed anyway
          self.get('wizardController').getMpackStackVersions().then(data => {
            data.items.forEach(versionDefinition => App.stackMapper.map(versionDefinition))

            //get info about services from specific stack versions and save to StackService model
            const selectedServices = self.get('content.selectedServices') || self.get('wizardController').getDBProperty('selectedServices');
            const servicePromises = selectedServices.map(service => self.get('wizardController').loadMpackServiceInfo(service.stackName, service.stackVersion, service.name));

            $.when(...servicePromises).then(() => {
              const serviceInfo = App.StackService.find();
              self.set('content.services', serviceInfo);

              const clients = [];
              serviceInfo.forEach(service => {
                const client = service.get('serviceComponents').filterProperty('isClient', true);
                client.forEach(clientComponent => {
                  clients.pushObject({
                    component_name: clientComponent.get('componentName'),
                    display_name: clientComponent.get('displayName'),
                    isInstalled: false
                  });
                });
              });
              self.set('content.clients', clients);
              self.get('wizardController').setDBProperty('clientInfo', clients);

              // - for now, pull the stack from the single mpack that we can install
              // - when we can support multiple mpacks, make this an array of selectedStacks (or just use the selectedServices array?) and add the repo data to it
              const selectedService = selectedServices[0];
              const selectedStack = self.get('wizardController').getStack(selectedService.stackName, selectedService.stackVersion);
              self.set('content.selectedStack', selectedStack);
              self.get('wizardController').setDBProperty('selectedStack', selectedStack);

              App.router.send('next');
            });
          });
        });
      });
    }
  }
});
