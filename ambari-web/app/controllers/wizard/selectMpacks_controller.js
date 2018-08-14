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
require('./wizardStep_controller');

App.WizardSelectMpacksController = App.WizardStepController.extend({
  //#region Properties

  name: 'wizardSelectMpacksController',

  stepName: 'selectMpacks',

  noRecommendationAvailable: false,

  filterMpacksText: "",

  filterServicesText: "",

  filterMpacksPlaceholder: Em.I18n.t('installer.selectMpacks.filterMpacks'),

  filterServicesPlaceholder: Em.I18n.t('installer.selectMpacks.filterServices'),

  /** 
   * Mpacks already registered on the server.
   */
  registeredMpacks: Em.computed.alias('content.registeredMpacks'),

  /**
   * Mpacks selected to be registered via the current wizard.
   */
  selectedMpacks: Em.computed.alias('content.selectedMpacks'),

  /**
   * Service instances already existin in the cluster.
   */
  serviceInstances: [],

  /**
   * Service instances added via the current wizard.
   */
  addedServiceInstances: [],
  
  /**
   * All service groups currently "in the cart."
   * This includes the ones already existing in the cluster
   * and any new ones added by the user in the current wizard.
   */
  allServiceInstances: function () {
    return this.get('serviceInstances').concat(this.get('addedServiceInstances'));
  }.property('serviceInstances.@each', 'addedServiceInstances.@each'),

  /**
   * Service groups already existing in the cluster.
   */
  serviceGroups: [],

  /**
   * Service groups added via the current wizard.
   */
  addedServiceGroups: [],

  /**
   * All service groups currently "in the cart."
   * This includes the ones already existing in the cluster
   * and any new ones added by the user in the current wizard.
   */
  allServiceGroups: function () {
    return this.get('serviceGroups').concat(this.get('addedServiceGroups'));
  }.property('serviceGroups.@each', 'addedServiceGroups.@each'),

  selectedUseCases: function selectedUseCases() {
    return this.get('content.mpackUseCases').filterProperty('selected');
  }.property('content.mpackUseCases.@each.selected'),

  selectedServices: function selectedServices() {
    const mpackServiceVersions = this.get('content.mpackServiceVersions');
    return mpackServiceVersions ? mpackServiceVersions.filter(s => s.get('selected') === true) : [];
  }.property('content.mpackServiceVersions.@each.selected'),

  selectedMpackVersions: function selectedMpackVersions() {
    const versions = this.get('content.mpackVersions');
    return versions ? versions.filter(v => v.get('selected') === true) : [];
  }.property('content.mpackVersions.@each.selected', 'selectedServices'),

  isSaved: function isSaved() {
    const wizardController = this.get('wizardController');
    if (wizardController) {
      return wizardController.getStepSavedState('selectMpacks');
    }
    return false;
  }.property('wizardController.content.stepsSavedState'),

  isSubmitDisabled: function isSubmitDisabled() {
    const mpackServiceVersions = this.get('content.mpackServiceVersions');
    return App.get('router.btnClickInProgress')
      || (this.get('wizardController.errors') && this.get('wizardController.errors').length > 0)
      || mpackServiceVersions.filterProperty('selected', true).length === 0;
  }.property('content.mpackServiceVersions.@each.selected', 'App.router.btnClickInProgress', 'wizardController.errors'),

  //#endregion

  //#region Registry

  loadRegistry: function () {
    return App.ajax.send({
      name: 'registry.all',
      showLoadingPopup: true,
      sender: this
    });
  },

  loadRegistrySucceeded: function (data) {
    //Returns the first (newest) version of each mpack matching the list of names provided.
    const getMpacksByName = mpackNames => {
      const getMpackByName = mpackName => {
        const mpacks = this.get('content.mpacks');
        
        if (mpacks) {
          //reinstate this if/when the test runner can handle for..of loops
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
      };
    
      return mpackNames.map(mpackName => getMpackByName(mpackName));
    };

    const mpacks = data.items.reduce(
      (mpacks, registry) => mpacks.concat(
        registry.mpacks.map(mpack => {
          return Em.Object.create({
            selected: function () { return this.versions.someProperty('selected', true); }.property('versions.@each.selected'),
            name: mpack.RegistryMpackInfo.mpack_name,
            displayName: mpack.RegistryMpackInfo.mpack_display_name,
            description: mpack.RegistryMpackInfo.mpack_description,
            registryId: mpack.RegistryMpackInfo.registry_id,
            //this is the text that will be used to filter this mpack in the UI
            //at this point, this is just the text that comes from this mpack
            //but additional text will be appended to form the final filterOn value
            //from the mpack's services, specifically the service name
            filterOn: (
              (mpack.RegistryMpackInfo.mpack_name || "") + " "
              + (mpack.RegistryMpackInfo.mpack_display_name || "") + " "
              + (mpack.RegistryMpackInfo.mpack_description || "")
            ).toLowerCase(),
            logoUrl: mpack.RegistryMpackInfo.mpack_logo_url,
            versions: mpack.versions ? mpack.versions.map((version, index) => {
              return Em.Object.create({
                selected: false,
                displayed: index === 0 ? true : false, //by default, display first version
                id: mpack.RegistryMpackInfo.mpack_name + version.RegistryMpackVersionInfo.mpack_version,
                version: version.RegistryMpackVersionInfo.mpack_version,
                mpackUrl: version.RegistryMpackVersionInfo.mpack_uri,
                logoUrl: version.RegistryMpackVersionInfo.mpack_logo_uri,
                docUrl: version.RegistryMpackVersionInfo.mpack_doc_uri,
                services: version.RegistryMpackVersionInfo.modules ? version.RegistryMpackVersionInfo.modules.map(service => {
                  return Em.Object.create({
                    selected: false,
                    displayed: index === 0 ? true : false, //by default, display first version
                    id: mpack.RegistryMpackInfo.mpack_name + version.RegistryMpackVersionInfo.mpack_version + service.name,
                    name: service.name,
                    displayName: service.displayName,
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
      []
    );

    const mpackServiceVersions = mpackVersions.reduce(
      (services, mpackVersion) => services.concat(
        mpackVersion.get('services').map(service => {
          service.set('mpackVersion', mpackVersion);
          return service;
        })
      ),
      []
    );

    const uniqueServices = {};
    mpackServiceVersions.forEach(service => {
      //append service name to filterOn of the containing mpack
      const mpackFilterOn = service.get('mpackVersion.mpack.filterOn');
      service.set('mpackVersion.mpack.filterOn', ((mpackFilterOn) + " " + (service.name || "")).toLowerCase());

      uniqueServices[service.name] = Em.Object.create({
        name: service.name,
        displayName: service.displayName,
        description: service.description,
        filterOn: (
          (service.name || "") + " "
          + (service.description || "") + " "
          + (service.get('mpackVersion.mpack.displayName') || "" )
        ).toLowerCase(),
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

    const useCases = data.items.reduce(
      (useCases, registry) => useCases.concat(
        registry.scenarios.map(useCase => {
          return Em.Object.create({
            selected: false,
            id: useCase.RegistryScenarioInfo.scenario_id || useCase.RegistryScenarioInfo.scenario_name, //TODO: mpacks - remove fallback when id is available
            name: useCase.RegistryScenarioInfo.scenario_name,
            displayName: useCase.RegistryScenarioInfo.scenario_display_name || useCase.RegistryScenarioInfo.scenario_name, //TODO: mpacks - remove fallback when display name is available
            description: useCase.RegistryScenarioInfo.scenario_description,
            mpacks: getMpacksByName(useCase.RegistryScenarioInfo.scenario_mpacks.map(mpack => mpack.name))
          });
        })
      ), []
    );

    this.set('content.mpackUseCases', useCases);
  },
  
  loadRegistryFailed: function () {
    this.set('content.mpacks', []);

    App.showAlertPopup(
      Em.I18n.t('common.error'), //header
      Em.I18n.t('installer.selectMpacks.loadRegistryFailed') //body
    );
  },

  registryLoaded: function () {
    const mpacks = this.get('content.mpacks');
    const mpackVersions = this.get('content.mpackVersions');
    const mpackServices = this.get('content.mpackServices');
    const mpackServiceVersions = this.get('content.mpackServiceVersions');

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

  //#endregion

  //#region Helpers

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

  getUseCaseById: function (useCaseId) {
    const useCases = this.get('content.mpackUseCases');
    const byUseCaseId = useCase => useCase.id === useCaseId;
    
    if (useCases) {
      const useCase = useCases.find(byUseCaseId);
      return useCase;
    }
    
    return null;
  },

  getServiceGroup: function (serviceGroupName) {
    return this.get('allServiceGroups').findProperty('name', serviceGroupName);
  },

  //#endregion

  //#region Version Display

  /**
   * Changes which version of an mpack is displayed.
   */
  displayMpackVersion: function (mpackVersionId) {
    const mpackVersion = this.getMpackVersionById(mpackVersionId);

    if (mpackVersion) {
      mpackVersion.get('mpack.versions').forEach(version => {
        if (version.get('id') === mpackVersionId) {
          version.set('displayed', true);
        } else {
          version.set('displayed', false);
        }
      })
    }
  },

  /**
   * Changes which version of a service is displayed.
   */
  displayServiceVersion: function (serviceVersionId) {
    const serviceVersion = this.getServiceVersionById(serviceVersionId);

    if (serviceVersion) {
      serviceVersion.get('service.versions').forEach(version => {
        if (version.get('id') === serviceVersionId) {
          version.set('displayed', true);
        } else {
          version.set('displayed', false);
        }
      })
    }
  },

  //#endregion

  //#region Use Cases

  toggleUseCaseHandler: function (useCaseId) {
    if (this.toggleUseCase(useCaseId)) {
      this.get('wizardController').setStepUnsaved('selectMpacks');
    }
  },

  toggleUseCase: function (useCaseId) {
    this.clearSelection();
    
    const useCase = this.getUseCaseById(useCaseId);
    if (useCase) {
      const selected = useCase.get('selected');
      useCase.set('selected', !selected);
      
      const useCasesSelected = this.get('selectedUseCases');
      if (useCasesSelected.length > 0) {
        this.getUseCaseRecommendation()
          .done(this.getUseCaseRecommendationSucceeded.bind(this))
          .fail(this.getUseCaseRecommendationFailed.bind(this));
      }
      
      return true;
    }

    return false;
  },

  getUseCaseRecommendation: function (registryId) {
    const useCases = this.get('content.mpackUseCases').filterProperty('selected').map(useCase =>
      ({
        scenario_name: useCase.get('name')
      })
    );

    return App.ajax.send({
      name: 'registry.recommendation.useCases',
      data: {
        registryId: registryId || 1,
        useCases: useCases
      },
      showLoadingPopup: true,
      sender: this
    });
  },

  getUseCaseRecommendationSucceeded: function (data) {
    this.clearSelection();
    
    let recommendations;
    if (data && data.resources && data.resources.length > 0 && data.resources[0].recommendations) {
      recommendations = data.resources[0].recommendations.mpack_bundles;
    }
    
    if (recommendations && recommendations.length > 0 && recommendations[0].mpacks && recommendations[0].mpacks.length > 0) {
      recommendations[0].mpacks.forEach(mpack => {
        const serviceGroup = this.addServiceGroup(mpack.mpack_name + mpack.mpack_version, mpack.mpack_name);
        serviceGroup.mpackVersion.services.forEach(service => this.addServiceInstance(service.name, service.name, serviceGroup));
      });
    } else {
      this.set('noRecommendationAvailable', true);
    }
  },

  getUseCaseRecommendationFailed: function () {
    App.showAlertPopup(
      Em.I18n.t('common.error'), //header
      Em.I18n.t('installer.selectMpacks.getRecommendationFailed') //body
    );
  },

  //#endregion

  //#region Add/Remove

  /**
   * Adds mpack version to list of mpacks to register.
   */
  addMpackVersion: function (mpackVersionId) {
    const mpackVersion = this.getMpackVersionById(mpackVersionId);
    
    if (mpackVersion) { 
      mpackVersion.set('selected', true);
      return mpackVersion;
    }
  },

  /**
   * Removes the specified mpack version from the list of mpacks to register.
   * Sets all of its services as unselected.
   */
  removeMpackVersion: function (mpackVersion) {
    mpackVersion.get('services').forEach(service => this.removeServiceVersion(service));
    mpackVersion.set('selected', false);
  },

  /**
   * Set service version as selected.
   */
  addServiceVersion: function (serviceVersionId) {
    const serviceVersion = this.getServiceVersionById(serviceVersionId);
    
    if (serviceVersion) { 
      serviceVersion.set('selected', true);
      return serviceVersion;
    }
  },

  /**
   * Sets the specified service version as unselected.
   */
  removeServiceVersion: function (serviceVersion) {
    serviceVersion.set('selected', false);
  },

  /**
   * Creates a service group object for use in this controller only.
   */
  createServiceGroup: function (name, mpackVersion, canRemove) {
    const self = this;

    return Em.Object.create({
      name: name,
      mpackVersion: mpackVersion,
      serviceInstances: function () { return self.get('allServiceInstances').filterProperty('serviceGroup.name', name); }.property().volatile(),
      canRemove: canRemove
    });
  },

  /**
   * Adds a service group bound to the mpack to the list of service groups to create.
   */
  addServiceGroup: function (mpackVersionId, serviceGroupName) {
    const mpackVersion = this.addMpackVersion(mpackVersionId);

    if (mpackVersion) {
      if (!this.get('allServiceGroups').someProperty('name', serviceGroupName)) { //prevent duplicate service group names
        const serviceGroup = this.createServiceGroup(serviceGroupName, mpackVersion, true);
        this.get('addedServiceGroups').pushObject(serviceGroup);
        return serviceGroup;
      }
    }
  },

  /** 
   * Removes a service group from the selection if it is removable.
   * Removes all of its service instances.
   * Returns true if service group was removed.
   */
  removeServiceGroup: function (serviceGroupName) {
    const serviceGroup = this.getServiceGroup(serviceGroupName);
    
    if (serviceGroup && serviceGroup.get('canRemove')) {
      serviceGroup.get('serviceInstances').forEach(serviceInstance => this.removeServiceInstance(serviceInstance.get('name'), serviceGroup));

      const serviceGroups = this.get('addedServiceGroups');
      const addedServiceGroups = serviceGroups.reject(serviceGroup => serviceGroup.get('name') === serviceGroupName);
      this.set('addedServiceGroups', addedServiceGroups);
      
      return true;
    }
  },

  /**
   * Create service instance object for use in this controller only.
   */
  createServiceInstance: function (name, service, serviceGroup, canRemove) {
    return Em.Object.create({
      id: serviceGroup.get('name') + name,
      name: name,
      service: service,
      serviceGroup: serviceGroup,
      canRemove: canRemove
    });
  },

  /**
   * Adds an instance of the specified service to the specified service group.
   */
  addServiceInstance: function (serviceName, serviceInstanceName, serviceGroup) {
    const service = serviceGroup.get('mpackVersion.services').findProperty('name', serviceName);

    if (service) {
      if (!serviceGroup.get('serviceInstances').someProperty('name', serviceInstanceName)) { //prevent duplicate service instance names
        const serviceInstance = this.createServiceInstance(serviceInstanceName, service, serviceGroup, true);
        
        //note that we do not add the serviceInstance directly to serviceGroup.serviceInstances
        //because serviceGroup.serviceInstances is a FUNCTION that filters addedServiceInstances...
        this.get('addedServiceInstances').pushObject(serviceInstance);
        //...but we do need to notify explicitly because this property pulls data from another object
        serviceGroup.notifyPropertyChange('serviceInstances'); 

        this.addServiceVersion(service.get('id'));

        return serviceInstance;
      }  
    }
  },

  /**
   * Removes a service instance from a service group if it is removable.
   * Returns true if service instance was removed.
   */
  removeServiceInstance: function (serviceInstanceName, serviceGroup) {
    const serviceInstanceToRemove = serviceGroup.get('serviceInstances').findProperty('name', serviceInstanceName);

    if (serviceInstanceToRemove && serviceInstanceToRemove.get('canRemove')) {
      const addedServiceInstances = this.get('addedServiceInstances').reject(serviceInstance => serviceInstance.get('id') === serviceInstanceToRemove.get('id'));
      this.set('addedServiceInstances', addedServiceInstances);
      serviceGroup.notifyPropertyChange('serviceInstances'); //need to notify explicitly because this property pulls data from another object

      return true;
    }
  },

  //#endregion

  //#region Add/Remove Handlers

  /**
   * Adds the mpack version to selection, creates a service group from the mpack version, 
   * and adds instances of all services to the service group.
   */
  addMpackHandler: function (mpackVersionId) {
    const mpackVersion = this.addMpackVersion(mpackVersionId);
    
    if (mpackVersion) {
      const serviceGroup = this.addServiceGroup(mpackVersionId, mpackVersion.get('mpack.name')); //TODO: for now we are setting the service group name equal to the mpack name
      
      if (serviceGroup) {
        mpackVersion.get('services').forEach(service => this.addServiceInstance(service.get('name'), service.get('name'), serviceGroup)); //TODO: for now we are setting the service instance name equal to the service name
      }
    
      this.get('wizardController').setStepUnsaved('selectMpacks');
    }
  },

  /**
   * Removes the service group corresponding to the specified mpack version
   * and removes the mpack version from the selection.
   */
  removeMpackHandler: function (mpackVersionId) {
    const mpackVersion = this.getMpackVersionById(mpackVersionId);
    
    if (mpackVersion) {
      const serviceGroupName = mpackVersion.get('mpack.name'); //TODO: later this will come from the UI
      
      if (this.removeServiceGroup(serviceGroupName)) {
        this.removeMpackVersion(mpackVersion);
  
        this.get('wizardController').setStepUnsaved('selectMpacks');
      }
    }  
  },

  /**
   * Adds an instance of a service.
   * Finds the service group to add it to by matching the mpack version id.
   * If the service group does not exist, it is created first.
   */
  addServiceHandler: function (serviceId) {
    const service = this.addServiceVersion(serviceId);
    
    if (service) {
      //add mpack containing the service to list of mpacks be registered
      const mpackVersion = service.get('mpackVersion');
      this.addMpackVersion(mpackVersion.get('id'));

      //find or create service group for the mpack containing the service
      const serviceGroupName = mpackVersion.get('mpack.name'); //TODO: later this will come from the UI
      const serviceGroup = this.getServiceGroup(serviceGroupName) || this.addServiceGroup(mpackVersion.get('id'), serviceGroupName);
      
      if (serviceGroup) {
        //add a service instance to the service group
        const serviceInstanceName = service.get('name'); //TODO: later this will come from the UI
        this.addServiceInstance(service.get('name'), serviceInstanceName, serviceGroup);
      }
      
      this.get('wizardController').setStepUnsaved('selectMpacks');
    }
  },

  /**
   * Removes the service instance corresponding to the service specified.
   * Deselects the service.
   */
  removeServiceHandler: function (serviceVersionId) {
    const serviceVersion = this.getServiceVersionById(serviceVersionId);
    
    if (serviceVersion) {
      const serviceInstanceName = serviceVersion.get('name'); //TODO: later this will come from the UI
      const mpackVersionId = serviceVersion.get('mpackVersion.id');
      const serviceGroupName = serviceVersion.get('mpackVersion.mpack.name'); //TODO: later this will come from the UI
      
      const serviceGroup = this.getServiceGroup(serviceGroupName);
      if (serviceGroup) {
        if (this.removeServiceInstance(serviceInstanceName, serviceGroup)) {
          this.removeServiceVersion(serviceVersion);

          if (serviceGroup.get('serviceInstances').length === 0) {
            this.removeMpackHandler(mpackVersionId);
          }

          this.get('wizardController').setStepUnsaved('selectMpacks');
        }
      }
    }  
  },

  /**
   * Removes a service group and deselects its associated mpack.
   * 
   * Note: This is just a thin wrapper for removeMpackHandler()
   */
  removeServiceGroupHandler: function (serviceGroupName) {
    const serviceGroup = this.getServiceGroup(serviceGroupName);

    if (serviceGroup) {
      this.removeMpackHandler(serviceGroup.get('mpackVersion.id'));
    }
  },

  /**
   * Removes the service instance specified.
   * Deselects the corresponding service.
   * 
   * Note: This is just a thin wrapper for removeServiceHandler()
   */
  removeServiceInstanceHandler: function (serviceInstanceId) {
    const allServiceGroups = this.get('allServiceGroups');
    
    allServiceGroups.forEach(serviceGroup => {
      const serviceInstance = serviceGroup.get('serviceInstances').findProperty('id', serviceInstanceId);
      
      if (serviceInstance) {
        this.removeServiceHandler(serviceInstance.get('service.id'));
      }
    })
  },

  //#endregion

  //#region Filtering

  filteredMpacks: function () {
    const mpacks = this.get('content.mpacks');
    const filterText = this.get('filterMpacksText').toLowerCase();

    if (filterText.length > 2) {
      const filteredMpacks = mpacks.filter(mpack => {
        return mpack.get('filterOn').indexOf(filterText) > -1;
      });
    
      return filteredMpacks;
    }

    return mpacks;
  }.property('content.mpacks', 'filterMpacksText'),

  clearFilterMpacks: function () {
    this.set('filterMpacksText', "");
  },

  filteredServices: function () {
    const services = this.get('content.mpackServices');
    const filterText = this.get('filterServicesText').toLowerCase();

    if (filterText.length > 2) {
      const filteredServices = services.filter(service => {
        return service.get('filterOn').indexOf(filterText) > -1;
      });

      return filteredServices;
    }

    return services;
  }.property('content.mpackServices', 'filterServicesText'),

  clearFilterServices: function () {
    this.set('filterServicesText', "");
  },

  //#endregion

  //#region Load/Save

  loadStep: function () {
    this.getRegistry().done(() => {
      this.getServiceGroups();
      this.getServiceInstances();
    });
  },

  /**
   * Shapes service group info into objects for use in this controller.
   */
  getServiceGroups: function () {
    const serviceGroups = this.get('content.serviceGroups');
    if (serviceGroups) {
      serviceGroups.forEach(serviceGroup => {
        const mpackVersion = this.getMpackVersionById(serviceGroup.mpackVersionId);
        if (mpackVersion) {
          const sg = this.createServiceGroup(serviceGroup.name, mpackVersion, false);
          this.get('serviceGroups').pushObject(sg);
        }
      });
    }

    const addedServiceGroups = this.get('content.addedServiceGroups');
    if (addedServiceGroups) {
      addedServiceGroups.forEach(serviceGroup => {
        this.addServiceGroup(serviceGroup.mpackVersionId, serviceGroup.name);
      });
    }
  },

  /**
   * Shapes service instance info into objects for use in this controller.
   */
  getServiceInstances: function () {
    const serviceInstances = this.get('content.serviceInstances');
    if (serviceInstances) {
      serviceInstances.forEach(serviceInstance => {
        const serviceGroup = this.getServiceGroup(serviceInstance.serviceGroupName);
      
        if (serviceGroup) {
          const service = serviceGroup.get('mpackVersion.services').findProperty('name', serviceInstance.serviceName);
        
          if (service) {
            this.get('serviceInstances').pushObject({
              id: serviceInstance.serviceGroupName + serviceInstance.name,
              name: serviceInstance.name,
              service: service,
              serviceGroup: serviceGroup,
              canRemove: false
            });
          }
        }
      });
    }

    const addedServiceInstance = this.get('content.addedServiceInstances');
    if (addedServiceInstance) {
      addedServiceInstance.forEach(serviceInstance => {
        const serviceGroup = this.getServiceGroup(serviceInstance.serviceGroupName);
      
        if (serviceGroup) {
          this.addServiceInstance(serviceInstance.serviceName, serviceInstance.name, serviceGroup);
        }
      });
    }
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

  clearSelection: function () {
    const content = this.get('content');

    if (content) {
      const mpackServiceVersions = content.get('mpackServiceVersions');
      if (mpackServiceVersions) {
        mpackServiceVersions.setEach('selected', false);
      }
    
      const versions = content.get('mpackVersions');
      if (versions) {
        versions.setEach('selected', false);
      }
    
      if (content.get('advancedMode')) {
        const useCases = content.get('mpackUseCases');
        if (useCases) {
          useCases.setEach('selected', false);
        }
      }
    }

    this.set('addedServiceGroups', []);
    this.set('addedServiceInstances', []);

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
      const addedServiceGroups = this.get('addedServiceGroups').map(serviceGroup =>
        ({
          name: serviceGroup.get('name'),
          mpackName: serviceGroup.get('mpackVersion.mpack.name'),
          mpackVersion: serviceGroup.get('mpackVersion.version')
        })
      );
      this.set('content.addedServiceGroups', addedServiceGroups);

      const addedServiceInstances = this.get('addedServiceInstances').map(serviceInstance =>
        ({
          id: serviceInstance.get('service.id'),
          name: serviceInstance.get('name'),
          serviceGroupName: serviceInstance.get('serviceGroup.name'),
          serviceName: serviceInstance.get('service.name'),
          mpackName: serviceInstance.get('service.mpackVersion.mpack.name'),
          mpackVersion: serviceInstance.get('service.mpackVersion.version')
        })
      )
      this.set('content.addedServiceInstances', addedServiceInstances);

      //content.selectedServices is populated for legacy code; may be able to remove later
      this.set('content.selectedServices', addedServiceInstances);
      //content.selectedServiceNames is populated for legacy code; may be able to remove later
      const selectedServiceNames = addedServiceInstances.map(serviceInstance => serviceInstance.name);
      this.set('content.selectedServiceNames', selectedServiceNames);

      const selectedMpacks = this.get('selectedMpackVersions').map(mpackVersion => {
        const selectedMpack = {
          id: `${mpackVersion.get('mpack.name')}-${mpackVersion.get('version')}`,
          name: mpackVersion.get('mpack.name'),
          version: mpackVersion.get('version'),
          displayName: mpackVersion.get('mpack.displayName'),
          publicUrl: mpackVersion.get('mpackUrl'),
          downloadUrl: mpackVersion.get('mpackUrl'),
          registryId: mpackVersion.get('mpack.registryId')
        };
        
        //update selected mpack version with previously customized repo URLs for same mpack version
        //this will need to be done when the user has returned to this step
        //after previously going forward, customizing the URLs, and then going back to mpack selection
        const oldSelectedMpacks = this.get('content.selectedMpacks');
        if (oldSelectedMpacks) {
          const oldSelectedMpack = oldSelectedMpacks.find(mpack => mpack.name === mpackVersion.get('mpack.name') && mpack.version === mpackVersion.get('version'));
          if (oldSelectedMpack) {
            selectedMpack.downloadUrl = oldSelectedMpack.downloadUrl;
            selectedMpack.operatingSystems = oldSelectedMpack.operatingSystems;
          }
        }
        
        return selectedMpack;
      });
      this.set('content.selectedMpacks', selectedMpacks);

      App.router.send('next');
    }
  }

  //#endregion
});
