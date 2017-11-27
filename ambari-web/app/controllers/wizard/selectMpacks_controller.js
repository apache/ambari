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

  noRecommendationAvailable: false,

  loadRegistry: function () {
    return App.ajax.send({
      name: 'registry.all',
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
            displayName: mpack.RegistryMpackInfo.mpack_display_name,
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
                stackName: version.RegistryMpackVersionInfo.stack_name,
                stackVersion: version.RegistryMpackVersionInfo.stack_version,
                services: version.RegistryMpackVersionInfo.services ? version.RegistryMpackVersionInfo.services.map(service => {
                  return Em.Object.create({
                    selected: false,
                    displayed: index === 0 ? true : false, //by default, display first version
                    id: mpack.RegistryMpackInfo.mpack_name + version.RegistryMpackVersionInfo.mpack_version + service.name,
                    name: service.name,
                    displayName: service.displayName || service.name, //TODO: mpacks - remove fallback when display name is available
                    version: service.version
                  })
                }) : []
              })
            }) : []
          })
        })
      ), []
    );

    const mpackVersions = mpacks.reduce(
      (versions, mpack) => versions.concat(
        mpack.get('versions').map(version => {
          version.set('mpack', mpack);
          return version;
        })
      ),
      []);

    const mpackServiceVersions = mpackVersions.reduce(
      (services, mpackVersion) => services.concat(
        mpackVersion.get('services').map(service => {
          service.set('mpackVersion', mpackVersion);
          return service;
        })
      ),
      []);

    const uniqueServices = {};
    mpackServiceVersions.forEach(service => {
      uniqueServices[service.name] = Em.Object.create({
        name: service.name,
        displayName: service.displayName,
        description: service.description,
        displayedVersion: function () {
          return this.get('versions').filterProperty('displayed')[0];
        }.property('versions.@each.displayed')
      })
    });
    
    const mpackServices = [];
    for (let serviceName in uniqueServices) {
      const service = uniqueServices[serviceName];
      const versions = mpackServiceVersions.filter(serviceVersion => serviceVersion.get('name') === service.name).map(serviceVersion => {
        serviceVersion.set('service', service);
        return serviceVersion;
      })

      service.set('versions', versions);
      mpackServices.push(service);
    }
    
    this.set('content.mpacks', mpacks);
    this.set('content.mpackVersions', mpackVersions);
    this.set('content.mpackServiceVersions', mpackServiceVersions);
    this.set('content.mpackServices', mpackServices);

    const usecases = data.items.reduce(
      (usecases, registry) => usecases.concat(
        registry.scenarios.map(usecase => {
          return Em.Object.create({
            selected: false,
            id: usecase.RegistryScenarioInfo.scenario_id || usecase.RegistryScenarioInfo.scenario_name, //TODO: mpacks - remove fallback when id is available
            name: usecase.RegistryScenarioInfo.scenario_name,
            displayName: usecase.RegistryScenarioInfo.scenario_display_name || usecase.RegistryScenarioInfo.scenario_name, //TODO: mpacks - remove fallback when display name is available
            description: usecase.RegistryScenarioInfo.scenario_description,
            mpacks: this.getMpacksByName(usecase.RegistryScenarioInfo.scenario_mpacks.map(mpack => mpack.name))
          });
        })
      ), []
    );

    this.set('content.mpackUsecases', usecases);
  },
  
  getMpacksByName: function (mpackNames) {
    return mpackNames.map(mpackName => this.getMpackByName(mpackName));
  },

  /**
   * Returns the first (newest) version of the mpack with name matching mpackName.
   * 
   * @param {string} mpackName 
   * @returns mpackVersion
   */
  getMpackByName: function (mpackName) {
    const mpacks = this.get('content.mpacks');

    if (mpacks) {
      //TODO: mpacks - reinstate this if/when the test runner can handle it
      //for (let mpack of mpacks) {
      //if (mpack.get('name') === mpackName) {
      //  return mpack.get('versions')[0]; //TODO: mpacks - change this to the last item when sort order is fixed
      //}
      for (let i = 0, length = mpacks.length; i < length; i++) {      
        if (mpacks[i].get('name') === mpackName) {
          return mpacks[i].get('versions')[0]; //TODO: mpacks - change this to the last item when sort order is fixed
        }
      }
    }
    
    return null;
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

  registryLoaded() {
    const mpacks = this.get('content.mpacks');
    const mpackVersions = this.get('content.mpackVersions');
    const mpackServices = this.get('content.mpackServices');
    const mpackServiceVersions = this.get('content.mpackServiceVersions');
    const mpackUsecases = this.get('content.mpackUsecases');

    if (!mpacks || mpacks.length === 0
      || !mpackVersions || mpackVersions.length === 0
      || !mpackServices || mpackServices.length === 0
      || !mpackServiceVersions || mpackServiceVersions.length === 0) {
      return false;
    }

    return true;
  },

  getRegistry: function () {
    const deferred = $.Deferred();

    if (!this.registryLoaded()) {
      this.loadRegistry().then(data => {
        this.loadRegistrySucceeded(data);
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

  toggleMode: function () {
    const isAdvancedMode = this.get('content.advancedMode');
    
    if (isAdvancedMode) { //toggling to Basic Mode
      this.clearSelection();
    } else { //toggling to Advanced Mode
      this.set('noRecommendationAvailable', false);
    }
    
    this.set('content.advancedMode', !isAdvancedMode);
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
    const mpackServiceVersions = this.get('content.mpackServiceVersions');
    return mpackServiceVersions.filterProperty('selected', true).length === 0 || App.get('router.btnClickInProgress');
  }.property('content.mpackServiceVersions.@each.selected', 'App.router.btnClickInProgress'),

  getMpackVersionById: function (versionId) {
    const mpackVersions = this.get('content.mpackVersions');
    const byVersionId = version => version.id === versionId;
    
    if (mpackVersions) {
      const version = mpackVersions.find(byVersionId);
      return version;
    }  

    return null;
  },

  getServiceVersionById: function (versionId) {
    const serviceVersions = this.get('content.mpackServiceVersions');
    const byVersionId = version => version.id === versionId;

    if (serviceVersions) {
      const version = serviceVersions.find(byVersionId);
      return version;
    }

    return null;
  },

  getUsecaseById: function (usecaseId) {
    const usecases = this.get('content.mpackUsecases');
    const byUsecaseId = usecase => usecase.id === usecaseId;
    
    if (usecases) {
      const usecase = usecases.find(byUsecaseId);
      return usecase;
    }
    
    return null;
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

  displayServiceVersion: function (versionId) {
    const version = this.getServiceVersionById(versionId);

    if (version) {
      version.service.versions.forEach(serviceVersion => {
        if (serviceVersion.get('id') === versionId) {
          serviceVersion.set('displayed', true);
        } else {
          serviceVersion.set('displayed', false);
        }
      })
    }
  },

  addMpackHandler: function (mpackVersionId) {
    if (this.addMpack(mpackVersionId)) {
      this.get('wizardController').setStepUnsaved('selectMpacks');
    }
  },

  addMpack: function (mpackVersionId) {
    const mpackVersion = this.getMpackVersionById(mpackVersionId);

    if (mpackVersion) {
      mpackVersion.services.forEach(service => this.addService(service.id))
      return true;
    }

    return false;
  },

  toggleUsecaseHandler: function (usecaseId) {
    if (this.toggleUsecase(usecaseId)) {
      this.get('wizardController').setStepUnsaved('selectMpacks');
    }
  },

  toggleUsecase: function (usecaseId) {
    this.clearSelection();
    
    const usecase = this.getUsecaseById(usecaseId);
    if (usecase) {
      const selected = usecase.get('selected');
      usecase.set('selected', !selected);
      
      const usecasesSelected = this.get('content.mpackUsecases').filterProperty('selected');
      if (usecasesSelected.length > 0) {
        this.getUsecaseRecommendation()
          .done(this.getUsecaseRecommendationSucceeded.bind(this))
          .fail(this.getUsecaseRecommendationFailed.bind(this));
      }
      
      return true;
    }

    return false;
  },

  getUsecaseRecommendation: function (registryId) {
    const usecases = this.get('content.mpackUsecases').filterProperty('selected').map(usecase =>
      ({
        scenario_name: usecase.name
      })
    );

    return App.ajax.send({
      name: 'registry.recommendation.usecases',
      data: {
        registryId: registryId || 1,
        usecases: usecases
      },
      showLoadingPopup: true,
      sender: this
    });
  },

  getUsecaseRecommendationSucceeded: function (data) {
    this.clearSelection();
    
    let recommendations;
    if (data && data.resources && data.resources.length > 0 && data.resources[0].recommendations) {
      recommendations = data.resources[0].recommendations.mpack_bundles;
    }
    
    if (recommendations && recommendations.length > 0
      && recommendations[0].mpacks && recommendations[0].mpacks.length > 0) {
      const mpackVersionIds = recommendations[0].mpacks.map(mpack => mpack.mpack_name + mpack.mpack_version);
      mpackVersionIds.forEach(this.addMpack.bind(this));
    } else {
      this.set('noRecommendationAvailable', true);
    }
  },

  getUsecaseRecommendationFailed: function () {
    App.showAlertPopup(
      Em.I18n.t('common.error'), //header
      Em.I18n.t('installer.selectMpacks.getRecommendationFailed') //body
    );
  },

  addServiceHandler: function (serviceId) {
    if (this.addService(serviceId)) {
      this.get('wizardController').setStepUnsaved('selectMpacks');
    }
  },

  addService: function (serviceId) {
    const service = this.getServiceVersionById(serviceId);

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
    const service = this.getServiceVersionById(serviceId);

    if (service) {
      service.set('selected', false);
      service.set('mpackVersion.selected', service.get('mpackVersion.services').some(s => s.get('selected') === true));
      return true;
    }

    return false;
  },

  removeMpackHandler: function (mpackId) {
    if (this.removeMpack(mpackId)) {
      this.get('wizardController').setStepUnsaved('selectMpacks');
    }
  },

  removeMpack: function (mpackId) {
    const mpackVersion = this.getMpackVersionById(mpackId);

    if (mpackVersion) {
      mpackVersion.get('services').forEach(service => this.removeService(service.get('id')));
      return true;
    }

    return false;
  },

  selectedServices: function () {
    const mpackServiceVersions = this.get('content.mpackServiceVersions');
    return mpackServiceVersions ? mpackServiceVersions.filter(s => s.get('selected') === true) : [];
  }.property('content.mpackServiceVersions.@each.selected'),

  selectedMpackVersions: function () {
    const versions = this.get('content.mpackVersions');
    return versions ? versions.filter(v => v.get('selected') === true) : [];
  }.property('content.mpackVersions.@each.selected', 'selectedServices'),

  hasSelectedMpackVersions: function () {
    const versions = this.get('content.mpackVersions');
    return versions ? versions.some(v => v.get('selected') === true) : false;
  }.property('content.mpackVersions.@each.selected', 'selectedServices'),

  clearSelection: function () {
    const mpackServiceVersions = this.get('content.mpackServiceVersions');
    if (mpackServiceVersions) {
      mpackServiceVersions.setEach('selected', false);
    }
    
    const versions = this.get('content.mpackVersions');
    if (versions) {
      versions.setEach('selected', false);
    }
    
    if (this.get('content.advancedMode')) {
      const usecases = this.get('content.mpackUsecases');
      if (usecases) {
        usecases.setEach('selected', false);
      }
    }  

    this.set('noRecommendationAvailable', false);
    this.get('wizardController').setStepUnsaved('selectMpacks');
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
