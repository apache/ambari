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

/**
 * @typedef {object} masterComponentHost
 * @property {string} component
 * @property {string} hostName
 * @property {boolean} isInstalled is component already installed on the this host or just going to be installed
 */

/**
 * @typedef {object} topologyLocalDB
 * @property {object[]} hosts list of hosts with information of their disks usage and dirs
 * @property {masterComponentHost[]} masterComponentHosts
 * @property {?object[]} slaveComponentHosts
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

  isConfigsLoaded: Em.computed.and('wizardController.stackConfigsLoaded', 'isAppliedConfigLoaded'),

  /**
   * PreInstall Checks allowed only for Install
   * @type {boolean}
   */
  supportsPreInstallChecks: function () {
    return App.get('supports.preInstallChecks') && 'installerController' === this.get('content.controllerName');
  }.property('App.supports.preInstallChecks', 'wizardController.name'),

  /**
   * Number of errors in the configs in the selected service
   * @type {number}
   */
  errorsCount: function() {
    return this.get('selectedService.configsWithErrors').filter(function(c) {
      return Em.isNone(c.get('widget'));
    }).length;
  }.property('selectedService.configsWithErrors.length'),

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

  installedServices: function () {
    return App.StackService.find().toArray().toMapByCallback('serviceName', function (item) {
      return Em.get(item, 'isInstalled');
    });
  }.property(),

  /**
   * List of master components
   * @type {Ember.Enumerable}
   */
  masterComponentHosts: Em.computed.alias('content.masterComponentHosts'),

  /**
   * List of slave components
   * @type {Ember.Enumerable}
   */
  slaveComponentHosts: Em.computed.alias('content.slaveGroupProperties'),

  customData: [],

  /**
   * Filter text will be located here
   * @type {string}
   */
  filter: '',

  /**
   * list of dependencies that are user to set init value of config
   *
   * @type {Object}
   */
  configDependencies: function() {
    var dependencies = {
      'sliderSelected': this.get('allSelectedServiceNames').contains('SLIDER')
    };
    var hiveMetastore = App.configsCollection.getConfigByName('hive.metastore.uris', 'hive-site.xml');
    var clientPort = App.configsCollection.getConfigByName('clientPort', 'zoo.cfg.xml');

    if (hiveMetastore) dependencies['hive.metastore.uris'] = hiveMetastore.recommendedValue;
    if (clientPort) dependencies['clientPort']  = clientPort.recommendedValue;
    return dependencies
  }.property('allSelectedServiceNames'),

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
    this.loadServiceConfigGroupOverrides(service.get('configs'), loadedGroupToOverrideSiteToTagMap, service.get('configGroups'));
  },

  /**
   * Get properties from server by type and tag with properties, that belong to group
   * push them to common {serviceConfigs} and call callback function
   */
  loadServiceConfigGroupOverrides: function (serviceConfigs, loadedGroupToOverrideSiteToTagMap, configGroups) {
    var configKeyToConfigMap = {};
    serviceConfigs.forEach(function (item) {
      if (!configKeyToConfigMap[item.filename]) {
        configKeyToConfigMap[item.filename] = {};
      }
      configKeyToConfigMap[item.filename][item.name] = item;
    });
    var typeTagToGroupMap = {};
    var urlParams = [];
    for (var group in loadedGroupToOverrideSiteToTagMap) {
      var overrideTypeTags = loadedGroupToOverrideSiteToTagMap[group];
      for (var type in overrideTypeTags) {
        var tag = overrideTypeTags[type];
        typeTagToGroupMap[type + "///" + tag] = configGroups.findProperty('name', group);
        urlParams.push('(type=' + type + '&tag=' + tag + ')');
      }
    }
    var params = urlParams.join('|');
    if (urlParams.length) {
      App.ajax.send({
        name: 'config.host_overrides',
        sender: this,
        data: {
          params: params,
          configKeyToConfigMap: configKeyToConfigMap,
          typeTagToGroupMap: typeTagToGroupMap,
          serviceConfigs: serviceConfigs
        },
        success: 'loadServiceConfigGroupOverridesSuccess'
      });
    } else {
      this.onLoadOverrides(serviceConfigs);
    }
  },

  loadServiceConfigGroupOverridesSuccess: function (data, opt, params) {
    data.items.forEach(function (config) {
      var group = params.typeTagToGroupMap[config.type + "///" + config.tag];
      var properties = config.properties;
      for (var prop in properties) {
        var fileName = App.config.getOriginalFileName(config.type);
        var serviceConfigProperty = !!params.configKeyToConfigMap[fileName] ? params.configKeyToConfigMap[fileName][prop] : false;
        if (serviceConfigProperty) {
          var hostOverrideValue = App.config.formatPropertyValue(serviceConfigProperty, properties[prop]);
          var hostOverrideIsFinal = !!(config.properties_attributes && config.properties_attributes.final && config.properties_attributes.final[prop]);

          App.config.createOverride(serviceConfigProperty, {
            "value": hostOverrideValue,
            "savedValue": hostOverrideValue,
            "isFinal": hostOverrideIsFinal,
            "savedIsFinal": hostOverrideIsFinal,
            "isEditable": false
          }, group, true);
        } else {
          params.serviceConfigs.push(App.config.createCustomGroupConfig(prop, fileName, config.properties[prop], group));
        }
      }
    });
    this.onLoadOverrides(params.serviceConfigs);
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
    if (App.isAuthorized('AMBARI.ADD_DELETE_CLUSTERS')) {
      if (defaultGroupSelected && !this.get('isHostsConfigsPage') && !Em.get(serviceConfigProperty, 'group')) {
        if (serviceConfigProperty.get('serviceName') === 'MISC') {
          var service = App.config.get('serviceByConfigTypeMap')[App.config.getConfigTagFromFileName(serviceConfigProperty.get('filename'))];
          serviceConfigProperty.set('isEditable', service && !this.get('installedServiceNames').contains(service.get('serviceName')));
        } else {
          serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isEditable') && serviceConfigProperty.get('isReconfigurable'));
        }
      } else if (!(Em.get(serviceConfigProperty, 'group') && Em.get(serviceConfigProperty, 'group.name') == this.get('selectedConfigGroup.name'))) {
        serviceConfigProperty.set('isEditable', false);
      }
    }
    else {
      serviceConfigProperty.set('isEditable', false);
    }
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
      if (!Em.isNone(serviceConfigProperty.get('group'))) {
        serviceConfigProperty.get('group.properties').pushObject(serviceConfigProperty);
      }
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
   * On load function
   * @method loadStep
   */
  loadStep: function () {
    if (!this.get('isConfigsLoaded')) {
      return;
    }
    console.time('wizard loadStep: ');
    this.clearStep();

    var self = this;

    App.config.setPreDefinedServiceConfigs(this.get('addMiscTabToPage'));

    var storedConfigs = this.get('content.serviceConfigProperties');

    var configs = (storedConfigs && storedConfigs.length) ? storedConfigs : App.configsCollection.getAll();

    this.set('groupsToDelete', this.get('wizardController').getDBProperty('groupsToDelete') || []);
    if (this.get('wizardController.name') === 'addServiceController' && !this.get('content.serviceConfigProperties.length')) {
      App.router.get('configurationController').getConfigsByTags(this.get('serviceConfigTags')).done(function (loadedConfigs) {
        configs = self.setInstalledServiceConfigs(configs, loadedConfigs, self.get('installedServiceNames'));
        self.applyServicesConfigs(configs);
      });
    } else {
      this.applyServicesConfigs(configs);
    }
  },

  /**
   * Update hawq configuration depending on the state of the cluster
   * @param {Array} configs
   */
  updateHawqConfigs: function (configs) {
    if (this.get('wizardController.name') == 'addServiceController') {
      if (App.get('isHaEnabled')) this.addHawqConfigsOnNnHa(configs);
      if (App.get('isRMHaEnabled')) this.addHawqConfigsOnRMHa(configs);
    }
    if (this.get('content.hosts') && Object.keys(this.get('content.hosts')).length === 1) this.removeHawqStandbyHostAddressConfig(configs);
    return configs
  },

  /**
   * Remove hawq_standby_address_host config from HAWQ configs
   * @param {Array} configs
   */
  removeHawqStandbyHostAddressConfig: function(configs) {
    var hawqStandbyAddressHostIndex = configs.indexOf(configs.findProperty('name', 'hawq_standby_address_host'));
    if (hawqStandbyAddressHostIndex > -1) configs.removeAt(hawqStandbyAddressHostIndex) ;
    return configs
  },

  applyServicesConfigs: function (configs) {
    if (!this.get('installedServiceNames').contains('HAWQ') && this.get('allSelectedServiceNames').contains('HAWQ')) {
      this.updateHawqConfigs(configs);
    }
    if (App.get('isKerberosEnabled') && this.get('wizardController.name') == 'addServiceController') {
      this.addKerberosDescriptorConfigs(configs, this.get('wizardController.kerberosDescriptorConfigs') || []);
    }
    App.configTheme.resolveConfigThemeConditions(configs);
    var stepConfigs = this.createStepConfigs();
    var serviceConfigs = this.renderConfigs(stepConfigs, configs);
    // if HA is enabled -> Make some reconfigurations
    if (this.get('wizardController.name') === 'addServiceController') {
      this.updateComponentActionConfigs(configs, serviceConfigs);
      if (App.get('isHaEnabled')) {
        serviceConfigs = this._reconfigureServicesOnNnHa(serviceConfigs);
      }
    }
    this.set('stepConfigs', serviceConfigs);
    this.checkHostOverrideInstaller();
    this.selectProperService();
    var self = this;
    var rangerService = App.StackService.find().findProperty('serviceName', 'RANGER');
    if (rangerService && !rangerService.get('isInstalled') && !rangerService.get('isSelected')) {
      App.config.removeRangerConfigs(self.get('stepConfigs'));
    }
    this.loadConfigRecommendations(null, this.completeConfigLoading.bind(this));
  },

  /**
   *
   * Makes installed service's configs resulting into component actions (add/delete) non editable on Add Service Wizard
   * @param configs Object[]
   * @param  stepConfigs Object[]
   * @private
   * @method updateComponentActionConfigs
   */
  updateComponentActionConfigs: function(configs, stepConfigs) {
    App.ConfigAction.find().forEach(function(item){
      var configName = item.get('configName');
      var fileName = item.get('fileName');
      var config =  configs.filterProperty('filename', fileName).findProperty('name', configName);
      if (config) {
        var isServiceInstalled = App.Service.find().findProperty('serviceName', config.serviceName);
        if (isServiceInstalled) {
          var serviceConfigs = stepConfigs.findProperty('serviceName', config.serviceName).get('configs');
          var serviceConfig =  serviceConfigs.filterProperty('filename', fileName).findProperty('name', configName);
          serviceConfig.set('isEditable', false);
          config.isEditable = false;
        }
      }
    }, this);
  },

  completeConfigLoading: function() {
    this.clearRecommendationsByServiceName(App.StackService.find().filterProperty('isSelected').mapProperty('serviceName'));
    console.timeEnd('wizard loadStep: ');
    this.set('isRecommendedLoaded', true);
    if (this.get('content.skipConfigStep')) {
      App.router.send('next');
    }
    this.set('hash', this.getHash());
  },

  /**
   * Update initialValues only while loading recommendations first time
   *
   * @param serviceName
   * @returns {boolean}
   * @override
   */
  updateInitialOnRecommendations: function(serviceName) {
    return this._super(serviceName) && !this.get('isRecommendedLoaded');
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
   * Create stepConfigs array with all info except configs list
   *
   * @return {Object[]}
   * @method createStepConfigs
   */
  createStepConfigs: function() {
    var stepConfigs = [];
    App.config.get('preDefinedServiceConfigs').forEach(function (service) {
      var serviceName = service.get('serviceName');
      if (['MISC'].concat(this.get('allSelectedServiceNames')).contains(serviceName)) {
        var serviceConfig = App.config.createServiceConfig(serviceName);
        serviceConfig.set('showConfig', App.StackService.find(serviceName).get('isInstallable'));
        if (this.get('wizardController.name') == 'addServiceController') {
          serviceConfig.set('selected', !this.get('installedServiceNames').concat('MISC').contains(serviceName));
          if (serviceName === 'MISC') {
            serviceConfig.set('configCategories', serviceConfig.get('configCategories').rejectProperty('name', 'Notifications'));
          }
        } else if (this.get('wizardController.name') == 'kerberosWizardController') {
          serviceConfig.set('showConfig', true);
        }
        stepConfigs.pushObject(serviceConfig);
      }
    }, this);
    return stepConfigs;
  },


  /**
   * For Namenode HA, HAWQ service requires additional config parameters in hdfs-client.xml
   * This method ensures that these additional parameters are added to hdfs-client.xml
   * @param configs existing configs on cluster
   * @returns {Object[]} existing configs + additional config parameters in hdfs-client.xml
   */
  addHawqConfigsOnNnHa: function(configs) {
    var nameService = configs.findProperty('id', 'dfs.nameservices__hdfs-site').value;
    var propertyNames = [
      'dfs.nameservices',
      'dfs.ha.namenodes.' + nameService,
      'dfs.namenode.rpc-address.'+ nameService +'.nn1',
      'dfs.namenode.rpc-address.'+ nameService +'.nn2',
      'dfs.namenode.http-address.'+ nameService +'.nn1',
      'dfs.namenode.http-address.'+ nameService +'.nn2'
    ];

    propertyNames.forEach(function(propertyName, propertyIndex) {
      var propertyFromHdfs = configs.findProperty('id', App.config.configId(propertyName, 'hdfs-site'));
      var newProperty = App.config.createDefaultConfig(propertyName, 'hdfs-client.xml', true);
      Em.setProperties(newProperty, {
        serviceName: 'HAWQ',
        description: propertyFromHdfs.description,
        displayName: propertyFromHdfs.displayName,
        displayType: 'string',
        index: propertyIndex,
        isOverridable: false,
        isReconfigurable: false,
        value: propertyFromHdfs.value,
        recommendedValue: propertyFromHdfs.recommendedValue
      });

      configs.push(App.ServiceConfigProperty.create(newProperty));
    });
    return configs;
  },

  /**
   * For ResourceManager HA, HAWQ service requires additional config parameters in yarn-client.xml
   * This method ensures that these additional parameters are added to yarn-client.xml
   * @param configs existing configs on cluster
   * @returns {Object[]} existing configs + additional config parameters in yarn-client.xml
   */
  addHawqConfigsOnRMHa: function(configs) {
    var rmHost1 = configs.findProperty('id', App.config.configId('yarn.resourcemanager.hostname.rm1', 'yarn-site')).value ;
    var rmHost2 = configs.findProperty('id', App.config.configId('yarn.resourcemanager.hostname.rm2', 'yarn-site')).value ;
    var yarnConfigToBeAdded = [
      {
        name: 'yarn.resourcemanager.ha',
        displayName: 'yarn.resourcemanager.ha',
        description: 'Comma separated yarn resourcemanager host addresses with port',
        port: '8032'
      },
      {
        name: 'yarn.resourcemanager.scheduler.ha',
        displayName: 'yarn.resourcemanager.scheduler.ha',
        description: 'Comma separated yarn resourcemanager scheduler addresses with port',
        port: '8030'
      }
    ];

    yarnConfigToBeAdded.forEach(function(propertyDetails) {
      var newProperty = App.config.createDefaultConfig(propertyDetails.name, 'yarn-client.xml', true);
      var value = rmHost1 + ':' + propertyDetails.port + ',' + rmHost2 + ':' + propertyDetails.port;
      Em.setProperties(newProperty, {
        serviceName: 'HAWQ',
        description: propertyDetails.description,
        displayName: propertyDetails.displayName,
        isOverridable: false,
        isReconfigurable: false,
        value: value,
        recommendedValue: value
      });

      configs.push(App.ServiceConfigProperty.create(newProperty));
    });
    return configs;
  },

  /**
   * render configs, distribute them by service
   * and wrap each in ServiceConfigProperty object
   * @param stepConfigs
   * @param configs
   * @return {App.ServiceConfig[]}
   */
  renderConfigs: function (stepConfigs, configs) {
    var localDB = {
      hosts: this.get('wizardController.content.hosts'),
      masterComponentHosts: this.get('wizardController.content.masterComponentHosts'),
      slaveComponentHosts: this.get('wizardController.content.slaveComponentHosts')
    };
    var configsByService = {}, dependencies = this.get('configDependencies');

    configs.forEach(function (_config) {
      if (!configsByService[_config.serviceName]) {
        configsByService[_config.serviceName] = [];
      }
      var serviceConfigProperty = App.ServiceConfigProperty.create(_config);
      this.updateHostOverrides(serviceConfigProperty, _config);
      if (this.get('wizardController.name') === 'addServiceController') {
        this._updateIsEditableFlagForConfig(serviceConfigProperty, true);
      }
      if (!this.get('content.serviceConfigProperties.length') && !serviceConfigProperty.get('hasInitialValue')) {
        App.ConfigInitializer.initialValue(serviceConfigProperty, localDB, dependencies);
      }
      serviceConfigProperty.validate();
      configsByService[_config.serviceName].pushObject(serviceConfigProperty);
    }, this);

    stepConfigs.forEach(function (service) {
      if (service.get('serviceName') === 'YARN') {
        configsByService[service.get('serviceName')] = App.config.addYarnCapacityScheduler(configsByService[service.get('serviceName')]);
      }
      service.set('configs', configsByService[service.get('serviceName')]);
      if (['addServiceController', 'installerController'].contains(this.get('wizardController.name'))) {
        this.addHostNamesToConfigs(service, localDB.masterComponentHosts, localDB.slaveComponentHosts);
      }
    }, this);
    return stepConfigs;
  },

  /**
   * Add host name properties to appropriate categories (for installer and add service)
   *
   * @param {Object} serviceConfig
   * @param {Object[]} masterComponents - info from localStorage
   * @param {Object[]} slaveComponents - info from localStorage
   */
  addHostNamesToConfigs: function(serviceConfig, masterComponents, slaveComponents) {
    serviceConfig.get('configCategories').forEach(function(c) {
      if (c.showHost) {
        var value = [];
        var componentName = c.name;
        var masters = masterComponents && masterComponents.filterProperty('component', componentName);
        if (masters.length) {
          value = masters.mapProperty('hostName');
        } else {
          var slaves = slaveComponents && slaveComponents.findProperty('componentName', componentName);
          if (slaves) {
            value = slaves.hosts.mapProperty('hostName');
          }
        }
        var stackComponent = App.StackServiceComponent.find(componentName);
        var hProperty = App.config.createHostNameProperty(serviceConfig.get('serviceName'), componentName, value, stackComponent);
        var newConfigName = Em.get(hProperty, 'name');
        if (!serviceConfig.get('configs').someProperty('name', newConfigName)) {
          serviceConfig.get('configs').push(App.ServiceConfigProperty.create(hProperty));
        }
      }
    }, this);
  },

  /**
   * create new child configs from overrides, attach them to parent config
   * override - value of config, related to particular host(s)
   * @param configProperty
   * @param storedConfigProperty
   */
  updateHostOverrides: function (configProperty, storedConfigProperty) {
    if (storedConfigProperty.overrides != null && storedConfigProperty.overrides.length > 0) {
      var overrides = [];
      storedConfigProperty.overrides.forEach(function (overrideEntry) {
        // create new override with new value
        var newSCP = App.ServiceConfigProperty.create(configProperty);
        newSCP.set('value', overrideEntry.value);
        newSCP.set('isOriginalSCP', false); // indicated this is overridden value,
        newSCP.set('parentSCP', configProperty);
        overrides.pushObject(newSCP);
      });
      configProperty.set('overrides', overrides);
    }
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
      },
      {
        serviceName: 'HAWQ',
        configToUpdate: 'hawq_dfs_url',
        regexPattern: /(^.*:[0-9]+)(?=\/)/,
        replacementValue: nameServiceId.get('value')
      }
    ]).forEach(function (c) {
      if (selectedServiceNames.contains(c.serviceName) && nameServiceId) {
        var cfg = serviceConfigs.findProperty('serviceName', c.serviceName).configs.findProperty('name', c.configToUpdate);
        var regexPattern = /\/\/.*:[0-9]+/i;
        var replacementValue = '//' + nameServiceId.get('value');
        if (typeof(c.regexPattern) !== "undefined" && typeof(c.replacementValue) !== "undefined") {
          regexPattern = c.regexPattern;
          replacementValue = c.replacementValue;
        }
        var newValue = cfg.get('value').replace(regexPattern, replacementValue);
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
   * @param configs
   * @param configsByTags
   * @param installedServiceNames
   * @method setInstalledServiceConfigs
   */
  setInstalledServiceConfigs: function (configs, configsByTags, installedServiceNames) {
    var configsMap = {};

    configsByTags.forEach(function (configSite) {
      configsMap[configSite.type] = configSite.properties || {};
    });
    var allConfigs = configs.filter(function (_config) {
      // filter out alert_notification configs on add service //TODO find better place for this!
      if (_config.filename === 'alert_notification') return false;
      if ((['MISC'].concat(installedServiceNames).contains(_config.serviceName))) {
        var type = _config.filename ? App.config.getConfigTagFromFileName(_config.filename) : null;
        var mappedConfigValue = type && configsMap[type] ? configsMap[type][_config.name] : null;
        if (Em.isNone(mappedConfigValue)) {
          //for now ranger plugin properties are not sending by recommendations if they are missed - it should be added
          return _config.serviceName == 'MISC' || /^ranger-/.test(_config.filename);
        } else {
          if (_config.savedValue != mappedConfigValue) {
            _config.savedValue = App.config.formatPropertyValue(_config, mappedConfigValue);
          }
          _config.value = App.config.formatPropertyValue(_config, mappedConfigValue);
          _config.hasInitialValue = true;
          this.updateDependencies(_config);
          delete configsMap[type][_config.name];
          return true;
        }
      } else {
        return true;
      }
    }, this);
    //add user properties
    Em.keys(configsMap).forEach(function (filename) {
      Em.keys(configsMap[filename]).forEach(function (propertyName) {
        allConfigs.push(App.config.createDefaultConfig(propertyName, App.config.getOriginalFileName(filename), false, {
            value: configsMap[filename][propertyName],
            savedValue: configsMap[filename][propertyName],
            hasInitialValue: true
        }));
      });
    });
    return allConfigs;
  },

  /**
   * update dependencies according to current config value
   *
   * @param config
   */
  updateDependencies: function(config) {
    if (config.name === 'hive.metastore.uris' && config.filename === 'hive-site.xml') {
      this.get('configDependencies')['hive.metastore.uris'] = config.savedValue;
    } else if (config.name === 'clientPort' && config.filename === 'hive-site.xml') {
      this.get('configDependencies')['clientPort'] = config.savedValue;
    }
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
        App.store.commit();
        App.store.loadMany(App.ServiceConfigGroup, serviceRawGroups);
        App.store.commit();
        serviceRawGroups.forEach(function(item){
          var modelGroup = App.ServiceConfigGroup.find(item.id);
          var wrappedProperties = [];

          item.properties.forEach(function (propertyData) {
            var overriddenSCP, parentSCP = service.configs.filterProperty('filename', propertyData.filename).findProperty('name', propertyData.name);
            if (parentSCP) {
              overriddenSCP = App.ServiceConfigProperty.create(parentSCP);
              overriddenSCP.set('parentSCP', parentSCP);
            } else {
              overriddenSCP = App.config.createCustomGroupConfig(propertyData.name, propertyData.filename, propertyData.value, modelGroup, true, false);
              this.get('stepConfigs').findProperty('serviceName', service.serviceName).get('configs').pushObject(overriddenSCP);
            }
              overriddenSCP.set('isOriginalSCP', false);
              overriddenSCP.set('group', modelGroup);
              overriddenSCP.setProperties(propertyData);
            wrappedProperties.pushObject(App.ServiceConfigProperty.create(overriddenSCP));
          }, this);
          modelGroup.set('properties', wrappedProperties);
        }, this);
        service.set('configGroups', App.ServiceConfigGroup.find().filterProperty('serviceName', service.get('serviceName')));
      }
    }, this);
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
      isEditable = (!isEditable || !config.get('isReconfigurable')) ? false : selectedGroup.get('isDefault');
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
    if (config.get('group')) return config;
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
   * @param serviceName
   * @returns {boolean}
   * @override
   */
  useInitialValue: function(serviceName) {
    return !App.Service.find(serviceName).get('serviceName', serviceName);
  },

  /**
   *
   * @param parentProperties
   * @param name
   * @param fileName
   * @returns {*}
   * @override
   */
  allowUpdateProperty: function(parentProperties, name, fileName) {
    if (name.contains('proxyuser')) return true;
    if (['installerController'].contains(this.get('wizardController.name')) || !!(parentProperties && parentProperties.length)) {
      return true;
    } else if (['addServiceController'].contains(this.get('wizardController.name'))) {
      var stackProperty = App.configsCollection.getConfigByName(name, fileName);
      if (!stackProperty || !this.get('installedServices')[stackProperty.serviceName]) {
        return true;
      } else if (stackProperty.propertyDependsOn.length) {
        return !!stackProperty.propertyDependsOn.filter(function (p) {
          var service = App.config.get('serviceByConfigTypeMap')[p.type];
          return service && !this.get('installedServices')[service.get('serviceName')];
        }, this).length;
      } else {
        return false;
      }
    }
    return true;
  },

  /**
   * remove config based on recommendations
   * @param config
   * @param configsCollection
   * @param parentProperties
   * @protected
   * @override
   */
  _removeConfigByRecommendation: function (config, configsCollection, parentProperties) {
    this._super(config, configsCollection, parentProperties);
    /**
     * need to update wizard info when removing configs for installed services;
     */
    var installedServices = this.get('installedServices'), wizardController = this.get('wizardController'),
      fileNamesToUpdate = wizardController ? wizardController.getDBProperty('fileNamesToUpdate') || [] : [],
      fileName = Em.get(config, 'filename'), serviceName = Em.get(config, 'serviceName');
    var modifiedFileNames = this.get('modifiedFileNames');
    if (modifiedFileNames && !modifiedFileNames.contains(fileName)) {
      modifiedFileNames.push(fileName);
    } else if (wizardController && installedServices[serviceName]) {
      if (!fileNamesToUpdate.contains(fileName)) {
        fileNamesToUpdate.push(fileName);
      }
    }
    if (wizardController) {
      wizardController.setDBProperty('fileNamesToUpdate', fileNamesToUpdate.uniq());
    }
  },
  /**
   * @method manageConfigurationGroup
   */
  manageConfigurationGroup: function () {
    App.router.get('manageConfigGroupsController').manageConfigurationGroups(this);
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
        fields : "?fields=hostComponents/RootServiceHostComponents/properties/server.jdbc.database_name,hostComponents/RootServiceHostComponents/properties/server.jdbc.url,hostComponents/RootServiceHostComponents/properties/server.jdbc.database"
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
    var ambariServerDBType = Em.getWithDefault(data.hostComponents, '0.RootServiceHostComponents.properties', {})['server.jdbc.database'],
        ambariServerHostName = Em.getWithDefault(data.hostComponents, '0.RootServiceHostComponents.host_name', false),
        hiveConnectionURL = Em.getWithDefault(App.config.findConfigProperty(this.get('stepConfigs'), 'javax.jdo.option.ConnectionURL', 'hive-site.xml') || {}, 'value', '');
    if (ambariServerHostName) {
      this.set('mySQLServerConflict', ambariServerDBType.contains('mysql') && hiveConnectionURL.contains(ambariServerHostName));
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
      return this.checkMySQLHost().done(function () {
        self.mySQLWarningHandler();
      });
    }
    else {
      this.moveNext();
    }
  },

  /**
   * Show warning popup about MySQL-DB issues (on post-submit)
   *
   * @returns {*}
   * @method mySQLWarningHandler
   */
  mySQLWarningHandler: function () {
    var self = this;
    if (this.get('mySQLServerConflict')) {
      // error popup before you can proceed
      return App.ModalPopup.show({
        header: Em.I18n.t('installer.step7.popup.mySQLWarning.header'),
        body:Em.I18n.t('installer.step7.popup.mySQLWarning.body'),
        secondary: Em.I18n.t('installer.step7.popup.mySQLWarning.button.gotostep5'),
        primary: Em.I18n.t('installer.step7.popup.mySQLWarning.button.dismiss'),
        encodeBody: false,
        onPrimary: function () {
          this._super();
          self.set('submitButtonClicked', false);
        },
        onSecondary: function () {
          var parent = this;
          return App.ModalPopup.show({
            header: Em.I18n.t('installer.step7.popup.mySQLWarning.confirmation.header'),
            body: Em.I18n.t('installer.step7.popup.mySQLWarning.confirmation.body'),
            onPrimary: function () {
              this.hide();
              parent.hide();
              // go back to step 5: assign masters and disable default navigation warning
              if ('installerController' === self.get('content.controllerName')) {
                App.router.get('installerController').gotoStep(5, true);
              }
              else {
                if ('addServiceController' === self.get('content.controllerName')) {
                  App.router.get('addServiceController').gotoStep(2, true);
                }
              }
            },
            onSecondary: function () {
              this._super();
              self.set('submitButtonClicked', false);
            }
          });
        }
      });
    }
    else {
      return this.moveNext();
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
      var databaseType = Em.getWithDefault(App.config.findConfigProperty(this.get('stepConfigs'), 'oozie_database', 'oozie-env.xml') || {}, 'value', '');
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
    App.router.nextBtnClickInProgress = true;
    App.router.send('next');
    this.set('submitButtonClicked', false);
  },

  /**
   * Click-handler on Next button
   * Disable "Submit"-button while server-side processes are running
   * @method submit
   */
  submit: function () {
    if (this.get('isSubmitDisabled') || App.router.nextBtnClickInProgress) {
      return false;
    }

    if (this.get('supportsPreInstallChecks')) {
      var preInstallChecksController = App.router.get('preInstallChecksController');
      if (preInstallChecksController.get('preInstallChecksWhereRun')) {
        return this.postSubmit();
      }
      return preInstallChecksController.notRunChecksWarnPopup(this.postSubmit.bind(this));
    }
    return this.postSubmit();
  },

  postSubmit: function () {
    var self = this;
    this.set('submitButtonClicked', true);
    this.serverSideValidation().done(function() {
      self.serverSideValidationCallback();
    })
      .fail(function (value) {
        if ("invalid_configs" == value) {
          self.set('submitButtonClicked', false);
          App.router.nextBtnClickInProgress = false;
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
        $('a[href="#' + service.serviceName + '"]').tab('show');
      }
    }
  }

});
