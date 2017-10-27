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

App.WizardSelectMpacksController = Em.Controller.extend({

  name: 'wizardSelectMpacksController',

  //mpacks: Em.computed.alias('content.mpacks'),

  getMpacks: function () {
    App.ajax.send({
      name: 'registry.mpacks.versions',
      showLoadingPopup: true,
      sender: this,
      success: 'getMpacksSucceeded',
      error: 'getMpacksFailed'
    });
  },

  getMpacksSucceeded: function (data) {
    const mpacks = data.items.reduce(
      (mpacks, registry) => mpacks.concat(
        registry.mpacks.map(mpack => {
          return Em.Object.create({
            name: mpack.RegistryMpackInfo.mpack_name,
            description: mpack.RegistryMpackInfo.mpack_description,
            logoUrl: mpack.RegistryMpackInfo.mpack_logo_url,
            versions: mpack.versions ? mpack.versions.map(version => {
              return Em.Object.create({
                selected: false,
                id: mpack.RegistryMpackInfo.mpack_name + version.RegistryMpackVersionInfo.mpack_version,
                version: version.RegistryMpackVersionInfo.mpack_version,
                docUrl: version.RegistryMpackVersionInfo.mpack_dock_url,
                mpackUrl: version.RegistryMpackVersionInfo.mpack_url,
                stackName: version.RegistryMpackVersionInfo.stack_name || "HDP", //TODO: remove default when stack_name is available
                stackVersion: version.RegistryMpackVersionInfo.stack_version || "3.0.0", //TODO: remove default when stack_version is available
                services: version.RegistryMpackVersionInfo.services ? version.RegistryMpackVersionInfo.services.map(service => {
                  return Em.Object.create({
                    selected: false,
                    id: mpack.RegistryMpackInfo.mpack_name + version.RegistryMpackVersionInfo.mpack_version + service.name,
                    name: service.name,
                    version: service.version
                  })
                }) : []
              })
            }) : []
          })
        })
      ),
    []);

    const mpackVersions = mpacks.reduce(
      (versions, mpack) => versions.concat(
        mpack.versions.map(version => {
          version.mpack = mpack;
          return version;
        })
      ),
    []);

    const mpackServices = mpackVersions.reduce(
      (services, mpackVersion) => services.concat(
        mpackVersion.services.map(service => {
          service.mpackVersion = mpackVersion;
          return service;
        })
      ),
    []);

    this.set('content.mpacks', mpacks);
    this.set('content.mpackVersions', mpackVersions);
    this.set('content.mpackServices', mpackServices);
  },

  getMpacksFailed: function () {
    this.set('content.mpacks', []);
  },

  loadStep: function () {
    const mpacks = this.get('content.mpacks');
    const mpackVersions = this.get('content.mpackVersions');
    const mpackServices = this.get('content.mpackServices');

    if (!mpacks || mpacks.length === 0 || !mpackVersions || mpackVersions.length === 0 || !mpackServices || mpackServices.length === 0) {
      //this.showLoadingSpinner();
      this.getMpacks();
    }
  },

  isSubmitDisabled: function () {
    const mpackServices = this.get('content.mpackServices');
    return mpackServices.filterProperty('selected', true).length === 0 || App.get('router.btnClickInProgress');
  }.property('content.mpackServices.@each.selected', 'App.router.btnClickInProgress'),

  loadSelectionFailed: function () {
    App.showAlertPopup(
      Em.I18n.t('common.error'), //header
      Em.I18n.t('installer.selectMpacks.loadSelectionFailed') //body
    );
  },

  /**
   * Adds service to selection.
   *
   * @param  {string} serviceName
   */
  selectService: function (event) {
    const serviceId = event.context;
    const mpackServices = this.get('content.mpackServices');
    const byServiceId = service => service.id === serviceId;

    const service = mpackServices.find(byServiceId);
    service.set('selected', true);
    service.set('mpackVersion.selected', true);
  },

  removeService: function (event) {
    const serviceId = event.context;
    const mpackServices = this.get('content.mpackServices');
    const byServiceId = service => service.id === serviceId;

    const service = mpackServices.find(byServiceId);
    service.set('selected', false);

    service.set('mpackVersion.selected', service.get('mpackVersion.services').some(s => s.get('selected') === true));
  },

  selectedServices: function () {
    const mpackServices = this.get('content.mpackServices');
    return mpackServices ? mpackServices.filter(s => s.get('selected') === true) : [];
  }.property('content.mpackServices.@each.selected'),

  selectedMpackVersions: function () {
    const versions = this.get('content.mpackVersions');
    return versions ? versions.filter(v => v.get('selected') === true) : [];
  }.property('content.mpackVersions.@each.selected', 'selectedServices'),

  hasSelectedMpackVersions: function () {
    const versions = this.get('content.mpackVersions');
    return versions ? versions.some(v => v.get('selected') === true) : false;
  }.property('content.mpackVersions.@each.selected', 'selectedServices'),

  /**
   * Onclick handler for <code>Next</code> button.
   * Disable 'Next' button while it is already under process. (using Router's property 'nextBtnClickInProgress')
   * @method submit
   */
  submit: function () {
    const self = this;

    if(App.get('router.nextBtnClickInProgress')) {
      return;
    }

    if (!this.get('isSubmitDisabled')) {
      const selectedServices = this.get('selectedServices').map(service =>
        ({
          name: service.name,
          mpackName: service.mpackVersion.name,
          mpackVersion: service.mpackVersion.version,
          stackName: service.mpackVersion.stackName,
          stackVersion: service.mpackVersion.stackVersion
        })
      );
      self.set('content.selectedServices', selectedServices);
      self.get('wizardController').setDBProperty('selectedServices', selectedServices);

      const selectedServiceNames = selectedServices.map(service => service.name);
      self.set('content.selectedServiceNames', selectedServiceNames);
      self.get('wizardController').setDBProperty('selectedServiceNames', selectedServiceNames);

      const selectedMpacks = self.get('selectedMpackVersions').map(mpackVersion =>
        ({
          name: mpackVersion.mpack.name,
          displayName: mpackVersion.mpack.displayName,
          url: mpackVersion.mpackUrl,
          version: mpackVersion.version
        })
      );
      self.set('content.selectedMpacks', selectedMpacks);
      self.get('wizardController').setDBProperty('selectedMpacks', selectedMpacks);

      App.router.send('next');
    }
  }
});
