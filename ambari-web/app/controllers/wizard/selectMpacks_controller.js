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

  loadRegistry: function () {
    return App.ajax.send({
      name: 'registry.mpacks.versions',
      showLoadingPopup: true,
      sender: this
    });
  },

  loadRegistrySucceeded: function (data) {
    const mpacks = data.items.reduce(
      (mpacks, registry) => mpacks.concat(
        registry.mpacks.map(mpack => {
          return Em.Object.create({
            name: mpack.RegistryMpackInfo.mpack_name,
            description: mpack.RegistryMpackInfo.mpack_description,
            logoUrl: mpack.RegistryMpackInfo.mpack_logo_url,
            versions: mpack.versions ? mpack.versions.map((version, index) => {
              return Em.Object.create({
                selected: false,
                displayed: index === 0 ? true : false, //by default, display first version
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

  isSaved: function () {
    const wizardController = this.get('wizardController');
    if (wizardController) {
      return wizardController.getStepSavedState('selectMpacks');
    }
    return false;
  }.property('wizardController.content.stepsSavedState'),

  loadRegistryFailed: function () {
    this.set('content.mpacks', []);

    App.showAlertPopup(
      Em.I18n.t('common.error'), //header
      Em.I18n.t('installer.selectMpacks.loadRegistryFailed') //body
    );
  },

  getRegistry: function () {
    const deferred = $.Deferred();

    const mpacks = this.get('content.mpacks');
    const mpackVersions = this.get('content.mpackVersions');
    const mpackServices = this.get('content.mpackServices');

    if (!mpacks || mpacks.length === 0 || !mpackVersions || mpackVersions.length === 0 || !mpackServices || mpackServices.length === 0) {
      this.loadRegistry().then(registry => {
        this.loadRegistrySucceeded(registry);
        deferred.resolve();
      },
      () => {
        this.loadRegistryFailed();
        deferred.reject();
      });
    } else {
      deferred.resolve();
    }

    return deferred.promise();
  },

  loadStep: function () {
    this.getRegistry().then(() => {
      //add previously selected services
      const selectedServices = this.get('content.selectedServices');
      if (selectedServices) {
        selectedServices.forEach(service => {
          this.addService(service.id);
        });
      }
    });
  },

  isSubmitDisabled: function () {
    const mpackServices = this.get('content.mpackServices');
    return mpackServices.filterProperty('selected', true).length === 0 || App.get('router.btnClickInProgress');
  }.property('content.mpackServices.@each.selected', 'App.router.btnClickInProgress'),

  getServiceById: function (serviceId) {
    const mpackServices = this.get('content.mpackServices');
    const byServiceId = service => service.id === serviceId;
    const service = mpackServices.find(byServiceId);
    return service;
  },

  getMpackVersionById: function (versionId) {
    const mpackVersions = this.get('content.mpackVersions');
    const byVersionId = version => version.id === versionId;
    const version = mpackVersions.find(byVersionId);
    return version;
  },

  displayMpackVersion: function (versionId) {
    const version = this.getMpackVersionById(versionId);

    if (version) {
      version.mpack.versions.forEach(mpackVersion => {
        if (mpackVersion.get('id') === versionId) {
          mpackVersion.set('displayed', true);
        } else {
          mpackVersion.set('displayed', false);
        }
      })
    }
  },

  addServiceHandler: function (serviceId) {
    if (this.addService(serviceId)) {
      this.get('wizardController').setStepUnsaved('selectMpacks');
    }
  },

  addService: function (serviceId) {
    const service = this.getServiceById(serviceId);

    if (service) {
      service.set('selected', true);
      service.set('mpackVersion.selected', true);
      return true;
    }

    return false;
  },

  removeServiceHandler: function (serviceId) {
    if (this.removeService(serviceId)) {
      this.get('wizardController').setStepUnsaved('selectMpacks');
    }
  },

  removeService: function (serviceId) {
    const service = this.getServiceById(serviceId);

    if (service) {
      service.set('selected', false);
      service.set('mpackVersion.selected', service.get('mpackVersion.services').some(s => s.get('selected') === true));
      return true;
    }

    return false;
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

  clearSelection: function () {
    const mpackServices = this.get('content.mpackServices');
    if (mpackServices) {
      mpackServices.setEach('selected', false);
    }

    const versions = this.get('content.mpackVersions');
    if (versions) {
      versions.setEach('selected', false);
    }
  },

  /**
   * Onclick handler for <code>Next</code> button.
   * Disable 'Next' button while it is already under process. (using Router's property 'nextBtnClickInProgress')
   * @method submit
   */
  submit: function () {
    if (App.get('router.nextBtnClickInProgress')) {
      return;
    }

    if (!this.get('isSubmitDisabled')) {
      const selectedServices = this.get('selectedServices').map(service =>
        ({
          id: service.id,
          name: service.name,
          mpackName: service.mpackVersion.name,
          mpackVersion: service.mpackVersion.version,
          stackName: service.mpackVersion.stackName,
          stackVersion: service.mpackVersion.stackVersion
        })
      );
      this.set('content.selectedServices', selectedServices);

      const selectedServiceNames = selectedServices.map(service => service.name);
      this.set('content.selectedServiceNames', selectedServiceNames);

      const selectedMpacks = this.get('selectedMpackVersions').map(mpackVersion =>
        ({
          name: mpackVersion.mpack.name,
          displayName: mpackVersion.mpack.displayName,
          url: mpackVersion.mpackUrl,
          version: mpackVersion.version
        })
      );
      this.set('content.selectedMpacks', selectedMpacks);

      App.router.send('next');
    }
  }
});
