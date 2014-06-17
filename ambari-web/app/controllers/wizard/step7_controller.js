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
var numberUtils = require('utils/number_utils');
/**
 * By Step 7, we have the following information stored in App.db and set on this
 * controller by the router.
 *
 *   selectedServices: App.db.selectedServices (the services that the user selected in Step 4)
 *   masterComponentHosts: App.db.masterComponentHosts (master-components-to-hosts mapping the user selected in Step 5)
 *   slaveComponentHosts: App.db.slaveComponentHosts (slave-components-to-hosts mapping the user selected in Step 6)
 *
 */

App.WizardStep7Controller = Em.Controller.extend({

  name: 'wizardStep7Controller',

  /**
   * Contains all field properties that are viewed in this step
   * @type {object[]}
   */
  stepConfigs: [],

  selectedService: null,

  slaveHostToGroup: null,

  secureConfigs: require('data/secure_mapping'),

  /**
   * If miscConfigChange Modal is shown
   * @type {bool}
   */
  miscModalVisible: false,

  gangliaAvailableSpace: null,

  /**
   * @type {string}
   */
  gangliaMoutDir: '/',

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

  serviceConfigsData: require('data/service_configs'),

  /**
   * Are advanced configs loaded
   * @type {bool}
   */
  isAdvancedConfigLoaded: true,

  /**
   * Should Next-button be disabled
   * @type {bool}
   */
  isSubmitDisabled: function () {
    return (!this.stepConfigs.filterProperty('showConfig', true).everyProperty('errorCount', 0) || this.get("miscModalVisible"));
  }.property('stepConfigs.@each.errorCount', 'miscModalVisible'),

  /**
   * List of selected to install service names
   * @type {string[]}
   */
  selectedServiceNames: function () {
    return this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false).mapProperty('serviceName');
  }.property('content.services').cacheable(),

  /**
   * List of installed and selected to install service names
   * @type {string[]}
   */
  allSelectedServiceNames: function () {
    return this.get('content.services').filterProperty('isSelected').mapProperty('serviceName');
  }.property('content.services').cacheable(),

  /**
   * List of installed service names
   * Sqoop and HCatalog are excluded if not installer wizard running
   * @type {string[]}
   */
  installedServiceNames: function () {
    var serviceNames = this.get('content.services').filterProperty('isInstalled').mapProperty('serviceName');
    if (this.get('content.controllerName') !== 'installerController') {
      return serviceNames.without('SQOOP').without('HCATALOG');
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
   * Dropdown menu items in filter combobox
   * @type {Ember.Object[]}
   */
  filterColumns: function () {
    var result = [];
    for (var i = 1; i < 2; i++) {
      result.push(Em.Object.create({
        name: this.t('common.combobox.dropdown.' + i),
        selected: false
      }));
    }
    return result;
  }.property(),

  /**
   * Clear controller's properties:
   *  <ul>
   *    <li>serviceConfigTags</li>
   *    <li>stepConfigs</li>
   *    <li>filter</li>
   *  </ul>
   *  and desect all <code>filterColumns</code>
   * @method clearStep
   */
  clearStep: function () {
    this.get('serviceConfigTags').clear();
    this.get('stepConfigs').clear();
    this.set('filter', '');
    this.get('filterColumns').setEach('selected', false);
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
        if (sites.indexOf(site) > -1) {
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
      siteToTagMap = this._createSiteToTagMap(data.Clusters.desired_configs, params.serviceConfigsDef.sites),
      selectedConfigGroup;

    this.set('loadedClusterSiteToTagMap', siteToTagMap);

    //parse loaded config groups
    if (App.get('supports.hostOverrides')) {
      var configGroups = [];
      if (data.config_groups.length) {
        data.config_groups.forEach(function (item) {
          item = item.ConfigGroup;
          if (item.tag === serviceName) {
            var groupHosts = item.hosts.mapProperty('host_name');
            var newConfigGroup = App.ConfigGroup.create({
              id: item.id,
              name: item.group_name,
              description: item.description,
              isDefault: false,
              parentConfigGroup: null,
              service: App.Service.find().findProperty('serviceName', item.tag),
              hosts: groupHosts,
              configSiteTags: []
            });
            groupHosts.forEach(function (host) {
              defaultConfigGroupHosts = defaultConfigGroupHosts.without(host);
            }, this);
            item.desired_configs.forEach(function (config) {
              newConfigGroup.configSiteTags.push(App.ConfigSiteTag.create({
                site: config.type,
                tag: config.tag
              }));
            }, this);
            configGroups.push(newConfigGroup);
          }
        }, this);
      }
    }
    var defaultConfigGroup = App.ConfigGroup.create({
      name: App.Service.DisplayNames[serviceName] + " Default",
      description: "Default cluster level " + serviceName + " configuration",
      isDefault: true,
      hosts: defaultConfigGroupHosts,
      parentConfigGroup: null,
      service: Em.Object.create({
        id: serviceName
      }),
      serviceName: serviceName,
      configSiteTags: []
    });
    if (!selectedConfigGroup) {
      selectedConfigGroup = defaultConfigGroup;
    }
    configGroups = configGroups.sortProperty('name');
    configGroups.unshift(defaultConfigGroup);
    if (App.get('supports.hostOverrides')) {
      service.set('configGroups', configGroups);
      var loadedGroupToOverrideSiteToTagMap = {};
      if (App.get('supports.hostOverrides')) {
        var configGroupsWithOverrides = selectedConfigGroup.get('isDefault') ? service.get('configGroups') : [selectedConfigGroup];
        configGroupsWithOverrides.forEach(function (item) {
          var groupName = item.get('name');
          loadedGroupToOverrideSiteToTagMap[groupName] = {};
          item.get('configSiteTags').forEach(function (siteTag) {
            var site = siteTag.get('site');
            loadedGroupToOverrideSiteToTagMap[groupName][site] = siteTag.get('tag');
          }, this);
        }, this);
      }
      App.config.loadServiceConfigGroupOverrides(service.get('configs'), loadedGroupToOverrideSiteToTagMap, service.get('configGroups'));
      var serviceConfig = App.config.createServiceConfig(serviceName);
      if (serviceConfig.get('serviceName') === 'HDFS') {
        App.config.OnNnHAHideSnn(serviceConfig);
      }
      service.set('selectedConfigGroup', selectedConfigGroup);
      this.loadComponentConfigs(service.get('configs'), serviceConfig, service);
    }
    service.set('configs', serviceConfig.get('configs'));
  },

  /**
   * Get object with recommended default values for config properties
   * Format:
   *  <code>
   *    {
   *      configName1: configValue1,
   *      configName2: configValue2
   *      ...
   *    }
   *  </code>
   * @param {string} serviceName
   * @returns {object}
   * @method _getRecommendedDefaultsForComponent
   */
  _getRecommendedDefaultsForComponent: function (serviceName) {
    var s = this.get('serviceConfigsData').findProperty('serviceName', serviceName),
      recommendedDefaults = {},
      localDB = this.getInfoForDefaults();
    if (s.defaultsProviders) {
      s.defaultsProviders.forEach(function (defaultsProvider) {
        var d = defaultsProvider.getDefaults(localDB);
        for (var name in d) {
          if (d.hasOwnProperty(name)) {
            recommendedDefaults[name] = d[name];
          }
        }
      });
    }
    return recommendedDefaults;
  },

  /**
   * Get info about hosts and host components to configDefaultsProviders
   * Work specifically in Add Service wizard
   * @slaveComponentHosts - contains slaves and clients as well
   * @returns {{masterComponentHosts: Array, slaveComponentHosts: Array, hosts: {}}}
   */
  getInfoForDefaults: function() {
    var slaveComponentHosts = [];
    var hosts = this.get('content.hosts');
    var slaveHostMap = {};

    //get clients and slaves from stack
    App.StackServiceComponent.find().forEach(function (component) {
      if (component.get('isClient') || component.get('isSlave')) {
        slaveHostMap[component.get('componentName')] = [];
      }
    });

    //assign hosts of every component
    for (var hostName in hosts) {
      hosts[hostName].hostComponents.forEach(function (componentName) {
        if (slaveHostMap[componentName]) {
          slaveHostMap[componentName].push({hostName: hostName});
        }
      });
    }

    //push slaves and clients into @slaveComponentHosts
    for (var componentName in slaveHostMap) {
      if(slaveHostMap[componentName].length > 0) {
        slaveComponentHosts.push({
          componentName: componentName,
          hosts: slaveHostMap[componentName]
        })
      }
    }

    var masterComponentHosts = App.HostComponent.find().filterProperty('isMaster', true).map(function(item) {
      return {
        component: item.get('componentName'),
        serviceId: item.get('service.serviceName'),
        host: item.get('hostName')
      }
    });

    return {
      masterComponentHosts: masterComponentHosts,
      slaveComponentHosts: slaveComponentHosts,
      hosts: hosts
    };
  },

  /**
   * By default <code>value</code>-property is string "true|false".
   * Should update it to boolean type
   * Also affects <code>defaultValue</code>
   * @param {Ember.Object} serviceConfigProperty
   * @returns {Ember.Object} Updated config-object
   * @method _updateValueForCheckBoxConfig
   */
  _updateValueForCheckBoxConfig: function (serviceConfigProperty) {
    var v = serviceConfigProperty.get('value');
    switch (serviceConfigProperty.get('value')) {
      case 'true':
        v = true;
        break;
      case 'false':
        v = false;
        break;
    }
    serviceConfigProperty.setProperties({value: v, defaultValue: v});
    return serviceConfigProperty;
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
    if (App.get('isAdmin')) {
      if (defaultGroupSelected && !this.get('isHostsConfigsPage')) {
        serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isReconfigurable'));
      }
      else {
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
      if (App.get('supports.hostOverrides') && defaultGroupSelected) {
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
    }, this);
    return serviceConfigProperty;
  },

  /**
   * Set <code>serviceValidator</code>-property to <code>serviceConfigProperty</code> if config's serviceName is equal
   * to component's serviceName
   * othervise set <code>isVisible</code>-property to <code>false</code>
   * @param {Ember.Object} serviceConfigProperty
   * @param {Ember.Object} component
   * @param {object} serviceConfigsData
   * @returns {Ember.Object} updated config-object
   * @mrthod _updateValidatorsForConfig
   */
  _updateValidatorsForConfig: function (serviceConfigProperty, component, serviceConfigsData) {
    if (serviceConfigProperty.get('serviceName') === component.get('serviceName')) {
      if (serviceConfigsData.configsValidator) {
        var validators = serviceConfigsData.configsValidator.get('configValidators');
        for (var validatorName in validators) {
          if (validators.hasOwnProperty(validatorName)) {
            if (serviceConfigProperty.get('name') == validatorName) {
              serviceConfigProperty.set('serviceValidator', serviceConfigsData.configsValidator);
            }
          }
        }
      }
    }
    else {
      serviceConfigProperty.set('isVisible', false);
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
    var s = this.get('serviceConfigsData').findProperty('serviceName', component.get('serviceName')),
      defaultGroupSelected = component.get('selectedConfigGroup.isDefault');

    if (s.configsValidator) {
      var recommendedDefaults = this._getRecommendedDefaultsForComponent(component.get('serviceName'));
      s.configsValidator.set('recommendedDefaults', recommendedDefaults);
    }

    configs.forEach(function (serviceConfigProperty) {
      if (!serviceConfigProperty) return;

      if (Em.isNone(serviceConfigProperty.get('isOverridable'))) {
        serviceConfigProperty.set('isOverridable', true);
      }
      if (serviceConfigProperty.get('displayType') === 'checkbox') {
        this._updateValueForCheckBoxConfig(serviceConfigProperty);
      }
      this._updateValidatorsForConfig(serviceConfigProperty, component, s);
      this._updateOverridesForConfig(serviceConfigProperty, component);
      this._updateIsEditableFlagForConfig(serviceConfigProperty, defaultGroupSelected);

      componentConfig.get('configs').pushObject(serviceConfigProperty);
      serviceConfigProperty.validate();

    }, this);

    var overrideToAdd = this.get('overrideToAdd');
    if (overrideToAdd) {
      overrideToAdd = componentConfig.get('configs').findProperty('name', overrideToAdd.name);
      if (overrideToAdd) {
        this.addOverrideProperty(overrideToAdd);
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
      default:
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
    var dependentConfigs, gangliaServerHost;
    dependentConfigs = ['nimbus.childopts', 'supervisor.childopts', 'worker.childopts'];
    // if Ganglia selected or installed, set ganglia host to configs
    if (this.get('installedServiceNames').contains('STORM') && this.get('installedServiceNames').contains('GANGLIA')) return;
    if (this.get('allSelectedServiceNames').contains('GANGLIA') || this.get('installedServiceNames').contains('GANGLIA')) {
      gangliaServerHost = this.get('wizardController').getDBProperty('masterComponentHosts').findProperty('component', 'GANGLIA_SERVER').hostName;
      dependentConfigs.forEach(function (configName) {
        var config = configs.findProperty('name', configName);
        var replaceStr = config.value.match(/.jar=host[^,]+/)[0];
        var replaceWith = replaceStr.slice(0, replaceStr.lastIndexOf('=') - replaceStr.length + 1) + gangliaServerHost;
        config.value = config.defaultValue = config.value.replace(replaceStr, replaceWith);
        config.forceUpdate = true;
      }, this);
    }
  },

  checkConfigLoad: function() {
    if (this.get('wizardController.name') === 'addServiceController') {
      this.set('isAdvancedConfigLoaded', false);
    }
  },

  /**
   * On load function
   * @method loadStep
   */
  loadStep: function () {
    console.log("TRACE: Loading step7: Configure Services");
    if (!this.get('isAdvancedConfigLoaded')) {
      this.getConfigTags();
      return;
    }
    this.clearStep();
    //STEP 1: Load advanced configs
    var advancedConfigs = this.get('content.advancedServiceConfig');
    //STEP 2: Load on-site configs by service from local DB
    var storedConfigs = this.get('content.serviceConfigProperties');
    //STEP 3: Merge pre-defined configs with loaded on-site configs
    var configs = App.config.mergePreDefinedWithStored(
      storedConfigs,
      advancedConfigs,
      this.get('selectedServiceNames').concat(this.get('installedServiceNames'))
    );
    //STEP 4: Add advanced configs
    App.config.addAdvancedConfigs(configs, advancedConfigs);
    //STEP 5: Add custom configs
    App.config.addCustomConfigs(configs);
    //put properties from capacity-scheduler.xml into one config with textarea view
    if (this.get('allSelectedServiceNames').contains('YARN') && !App.get('supports.capacitySchedulerUi')) {
      configs = App.config.fileConfigsIntoTextarea(configs, 'capacity-scheduler.xml');
    }

    this.set('groupsToDelete', this.get('wizardController').getDBProperty('groupsToDelete') || []);

    if (this.get('wizardController.name') === 'addServiceController') {
      this.setInstalledServiceConfigs(this.get('serviceConfigTags'), configs);
    }
    if (this.get('allSelectedServiceNames').contains('STORM') || this.get('installedServiceNames').contains('STORM')) {
      this.resolveServiceDependencyConfigs('STORM', configs);
    }
    //STEP 6: Distribute configs by service and wrap each one in App.ServiceConfigProperty (configs -> serviceConfigs)
    this.setStepConfigs(configs, storedConfigs);

    this.checkHostOverrideInstaller();
    this.activateSpecialConfigs();
    this.selectProperService();
    if (this.get('content.skipConfigStep')) {
      App.router.send('next');
    }
  },

  /**
   * If <code>App.supports.hostOverridesInstaller</code> is enabled should load config groups
   * and (if some services are already installed) load config groups for installed services
   * @method checkHostOverrideInstaller
   */
  checkHostOverrideInstaller: function () {
    if (App.get('supports.hostOverridesInstaller')) {
      this.loadConfigGroups(this.get('content.configGroups'));
      if (this.get('installedServiceNames').length > 0) {
        this.loadInstalledServicesConfigGroups(this.get('installedServiceNames'));
      }
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
      hosts: this.get('wizardController').getDBProperty('hosts'),
      masterComponentHosts: this.get('wizardController').getDBProperty('masterComponentHosts'),
      slaveComponentHosts: this.get('wizardController').getDBProperty('slaveComponentHosts')
    };
    var serviceConfigs = App.config.renderConfigs(configs, storedConfigs, this.get('allSelectedServiceNames'), this.get('installedServiceNames'), localDB);
    if (this.get('wizardController.name') === 'addServiceController') {
      serviceConfigs.setEach('showConfig', true);
      serviceConfigs.setEach('selected', false);
      this.get('selectedServiceNames').forEach(function (serviceName) {
        if (!serviceConfigs.findProperty('serviceName', serviceName)) return;
        serviceConfigs.findProperty('serviceName', serviceName).set('selected', true);
      });

      // Remove SNameNode if HA is enabled
      if (App.get('isHaEnabled')) {
        var c = serviceConfigs.findProperty('serviceName', 'HDFS').configs;
        var removedConfigs = c.filterProperty('category', 'SNameNode');
        removedConfigs.map(function (config) {
          c = c.without(config);
        });
        serviceConfigs.findProperty('serviceName', 'HDFS').configs = c;
      }
    }

    this.set('stepConfigs', serviceConfigs);
  },

  /**
   * Select first addable service for <code>addServiceWizard</code>
   * Select first service at all in other cases
   * @method selectProperService
   */
  selectProperService: function () {
    if (this.get('wizardController.name') === 'addServiceController') {
      this.set('selectedService', this.get('stepConfigs').filterProperty('selected', true).get('firstObject'));
    }
    else {
      this.set('selectedService', this.get('stepConfigs').filterProperty('showConfig', true).objectAt(0));
    }
  },

  /**
   * Load config tags
   * @return {$.ajax|null}
   * @method getConfigTags
   */
  getConfigTags: function () {
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
    this.get('serviceConfigsData').filter(function (service) {
      if (this.get('installedServiceNames').contains(service.serviceName)) {
        installedServiceSites = installedServiceSites.concat(service.sites);
      }
    }, this);
    installedServiceSites = installedServiceSites.uniq();
    var serviceConfigTags = [];
    for (var site in data.Clusters.desired_configs) {
      if (data.Clusters.desired_configs.hasOwnProperty(site)) {
        if (installedServiceSites.contains(site)) {
          serviceConfigTags.push({
            siteName: site,
            tagName: data.Clusters.desired_configs[site].tag,
            newTagName: null
          });
        }
      }
    }
    this.set('serviceConfigTags', serviceConfigTags);
    this.set('isAdvancedConfigLoaded', true);
  },

  /**
   * set configs actual values from server
   * @param serviceConfigTags
   * @param configs
   * @method setInstalledServiceConfigs
   */
  setInstalledServiceConfigs: function (serviceConfigTags, configs) {
    var configsMap = {};
    App.router.get('configurationController').getConfigsByTags(serviceConfigTags).forEach(function (configSite) {
      $.extend(configsMap, configSite.properties);
    });
    configs.forEach(function (_config) {
      if (configsMap[_config.name] !== undefined) {
        // prevent overriding already edited properties
        if (_config.defaultValue != configsMap[_config.name])
          _config.value = configsMap[_config.name];
        _config.defaultValue = configsMap[_config.name];
        App.config.handleSpecialProperties(_config);
      }
    })
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
    if (App.supports.hostOverridesInstaller && this.get('selectedService') && (this.get('selectedService.serviceName') !== 'MISC')) {
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
      var serviceRawGroups = serviceConfigGroups.filterProperty('service.id', service.serviceName);
      if (!serviceRawGroups.length) {
        service.set('configGroups', [
          App.ConfigGroup.create({
            name: App.Service.DisplayNames[service.serviceName] + " Default",
            description: "Default cluster level " + service.serviceName + " configuration",
            isDefault: true,
            hosts: Em.copy(hosts),
            service: Em.Object.create({
              id: service.serviceName
            }),
            serviceName: service.serviceName
          })
        ]);
      }
      else {
        var defaultGroup = App.ConfigGroup.create(serviceRawGroups.findProperty('isDefault'));
        var serviceGroups = service.get('configGroups');
        serviceRawGroups.filterProperty('isDefault', false).forEach(function (configGroup) {
          var readyGroup = App.ConfigGroup.create(configGroup);
          var wrappedProperties = [];
          readyGroup.get('properties').forEach(function (property) {
            wrappedProperties.pushObject(App.ServiceConfigProperty.create(property));
          });
          wrappedProperties.setEach('group', readyGroup);
          readyGroup.set('properties', wrappedProperties);
          readyGroup.set('parentConfigGroup', defaultGroup);
          serviceGroups.pushObject(readyGroup);
        });
        defaultGroup.set('childConfigGroups', serviceGroups);
        serviceGroups.pushObject(defaultGroup);
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
      configOverrides.push(this.addOverrideProperty(config));
      this.set('overrideToAdd', null);
    }
    configOverrides.setEach('isEditable', !selectedGroup.get('isDefault'));
    configOverrides.setEach('parentSCP', config);
    config.set('overrides', configOverrides);
    return config;
  },

  /**
   * create overriden property and push it into Config group
   * @param {App.ServiceConfigProperty} serviceConfigProperty
   * @return {App.ServiceConfigProperty}
   * @method addOverrideProperty
   */
  addOverrideProperty: function (serviceConfigProperty) {
    var overrides = serviceConfigProperty.get('overrides') || [];
    var newSCP = App.ServiceConfigProperty.create(serviceConfigProperty);
    var group = this.get('selectedService.configGroups').findProperty('name', this.get('selectedConfigGroup.name'));
    newSCP.set('group', group);
    newSCP.set('value', '');
    newSCP.set('isOriginalSCP', false); // indicated this is overridden value,
    newSCP.set('parentSCP', serviceConfigProperty);
    newSCP.set('isEditable', true);
    group.get('properties').pushObject(newSCP);
    overrides.pushObject(newSCP);
    return newSCP;
  },

  /**
   * @method manageConfigurationGroup
   */
  manageConfigurationGroup: function () {
    App.router.get('mainServiceInfoConfigsController').manageConfigurationGroups(this);
  },

  /**
   * Make some configs visible depending on active services
   * @method activateSpecialConfigs
   */
  activateSpecialConfigs: function () {
    var miscConfigs = this.get('stepConfigs').findProperty('serviceName', 'MISC').configs;
    miscConfigs = App.config.miscConfigVisibleProperty(miscConfigs, this.get('selectedServiceNames'));
  },

  /**
    * Check whether hive New MySQL database is on the same host as Ambari server MySQL server
    * @return {$.ajax|null}
    * @method checkMySQLHost
    */
  checkMySQLHost: function () {
    // get ambari database type and hostname
    return App.ajax.send({
      name: 'config.ambari.database.info',
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
    var hiveDBHostname = this.get('stepConfigs').findProperty('serviceName', 'HIVE').configs.findProperty('name', 'hivemetastore_host').value;
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
  resolveHiveMysqlDatabase: function() {
    var hiveService = this.get('content.services').findProperty('serviceName', 'HIVE');
    if (!hiveService || !hiveService.get('isSelected') || hiveService.get('isInstalled')) {
      this.moveNext();
      return;
    }
    var hiveDBType = this.get('stepConfigs').findProperty('serviceName', 'HIVE').configs.findProperty('name', 'hive_database').value;
    if (hiveDBType == 'New MySQL Database') {
      var self= this;
      this.checkMySQLHost().done(function () {
        if (self.get('mySQLServerConflict')) {
          // error popup before you can proceed
          return App.ModalPopup.show({
            header: Em.I18n.t('installer.step7.popup.mySQLWarning.header'),
            bodyClass: Ember.View.extend({
              template: Ember.Handlebars.compile(Em.I18n.t('installer.step7.popup.mySQLWarning.body'))
            }),
            secondary: Em.I18n.t('installer.step7.popup.mySQLWarning.button.gotostep5'),
            primary: Em.I18n.t('installer.step7.popup.mySQLWarning.button.dismiss'),
            onSecondary: function (){
              var parent = this;
              return App.ModalPopup.show({
                header: Em.I18n.t('installer.step7.popup.mySQLWarning.confirmation.header'),
                bodyClass: Ember.View.extend({
                  template: Ember.Handlebars.compile( Em.I18n.t('installer.step7.popup.mySQLWarning.confirmation.body'))
                }),
                onPrimary: function (){
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

  checkDatabaseConnectionTest: function() {
    var deferred = $.Deferred();
    if (!App.supports.databaseConnection) {
      deferred.resolve();
      return deferred;
    }
    var configMap = [
      {
        serviceName: 'OOZIE',
        ignored: Em.I18n.t('installer.step7.oozie.database.new')
      },
      {
        serviceName: 'HIVE',
        ignored: Em.I18n.t('installer.step7.hive.database.new')
      }
    ];
    configMap.forEach(function(config) {
      var isConnectionNotTested = false;
      var service = this.get('content.services').findProperty('serviceName', config.serviceName);
      if (service && service.get('isSelected') && !service.get('isInstalled')) {
        var serviceConfigs = this.get('stepConfigs').findProperty('serviceName', config.serviceName).configs;
        var serviceDatabase = serviceConfigs.findProperty('name', config.serviceName.toLowerCase() + '_database').get('value');
        if (serviceDatabase !== config.ignored) {
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
      var displayedServiceNames = ignoredServices.mapProperty('serviceName').map(function(serviceName) {
        return this.get('content.services').findProperty('serviceName', serviceName).get('displayName');
      }, this);
      this.showDatabaseConnectionWarningPopup(displayedServiceNames, deferred);
    }
    else {
      deferred.resolve();
    }
    return deferred;
  },

  showDatabaseConnectionWarningPopup: function(serviceNames, deferred) {
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step7.popup.database.connection.header'),
      body: Em.I18n.t('installer.step7.popup.database.connection.body').format(serviceNames.join(', ')),
      secondary: Em.I18n.t('common.cancel'),
      primary: Em.I18n.t('common.proceedAnyway'),
      onPrimary: function() {
        deferred.resolve();
        this._super();
      },
      onSecondary: function() {
        deferred.reject();
        this._super();
      }
    })
  },
  /**
   * Proceed to the next step
   **/
  moveNext: function() {
    App.router.send('next');
  },

  /**
   * Click-handler on Next button
   * @method submit
   */
  submit: function () {
    var _this = this;
    if (!this.get('isSubmitDisabled')) {
      this.checkDatabaseConnectionTest().done(function() {
        _this.resolveHiveMysqlDatabase();
      });
    }
  }

});
