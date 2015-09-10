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
/**
 * By Step 7, we have the following information stored in App.db and set on this
 * controller by the router.
 *
 *   selectedServices: App.db.selectedServices (the services that the user selected in Step 4)
 *   masterComponentHosts: App.db.masterComponentHosts (master-components-to-hosts mapping the user selected in Step 5)
 *   slaveComponentHosts: App.db.slaveComponentHosts (slave-components-to-hosts mapping the user selected in Step 6)
 *
 */

App.WizardStep7Controller = Em.Controller.extend(App.ServerValidatorMixin, App.EnhancedConfigsMixin, App.ToggleIsRequiredMixin, {

  name: 'wizardStep7Controller',

  /**
   * Contains all field properties that are viewed in this step
   * @type {object[]}
   */
  stepConfigs: [],

  hash: null,

  selectedService: null,

  slaveHostToGroup: null,

  addMiscTabToPage: true,

  selectedServiceNameTrigger: null,

  /**
   * Is Submit-click processing now
   * @type {bool}
   */
  submitButtonClicked: false,

  isRecommendedLoaded: false,
  /**
   * used in services_config.js view to mark a config with security icon
   */
  secureConfigs: require('data/HDP2/secure_mapping'),

  /**
   * config categories with secure properties
   * use only for add service wizard when security is enabled;
   */
  secureServices: function () {
    return $.extend(true, [], require('data/HDP2/secure_configs'));
  }.property(),

  /**
   * If configChangeObserver Modal is shown
   * @type {bool}
   */
  miscModalVisible: false,

  overrideToAdd: null,

  /**
   * Is installer controller used
   * @type {bool}
   */
  isInstaller: true,

  /**
   * List of config groups
   * @type {object[]}
   */
  configGroups: [],

  /**
   * List of config group to be deleted
   * @type {object[]}
   */
  groupsToDelete: [],

  preSelectedConfigGroup: null,

  /**
   * Currently selected config group
   * @type {object}
   */
  selectedConfigGroup: null,

  /**
   * Config tags of actually installed services
   * @type {array}
   */
  serviceConfigTags: [],

  /**
   * Are applied to service configs loaded
   * @type {bool}
   */
  isAppliedConfigLoaded: true,

  isConfigsLoaded: function () {
    return (this.get('wizardController.stackConfigsLoaded') && this.get('isAppliedConfigLoaded'));
  }.property('wizardController.stackConfigsLoaded', 'isAppliedConfigLoaded'),

  /**
   * Number of errors in the configs in the selected service
   * @type {number}
   */
  errorsCount: function () {
    return this.get('selectedService.configs').filter(function (config) {
      return Em.isNone(config.get('widget'));
    }).filter(function(config) {
      return !config.get('isValid') || (config.get('overrides') || []).someProperty('isValid', false);
    }).filterProperty('isVisible').length;
  }.property('selectedService.configs.@each.isValid', 'selectedService.configs.@each.overrideErrorTrigger'),

  /**
   * Should Next-button be disabled
   * @type {bool}
   */
  isSubmitDisabled: function () {
    if (!this.get('stepConfigs.length')) return true;
    if (this.get('submitButtonClicked')) return true;
    return (!this.get('stepConfigs').filterProperty('showConfig', true).everyProperty('errorCount', 0) || this.get("miscModalVisible"));
  }.property('stepConfigs.@each.errorCount', 'miscModalVisible', 'submitButtonClicked'),

  /**
   * List of selected to install service names
   * @type {string[]}
   */
  selectedServiceNames: function () {
    return this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false).mapProperty('serviceName');
  }.property('content.services', 'content.services.@each.isSelected', 'content.services.@each.isInstalled', 'content.stacks.@each.isSelected').cacheable(),

  /**
   * List of installed and selected to install service names
   * @type {string[]}
   */
  allSelectedServiceNames: function () {
    return this.get('content.services').filter(function (service) {
      return service.get('isInstalled') || service.get('isSelected');
    }).mapProperty('serviceName');
  }.property('content.services', 'content.services.@each.isSelected', 'content.services.@each.isInstalled', 'content.stacks.@each.isSelected').cacheable(),

  /**
   * List of installed service names
   * @type {string[]}
   */
  installedServiceNames: function () {
    var serviceNames = this.get('content.services').filterProperty('isInstalled').mapProperty('serviceName');
    if (this.get('content.controllerName') !== 'installerController') {
      serviceNames = serviceNames.filter(function (_serviceName) {
        return !App.get('services.noConfigTypes').contains(_serviceName);
      });
    }
    return serviceNames;
  }.property('content.services').cacheable(),

  /**
   * List of master components
   * @type {Ember.Enumerable}
   */
  masterComponentHosts: function () {
    return this.get('content.masterComponentHosts');
  }.property('content.masterComponentHosts'),

  /**
   * List of slave components
   * @type {Ember.Enumerable}
   */
  slaveComponentHosts: function () {
    return this.get('content.slaveGroupProperties');
  }.property('content.slaveGroupProperties', 'content.slaveComponentHosts'),

  customData: [],

  /**
   * Filter text will be located here
   * @type {string}
   */
  filter: '',

  /**
   * List of filters for config properties to populate filter combobox
   */
  propertyFilters: [
    {
      attributeName: 'isOverridden',
      attributeValue: true,
      caption: 'common.combobox.dropdown.overridden'
    },
    {
      attributeName: 'isFinal',
      attributeValue: true,
      caption: 'common.combobox.dropdown.final'
    },
    {
      attributeName: 'hasIssues',
      attributeValue: true,
      caption: 'common.combobox.dropdown.issues'
    }
  ],

  issuesFilterText: function () {
    return (this.get('isSubmitDisabled') && !this.get('submitButtonClicked') &&
      this.get('filterColumns').findProperty('attributeName', 'hasIssues').get('selected')) ?
        Em.I18n.t('installer.step7.showingPropertiesWithIssues') : '';
  }.property('isSubmitDisabled', 'submitButtonClicked', 'filterColumns.@each.selected'),

  issuesFilterLinkText: function () {
    if (this.get('filterColumns').findProperty('attributeName', 'hasIssues').get('selected')) {
      return Em.I18n.t('installer.step7.showAllProperties');
    }

    return (this.get('isSubmitDisabled') && !this.get('submitButtonClicked')) ?
      (
        this.get('filterColumns').findProperty('attributeName', 'hasIssues').get('selected') ?
          Em.I18n.t('installer.step7.showAllProperties') : Em.I18n.t('installer.step7.showPropertiesWithIssues')
      ) : '';
  }.property('isSubmitDisabled', 'submitButtonClicked', 'filterColumns.@each.selected'),

  /**
   * Dropdown menu items in filter combobox
   */
  filterColumns: function () {
    return this.get('propertyFilters').map(function (filter) {
      return Ember.Object.create({
        attributeName: filter.attributeName,
        attributeValue: filter.attributeValue,
        name: this.t(filter.caption),
        selected: false
      });
    }, this);
  }.property('propertyFilters'),

  /**
   * Clear controller's properties:
   *  <ul>
   *    <li>stepConfigs</li>
   *    <li>filter</li>
   *  </ul>
   *  and desect all <code>filterColumns</code>
   * @method clearStep
   */
  clearStep: function () {
    this.setProperties({
      configValidationGlobalMessage: [],
      submitButtonClicked: false,
      isSubmitDisabled: true,
      isRecommendedLoaded: false
    });
    this.get('stepConfigs').clear();
    this.set('filter', '');
    this.get('filterColumns').setEach('selected', false);
  },

  /**
   * Generate "finger-print" for current <code>stepConfigs[0]</code>
   * Used to determine, if user has some unsaved changes (comparing with <code>hash</code>)
   * @returns {string|null}
   * @method getHash
   */
  getHash: function () {
    if (!this.get('stepConfigs')[0]) {
      return null;
    }
    var hash = {};
    this.get('stepConfigs').forEach(function(stepConfig){
      stepConfig.configs.forEach(function (config) {
        hash[config.get('name')] = {value: config.get('value'), overrides: [], isFinal: config.get('isFinal')};
        if (!config.get('overrides')) return;
        if (!config.get('overrides.length')) return;

        config.get('overrides').forEach(function (override) {
          hash[config.get('name')].overrides.push(override.get('value'));
        });
      });
    });
    return JSON.stringify(hash);
  },

   /**
   * Are some changes available
   */
  hasChanges: function () {
    return this.get('hash') != this.getHash();
  },

  /**
   * Load config groups for installed services
   * One ajax-request for each service
   * @param {string[]} servicesNames
   * @method loadInstalledServicesConfigGroups
   */
  loadInstalledServicesConfigGroups: function (servicesNames) {
    servicesNames.forEach(function (serviceName) {
      App.ajax.send({
        name: 'config.tags_and_groups',
        sender: this,
        data: {
          serviceName: serviceName,
          serviceConfigsDef: App.config.get('preDefinedServiceConfigs').findProperty('serviceName', serviceName)
        },
        success: 'loadServiceTagsSuccess'
      });
    }, this);
  },

  /**
   * Create site to tag map. Format:
   * <code>
   *   {
   *    site1: tag1,
   *    site1: tag2,
   *    site2: tag3
   *    ...
   *   }
   * </code>
   * @param {object} desired_configs
   * @param {string[]} sites
   * @returns {object}
   * @private
   * @method _createSiteToTagMap
   */
  _createSiteToTagMap: function (desired_configs, sites) {
    var siteToTagMap = {};
    for (var site in desired_configs) {
      if (desired_configs.hasOwnProperty(site)) {
        if (!!sites[site]) {
          siteToTagMap[site] = desired_configs[site].tag;
        }
      }
    }
    return siteToTagMap;
  },

  /**
   * Load config groups success callback
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method loadServiceTagsSuccess
   */
  loadServiceTagsSuccess: function (data, opt, params) {
    var serviceName = params.serviceName,
      service = this.get('stepConfigs').findProperty('serviceName', serviceName),
      defaultConfigGroupHosts = this.get('wizardController.allHosts').mapProperty('hostName'),
      siteToTagMap = this._createSiteToTagMap(data.Clusters.desired_configs, params.serviceConfigsDef.get('configTypes'));
    this.set('loadedClusterSiteToTagMap', siteToTagMap);

    //parse loaded config groups
    var configGroups = [];
    if (data.config_groups.length) {
      data.config_groups.forEach(function (item) {
        item = item.ConfigGroup;
        if (item.tag === serviceName) {
          var groupHosts = item.hosts.mapProperty('host_name');
          configGroups.push({
            id: serviceName + item.id,
            config_group_id: item.id,
            name: item.group_name,
            description: item.description,
            is_default: false,
            parent_config_group_id: App.ServiceConfigGroup.getParentConfigGroupId(serviceName),
            service_id: serviceName,
            service_name: serviceName,
            hosts: groupHosts,
            desired_configs: item.desired_configs
          });
          groupHosts.forEach(function (host) {
            defaultConfigGroupHosts = defaultConfigGroupHosts.without(host);
          }, this);
        }
      }, this);
    }

    var defaultConfigGroup = App.configGroupsMapper.generateDefaultGroup(serviceName, defaultConfigGroupHosts);

    configGroups = configGroups.sortProperty('name');
    configGroups.unshift(defaultConfigGroup);
    App.store.loadMany(App.ServiceConfigGroup, configGroups);
    App.store.commit();
    service.set('configGroups', App.ServiceConfigGroup.find().filterProperty('serviceName', serviceName));

    var loadedGroupToOverrideSiteToTagMap = {};
    configGroups.forEach(function (item) {
      var groupName = item.name;
      loadedGroupToOverrideSiteToTagMap[groupName] = {};
      item.desired_configs.forEach(function (site) {
        loadedGroupToOverrideSiteToTagMap[groupName][site.type] = site.tag;
      }, this);
    }, this);
    this.set('preSelectedConfigGroup', App.ServiceConfigGroup.find(App.ServiceConfigGroup.getParentConfigGroupId(serviceName)));
    App.config.loadServiceConfigGroupOverrides(service.get('configs'), loadedGroupToOverrideSiteToTagMap, service.get('configGroups'), this.onLoadOverrides, this);
  },

  onLoadOverrides: function (configs) {
    var serviceName = configs[0].serviceName,
      service = this.get('stepConfigs').findProperty('serviceName', serviceName);
    var serviceConfig = App.config.createServiceConfig(serviceName);
    service.set('selectedConfigGroup', this.get('preSelectedConfigGroup'));
    this.loadComponentConfigs(service.get('configs'), serviceConfig, service);
    // override if a property isRequired or not
    this.overrideConfigIsRequired(service);
    service.set('configs', serviceConfig.get('configs'));
  },

  /**
   * Set <code>isEditable</code>-property to <code>serviceConfigProperty</code>
   * Based on user's permissions and selected config group
   * @param {Ember.Object} serviceConfigProperty
   * @param {bool} defaultGroupSelected
   * @returns {Ember.Object} Updated config-object
   * @method _updateIsEditableFlagForConfig
   */
  _updateIsEditableFlagForConfig: function (serviceConfigProperty, defaultGroupSelected) {
    if (App.isAccessible('ADMIN')) {
      if (defaultGroupSelected && !this.get('isHostsConfigsPage') && !Em.get(serviceConfigProperty, 'group')) {
        serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isReconfigurable'));
      } else if (Em.get(serviceConfigProperty, 'group') && Em.get(serviceConfigProperty, 'group.name') == this.get('selectedConfigGroup.name')) {
        serviceConfigProperty.set('isEditable', true);
      } else {
        serviceConfigProperty.set('isEditable', false);
      }
    }
    else {
      serviceConfigProperty.set('isEditable', false);
    }
    return serviceConfigProperty;
  },

  /**
   * Set <code>overrides</code>-property to <code>serviceConfigProperty<code>
   * @param {Ember.Object} serviceConfigProperty
   * @param {Ember.Object} component
   * @return {Ember.Object} Updated config-object
   * @method _updateOverridesForConfig
   */
  _updateOverridesForConfig: function (serviceConfigProperty, component) {

    var overrides = serviceConfigProperty.get('overrides');

    if (Em.isNone(overrides)) {
      serviceConfigProperty.set('overrides', Em.A([]));
      return serviceConfigProperty;
    }
    serviceConfigProperty.set('overrides', null);
    var defaultGroupSelected = component.get('selectedConfigGroup.isDefault');

    // Wrap each override to App.ServiceConfigProperty
    overrides.forEach(function (override) {
      var newSCP = App.ServiceConfigProperty.create(serviceConfigProperty);
      newSCP.set('value', override.value);
      newSCP.set('isOriginalSCP', false); // indicated this is overridden value,
      newSCP.set('parentSCP', serviceConfigProperty);
      if (defaultGroupSelected) {
        var group = component.get('configGroups').findProperty('name', override.group.get('name'));
        // prevent cycle in proto object, clean link
        if (group.get('properties').length == 0) {
          group.set('properties', Em.A([]));
        }
        group.get('properties').push(newSCP);
        newSCP.set('group', override.group);
        newSCP.set('isEditable', false);
      }
      var parentOverridesArray = serviceConfigProperty.get('overrides');
      if (Em.isNone(parentOverridesArray)) {
        parentOverridesArray = Em.A([]);
        serviceConfigProperty.set('overrides', parentOverridesArray);
      }
      serviceConfigProperty.get('overrides').pushObject(newSCP);
      newSCP.validate();
    }, this);
    return serviceConfigProperty;
  },

  /**
   * Set configs with overrides, recommended defaults to component
   * @param {Ember.Object[]} configs
   * @param {Ember.Object} componentConfig
   * @param {Ember.Object} component
   * @method loadComponentConfigs
   */
  loadComponentConfigs: function (configs, componentConfig, component) {
    var defaultGroupSelected = component.get('selectedConfigGroup.isDefault');

    configs.forEach(function (serviceConfigProperty) {
      if (!serviceConfigProperty) return;

      if (Em.isNone(serviceConfigProperty.get('isOverridable'))) {
        serviceConfigProperty.set('isOverridable', true);
      }
      this._updateOverridesForConfig(serviceConfigProperty, component);
      this._updateIsEditableFlagForConfig(serviceConfigProperty, defaultGroupSelected);

      componentConfig.get('configs').pushObject(serviceConfigProperty);
      serviceConfigProperty.validate();

    }, this);
    component.get('configGroups').filterProperty('isDefault', false).forEach(function (configGroup) {
      configGroup.set('hash', this.get('wizardController').getConfigGroupHash(configGroup));
    }, this);
    var overrideToAdd = this.get('overrideToAdd');
    if (overrideToAdd) {
      overrideToAdd = componentConfig.get('configs').findProperty('name', overrideToAdd.name);
      if (overrideToAdd) {
        var group = this.get('selectedService.configGroups').findProperty('name', this.get('selectedConfigGroup.name'));
        var newSCP = App.config.createOverride(overrideToAdd, {isEditable: true}, group);
        group.get('properties').pushObject(newSCP);
        component.set('overrideToAdd', null);
      }
    }
  },

  /**
   *  Resolve dependency between configs.
   *  @param serviceName {String}
   *  @param configs {Ember.Enumerable}
   */
  resolveServiceDependencyConfigs: function (serviceName, configs) {
    switch (serviceName) {
      case 'STORM':
        this.resolveStormConfigs(configs);
        break;
      case 'YARN':
        this.resolveYarnConfigs(configs);
        break;
    }
  },

  /**
   * Update some Storm configs
   * If Ganglia is selected to install or already installed, Ganglia host should be added to configs
   * @param {Ember.Enumerable} configs
   * @method resolveStormConfigs
   */
  resolveStormConfigs: function (configs) {
    var dependentConfigs, gangliaServerHost, gangliaHostId;
    dependentConfigs = ['nimbus.childopts', 'supervisor.childopts', 'worker.childopts'];
    var props = this.get('wizardController').getDBProperties(['masterComponentHosts', 'hosts']);
    var masterComponentHosts = props.masterComponentHosts;
    var hosts = props.hosts;
    // if Ganglia selected or installed, set ganglia host to configs
    if (this.get('installedServiceNames').contains('STORM') && this.get('installedServiceNames').contains('GANGLIA')) return;
    if (this.get('allSelectedServiceNames').contains('GANGLIA') || this.get('installedServiceNames').contains('GANGLIA')) {
      if (this.get('wizardController.name') === 'addServiceController') {
        gangliaServerHost = masterComponentHosts.findProperty('component', 'GANGLIA_SERVER').hostName;
      } else {
        gangliaHostId = masterComponentHosts.findProperty('component', 'GANGLIA_SERVER').host_id;
        for (var hostName in hosts) {
          if (hosts[hostName].id == gangliaHostId) gangliaServerHost = hosts[hostName].name;
        }
      }
      dependentConfigs.forEach(function (configName) {
        var config = configs.findProperty('name', configName);
        if (!Em.isNone(config.value)) {
          var replaceStr = config.value.match(/.jar=host[^,]+/)[0];
          var replaceWith = replaceStr.slice(0, replaceStr.lastIndexOf('=') - replaceStr.length + 1) + gangliaServerHost;
          config.value = config.recommendedValue = config.value.replace(replaceStr, replaceWith);
        }
      }, this);
    }
  },

  /**
   * Update some Storm configs
   * If SLIDER is selected to install or already installed,
   * some Yarn properties must be changed
   * @param {Ember.Enumerable} configs
   * @method resolveYarnConfigs
   */
  resolveYarnConfigs: function (configs) {
    var cfgToChange = configs.findProperty('name', 'hadoop.registry.rm.enabled');
    if (cfgToChange) {
      var res = this.get('allSelectedServiceNames').contains('SLIDER').toString();
      if (Em.get(cfgToChange, 'value') !== res) {
        Em.set(cfgToChange, 'recommendedValue', res);
        Em.set(cfgToChange, 'value', res);
      }
    }
  },

  /**
   * On load function
   * @method loadStep
   */
  loadStep: function () {
    console.log("TRACE: Loading step7: Configure Services");
    if (!this.get('isConfigsLoaded')) {
      return;
    }
    this.clearStep();

    var self = this;
    //STEP 2: Load on-site configs by service from local DB
    var storedConfigs = this.get('content.serviceConfigProperties');
    //STEP 3: Merge pre-defined configs with loaded on-site configs
    var configs = (storedConfigs && storedConfigs.length)
      ? storedConfigs
      : App.config.mergePreDefinedWithStack(this.get('selectedServiceNames').concat(this.get('installedServiceNames')));
    App.config.setPreDefinedServiceConfigs(this.get('addMiscTabToPage'));

    this.set('groupsToDelete', this.get('wizardController').getDBProperty('groupsToDelete') || []);
    if (this.get('wizardController.name') === 'addServiceController') {
      App.router.get('configurationController').getConfigsByTags(this.get('serviceConfigTags')).done(function (loadedConfigs) {
        self.setInstalledServiceConfigs(self.get('serviceConfigTags'), configs, loadedConfigs, self.get('installedServiceNames'));
        self.applyServicesConfigs(configs, storedConfigs);
      });
    } else {
      this.applyServicesConfigs(configs, storedConfigs);
    }
  },

  applyServicesConfigs: function (configs, storedConfigs) {
    if (this.get('allSelectedServiceNames').contains('YARN')) {
      configs = App.config.fileConfigsIntoTextarea(configs, 'capacity-scheduler.xml', []);
    }
    var dependedServices = ["STORM", "YARN"];
    dependedServices.forEach(function (serviceName) {
      if (this.get('allSelectedServiceNames').contains(serviceName)) {
        this.resolveServiceDependencyConfigs(serviceName, configs);
      }
    }, this);
    //STEP 6: Distribute configs by service and wrap each one in App.ServiceConfigProperty (configs -> serviceConfigs)
    if (App.get('isKerberosEnabled') && this.get('wizardController.name') == 'addServiceController') {
      this.addKerberosDescriptorConfigs(configs, this.get('wizardController.kerberosDescriptorConfigs') || []);
    }
    this.setStepConfigs(configs, storedConfigs);
    this.checkHostOverrideInstaller();
    this.activateSpecialConfigs();
    this.selectProperService();
    var self = this;
    var rangerService = App.StackService.find().findProperty('serviceName', 'RANGER');
    if (rangerService && !rangerService.get('isInstalled') && !rangerService.get('isSelected')) {
      App.config.removeRangerConfigs(self.get('stepConfigs'));
    }
    if (this.get('content.serviceConfigProperties.length') > 0) {
      this.completeConfigLoading();
    } else {
      this.loadServerSideConfigsRecommendations().always(function () {
        if (self.get('wizardController.name') == 'addServiceController') {
          // for Add Service just remove or add dependent properties and ignore config values changes
          // for installed services only
          self.addRemoveDependentConfigs(self.get('installedServiceNames'));
          self.clearDependenciesForInstalledServices(self.get('installedServiceNames'), self.get('stepConfigs'));
        }
        // * add dependencies based on recommendations
        // * update config values with recommended
        // * remove properties received from recommendations
        self.updateDependentConfigs();
        self.completeConfigLoading();
      });
    }
  },

  completeConfigLoading: function() {
    this.clearDependentConfigsByService(App.StackService.find().filterProperty('isSelected').mapProperty('serviceName'));
    this.set('isRecommendedLoaded', true);
    if (this.get('content.skipConfigStep')) {
      App.router.send('next');
    }
    this.set('hash', this.getHash());
  },
  /**
   * After user navigates back to step7, values for depended configs should be set to values set by user and not to default values
   * @method restoreRecommendedConfigs
   */
  restoreRecommendedConfigs: function () {
    var recommendationsConfigs = this.get('recommendationsConfigs') || {};
    var serviceConfigProperties = this.get('content.serviceConfigProperties') || [];
    var stepConfigs = this.get('stepConfigs');
    Em.keys(recommendationsConfigs).forEach(function (file) {
      (Em.keys(recommendationsConfigs[file].properties).concat(Em.keys(recommendationsConfigs[file].property_attributes || {}))).forEach(function (configName) {
        stepConfigs.forEach(function (stepConfig) {
          stepConfig.get('configs').filterProperty('name', configName).forEach(function (configProperty) {
            if (Em.get(configProperty, 'filename').contains(file)) {
              var scps = serviceConfigProperties.filterProperty('name', configName).filter(function (cp) {
                return Em.get(cp, 'filename').contains(file);
              });
              if (scps.length) {
                Em.set(configProperty, 'value', Em.get(scps[0], 'value'));
              }
            }
          });
        });
      });
    });
  },

  /**
   * Mark descriptor properties in configuration object.
   *
   * @param {Object[]} configs - config properties to change
   * @param {App.ServiceConfigProperty[]} descriptor - parsed kerberos descriptor
   * @method addKerberosDescriptorConfigs
   */
  addKerberosDescriptorConfigs: function (configs, descriptor) {
    descriptor.forEach(function (item) {
      var property = configs.findProperty('name', item.get('name'));
      if (property) {
        Em.setProperties(property, {
          isSecureConfig: true,
          displayName: Em.get(item, 'name'),
          isUserProperty: false,
          isOverridable: false,
          category: 'Advanced ' + Em.get(item, 'filename')
        });
      }
    });
  },

  /**
   * Load config groups
   * and (if some services are already installed) load config groups for installed services
   * @method checkHostOverrideInstaller
   */
  checkHostOverrideInstaller: function () {
    if (this.get('wizardController.name') !== 'kerberosWizardController') {
      this.loadConfigGroups(this.get('content.configGroups'));
    }
    if (this.get('installedServiceNames').length > 0 && !this.get('wizardController.areInstalledConfigGroupsLoaded')) {
      this.loadInstalledServicesConfigGroups(this.get('installedServiceNames'));
    }
  },

  /**
   * Set init <code>stepConfigs</code> value
   * Set <code>selected</code> for addable services if addServiceController is used
   * Remove SNameNode if HA is enabled (and if addServiceController is used)
   * @param {Ember.Object[]} configs
   * @param {Ember.Object[]} storedConfigs
   * @method setStepConfigs
   */
  setStepConfigs: function (configs, storedConfigs) {
    var localDB = {
      hosts: this.get('wizardController.content.hosts'),
      masterComponentHosts: this.get('wizardController.content.masterComponentHosts'),
      slaveComponentHosts: this.get('wizardController.content.slaveComponentHosts')
    };
    var serviceConfigs = App.config.renderConfigs(configs, storedConfigs, this.get('allSelectedServiceNames'), this.get('installedServiceNames'), localDB);
    if (this.get('wizardController.name') === 'addServiceController') {
      serviceConfigs.setEach('showConfig', true);
      serviceConfigs.setEach('selected', false);
      this.get('selectedServiceNames').forEach(function (serviceName) {
        if (!serviceConfigs.findProperty('serviceName', serviceName)) return;
        serviceConfigs.findProperty('serviceName', serviceName).set('selected', true);
      }, this);
      this.get('installedServiceNames').forEach(function (serviceName) {
        var serviceConfigObj = serviceConfigs.findProperty('serviceName', serviceName);
        var isInstallableService = App.StackService.find(serviceName).get('isInstallable');
        if (!isInstallableService) serviceConfigObj.set('showConfig', false);
      }, this);
      // if HA is enabled -> Remove SNameNode
      if (App.get('isHaEnabled')) {
        var c = serviceConfigs.findProperty('serviceName', 'HDFS').configs,
          removedConfigs = c.filterProperty('category', 'SECONDARY_NAMENODE');
        removedConfigs.setEach('isVisible', false);
        serviceConfigs.findProperty('serviceName', 'HDFS').configs = c;

        serviceConfigs = this._reconfigureServicesOnNnHa(serviceConfigs);
      }
    }

    // Remove Notifications from MISC if it isn't Installer Controller
    if (this.get('wizardController.name') !== 'installerController') {
      var miscService = serviceConfigs.findProperty('serviceName', 'MISC');
      if (miscService) {
        c = miscService.configs;
        removedConfigs = c.filterProperty('category', 'Notifications');
        removedConfigs.map(function (config) {
          c = c.without(config);
        });
        miscService.configs = c;
      }
    }
    this.set('stepConfigs', serviceConfigs);
  },

  /**
   * When NameNode HA is enabled some configs based on <code>dfs.nameservices</code> should be changed
   * This happens only if service is added AFTER NN HA is enabled
   *
   * @param {App.ServiceConfig[]} serviceConfigs
   * @method _reconfigureServiceOnNnHa
   * @private
   * @returns {App.ServiceConfig[]}
   */
  _reconfigureServicesOnNnHa: function (serviceConfigs) {
    var selectedServiceNames = this.get('selectedServiceNames');
    var nameServiceId = serviceConfigs.findProperty('serviceName', 'HDFS').configs.findProperty('name', 'dfs.nameservices');
    Em.A([
      {
        serviceName: 'HBASE',
        configToUpdate: 'hbase.rootdir'
      },
      {
        serviceName: 'ACCUMULO',
        configToUpdate: 'instance.volumes'
      }
    ]).forEach(function (c) {
      if (selectedServiceNames.contains(c.serviceName) && nameServiceId) {
        var cfg = serviceConfigs.findProperty('serviceName', c.serviceName).configs.findProperty('name', c.configToUpdate),
          newValue = cfg.get('value').replace(/\/\/.*:[0-9]+/i, '//' + nameServiceId.get('value'));

        cfg.setProperties({
          value: newValue,
          recommendedValue: newValue
        });
      }
    });
    return serviceConfigs;
  },

  /**
   * Select first addable service for <code>addServiceWizard</code>
   * Select first service at all in other cases
   * @method selectProperService
   */
  selectProperService: function () {
    if (this.get('wizardController.name') === 'addServiceController') {
      this.set('selectedService', this.get('stepConfigs').filterProperty('selected', true).get('firstObject'));
    } else {
      this.set('selectedService', this.get('stepConfigs').filterProperty('showConfig', true).objectAt(0));
    }
  },

  /**
   * Load config tags
   * @return {$.ajax|null}
   * @method getConfigTags
   */
  getConfigTags: function () {
    this.set('isAppliedConfigLoaded', false);
    return App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'getConfigTagsSuccess'
    });
  },

  /**
   * Success callback for config tags request
   * Updates <code>serviceConfigTags</code> with tags received from server
   * @param {object} data
   * @method getConfigTagsSuccess
   */
  getConfigTagsSuccess: function (data) {
    var installedServiceSites = [];
    App.StackService.find().filterProperty('isInstalled').forEach(function (service) {
      if (!service.get('configTypes')) return;
      var configTypes = Object.keys(service.get('configTypes'));
      installedServiceSites = installedServiceSites.concat(configTypes);
    }, this);
    installedServiceSites = installedServiceSites.uniq();
    var serviceConfigTags = [];
    for (var site in data.Clusters.desired_configs) {
      if (data.Clusters.desired_configs.hasOwnProperty(site)) {
        if (installedServiceSites.contains(site) || site == 'cluster-env') {
          serviceConfigTags.push({
            siteName: site,
            tagName: data.Clusters.desired_configs[site].tag,
            newTagName: null
          });
        }
      }
    }
    this.set('serviceConfigTags', serviceConfigTags);
    this.set('isAppliedConfigLoaded', true);
  },

  /**
   * set configs actual values from server
   * @param serviceConfigTags
   * @param configs
   * @param configsByTags
   * @param installedServiceNames
   * @method setInstalledServiceConfigs
   */
  setInstalledServiceConfigs: function (serviceConfigTags, configs, configsByTags, installedServiceNames) {
    var configsMap = {};
    var configMixin = App.get('config');
    var nonServiceTab = require('data/service_configs');

    configsByTags.forEach(function (configSite) {
      configsMap[configSite.type] = configSite.properties || {};
    });
    configs.forEach(function (_config) {
      var type = _config.filename ? App.config.getConfigTagFromFileName(_config.filename) : null;
      var mappedConfigValue = type && configsMap[type] ? configsMap[type][_config.name] : null;
      if (!Em.isNone(mappedConfigValue) && ((installedServiceNames && installedServiceNames.contains(_config.serviceName) || nonServiceTab.someProperty('serviceName', _config.serviceName)))) {
        // prevent overriding already edited properties
        if (_config.savedValue != mappedConfigValue || _config.displayType == 'password') {
          _config.value = App.config.formatPropertyValue(_config, mappedConfigValue);
        }
        _config.savedValue = App.config.formatPropertyValue(_config, mappedConfigValue);
        _config.hasInitialValue = true;
        delete configsMap[type][_config.name];
      }
    });
    //add user properties

    Em.keys(configsMap).forEach(function (filename) {
      Em.keys(configsMap[filename]).forEach(function (propertyName) {
        configs.push(configMixin.addUserProperty({
          id: 'site property',
          name: propertyName,
          serviceName: configMixin.getServiceNameByConfigType(filename),
          value: configsMap[filename][propertyName],
          savedValue: configsMap[filename][propertyName],
          filename: configMixin.get('filenameExceptions').contains(filename) ? filename : filename + '.xml',
          category: 'Advanced',
          hasInitialValue: true,
          isUserProperty: true,
          isOverridable: true,
          overrides: [],
          isRequired: true,
          isVisible: true,
          showLabel: true
        }, false, []));
      });
    });
  },

  /**
   * Add group ids to <code>groupsToDelete</code>
   * Also save <code>groupsToDelete</code> to local storage
   * @param {Ember.Object[]} groups
   * @method setGroupsToDelete
   */
  setGroupsToDelete: function (groups) {
    var groupsToDelete = this.get('groupsToDelete');
    groups.forEach(function (group) {
      if (group.get('id'))
        groupsToDelete.push({
          id: group.get('id')
        });
    });
    this.get('wizardController').setDBProperty('groupsToDelete', groupsToDelete);
  },

  /**
   * Update <code>configGroups</code> with selected service configGroups
   * Also set default group to first position
   * Update <code>selectedConfigGroup</code> with new default group
   * @method selectedServiceObserver
   */
  selectedServiceObserver: function () {
    if (this.get('selectedService') && (this.get('selectedService.serviceName') !== 'MISC')) {
      var serviceGroups = this.get('selectedService.configGroups');
      serviceGroups.forEach(function (item, index, array) {
        if (item.isDefault) {
          array.unshift(item);
          array.splice(index + 1, 1);
        }
      });
      this.set('configGroups', serviceGroups);
      this.set('selectedConfigGroup', serviceGroups.findProperty('isDefault'));
    }
  }.observes('selectedService.configGroups.@each'),

  /**
   * load default groups for each service in case of initial load
   * @param serviceConfigGroups
   * @method loadConfigGroups
   */
  loadConfigGroups: function (serviceConfigGroups) {
    var services = this.get('stepConfigs');
    var hosts = this.get('wizardController.allHosts').mapProperty('hostName');

    services.forEach(function (service) {
      if (service.get('serviceName') === 'MISC') return;
      var serviceRawGroups = serviceConfigGroups.filterProperty('service_name', service.serviceName);
      var id = App.ServiceConfigGroup.getParentConfigGroupId(service.get('serviceName'));
      if (!serviceRawGroups.length) {
        App.store.load(App.ServiceConfigGroup, App.configGroupsMapper.generateDefaultGroup(service.get('serviceName'), hosts));
        App.store.commit();
        service.set('configGroups', [App.ServiceConfigGroup.find(id)]);
      }
      else {
        App.store.loadMany(App.ServiceConfigGroup, serviceRawGroups);
        App.store.commit();
        serviceRawGroups.forEach(function(item){
          var modelGroup = App.ServiceConfigGroup.find(item.id);
          var wrappedProperties = [];

          item.properties.forEach(function (propertyData) {
            var parentSCP = service.configs.filterProperty('filename', propertyData.filename).findProperty('name', propertyData.name);
            var overriddenSCP = App.ServiceConfigProperty.create(parentSCP);
            overriddenSCP.set('isOriginalSCP', false);
            overriddenSCP.set('parentSCP', parentSCP);
            overriddenSCP.set('group', modelGroup);
            overriddenSCP.setProperties(propertyData);
            wrappedProperties.pushObject(App.ServiceConfigProperty.create(overriddenSCP));
          });
          modelGroup.set('properties', wrappedProperties);
        }, this);
        service.set('configGroups', App.ServiceConfigGroup.find().filterProperty('serviceName', service.get('serviceName')));
      }
    });
  },

  /**
   * Click-handler on config-group to make it selected
   * @param {object} event
   * @method selectConfigGroup
   */
  selectConfigGroup: function (event) {
    this.set('selectedConfigGroup', event.context);
  },

  /**
   * Rebuild list of configs switch of config group:
   * on default - display all configs from default group and configs from non-default groups as disabled
   * on non-default - display all from default group as disabled and configs from selected non-default group
   * @method switchConfigGroupConfigs
   */
  switchConfigGroupConfigs: function () {
    var serviceConfigs = this.get('selectedService.configs'),
      selectedGroup = this.get('selectedConfigGroup'),
      overrideToAdd = this.get('overrideToAdd'),
      overrides = [];
    if (!selectedGroup) return;

    var displayedConfigGroups = this._getDisplayedConfigGroups();
    displayedConfigGroups.forEach(function (group) {
      overrides.pushObjects(group.get('properties'));
    });
    serviceConfigs.forEach(function (config) {
      this._setEditableValue(config);
      this._setOverrides(config, overrides);
    }, this);
    //this.getRecommendationsForDependencies(null, true, Em.K);
  }.observes('selectedConfigGroup'),

  /**
   * Get list of config groups to display
   * Returns empty array if no <code>selectedConfigGroup</code>
   * @return {Array}
   * @method _getDisplayedConfigGroups
   */
  _getDisplayedConfigGroups: function () {
    var selectedGroup = this.get('selectedConfigGroup');
    if (!selectedGroup) return [];
    return (selectedGroup.get('isDefault')) ?
      this.get('selectedService.configGroups').filterProperty('isDefault', false) :
      [this.get('selectedConfigGroup')];
  },

  /**
   * Set <code>isEditable</code> property to <code>config</code>
   * @param {Ember.Object} config
   * @return {Ember.Object} updated config-object
   * @method _setEditableValue
   */
  _setEditableValue: function (config) {
    var selectedGroup = this.get('selectedConfigGroup');
    if (!selectedGroup) return config;
    var isEditable = config.get('isEditable'),
      isServiceInstalled = this.get('installedServiceNames').contains(this.get('selectedService.serviceName'));
    if (isServiceInstalled) {
      isEditable = (!isEditable && !config.get('isReconfigurable')) ? false : selectedGroup.get('isDefault');
    }
    else {
      isEditable = selectedGroup.get('isDefault');
    }
    if (config.get('group')) {
      isEditable = config.get('group.name') == this.get('selectedConfigGroup.name');
    }
    config.set('isEditable', isEditable);
    return config;
  },

  /**
   * Set <code>overrides</code> property to <code>config</code>
   * @param {Ember.Object} config
   * @param {Ember.Enumerable} overrides
   * @returns {Ember.Object}
   * @method _setOverrides
   */
  _setOverrides: function (config, overrides) {
    var selectedGroup = this.get('selectedConfigGroup'),
      overrideToAdd = this.get('overrideToAdd'),
      configOverrides = overrides.filterProperty('name', config.get('name'));
    if (!selectedGroup) return config;
    if (overrideToAdd && overrideToAdd.get('name') === config.get('name')) {
      var valueForOverride = (config.get('widget') || config.get('displayType') == 'checkbox') ? config.get('value') : '';
      var group = this.get('selectedService.configGroups').findProperty('name', selectedGroup.get('name'));
      var newSCP = App.config.createOverride(config, {value: valueForOverride, recommendedValue: valueForOverride}, group);
      configOverrides.push(newSCP);
      group.get('properties').pushObject(newSCP);
      this.set('overrideToAdd', null);
    }
    configOverrides.setEach('isEditable', !selectedGroup.get('isDefault'));
    configOverrides.setEach('parentSCP', config);
    config.set('overrides', configOverrides);
    return config;
  },

  /**
   * @method manageConfigurationGroup
   */
  manageConfigurationGroup: function () {
    App.router.get('manageConfigGroupsController').manageConfigurationGroups(this);
  },

  /**
   * Make some configs visible depending on active services
   * @method activateSpecialConfigs
   */
  activateSpecialConfigs: function () {
    if (this.get('addMiscTabToPage')) {
      var serviceToShow = this.get('selectedServiceNames').concat('MISC');
      var miscConfigs = this.get('stepConfigs').findProperty('serviceName', 'MISC').configs;
      if (this.get('wizardController.name') == "addServiceController") {
        miscConfigs.findProperty('name', 'smokeuser').set('isEditable', false);
        miscConfigs.findProperty('name', 'user_group').set('isEditable', false);
        if (this.get('content.smokeuser')) {
          miscConfigs.findProperty('name', 'smokeuser').set('value', this.get('content.smokeuser'));
        }
        if (this.get('content.group')) {
          miscConfigs.findProperty('name', 'user_group').set('value', this.get('content.group'));
        }
      }
      App.config.miscConfigVisibleProperty(miscConfigs, serviceToShow);
    }
    var wizardController = this.get('wizardController');
    if (wizardController.get('name') === "kerberosWizardController")  {
      var kerberosConfigs =  this.get('stepConfigs').findProperty('serviceName', 'KERBEROS').configs;
      kerberosConfigs.findProperty('name', 'kdc_type').set('value', wizardController.get('content.kerberosOption'));
    }
  },

  /**
   * Check whether hive New MySQL database is on the same host as Ambari server MySQL server
   * @return {$.ajax|null}
   * @method checkMySQLHost
   */
  checkMySQLHost: function () {
    // get ambari database type and hostname
    return App.ajax.send({
      name: 'ambari.service',
      data: {
        fields : "?fields=hostComponents/RootServiceHostComponents/properties/server.jdbc.database_name,hostComponents/RootServiceHostComponents/properties/server.jdbc.url"
      },
      sender: this,
      success: 'getAmbariDatabaseSuccess'
    });
  },

  /**
   * Success callback for ambari database, get Ambari DB type and DB server hostname, then
   * Check whether hive New MySQL database is on the same host as Ambari server MySQL server
   * @param {object} data
   * @method getAmbariDatabaseSuccess
   */
  getAmbariDatabaseSuccess: function (data) {
    var hiveDBHostname = this.get('stepConfigs').findProperty('serviceName', 'HIVE').configs.findProperty('name', 'hive_ambari_host').value;
    var ambariServiceHostComponents = data.hostComponents;
    if (!!ambariServiceHostComponents.length) {
      var ambariDBInfo = JSON.stringify(ambariServiceHostComponents[0].RootServiceHostComponents.properties);
      this.set('mySQLServerConflict', ambariDBInfo.indexOf('mysql') > 0 && ambariDBInfo.indexOf(hiveDBHostname) > 0);
    } else {
      this.set('mySQLServerConflict', false);
    }
  },

  /**
   * Check if new MySql database was chosen for Hive service
   * and it is not located on the same host as Ambari server
   * that using MySql database too.
   *
   * @method resolveHiveMysqlDatabase
   **/
  resolveHiveMysqlDatabase: function () {
    var hiveService = this.get('content.services').findProperty('serviceName', 'HIVE');
    if (!hiveService || !hiveService.get('isSelected') || hiveService.get('isInstalled')) {
      this.moveNext();
      return;
    }
    var hiveDBType = this.get('stepConfigs').findProperty('serviceName', 'HIVE').configs.findProperty('name', 'hive_database').value;
    if (hiveDBType == 'New MySQL Database') {
      var self = this;
      this.checkMySQLHost().done(function () {
        if (self.get('mySQLServerConflict')) {
          // error popup before you can proceed
          return App.ModalPopup.show({
            header: Em.I18n.t('installer.step7.popup.mySQLWarning.header'),
            body:Em.I18n.t('installer.step7.popup.mySQLWarning.body'),
            secondary: Em.I18n.t('installer.step7.popup.mySQLWarning.button.gotostep5'),
            primary: Em.I18n.t('installer.step7.popup.mySQLWarning.button.dismiss'),
            onSecondary: function () {
              var parent = this;
              return App.ModalPopup.show({
                header: Em.I18n.t('installer.step7.popup.mySQLWarning.confirmation.header'),
                body: Em.I18n.t('installer.step7.popup.mySQLWarning.confirmation.body'),
                onPrimary: function () {
                  this.hide();
                  parent.hide();
                  // go back to step 5: assign masters and disable default navigation warning
                  App.router.get('installerController').gotoStep(5, true);
                }
              });
            }
          });
        } else {
          self.moveNext();
        }
      });
    } else {
      this.moveNext();
    }
  },

  checkDatabaseConnectionTest: function () {
    var deferred = $.Deferred();
    var configMap = [
      {
        serviceName: 'OOZIE',
        ignored: [Em.I18n.t('installer.step7.oozie.database.new')]
      },
      {
        serviceName: 'HIVE',
        ignored: [Em.I18n.t('installer.step7.hive.database.new.mysql'), Em.I18n.t('installer.step7.hive.database.new.postgres')]
      }
    ];
    configMap.forEach(function (config) {
      var isConnectionNotTested = false;
      var service = this.get('content.services').findProperty('serviceName', config.serviceName);
      if (service && service.get('isSelected') && !service.get('isInstalled')) {
        var serviceConfigs = this.get('stepConfigs').findProperty('serviceName', config.serviceName).configs;
        var serviceDatabase = serviceConfigs.findProperty('name', config.serviceName.toLowerCase() + '_database').get('value');
        if (!config.ignored.contains(serviceDatabase)) {
          var filledProperties = App.db.get('tmp', config.serviceName + '_connection');
          if (!filledProperties || App.isEmptyObject(filledProperties)) {
            isConnectionNotTested = true;
          } else {
            for (var key in filledProperties) {
              if (serviceConfigs.findProperty('name', key).get('value') !== filledProperties[key])
                isConnectionNotTested = true;
            }
          }
        }
      }
      config.isCheckIgnored = isConnectionNotTested;
    }, this);
    var ignoredServices = configMap.filterProperty('isCheckIgnored', true);
    if (ignoredServices.length) {
      var displayedServiceNames = ignoredServices.mapProperty('serviceName').map(function (serviceName) {
        return this.get('content.services').findProperty('serviceName', serviceName).get('displayName');
      }, this);
      this.showDatabaseConnectionWarningPopup(displayedServiceNames, deferred);
    }
    else {
      deferred.resolve();
    }
    return deferred;
  },

  showChangesWarningPopup: function(goToNextStep) {
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      body: Em.I18n.t('services.service.config.exitChangesPopup.body'),
      secondary: Em.I18n.t('common.cancel'),
      primary: Em.I18n.t('yes'),
      onPrimary: function () {
        if (goToNextStep) {
          goToNextStep();
          this.hide(); 
        }
      },
      onSecondary: function () {
        this.hide();
      }
    });
  },

  showDatabaseConnectionWarningPopup: function (serviceNames, deferred) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step7.popup.database.connection.header'),
      body: Em.I18n.t('installer.step7.popup.database.connection.body').format(serviceNames.join(', ')),
      secondary: Em.I18n.t('common.cancel'),
      primary: Em.I18n.t('common.proceedAnyway'),
      onPrimary: function () {
        deferred.resolve();
        this._super();
      },
      onSecondary: function () {
        self.set('submitButtonClicked', false);
        deferred.reject();
        this._super();
      }
    });
  },

  showOozieDerbyWarningPopup: function(callback) {
    var self = this;
    if (this.get('selectedServiceNames').contains('OOZIE')) {
      var databaseType = Em.getWithDefault(this.findConfigProperty('oozie_database', 'oozie-env.xml') || {}, 'value', '');
      if (databaseType == Em.I18n.t('installer.step7.oozie.database.new')) {
        return App.ModalPopup.show({
          header: Em.I18n.t('common.warning'),
          body: Em.I18n.t('installer.step7.popup.oozie.derby.warning'),
          secondary: Em.I18n.t('common.cancel'),
          primary: Em.I18n.t('common.proceedAnyway'),
          onPrimary: function() {
            this.hide();
            if (callback) {
              callback();
            }
          },
          onSecondary: function() {
            self.set('submitButtonClicked', false);
            this.hide();
          },
          onClose: function() {
            this.onSecondary();
          }
        });
      }
    }
    if (callback) {
      callback();
    }
    return false;
  },

  /**
   * Proceed to the next step
   **/
  moveNext: function () {
    App.router.send('next');
  },

  /**
   * Click-handler on Next button
   * Disable "Submit"-button while server-side processes are running
   * @method submit
   */
  submit: function () {
    if (this.get('isSubmitDisabled')) {
      return;
    }
    var self = this;
    this.set('submitButtonClicked', true);
    this.serverSideValidation().done(function() {
      self.serverSideValidationCallback();
    })
    .fail(function (value) {
      if ("invalid_configs" == value) {
        self.set('submitButtonClicked', false);
      } else {
        // Failed due to validation mechanism failure.
        // Should proceed with other checks
        self.serverSideValidationCallback();
      }
    });
  },

  /**
   * @method serverSideValidationCallback
   */
  serverSideValidationCallback: function() {
    var self = this;
    this.showOozieDerbyWarningPopup(function() {
      self.checkDatabaseConnectionTest().done(function () {
        self.resolveHiveMysqlDatabase();
        self.set('submitButtonClicked', false);
      });
    });
  },

  toggleIssuesFilter: function () {
    this.get('filterColumns').findProperty('attributeName', 'hasIssues').toggleProperty('selected');

    // if currently selected service does not have issue, jump to the first service with issue.
    if (this.get('selectedService.errorCount') == 0 )
    {
      var errorServices = this.get('stepConfigs').filterProperty('errorCount');
      if (errorServices.length > 0)
      {
        var service = errorServices[0];
        this.set('selectedService', service);
        this.propertyDidChange('selectedServiceNameTrigger');
        $('a[href="#' + service.serviceName + '"]').tab('show');
      }
    }
  }
});
