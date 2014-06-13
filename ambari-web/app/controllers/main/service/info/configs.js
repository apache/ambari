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
require('controllers/wizard/slave_component_groups_controller');
var batchUtils = require('utils/batch_scheduled_requests');
var lazyLoading = require('utils/lazy_loading');

App.MainServiceInfoConfigsController = Em.Controller.extend({
  name: 'mainServiceInfoConfigsController',
  isHostsConfigsPage: false,
  forceTransition: false,
  dataIsLoaded: false,
  stepConfigs: [], //contains all field properties that are viewed in this service
  selectedService: null,
  serviceConfigTags: null,
  selectedConfigGroup: null,
  configGroups: [],
  globalConfigs: [],
  uiConfigs: [],
  customConfig: [],
  serviceConfigsData: require('data/service_configs'),
  isApplyingChanges: false,
  // contain Service Config Property, when user proceed from Select Config Group dialog
  overrideToAdd: null,
  serviceConfigs: function () {
    return App.config.get('preDefinedServiceConfigs');
  }.property('App.config.preDefinedServiceConfigs'),
  customConfigs: function () {
    return App.config.get('preDefinedCustomConfigs');
  }.property('App.config.preDefinedCustomConfigs'),
  configMapping: function () {
    return App.config.get('configMapping');
  }.property('App.config.configMapping'),
  configs: function () {
    return  App.config.get('preDefinedGlobalProperties');
  }.property('App.config.preDefinedGlobalProperties'),

  secureConfigs: function () {
    if (App.get('isHadoop2Stack')) {
      return require('data/HDP2/secure_mapping');
    } else {
      return require('data/secure_mapping');
    }
  }.property('App.isHadoop2Stack'),

  /**
   * Map, which contains relation between group and site
   * to upload overriden properties
   */
  loadedGroupToOverrideSiteToTagMap: {},
  /**
   * During page load time the cluster level site to tag
   * mapping is stored here.
   *
   * Example:
   * {
   *  'global': 'version1',
   *  'hdfs-site': 'version1',
   *  'core-site': 'version1'
   * }
   */
  loadedClusterSiteToTagMap: {},
  /**
   * Holds the actual base service-config server data uploaded.
   * This is used by the host-override mechanism to update host
   * specific values.
   */
  savedSiteNameToServerServiceConfigDataMap: {},

  isSubmitDisabled: function () {
    return (!(this.stepConfigs.everyProperty('errorCount', 0)) || this.get('isApplyingChanges'));
  }.property('stepConfigs.@each.errorCount', 'isApplyingChanges'),

  slaveComponentGroups: null,

  /**
   * Filter text will be located here
   */
  filter: '',

  /**
   * Dropdown menu items in filter combobox
   */
  filterColumns: function () {
    var result = [];
    for (var i = 1; i < 2; i++) {
      result.push(Ember.Object.create({
        name: this.t('common.combobox.dropdown.' + i),
        selected: false
      }));
    }
    return result;
  }.property(),

  /**
   * clear and set properties to default value
   */
  clearStep: function () {
    this.set('isInit', true);
    this.set('hash', null);
    this.set('forceTransition', false);
    this.set('dataIsLoaded', false);
    this.set('filter', '');
    this.get('filterColumns').setEach('selected', false);
    this.get('stepConfigs').clear();
    this.get('globalConfigs').clear();
    this.get('uiConfigs').clear();
    this.get('customConfig').clear();
    this.set('loadedGroupToOverrideSiteToTagMap', {});
    this.set('savedSiteNameToServerServiceConfigDataMap', {});
    if (this.get('serviceConfigTags')) {
      this.set('serviceConfigTags', null);
    }
  },

  serviceConfigProperties: function () {
    return App.db.getServiceConfigProperties();
  }.property('content'),

  /**
   * "Finger-print" of the <code>stepConfigs</code>. Filled after first configGroup selecting
   * Used to determine if some changes were made (when user navigates away from this page)
   * {String}
   */
  hash: null,
  /**
   * Is this initial config group changing
   * {Boolean}
   */
  isInit: true,

  restartHosts: Em.A(),

  defaultsInfo: null,
  /**
   * On load function
   */
  loadStep: function () {
    console.log("TRACE: Loading configure for service");
    this.clearStep();
    this.loadServiceConfigs();
  },

  getHash: function() {
    var hash = {};
    this.get('stepConfigs')[0].configs.forEach(function(config) {
      hash[config.get('name')] = {value: config.get('value'), overrides: []};
      if (!config.get('overrides')) return;
      if (!config.get('overrides.length')) return;

      config.get('overrides').forEach(function(override) {
        hash[config.get('name')].overrides.push(override.get('value'));
      });
    });
    return JSON.stringify(hash);
  },

  /**
   * Loads the actual configuration of all host components.
   * This helps in determining which services need a restart, and also
   * in showing which properties are actually applied or not.
   * This method also compares the actual_configs with the desired_configs
   * and builds a diff structure.
   *
   * Internally it calculates an array of host-components which need restart.
   * Example:
   * [
   *  {
   *    componentName: 'DATANODE',
   *    serviceName: 'HDFS',
   *    host: 'host.name',
   *    type: 'core-site',
   *    desiredConfigTags: {tag:'version1'},
   *    actualConfigTags: {tag:'version4'. host_override:'version2'}
   *  },
   *  ...
   * ]
   *
   * From there it return the following restart-data for this service.
   * It represents the hosts, whose components need restart, and the
   * properties which require restart.
   *
   * {
   *  hostAndHostComponents: {
   *   'hostname1': {
   *     'DATANODE': {
   *       'property1': 'value1',
   *       'property2': 'value2'
   *     },
   *     'TASKTRACKER': {
   *       'prop1': 'val1'
   *     }
   *    },
   *    'hostname6': {
   *     'ZOOKEEPER': {
   *       'property1': 'value3'
   *     }
   *    }
   *  },
   *  propertyToHostAndComponent: {
   *    'property1': {
   *      'hostname1': ['DATANODE'],
   *      'hostname6': ['ZOOKEEPER']
   *    },
   *    'property2': {
   *      'hostname1': ['DATANODE']
   *    },
   *    'prop1': {
   *      'hostname1': ['TASKTRACKER']
   *    }
   *  }
   * }
   */
  loadActualConfigsAndCalculateRestarts: function () {
    var restartData = {
      hostAndHostComponents: {},
      propertyToHostAndComponent: {}
    };
    console.log("loadActualConfigsAndCalculateRestarts(): Restart data = ", restartData);
    return restartData;
  },

  /**
   * Loads service configurations
   */
  loadServiceConfigs: function () {
    App.ajax.send({
      name: 'config.tags_and_groups',
      sender: this,
      data: {
        serviceName: this.get('content.serviceName'),
        serviceConfigsDef: this.get('serviceConfigs').findProperty('serviceName', this.get('content.serviceName')),
        urlParams: ',hosts'
      },
      success: 'loadServiceTagsSuccess'
    });
  },

  loadServiceTagsSuccess: function (data, opt, params) {
    var serviceConfigsDef = params.serviceConfigsDef;
    var serviceName = this.get('content.serviceName');
    console.debug("loadServiceConfigs(): data=", data);
    // Create default configuration group
    var defaultConfigGroupHosts = data.hosts.mapProperty('Hosts.host_name');
    var selectedConfigGroup;
    var siteToTagMap = {};
    serviceConfigsDef.sites.forEach(function(siteName){
      if(data.Clusters.desired_configs[siteName]){
        siteToTagMap[siteName] = data.Clusters.desired_configs[siteName].tag;
      } else {
        siteToTagMap[siteName] = 'version1';
      }
    }, this);
    this.loadedClusterSiteToTagMap = siteToTagMap;
    //parse loaded config groups
    if (App.supports.hostOverrides) {
      var configGroups = [];
      if (data.config_groups.length) {
        data.config_groups.forEach(function (item) {
          item = item.ConfigGroup;
          if (item.tag === this.get('content.serviceName')) {
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
            // select default selected group for hosts page
            if (!selectedConfigGroup && this.get('isHostsConfigsPage') && newConfigGroup.get('hosts').contains(this.get('host.hostName')) && this.get('content.serviceName') === item.tag) {
              selectedConfigGroup = newConfigGroup;
            }
            configGroups.push(newConfigGroup);
          }
        }, this);
      }
      this.set('configGroups', []);
      lazyLoading.run({
        initSize: 20,
        chunkSize: 50,
        delay: 50,
        destination: this.get('configGroups'),
        source: configGroups,
        context: this
      });
    }
    var defaultConfigGroup = App.ConfigGroup.create({
      name: App.Service.DisplayNames[serviceName] + " Default",
      description: "Default cluster level " + serviceName + " configuration",
      isDefault: true,
      hosts: [],
      parentConfigGroup: null,
      service: this.get('content'),
      serviceName: serviceName,
      configSiteTags: []
    });
    if (!selectedConfigGroup) {
      selectedConfigGroup = defaultConfigGroup;
      lazyLoading.run({
        initSize: 20,
        chunkSize: 50,
        delay: 50,
        destination: selectedConfigGroup.get('hosts'),
        source: defaultConfigGroupHosts,
        context: Em.Object.create()
      });
    }

    this.get('configGroups').sort(function(configGroupA, configGroupB){
      return (configGroupA.name > configGroupB.name);
    });
    this.get('configGroups').unshift(defaultConfigGroup);
    this.set('selectedConfigGroup', selectedConfigGroup);
  },

  onConfigGroupChange: function () {
    this.get('stepConfigs').clear();
    var selectedConfigGroup = this.get('selectedConfigGroup');
    var serviceName = this.get('content.serviceName');
    //STEP 1: handle tags from JSON data for host overrides
    this.loadedGroupToOverrideSiteToTagMap = {};
    if (App.supports.hostOverrides) {
      var configGroupsWithOverrides = selectedConfigGroup.get('isDefault') && !this.get('isHostsConfigsPage') ? this.get('configGroups') : [selectedConfigGroup];
      configGroupsWithOverrides.forEach(function (item) {
        var groupName = item.get('name');
        this.loadedGroupToOverrideSiteToTagMap[groupName] = {};
        item.get('configSiteTags').forEach(function (siteTag) {
          var site = siteTag.get('site');
          this.loadedGroupToOverrideSiteToTagMap[groupName][site] = siteTag.get('tag');
        }, this);
      }, this);
    }
    //STEP 2: Create an array of objects defining tag names to be polled and new tag names to be set after submit
    this.setServiceConfigTags(this.loadedClusterSiteToTagMap);
    //STEP 3: Load advanced configs from server
    var advancedConfigs = [];
    App.config.loadAdvancedConfig(serviceName, function (properties) {
      advancedConfigs.pushObjects(properties);
    }, true);
    //STEP 4: Load on-site config by service from server
    var configGroups = App.router.get('configurationController').getConfigsByTags(this.get('serviceConfigTags'));
    //STEP 5: Merge global and on-site configs with pre-defined
    var configSet = App.config.mergePreDefinedWithLoaded(configGroups, advancedConfigs, this.get('serviceConfigTags'), serviceName);
    configSet = App.config.syncOrderWithPredefined(configSet);
    //var serviceConfigs = this.getSitesConfigProperties(advancedConfigs);
    var configs = configSet.configs;
    //put global configs into globalConfigs to save them separately
    this.set('globalConfigs', configSet.globalConfigs);

    //STEP 6: add advanced configs
    //App.config.addAdvancedConfigs(configs, advancedConfigs, serviceName);
    //STEP 7: add custom configs
    App.config.addCustomConfigs(configs);
    //put properties from capacity-scheduler.xml into one config with textarea view
    if (this.get('content.serviceName') === 'YARN' && !App.supports.capacitySchedulerUi) {
      configs = App.config.fileConfigsIntoTextarea(configs, 'capacity-scheduler.xml');
    }
    //STEP 8: add configs as names of host components
    this.addHostNamesToGlobalConfig();

    var allConfigs = this.get('globalConfigs').concat(configs);
    //STEP 9: Load and add overriden configs of group
    App.config.loadServiceConfigGroupOverrides(allConfigs, this.loadedGroupToOverrideSiteToTagMap, this.get('configGroups'));
    var restartData = this.loadActualConfigsAndCalculateRestarts();
    //STEP 10: creation of serviceConfig object which contains configs for current service
    var serviceConfig = App.config.createServiceConfig(serviceName);
    //STEP11: Make SecondaryNameNode invisible on enabling namenode HA
    if (serviceConfig.get('serviceName') === 'HDFS') {
      App.config.OnNnHAHideSnn(serviceConfig);
    }
    this.checkForRestart(serviceConfig, restartData);

    if (serviceName || serviceConfig.serviceName === 'MISC') {
      //STEP 11: render configs and wrap each in ServiceConfigProperty object
      var self =this;
      // set recommended Defaults first then load the configs (including set validator)
      this.setRecommendedDefaults(advancedConfigs).done(function () {
        self.loadConfigs(allConfigs, serviceConfig, restartData);
        self.checkOverrideProperty(serviceConfig);
        self.get('stepConfigs').pushObject(serviceConfig);
        self.set('selectedService', self.get('stepConfigs').objectAt(0));
        self.checkForSecureConfig(self.get('selectedService'));
        self.set('dataIsLoaded', true);
        self.set('hash', self.getHash());
        self.set('isInit', false);
      });
    } else {
      this.set('selectedService', this.get('stepConfigs').objectAt(0));
      this.checkForSecureConfig(this.get('selectedService'));
      this.set('dataIsLoaded', true);

      this.set('hash', this.getHash());
      this.set('isInit', false);
    }

  }.observes('selectedConfigGroup'),

  /**
   * Changes format from Object to Array
   *
   * {
   *  'core-site': 'version1',
   *  'hdfs-site': 'version1',
   *  'global': 'version2',
   *  ...
   * }
   *
   * to
   *
   * [
   *  {
   *    siteName: 'core-site',
   *    tagName: 'version1',
   *    newTageName: null
   *  },
   *  ...
   * ]
   *
   * set tagnames for configuration of the *-site.xml
   */
  setServiceConfigTags: function (desiredConfigsSiteTags) {
    console.debug("setServiceConfigTags(): Trying to set ", desiredConfigsSiteTags);
    var newServiceConfigTags = [];
    for (var index in desiredConfigsSiteTags) {
      newServiceConfigTags.pushObject({
        siteName: index,
        tagName: desiredConfigsSiteTags[index],
        newTagName: null
      }, this);
    }
    console.debug("setServiceConfigTags(): Setting 'serviceConfigTags' to ", newServiceConfigTags);
    this.set('serviceConfigTags', newServiceConfigTags);
  },

  /**
   * check whether host component must be restarted
   * @param serviceConfig
   * @param restartData
   */
  checkForRestart: function (serviceConfig, restartData) {
    var hostsCount = 0;
    var hostComponentCount = 0;
    if (restartData != null && restartData.hostAndHostComponents != null && !jQuery.isEmptyObject(restartData.hostAndHostComponents)) {
      serviceConfig.set('restartRequired', true);
      for (var host in restartData.hostAndHostComponents) {
        hostsCount++;
        var componentsArray = Ember.A([]);
        for (var component in restartData.hostAndHostComponents[host]) {
          componentsArray.push(Ember.Object.create({name: App.format.role(component)}));
          hostComponentCount++;
        }
        App.ajax.send({
          name: 'hosts.with_public_host_names',
          sender: this,
          data: {
            clusterName: App.get('clusterName'),
            componentsArray: componentsArray,
            host: host,
            serviceConfig: serviceConfig,
            hostsCount: hostsCount,
            hostComponentCount: hostComponentCount
          },
          success: 'hostObjSuccessCallback'
        });
      }
    }
  },

  hostObjSuccessCallback: function (response, request, data) {
    var hostObj = Em.Object.create({
      hostName: response.items.findProperty('Hosts.host_name'),
      publicHostName: response.items.findProperty('Hosts.public_host_name'),
      id: response.items.findProperty('Hosts.host_name')
    });
    this.get('restartHosts').push(Ember.Object.create({hostData: hostObj, components: data.componentsArray}))
    data.serviceConfig.set('restartRequiredHostsAndComponents', []);
    lazyLoading.run({
      initSize: 20,
      chunkSize: 50,
      delay: 50,
      destination: data.serviceConfig.get('restartRequiredHostsAndComponents'),
      source: this.get('restartHosts'),
      context: this
    });
    data.serviceConfig.set('restartRequiredMessage', 'Service needs ' + data.hostComponentCount + ' components on ' + data.hostsCount + ' hosts to be restarted.')
  },

  /**
   * check whether the config property is a security related knob
   * @param serviceConfig
   */
  checkForSecureConfig: function (serviceConfig) {
    serviceConfig.get('configs').forEach(function (_config) {
      this.get('secureConfigs').forEach(function (_secureConfig) {
        if (_config.get('name') === _secureConfig.name) {
          _config.set('isSecureConfig', true);
        }
      }, this)
    }, this)
  },

  /**
   * Get info about hosts and host components to configDefaultsProviders
   * @returns {{masterComponentHosts: Array, slaveComponentHosts: Array, hosts: {}}}
   */
  getInfoForDefaults: function() {

    App.ajax.send({
      name: 'host_components.all',
      sender: this,
      data: {
        clusterName: App.get('clusterName')
      },
      success: 'slavesSuccessCallback'
    });

  },

  slavesSuccessCallback: function (response) {
    var slaveComponentHosts = [];
    var slaves = response.items.mapProperty('HostRoles').filter(function (c) {
      return App.StackServiceComponent.find().findProperty('componentName', c.component_name).get('isSlave');
    }).map(function(item) {
      return Em.Object.create({
        host: item.host_name,
        componentName: item.component_name
      });
    });
    slaves.forEach(function(slave) {
      var s = slaveComponentHosts.findProperty('componentName', slave.componentName);
      if (s) {
        s.hosts.push({hostName: slave.host});
      }
      else {
        slaveComponentHosts.push({
          componentName: slave.get('componentName'),
          hosts: [{hostName: slave.host}]
        });
      }
    });

    App.ajax.send({
      name: 'host_components.with_services_names',
      sender: this,
      data: {
        clusterName: App.get('clusterName'),
        slaveComponentHosts: slaveComponentHosts
      },
      success: 'mastersSuccessCallback'
    });

  },

  mastersSuccessCallback: function (response, request, data) {

    var masterComponentHosts = response.items.filter(function (c) {
      return App.StackServiceComponent.find().findProperty('componentName', c.HostRoles.component_name).get('isMaster');
    }).map(function(item) {
      return {
        component: item.HostRoles.component_name,
        serviceId: item.component[0].ServiceComponentInfo.service_name,
        host: item.HostRoles.host_name
      }
    });

    App.ajax.send({
      name: 'hosts.confirmed',
      sender: this,
      data: {
        clusterName: App.get('clusterName'),
        masterComponentHosts: masterComponentHosts,
        slaveComponentHosts: data.slaveComponentHosts
      },
      success: 'hostsSuccessCallback'
    });

  },

  hostsSuccessCallback: function (response, request, data) {
    var hosts = {};
    response.items.mapProperty('Hosts').map(function(host) {
      hosts[host.host_name] = {
        name: host.host_name,
        cpu: host.cpu_count,
        memory: host.total_mem,
        disk_info: host.disk_info
      };
    });
    var obj =  {
      masterComponentHosts: [],
      slaveComponentHosts: [],
      hosts: hosts
    };
    lazyLoading.run({
      initSize: 20,
      chunkSize: 50,
      delay: 50,
      destination: obj.masterComponentHosts,
      source: data.masterComponentHosts,
      context: Em.Object.create()
    });
    lazyLoading.run({
      initSize: 20,
      chunkSize: 50,
      delay: 50,
      destination: obj.slaveComponentHosts,
      source: obj.slaveComponentHosts,
      context: Em.Object.create()
    });
    this.set('defaultsInfo', obj);
  },

  /**
   * Load child components to service config object
   * @param {Array} configs - array of configs
   * @param {Object} componentConfig - component config object
   * @param {Object} restartData
   * {
   *  {Object} hostAndHostComponents,
   *  {Object} propertyToHostAndComponent
   * }
   * @method loadConfigs
   */
  loadConfigs: function(configs, componentConfig, restartData) {
    var serviceConfigsData = this.get('serviceConfigsData').findProperty('serviceName', this.get('content.serviceName'));
    var defaultGroupSelected = this.get('selectedConfigGroup.isDefault');
    configs.forEach(function (_serviceConfigProperty) {
      var serviceConfigProperty = this.createConfigProperty(_serviceConfigProperty, defaultGroupSelected, restartData, serviceConfigsData);
      componentConfig.configs.pushObject(serviceConfigProperty);
      serviceConfigProperty.validate();
    }, this);
  },

  /**
   * create {Em.Object}service_cfg_property based on {Object}_serviceConfigProperty and additional info
   * @param {Object} _serviceConfigProperty - config object
   * @param {Boolean} defaultGroupSelected - true if selected cfg group is default
   * @param {Object} restartData
   * {
   *  {Object} hostAndHostComponents,
   *  {Object} propertyToHostAndComponent
   * }
   * @param {Object} serviceConfigsData - service cfg object
   * @returns {Ember.Object|null}
   * @method createConfigProperty
   */
  createConfigProperty: function(_serviceConfigProperty, defaultGroupSelected, restartData, serviceConfigsData) {
    console.log("config", _serviceConfigProperty);
    if (!_serviceConfigProperty) return null;
    var overrides = _serviceConfigProperty.overrides;
    // we will populate the override properties below
    _serviceConfigProperty.overrides = null;
    _serviceConfigProperty.isOverridable = Em.isNone(_serviceConfigProperty.isOverridable) ? true : _serviceConfigProperty.isOverridable;

    var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);

    this.setValueForCheckBox(serviceConfigProperty);
    this.setRestartInfo(restartData, serviceConfigProperty);
    this.setValidator(serviceConfigProperty, serviceConfigsData);
    this.setValuesForOverrides(overrides, _serviceConfigProperty, serviceConfigProperty, defaultGroupSelected);
    this.setEditability(serviceConfigProperty, defaultGroupSelected);

    return serviceConfigProperty;
  },

  /**
   * trigger addOverrideProperty
   * @param {Object} componentConfig
   * @method checkOverrideProperty
   */
  checkOverrideProperty: function(componentConfig) {
    var overrideToAdd = this.get('overrideToAdd');
    if (overrideToAdd) {
      overrideToAdd = componentConfig.configs.findProperty('name', overrideToAdd.name);
      if (overrideToAdd) {
        this.addOverrideProperty(overrideToAdd);
        this.set('overrideToAdd', null);
      }
    }
  },

  /**
   * set recommended defaults for advanced configs for current service
   * @param {Array} advancedConfigs
   * @mrethod setRecommendedDefaults
   */
  setRecommendedDefaults: function (advancedConfigs) {
    var s = this.get('serviceConfigsData').findProperty('serviceName', this.get('content.serviceName'));
    var dfd = $.Deferred();
    this.getInfoForDefaults();
    this.addObserver('defaultsInfo.hosts.length', this, function() {
      var localDB = this.get('defaultsInfo');
      var recommendedDefaults = {};
      if (s.defaultsProviders) {
        s.defaultsProviders.forEach(function (defaultsProvider) {
          var d = defaultsProvider.getDefaults(localDB);
          for (var name in d) {
            if (!!d[name]) {
              recommendedDefaults[name] = d[name];
            } else {
              var defaultValueFromStack = advancedConfigs.findProperty('name', name);
              // If property default value is not declared on client, fetch it from stack definition
              // If it's not declared with any valid value in both server stack and client, then js reference error is expected to be thrown
              recommendedDefaults[name] = defaultValueFromStack.value
            }
          }
        });
      }
      if (s.configsValidator) {
        s.configsValidator.set('recommendedDefaults', recommendedDefaults);
      }
      dfd.resolve();
    });
    return dfd.promise();
  },

  /**
   * set isEditable property of config for admin
   * if default cfg group and not on the host config page
   * @param {Ember.Object} serviceConfigProperty
   * @param {Boolean} defaultGroupSelected
   * @method setEditability
   */
  setEditability: function (serviceConfigProperty, defaultGroupSelected) {
    serviceConfigProperty.set('isEditable', false);
    if (App.get('isAdmin') && defaultGroupSelected && !this.get('isHostsConfigsPage')) {
      serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isReconfigurable'));
    }
  },

  /**
   * set serviceValidator for config property
   * hide properties for other services
   * @param {Ember.Object} serviceConfigProperty
   * @param {Object} serviceConfigsData
   * @method setValidator
   */
  setValidator: function(serviceConfigProperty, serviceConfigsData) {
    if (serviceConfigProperty.get('serviceName') === this.get('content.serviceName')) {
      if (serviceConfigsData.configsValidator) {
        for (var validatorName in serviceConfigsData.configsValidator.get('configValidators')) {
          if (serviceConfigProperty.get("name") == validatorName) {
            serviceConfigProperty.set('serviceValidator', serviceConfigsData.configsValidator);
          }
        }
      }
      console.log("config result", serviceConfigProperty);
    } else {
      serviceConfigProperty.set('isVisible', false);
    }
  },

  /**
   * generate restart mesage with components and host to restart
   * @param {Object} restartData
   * {
   *  {Object} hostAndHostComponents,
   *  {Object} propertyToHostAndComponent
   * }
   * @param {Ember.Object} serviceConfigProperty
   * @method setRestartInfo
   */
  setRestartInfo: function(restartData, serviceConfigProperty) {
    App.ajax.send({
      name: 'hosts.with_public_host_names',
      sender: this,
      data: {
        clusterName: App.get('clusterName'),
        restartData: restartData,
        serviceConfigProperty: serviceConfigProperty
      },
      success: 'setRestartInfoSuccessCallback'
    });
  },

  setRestartInfoSuccessCallback: function (response, request, data) {
    var propertyName = data.serviceConfigProperty.get('name');
    if (data.restartData != null && propertyName in data.restartData.propertyToHostAndComponent) {
      data.serviceConfigProperty.set('isRestartRequired', true);
      var message = '<ul>';
      for (var host in data.restartData.propertyToHostAndComponent[propertyName]) {
        var appHost = response.items.mapProperty('Hosts').filterProperty('host_name', host);
        message += "<li>" + appHost.public_host_name;
        message += "<ul>";
        data.restartData.propertyToHostAndComponent[propertyName][host].forEach(function (comp) {
          message += "<li>" + App.format.role(comp) + "</li>"
        });
        message += "</ul></li>";
      }
      message += "</ul>";
      data.serviceConfigProperty.set('restartRequiredMessage', message);
    }
  },

  /**
   * set override values
   * @param overrides
   * @param _serviceConfigProperty
   * @param serviceConfigProperty
   * @param defaultGroupSelected
   */
  setValuesForOverrides: function (overrides, _serviceConfigProperty, serviceConfigProperty, defaultGroupSelected) {
    if (Em.isNone(overrides)) return;
      overrides.forEach(function (override) {
        var newSCP = this.createNewSCP(override, _serviceConfigProperty, serviceConfigProperty, defaultGroupSelected);
        var parentOverridesArray = serviceConfigProperty.get('overrides');
        if (parentOverridesArray == null) {
          parentOverridesArray = Em.A([]);
          serviceConfigProperty.set('overrides', parentOverridesArray);
        }
        parentOverridesArray.pushObject(newSCP);
        console.debug("createOverrideProperty(): Added:", newSCP, " to main-property:", serviceConfigProperty)
      }, this);
  },

  /**
   * create new overiden property and set approperiate fields
   * @param override
   * @param _serviceConfigProperty
   * @param serviceConfigProperty
   * @param defaultGroupSelected
   * @returns {*}
   */
  createNewSCP: function(override, _serviceConfigProperty, serviceConfigProperty, defaultGroupSelected) {
    var newSCP = App.ServiceConfigProperty.create(_serviceConfigProperty);
    newSCP.set('value', override.value);
    newSCP.set('isOriginalSCP', false); // indicated this is overridden value,
    newSCP.set('parentSCP', serviceConfigProperty);
    if (App.supports.hostOverrides && defaultGroupSelected) {
      newSCP.set('group', override.group);
      newSCP.set('isEditable', false);
    }
    return newSCP;
  },

  /**
   * convert string values to boolean for checkboxes
   * @param {Ember.Object} serviceConfigProperty
   */
  setValueForCheckBox: function(serviceConfigProperty) {
    if (serviceConfigProperty.get("displayType") == 'checkbox') {
      switch (serviceConfigProperty.get("value")) {
        case 'true':
          serviceConfigProperty.set("value", true);
          serviceConfigProperty.set("defaultValue", true);
          break;
        case 'false':
          serviceConfigProperty.set("value", false);
          serviceConfigProperty.set("defaultValue", false);
          break;
      }
    }
  },

  /**
   * open popup with appropriate message
   */
  restartServicePopup: function (event) {
    if (this.get("isSubmitDisabled")) {
      return;
    }
    var header;
    var message;
    var messageClass;
    var hasUnknown = false;
    var value;
    var flag = false;

    var dfd = $.Deferred();
    var self = this;
    var serviceName = this.get('content.serviceName');
    var displayName = this.get('content.displayName');

    var urlParams = '';
    var status;

    if (App.supports.hostOverrides ||
      (serviceName !== 'HDFS' && this.get('content.isStopped') === true) ||
      ((serviceName === 'HDFS') && this.get('content.isStopped') === true && (!App.Service.find().someProperty('id', 'MAPREDUCE') || App.Service.find('MAPREDUCE').get('isStopped')))) {

      // warn the user if any service directories are being changed
      var dirChanged = false;

      if (serviceName === 'HDFS') {
        var hdfsConfigs = self.get('stepConfigs').findProperty('serviceName', 'HDFS').get('configs');
        if (App.get('isHadoop2Stack')) {
          if (
            hdfsConfigs.findProperty('name', 'dfs.namenode.name.dir').get('isNotDefaultValue') ||
              hdfsConfigs.findProperty('name', 'dfs.namenode.checkpoint.dir').get('isNotDefaultValue') ||
              hdfsConfigs.findProperty('name', 'dfs.datanode.data.dir').get('isNotDefaultValue')
            ) {
            dirChanged = true;
          }
        } else {
          if (
            hdfsConfigs.findProperty('name', 'dfs.name.dir').get('isNotDefaultValue') ||
              hdfsConfigs.findProperty('name', 'fs.checkpoint.dir').get('isNotDefaultValue') ||
              hdfsConfigs.findProperty('name', 'dfs.data.dir').get('isNotDefaultValue')
            ) {
            dirChanged = true;
          }
        }
      } else if (serviceName === 'MAPREDUCE') {
        var mapredConfigs = self.get('stepConfigs').findProperty('serviceName', 'MAPREDUCE').get('configs');
        if (
          mapredConfigs.findProperty('name', 'mapred.local.dir').get('isNotDefaultValue') ||
            mapredConfigs.findProperty('name', 'mapred.system.dir').get('isNotDefaultValue')
          ) {
          dirChanged = true;
        }
      }

      if (dirChanged) {
        App.showConfirmationPopup(function () {
          dfd.resolve();
        }, Em.I18n.t('services.service.config.confirmDirectoryChange').format(displayName));
      } else {
        dfd.resolve();
      }

      dfd.done(function () {
        var result = self.saveServiceConfigProperties();
        App.router.get('clusterController').updateClusterData();
        App.router.get('updateController').updateComponentConfig(function(){});
        flag = result.flag;
        if (result.flag === true) {
          header = Em.I18n.t('services.service.config.saved');
          message = Em.I18n.t('services.service.config.saved.message');
          messageClass = 'alert alert-success';
          // warn the user if any of the components are in UNKNOWN state
          status = 'unknown';
          urlParams += ',ServiceComponentInfo/installed_count,ServiceComponentInfo/total_count';
          if (self.get('content.serviceName') === 'HDFS' || App.Service.find().someProperty('id', 'MAPREDUCE')) {
            urlParams += '&ServiceComponentInfo/service_name.in(HDFS,MAPREDUCE)'
          }
        } else {
          header = Em.I18n.t('common.failure');
          message = result.message;
          messageClass = 'alert alert-error';
          value = result.value;
        }
      });
    } else {
      status = 'started';
      if (this.get('content.serviceName') !== 'HDFS' || (this.get('content.serviceName') === 'HDFS' && !App.Service.find().someProperty('id', 'MAPREDUCE'))) {
        header = Em.I18n.t('services.service.config.notSaved');
        message = Em.I18n.t('services.service.config.msgServiceStop');
      } else {
        header = Em.I18n.t('services.service.config.notSaved');
        message = Em.I18n.t('services.service.config.msgHDFSMapRServiceStop');
        urlParams += '&ServiceComponentInfo/service_name.in(HDFS,MAPREDUCE)';
      }
      messageClass = 'alert alert-error';
      dfd.resolve();
    }

    dfd.done(function () {
      App.ModalPopup.show({
        header: header,
        primary: Em.I18n.t('ok'),
        secondary: null,
        onPrimary: function () {
          this.hide();
          if (flag) {
            self.loadStep();
          }
        },
        bodyClass: Ember.View.extend({
          flag: flag,
          message: message,
          messageClass: messageClass,
          runningHosts: [],
          runningComponentCount: 0,
          unknownHosts: [],
          unknownComponentCount: 0,
          siteProperties: value,
          isLoaded: false,
          componentsFilterSuccessCallback: function (response) {
            var count = 0,
              self = this,
              lazyLoadHosts = function (dest) {
                lazyLoading.run({
                  initSize: 20,
                  chunkSize: 50,
                  delay: 50,
                  destination: dest,
                  source: hosts,
                  context: self
                });
              },
              setComponents = function (item, components) {
                item.host_components.forEach(function (c) {
                  var name = c.HostRoles.host_name;
                  if (!components[name]) {
                    components[name] = [];
                  } else {
                    components[name].push(App.format.role(item.ServiceComponentInfo.component_name));
                  }
                });
                return components;
              },
              setHosts = function (components) {
                var hosts = [];
                Em.keys(components).forEach(function (key) {
                  hosts.push({
                    name: key,
                    components: components[key].join(', ')
                  });
                });
                return hosts;
              },
              components = {},
              hosts = [];
            switch (status) {
              case 'unknown':
                response.items.filter(function (item) {
                  return (item.ServiceComponentInfo.total_count > item.ServiceComponentInfo.started_count + item.ServiceComponentInfo.installed_count);
                }).forEach(function (item) {
                    var total = item.ServiceComponentInfo.total_count,
                      started = item.ServiceComponentInfo.started_count,
                      installed = item.ServiceComponentInfo.installed_count,
                      unknown = total - started + installed;
                    components = setComponents(item, components);
                    count += unknown;
                  });
                hosts = setHosts(components);
                this.set('unknownComponentCount', count);
                lazyLoadHosts(this.get('unknownHosts'));
                break;
              case 'started':
                response.items.filterProperty('ServiceComponentInfo.started_count').forEach(function (item) {
                  var started = item.ServiceComponentInfo.started_count;
                  components = setComponents(item, components);
                  count += started;
                  hosts = setHosts(components);
                });
                this.set('runningComponentCount', count);
                lazyLoadHosts(this.get('runningHosts'));
                break;
            }
          },
          componentsFilterErrorCallback: function () {
            this.set('isLoaded', true);
          },
          didInsertElement: function () {
            App.ajax.send({
              name: 'components.filter_by_status',
              sender: this,
              data: {
                clusterName: App.get('clusterName'),
                urlParams: urlParams
              },
              success: 'componentsFilterSuccessCallback',
              error: 'componentsFilterErrorCallback'
            });
          },
          getDisplayMessage: function () {
            var displayMsg = [];
            var siteProperties = this.get('siteProperties');
            if (siteProperties) {
              siteProperties.forEach(function (_siteProperty) {
                var displayProperty = _siteProperty.siteProperty;
                var displayNames = _siteProperty.displayNames;
                /////////
                if (displayNames && displayNames.length) {
                  if (displayNames.length === 1) {
                    displayMsg.push(displayProperty + Em.I18n.t('as') + displayNames[0]);
                  } else {
                    var name;
                    displayNames.forEach(function (_name, index) {
                      if (index === 0) {
                        name = _name;
                      } else if (index === siteProperties.length - 1) {
                        name = name + Em.I18n.t('and') + _name;
                      } else {
                        name = name + ', ' + _name;
                      }
                    }, this);
                    displayMsg.push(displayProperty + Em.I18n.t('as') + name);

                  }
                } else {
                  displayMsg.push(displayProperty);
                }
              }, this);
            }
            return displayMsg;

          }.property('siteProperties'),

          runningHostsMessage: function () {
            return Em.I18n.t('services.service.config.stopService.runningHostComponents').format(this.get('runningComponentCount'), this.get('runningHosts.length'));
          }.property('runningComponentCount', 'runningHosts.length'),

          unknownHostsMessage: function () {
            return Em.I18n.t('services.service.config.stopService.unknownHostComponents').format(this.get('unknownComponentCount'), this.get('unknownHosts.length'));
          }.property('unknownComponentCount', 'unknownHosts.length'),

          templateName: require('templates/main/service/info/configs_save_popup')
        })
      })
    });
  },

  /**
   * Save config properties
   */
  saveServiceConfigProperties: function () {
    var result = {
      flag: false,
      message: null,
      value: null
    };
    var selectedConfigGroup = this.get('selectedConfigGroup');
    var configs = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).get('configs');

    if (selectedConfigGroup.get('isDefault')) {
      this.saveGlobalConfigs(configs);
      if (this.get('content.serviceName') === 'YARN' && !App.supports.capacitySchedulerUi) {
        configs = App.config.textareaIntoFileConfigs(configs, 'capacity-scheduler.xml');
      }
      this.saveSiteConfigs(configs);

      /**
       * First we put cluster configurations, which automatically creates /configurations
       * resources. Next we update host level overrides.
       */
      result.flag = this.doPUTClusterConfigurations();
    } else {
      var overridenConfigs = [];
      var groupHosts = [];
      configs.filterProperty('isOverridden', true).forEach(function (config) {
        lazyLoading.run({
          initSize: 20,
          chunkSize: 50,
          delay: 50,
          destination: overridenConfigs,
          source: config.get('overrides'),
          context: Em.Object.create()
        });
      });
      this.formatConfigValues(overridenConfigs);
      selectedConfigGroup.get('hosts').forEach(function(hostName){
        groupHosts.push({"host_name": hostName});
      });

      this.putConfigGroupChanges({
        ConfigGroup: {
          "id": selectedConfigGroup.get('id'),
          "cluster_name": App.get('clusterName'),
          "group_name": selectedConfigGroup.get('name'),
          "tag": selectedConfigGroup.get('service.id'),
          "description": selectedConfigGroup.get('description'),
          "hosts": groupHosts,
          "desired_configs": this.buildGroupDesiredConfigs(overridenConfigs)
        }
      });
      result.flag = this.get('isPutConfigGroupChangesSuccess');
    }
    if (!result.flag) {
      result.message = Em.I18n.t('services.service.config.failSaveConfig');
    } else {
      if (!result.flag) {
        result.message = Em.I18n.t('services.service.config.failSaveConfigHostOverrides');
      }
    }
    console.log("The result from applyCreatdConfToService is: " + result);
    return result;
  },
  /**
   * construct desired_configs for config groups from overriden properties
   * @param configs
   * @param timeTag
   * @return {Array}
   */
  buildGroupDesiredConfigs: function (configs, timeTag) {
    var sites = [];
    var time = timeTag || (new Date).getTime();
    configs.forEach(function (config) {
      var type = config.get('filename').replace('.xml', '');
      var site = sites.findProperty('type', type);
      if (site) {
        site.properties.push({
          name: config.get('name'),
          value: config.get('value')
        });
      } else {
        site = {
          type: type,
          tag: 'version' + time,
          properties: [{
            name: config.get('name'),
            value: config.get('value')
          }]
        };
        sites.push(site);
      }
    });
    sites.forEach(function(site){
      if(site.type === 'global') {
        site.properties = this.createGlobalSiteObj(site.tag, site.properties).properties;
      } else {
        site.properties = this.createSiteObj(site.type, site.tag, site.properties).properties;
      }
    }, this);
    return sites;
  },
  /**
   * persist properties of config groups to server
   * @param data
   */
  putConfigGroupChanges: function (data) {
    App.ajax.send({
      name: 'config_groups.update_config_group',
      sender: this,
      data: {
        id: data.ConfigGroup.id,
        configGroup: data
      },
      success: "putConfigGroupChangesSuccess"
    });
  },
  isPutConfigGroupChangesSuccess: false,
  putConfigGroupChangesSuccess: function () {
    this.set('isPutConfigGroupChangesSuccess', true);
  },
  /**
   * save new or change exist configs in global configs
   * @param configs
   */
  saveGlobalConfigs: function (configs) {
    var globalConfigs = this.get('globalConfigs');
    configs.filterProperty('id', 'puppet var').forEach(function (uiConfigProperty) {
      uiConfigProperty.set('value', App.config.trimProperty(uiConfigProperty));
      if (globalConfigs.someProperty('name', uiConfigProperty.name)) {
        var modelGlobalConfig = globalConfigs.findProperty('name', uiConfigProperty.name);
        modelGlobalConfig.value = uiConfigProperty.value;
      } else {
        globalConfigs.pushObject({
          name: uiConfigProperty.name,
          value: uiConfigProperty.value
        });
      }
    }, this);

    this.setHiveHostName(globalConfigs);
    this.setOozieHostName(globalConfigs);
    this.set('globalConfigs', globalConfigs);
  },

  /**
   * set hive hostnames in global configs
   * @param globals
   */
  setHiveHostName: function (globals) {
    if (globals.someProperty('name', 'hive_database')) {
      var hiveDb = globals.findProperty('name', 'hive_database');
      if (hiveDb.value === 'New MySQL Database') {
        var ambariHost = globals.findProperty('name', 'hive_ambari_host');
        if (ambariHost) {
          ambariHost.name = 'hive_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'hive_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_mysql_database'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_oracle_database'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_postgresql_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_postgresql_database'));
      } else if (hiveDb.value === 'Existing MySQL Database') {
        var existingMySqlHost = globals.findProperty('name', 'hive_existing_mysql_host');
        if (existingMySqlHost) {
          existingMySqlHost.name = 'hive_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'hive_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'hive_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_oracle_database'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_postgresql_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_postgresql_database'));
      } else if (hiveDb.value === Em.I18n.t('services.service.config.hive.oozie.postgresql')) {
        var existingPostgreSqlHost = globals.findProperty('name', 'hive_existing_postgresql_host');
        if (existingPostgreSqlHost) {
          existingPostgreSqlHost.name = 'hive_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'hive_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'hive_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_mysql_database'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_oracle_database'));
      }
      else { //existing oracle database
        var existingOracleHost = globals.findProperty('name', 'hive_existing_oracle_host');
        if (existingOracleHost) {
          existingOracleHost.name = 'hive_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'hive_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'hive_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_mysql_database'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_postgresql_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_postgresql_database'));
      }

    }
  },

  /**
   * set oozie hostnames in global configs
   * @param globals
   */
  setOozieHostName: function (globals) {
    if (globals.someProperty('name', 'oozie_database')) {
      var oozieDb = globals.findProperty('name', 'oozie_database');
      if (oozieDb.value === 'New Derby Database') {
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_postgresql_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_postgresql_database'));
      } else if (oozieDb.value === 'New MySQL Database') {
        var ambariHost = globals.findProperty('name', 'oozie_ambari_host');
        if (ambariHost) {
          ambariHost.name = 'oozie_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_derby_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_postgresql_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_postgresql_database'));

      } else if (oozieDb.value === 'Existing MySQL Database') {
        var existingMySqlHost = globals.findProperty('name', 'oozie_existing_mysql_host');
        if (existingMySqlHost) {
          existingMySqlHost.name = 'oozie_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_derby_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_postgresql_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_postgresql_database'));
      } else if (oozieDb.value === Em.I18n.t('services.service.config.hive.oozie.postgresql')) {
        var existingPostgreSqlHost = globals.findProperty('name', 'oozie_existing_postgresql_host');
        if (existingPostgreSqlHost) {
          existingPostgreSqlHost.name = 'oozie_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_database'));
      }
      else { //existing oracle database
        var existingOracleHost = globals.findProperty('name', 'oozie_existing_oracle_host');
        if (existingOracleHost) {
          existingOracleHost.name = 'oozie_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_derby_database'));
      }

    }
  },

  /**
   * save site configs
   * @param configs
   */
  saveSiteConfigs: function (configs) {
    //storedConfigs contains custom configs as well
    var serviceConfigProperties = configs.filterProperty('id', 'site property');
    this.formatConfigValues(serviceConfigProperties);
    var storedConfigs = serviceConfigProperties.filterProperty('value');
    var mappedConfigs = App.config.excludeUnsupportedConfigs(this.get('configMapping').all(), App.Service.find().mapProperty('serviceName'));
    var allUiConfigs = this.loadUiSideConfigs(mappedConfigs);
    this.set('uiConfigs', storedConfigs.concat(allUiConfigs));
  },

  formatConfigValues: function(serviceConfigProperties){
    serviceConfigProperties.forEach(function (_config) {
      if (typeof _config.get('value') === "boolean") _config.set('value', _config.value.toString());
      _config.set('value', App.config.trimProperty(_config, true));
    });
  },

  /**
   * return configs from the UI side
   * @param configMapping array with configs
   * @return {Array}
   */
  loadUiSideConfigs: function (configMapping) {
    var uiConfig = [];
    var configs = configMapping.filterProperty('foreignKey', null);
    this.addDynamicProperties(configs);
    configs.forEach(function (_config) {
      var valueWithOverrides = this.getGlobConfigValueWithOverrides(_config.templateName, _config.value);
      if (valueWithOverrides !== null) {
        uiConfig.pushObject({
          "id": "site property",
          "name": _config.name,
          "value": valueWithOverrides.value,
          "filename": _config.filename,
          "overrides": valueWithOverrides.overrides
        });
      }
    }, this);
    return uiConfig;
  },


  addDynamicProperties: function (configs) {
    var allConfigs = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).get('configs');
    var templetonHiveProperty = allConfigs.someProperty('name', 'templeton.hive.properties');
    if (!templetonHiveProperty && this.get('content.serviceName') === 'WEBHCAT') {
      configs.pushObject({
        "name": "templeton.hive.properties",
        "templateName": ["hivemetastore_host"],
        "foreignKey": null,
        "value": "hive.metastore.local=false,hive.metastore.uris=thrift://<templateName[0]>:9083,hive.metastore.sasl.enabled=yes,hive.metastore.execute.setugi=true,hive.metastore.warehouse.dir=/apps/hive/warehouse",
        "filename": "webhcat-site.xml"
      });
    }
  },

  /**
   * return global config value
   * @param templateName
   * @param expression
   * @return {Object}
   * example: <code>{
   *   value: '...',
   *   overrides: {
   *    'value1': [h1, h2],
   *    'value2': [h3]
   *   }
   * }</code>
   */
  getGlobConfigValueWithOverrides: function (templateName, expression) {
    var express = expression.match(/<(.*?)>/g);
    var value = expression;
    var overrideHostToValue = {};
    if (express != null) {
      express.forEach(function (_express) {
        var index = parseInt(_express.match(/\[([\d]*)(?=\])/)[1]);
        var globalObj = this.get('globalConfigs').findProperty('name', templateName[index]);
        if (globalObj) {
          var globOverride = globalObj.overrides;
          if (globOverride != null) {
            for (var ov in globOverride) {
              globOverride[ov].forEach(function (host) {
                var replacedVal = (host in overrideHostToValue) ? overrideHostToValue[host] : expression;
                overrideHostToValue[host] = replacedVal.replace(_express, ov);
              }, this);
            }
          }
          value = expression.replace(_express, globalObj.value);
        } else {
          value = null;
        }
      }, this);
    }
    return this.getValueWithOverrides(value, overrideHostToValue)
  },

  getValueWithOverrides: function(value, overrideHostToValue) {
    var valueWithOverrides = {
      value: value,
      overrides: {}
    };
    if (!jQuery.isEmptyObject(overrideHostToValue)) {
      for (var host in overrideHostToValue) {
        var hostVal = overrideHostToValue[host];
        if (!(hostVal in valueWithOverrides.overrides)) {
          valueWithOverrides.overrides[hostVal] = [];
        }
        valueWithOverrides.overrides[hostVal].push(host);
      }
    }
    return valueWithOverrides;
  },

  /**
   * Saves cluster level configurations for all necessary sites.
   * PUT calls are made to /api/v1/clusters/clusterName for each site.
   * @return {Boolean}
   * @method doPUTClusterConfigurations
   */
  doPUTClusterConfigurations: function () {
    var result = true;
    var serviceConfigTags = this.get('serviceConfigTags');
    this.setNewTagNames(serviceConfigTags);
    var siteNameToServerDataMap = {};

    serviceConfigTags.forEach(function (_serviceTags) {
      var configs = this.createConfigObject(_serviceTags.siteName, _serviceTags.newTagName);
      if (configs) {
        result = result && this.doPUTClusterConfiguration(siteNameToServerDataMap, _serviceTags.siteName, configs);
      }
    }, this);
    this.set("savedSiteNameToServerServiceConfigDataMap", siteNameToServerDataMap);
    return result;
  },

  /**
   * create different config object depending on siteName
   * @param {String} siteName
   * @param {String} tagName
   * @returns {Object|null}
   * @method createConfigObject
   */
  createConfigObject: function(siteName, tagName) {
    console.log("TRACE: Inside " + siteName);
    switch(siteName) {
      case 'global':
        return this.createGlobalSiteObj(tagName, this.get('globalConfigs'));
      case 'core-site':
        if(this.get('content.serviceName') === 'HDFS' || this.get('content.serviceName') === 'GLUSTERFS') {
          return this.createCoreSiteObj(tagName);
        }
        return null;
      default:
        var filename = (App.config.get('filenameExceptions').contains(siteName)) ? siteName : siteName + '.xml';
        if (filename === 'mapred-queue-acls.xml' && !App.supports.capacitySchedulerUi) {
          return null;
        }
      return this.createSiteObj(siteName, tagName, this.get('uiConfigs').filterProperty('filename', filename));
    }
  },

  /**
   * load existen properties and compare them with current if there are
   * differences - trigger doPUTClusterConfigurationSite to save new properties
   * @param {Object} siteNameToServerDataMap
   * @param {String} siteName
   * @param {Object} configs
   * @returns {Boolean} true if config successfully saved or there
   *                    is no need to save them
   * @method doPUTClusterConfiguration
   */
  doPUTClusterConfiguration: function(siteNameToServerDataMap, siteName, configs) {
    var loadedProperties;
    siteNameToServerDataMap[siteName] = configs;
    loadedProperties = App.router.get('configurationController').getConfigsByTags([{siteName: siteName, tagName: this.loadedClusterSiteToTagMap[siteName]}]);
    if (loadedProperties && loadedProperties[0]) {
      loadedProperties = loadedProperties[0].properties;
    }
    if (!loadedProperties) {
      loadedProperties = {};
    }
    if (this.isConfigChanged(loadedProperties, configs.properties)) {
      return this.doPUTClusterConfigurationSite(configs);
    }
    return true;
  },


  /**
   * Compares the loaded config values with the saving config values.
   * @param {Object} loadedConfig -
   * loadedConfig: {
   *      configName1: "configValue1",
   *      configName2: "configValue2"
   *   }
   * @param {Object} savingConfig
   * savingConfig: {
   *      configName1: "configValue1",
   *      configName2: "configValue2"
   *   }
   * @returns {boolean}
   * @method isConfigChanged
   */
  isConfigChanged: function (loadedConfig, savingConfig) {
    var changed = false;
    if (loadedConfig != null && savingConfig != null) {
      var seenLoadKeys = [];
      for (var loadKey in loadedConfig) {
        if (!loadedConfig.hasOwnProperty(loadKey)) continue;
        seenLoadKeys.push(loadKey);
        var loadValue = loadedConfig[loadKey];
        var saveValue = savingConfig[loadKey];
        if ("boolean" == typeof(saveValue)) {
          saveValue = saveValue.toString();
        }
        if (saveValue == null) {
          saveValue = "null";
        }
        if (loadValue !== saveValue) {
          changed = true;
          break;
        }
      }
      for (var saveKey in savingConfig) {
        if (seenLoadKeys.indexOf(saveKey) < 0) {
          changed = true;
          break;
        }
      }
    }
    return changed;
  },

  /**
   * Saves configuration of a particular site. The provided data
   * contains the site name and tag to be used.
   * @param {Object} data
   * @return {bool}
   * @method doPUTClusterConfigurationSite
   */
  doPUTClusterConfigurationSite: function (data) {
    App.ajax.send({
      name: 'config.cluster_configuration.put',
      sender: this,
      data: {
        data: JSON.stringify({
          Clusters: {
            desired_config: data
          }
        }),
        cluster: App.router.getClusterName()
      },
      success: 'doPUTClusterConfigurationSiteSuccessCallback',
      error: 'doPUTClusterConfigurationSiteErrorCallback'
    });
    return this.get('doPUTClusterConfigurationSiteResult');
  },

  /**
   * @type {bool}
   */
  doPUTClusterConfigurationSiteResult: null,

  /**
   * set doPUTClusterConfigurationSiteResult to true and write info to log
   * @param {Object} data
   * @method doPUTClusterConfigurationSiteSuccessCallback
   */
  doPUTClusterConfigurationSiteSuccessCallback: function(data) {
    console.log("applyClusterConfigurationToSite(): In success for data:", data);
    this.set('doPUTClusterConfigurationSiteResult', true);
  },

  /**
   * set doPUTClusterConfigurationSiteResult to false and write info to log
   * @param {Object} request
   * @param {Object} ajaxOptions
   * @param {String} error
   * @method doPUTClusterConfigurationSiteSuccessCallback
   */
  doPUTClusterConfigurationSiteErrorCallback: function(request, ajaxOptions, error) {
    console.log('applyClusterConfigurationToSite(): ERROR:', request.responseText, ", error=", error);
    this.set('doPUTClusterConfigurationSiteResult', false);
  },

  /**
   * add newTagName property to each config in serviceConfigs
   * @param serviceConfigs
   */
  setNewTagNames: function (serviceConfigs) {
    var time = (new Date).getTime();
    serviceConfigs.forEach(function (_serviceConfigs) {
      _serviceConfigs.newTagName = 'version' + time;
    }, this);
  },

  /**
   * create global site object
   * @param {String} tagName
   * @param {Array} globalConfigs array of config objects
   * @return {{"type": String, "tag": String, "properties": Object}}
   * @method createGlobalSiteObj
   */
  createGlobalSiteObj: function (tagName, globalConfigs) {
    var heapsizeException = ['hadoop_heapsize', 'yarn_heapsize', 'nodemanager_heapsize', 'resourcemanager_heapsize', 'apptimelineserver_heapsize', 'jobhistory_heapsize'];
    var globalSiteProperties = {};
    globalConfigs.forEach(function (_globalSiteObj) {
      // do not pass any globalConfigs whose name ends with _host or _hosts
      if (_globalSiteObj.isRequiredByAgent !== false) {
        // append "m" to JVM memory options except for hadoop_heapsize
        if (/_heapsize|_newsize|_maxnewsize$/.test(_globalSiteObj.name) && !heapsizeException.contains(_globalSiteObj.name)) {
          _globalSiteObj.value += "m";
        }
        globalSiteProperties[_globalSiteObj.name] = App.config.escapeXMLCharacters(_globalSiteObj.value);
        //this.recordHostOverride(_globalSiteObj, 'global', tagName, this);
        //console.log("TRACE: name of the global property is: " + _globalSiteObj.name);
        //console.log("TRACE: value of the global property is: " + _globalSiteObj.value);
      }
    }, this);
    return {"type": "global", "tag": tagName, "properties": globalSiteProperties};
  },

  /**
   * create core site object
   * @param tagName
   * @return {{"type": String, "tag": String, "properties": Object}}
   * @method createCoreSiteObj
   */
  createCoreSiteObj: function (tagName) {
    var coreSiteObj = this.get('uiConfigs').filterProperty('filename', 'core-site.xml');
    var coreSiteProperties = {};
    coreSiteObj.forEach(function (_coreSiteObj) {
      coreSiteProperties[_coreSiteObj.name] = App.config.escapeXMLCharacters(_coreSiteObj.value);
      //this.recordHostOverride(_coreSiteObj, 'core-site', tagName, this);
    }, this);
    return {"type": "core-site", "tag": tagName, "properties": coreSiteProperties};
  },

  /**
   * create site object
   * @param siteName
   * @param tagName
   * @param siteObj
   * @return {Object}
   */
  createSiteObj: function (siteName, tagName, siteObj) {
    var siteProperties = {};
    siteObj.forEach(function (_siteObj) {
      switch(siteName) {
        case 'falcon-startup.properties':
        case 'falcon-runtime.properties':
        case 'pig-properties':
          siteProperties[_siteObj.name] = _siteObj.value;
          break;
        default:
          siteProperties[_siteObj.name] = this.setServerConfigValue(_siteObj.name, _siteObj.value);
      }
    }, this);
    return {"type": siteName, "tag": tagName, "properties": siteProperties};
  },
  /**
   * This method will be moved to config's decorators class.
   *
   * For now, provide handling for special properties that need
   * be specified in special format required for server.
   *
   * @param configName {String} - name of config property
   * @param value {Mixed} - value of config property
   *
   * @return {String} - formated value
   */
  setServerConfigValue: function (configName, value) {
    switch (configName) {
      case 'storm.zookeeper.servers':
        return JSON.stringify(value).replace(/"/g, "'");
        break;
      case 'content':
        return value;
        break;
      default:
        return App.config.escapeXMLCharacters(value);
    }
  },

  /**
   * Array of Objects
   * {
   *  hostProperty - hostName property name for current component
   *  componentName - master componentName
   *  serviceName - serviceName of component
   *  serviceUseThis - services that use hostname property of component(componentName)
   *  m(multiple) - true if can be more than one components installed on cluster
   * }
   */

  hostComponentsmapping: [
    {
      hostProperty: 'namenode_host',
      componentName: 'NAMENODE',
      serviceName: 'HDFS',
      serviceUseThis: [],
      m: true
    },
    {
      hostProperty: 'snamenode_host',
      componentName: 'SECONDARY_NAMENODE',
      serviceName: 'HDFS',
      serviceUseThis: []
    },
    {
      hostProperty: 'jobtracker_host',
      componentName: 'JOBTRACKER',
      serviceName: 'MAPREDUCE',
      serviceUseThis: []
    },
    {
      hostProperty: 'jobtracker_host',
      componentName: 'JOBTRACKER',
      serviceName: 'MAPREDUCE2',
      serviceUseThis: []
    },
    {
      hostProperty: 'hs_host',
      componentName: 'HISTORYSERVER',
      serviceName: 'MAPREDUCE2',
      serviceUseThis: ['YARN']
    },
    {
      hostProperty: 'ats_host',
      componentName: 'APP_TIMELINE_SERVER',
      serviceName: 'YARN',
      serviceUseThis: []
    },
    {
      hostProperty: 'rm_host',
      componentName: 'RESOURCEMANAGER',
      serviceName: 'YARN',
      serviceUseThis: []
    },
    {
      hostProperty: 'hivemetastore_host',
      componentName: 'HIVE_SERVER',
      serviceName: 'HIVE',
      serviceUseThis: ['WEBHCAT']
    },
    {
      hostProperty: 'oozieserver_host',
      componentName: 'OOZIE_SERVER',
      serviceName: 'OOZIE',
      serviceUseThis: []
    },
    {
      hostProperty: 'hbasemaster_host',
      componentName: 'HBASE_MASTER',
      serviceName: 'HBASE',
      serviceUseThis: [],
      m: true
    },
    {
      hostProperty: 'hueserver_host',
      componentName: 'HUE_SERVER',
      serviceName: 'HUE',
      serviceUseThis: []
    },
    {
      hostProperty: 'webhcatserver_host',
      componentName: 'WEBHCAT_SERVER',
      serviceName: 'WEBHCAT',
      serviceUseThis: [],
      m: true
    },
    {
      hostProperty: 'zookeeperserver_hosts',
      componentName: 'ZOOKEEPER_SERVER',
      serviceName: 'ZOOKEEPER',
      serviceUseThis: ['HBASE', 'WEBHCAT'],
      m: true
    },
    {
      hostProperty: 'stormuiserver_host',
      componentName: 'STORM_UI_SERVER',
      serviceName: 'STORM',
      serviceUseThis: []
    },
    {
      hostProperty: 'drpcserver_host',
      componentName: 'DRPC_SERVER',
      serviceName: 'STORM',
      serviceUseThis: []
    },
    {
      hostProperty: 'storm_rest_api_host',
      componentName: 'STORM_REST_API',
      serviceName: 'STORM',
      serviceUseThis: []
    },
    {
      hostProperty: 'supervisor_hosts',
      componentName: 'SUPERVISOR',
      serviceName: 'STORM',
      serviceUseThis: [],
      m: true
    }
  ],

  /**
   * Adds host name of master component to global config;
   * @method addHostNamesToGlobalConfig
   */
  addHostNamesToGlobalConfig: function () {
    var serviceName = this.get('content.serviceName');
    var globalConfigs = this.get('globalConfigs');
    //namenode_host is required to derive "fs.default.name" a property of core-site
    try {
      this.setHostForService('HDFS', 'NAMENODE', 'namenode_host', true);
    } catch (err) {
      console.log("No NameNode Host available.  This is expected if you're using GLUSTERFS rather than HDFS.");
    }

    var hostProperties = this.get('hostComponentsmapping').filter(function(h) {
      return h.serviceUseThis.contains(serviceName) || h.serviceName == serviceName;
    });
    hostProperties.forEach(function(h) {
      this.setHostForService(h.serviceName, h.componentName, h.hostProperty, h.m);
    }, this);

    if (serviceName === 'HIVE') {
      var hiveDb = globalConfigs.findProperty('name', 'hive_database').value;
      if (['Existing MySQL Database', 'Existing Oracle Database'].contains(hiveDb)) {
        globalConfigs.findProperty('name', 'hive_hostname').isVisible = true;
      }
    }
    if (serviceName === 'OOZIE') {
      var oozieDb = globalConfigs.findProperty('name', 'oozie_database').value;
      if (['Existing MySQL Database', 'Existing Oracle Database'].contains(oozieDb)) {
        globalConfigs.findProperty('name', 'oozie_hostname').isVisible = true;
      }
    }
  },
  /**
   * set host name(s) property for component
   * @param {String} serviceName - service name of component
   * @param {String} componentName - component name whic host we want to know
   * @param {String} hostProperty - name of host property for current component
   * @param {Boolean} multiple - true if can be more than one component
   * @method setHostForService
   */
  setHostForService: function(serviceName, componentName, hostProperty, multiple) {
    var globalConfigs = this.get('globalConfigs');
    var serviceConfigs = this.get('serviceConfigs').findProperty('serviceName', serviceName).configs;
    var hostConfig = serviceConfigs.findProperty('name', hostProperty);
    if (hostConfig) {
      hostConfig.defaultValue = this.getMasterComponentHostValue(componentName, multiple);
      globalConfigs.push(hostConfig);
    }
  },

  /**
   * get hostName of component
   * @param {String} componentName
   * @param {Boolean} multiple - true if can be more than one component installed on cluster
   * @return {String|Array|Boolean} hostName|hostNames|false if missing component
   * @method getMasterComponentHostValue
   */
  getMasterComponentHostValue: function(componentName, multiple) {
    var components = this.get('content.hostComponents').filterProperty('componentName', componentName);
    if (components.length > 0) {
      return multiple ? components.mapProperty('hostName') : components[0].get('hostName');
    }
    return false;
  },
  /**
   * Provides service component name and display-name information for
   * the current selected service.
   * @return {Em.Array} validComponents - array of valid components
   * @method getCurrentServiceComponents
   */
  getCurrentServiceComponents: function () {
    var components = this.get('content.hostComponents');
    var validComponents = Ember.A([]);
    var seenComponents = {};
    components.forEach(function (component) {
      var cn = component.get('componentName');
      var cdn = component.get('displayName');
      if (!seenComponents[cn]) {
        validComponents.push(Ember.Object.create({
          componentName: cn,
          displayName: cdn,
          selected: false
        }));
        seenComponents[cn] = cn;
      }
    });
    return validComponents;
  }.property('content'),

  /**
   * trigger loadStep
   * @method loadStep
   */
  doCancel: function () {
    this.loadStep();
  },

  /**
   * trigger restartAllServiceHostComponents(batchUtils) if confirmed in popup
   * @method restartAllStaleConfigComponents
   * @return App.showConfirmationFeedBackPopup
   */
  restartAllStaleConfigComponents: function() {
    var self = this;
    var serviceDisplayName = this.get('content.displayName');
    var bodyMessage = Em.Object.create({
      confirmMsg: Em.I18n.t('services.service.restartAll.confirmMsg').format(serviceDisplayName),
      confirmButton: Em.I18n.t('services.service.restartAll.confirmButton'),
      additionalWarningMsg: this.get('content.passiveState') === 'OFF' ? Em.I18n.t('services.service.restartAll.warningMsg.turnOnMM').format(serviceDisplayName): null
    });
    return App.showConfirmationFeedBackPopup(function(query) {
      var selectedService = self.get('content.id');
      batchUtils.restartAllServiceHostComponents(selectedService, true, query);
    }, bodyMessage);
  },

  /**
   * trigger launchHostComponentRollingRestart(batchUtils)
   * @method rollingRestartStaleConfigSlaveComponents
   */
  rollingRestartStaleConfigSlaveComponents: function(componentName) {
    batchUtils.launchHostComponentRollingRestart(componentName.context, this.get('content.displayName'), this.get('content.passiveState') === "ON", true);
  },

  /**
   * trigger showItemsShouldBeRestarted popup with hosts that requires resetart
   * @method showHostsShouldBeRestarted
   */
  showHostsShouldBeRestarted: function() {
    var hosts = [];
    for(var hostName in this.get('content.restartRequiredHostsAndComponents')) {
      hosts.push(hostName);
    }
    var hostsText = hosts.length == 1 ? Em.I18n.t('common.host') : Em.I18n.t('common.hosts');
    hosts = hosts.join(', ');
    this.showItemsShouldBeRestarted(hosts, Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(hostsText));
  },

  /**
   * trigger showItemsShouldBeRestarted popup with components that requires resetart
   * @method showComponentsShouldBeRestarted
   */
  showComponentsShouldBeRestarted: function() {
    var rhc = this.get('content.restartRequiredHostsAndComponents');
    var hostsComponets = [];
    var componentsObject = {};
    for(var hostName in rhc) {
      rhc[hostName].forEach(function(hostComponent) {
        hostsComponets.push(hostComponent);
        if(componentsObject[hostComponent] != undefined) {
          componentsObject[hostComponent]++;
        } else {
          componentsObject[hostComponent] = 1;
        }
      })
    }
    var componentsList = [];
    for( var obj in componentsObject) {
      var componentDisplayName = (componentsObject[obj] > 1) ? obj + 's' : obj;
      componentsList.push(componentsObject[obj] + ' ' + componentDisplayName);
    }
    var componentsText = componentsList.length == 1 ? Em.I18n.t('common.component') : Em.I18n.t('common.components');
    hostsComponets = componentsList.join(', ');
    this.showItemsShouldBeRestarted(hostsComponets, Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(componentsText));
  },

  showItemsShouldBeRestarted: function(content, header) {
    App.ModalPopup.show({
      content: content,
      header: header,
      bodyClass: Em.View.extend({
        templateName: require('templates/common/selectable_popup'),
        textareaVisible: false,
        textTrigger: function() {
          this.set('textareaVisible', !this.get('textareaVisible'));
        },
        putContentToTextarea: function() {
          var content = this.get('parentView.content');
          if (this.get('textareaVisible')) {
            var wrapper = $(".task-detail-log-maintext");
            $('.task-detail-log-clipboard').html(content).width(wrapper.width()).height(wrapper.height());
            Em.run.next(function() {
              $('.task-detail-log-clipboard').select();
            });
          }
        }.observes('textareaVisible')
      }),
      secondary: null
    });
  },

  /**
   * add new overridden property to config property object
   * @param {object} serviceConfigProperty - config property object
   * @method addOverrideProperty
   */
  addOverrideProperty: function(serviceConfigProperty) {
    var overrides = serviceConfigProperty.get('overrides');
    if (!overrides) {
      overrides = [];
      serviceConfigProperty.set('overrides', overrides);
    }
    // create new override with new value
    var newSCP = App.ServiceConfigProperty.create(serviceConfigProperty);
    newSCP.set('value', '');
    newSCP.set('isOriginalSCP', false); // indicated this is overridden value,
    newSCP.set('parentSCP', serviceConfigProperty);
    newSCP.set('isEditable', true);
    console.debug("createOverrideProperty(): Added:", newSCP, " to main-property:", serviceConfigProperty);
    overrides.pushObject(newSCP);
  },

  /**
   * trigger manageConfigurationGroups
   * @method manageConfigurationGroup
   */
  manageConfigurationGroup: function () {
    this.manageConfigurationGroups();
  },

  manageConfigurationGroups: function (controller) {
    var serviceData = (controller && controller.get('selectedService')) || this.get('content');
    var serviceName = serviceData.get('serviceName');
    var displayName = serviceData.get('displayName');
    App.router.get('manageConfigGroupsController').set('isInstaller', !!controller);
    return App.ModalPopup.show({
      header: Em.I18n.t('services.service.config_groups_popup.header').format(displayName),
      bodyClass: App.MainServiceManageConfigGroupView.extend({
        serviceName: serviceName,
        displayName: displayName,
        controllerBinding: 'App.router.manageConfigGroupsController'
      }),
      classNames: ['sixty-percent-width-modal', 'manage-configuration-group-popup'],
      primary: Em.I18n.t('common.save'),
      onPrimary: function() {
        var modifiedConfigGroups = this.get('subViewController.hostsModifiedConfigGroups');
        // Save modified config-groups
        if (!!controller) {
          controller.set('selectedService.configGroups', App.router.get('manageConfigGroupsController.configGroups'));
          controller.selectedServiceObserver();
          if (controller.get('name') == "wizardStep7Controller") {
            if (controller.get('selectedService.selected') === false && modifiedConfigGroups.toDelete.length > 0) {
              controller.setGroupsToDelete(modifiedConfigGroups.toDelete);
            }
            App.config.persistWizardStep7ConfigGroups();
            this.updateConfigGroupOnServicePage();
          }
          this.hide();
          return;
        }
        console.log("manageConfigurationGroups(): Saving modified config-groups: ", modifiedConfigGroups);
        var self = this;
        var errors = [];
        var deleteQueriesCounter = modifiedConfigGroups.toClearHosts.length + modifiedConfigGroups.toDelete.length;
        var createQueriesCounter = modifiedConfigGroups.toSetHosts.length + modifiedConfigGroups.toCreate.length;
        var deleteQueriesRun = false;
        var createQueriesRun = false;
        var runNextQuery = function () {
          if (!deleteQueriesRun && deleteQueriesCounter > 0) {
            deleteQueriesRun = true;
            modifiedConfigGroups.toClearHosts.forEach(function (cg) {
              App.config.clearConfigurationGroupHosts(cg, finishFunction, finishFunction);
            }, this);
            modifiedConfigGroups.toDelete.forEach(function (cg) {
              App.config.deleteConfigGroup(cg, finishFunction, finishFunction);
            }, this);
          } else if (!createQueriesRun && deleteQueriesCounter < 1) {
            createQueriesRun = true;
            modifiedConfigGroups.toSetHosts.forEach(function (cg) {
              App.config.updateConfigurationGroup(cg, finishFunction, finishFunction);
            }, this);
            modifiedConfigGroups.toCreate.forEach(function (cg) {
              App.config.postNewConfigurationGroup(cg, finishFunction);
            }, this);
          }
        };
        var finishFunction = function (xhr, text, errorThrown) {
          if (xhr && errorThrown) {
            var error = xhr.status + "(" + errorThrown + ") ";
            try {
              var json = $.parseJSON(xhr.responseText);
              error += json.message;
            } catch (err) {
            }
            console.error('Error updating Config Group:', error);
            errors.push(error);
          }
          if (createQueriesRun) {
            createQueriesCounter--;
          } else {
            deleteQueriesCounter--;
          }
          if (deleteQueriesCounter + createQueriesCounter < 1) {
            if (errors.length > 0) {
              console.log(errors);
              self.get('subViewController').set('errorMessage', errors.join(". "));
            } else {
              self.updateConfigGroupOnServicePage();
              self.hide();
            }
          } else {
            runNextQuery();
          }
        };
       runNextQuery();
      },
      onSecondary: function () {
        this.hide();
      },
      onClose: function () {
        this.hide();
      },
      subViewController: function () {
        return App.router.get('manageConfigGroupsController');
      }.property('App.router.manageConfigGroupsController'),

      updateConfigGroupOnServicePage: function () {
        var subViewController = this.get('subViewController');
        var selectedConfigGroup = subViewController.get('selectedConfigGroup');
        var managedConfigGroups = subViewController.get('configGroups');
        if (!controller) {
          controller = App.router.get('mainServiceInfoConfigsController');
          controller.set('configGroups', managedConfigGroups);
        } else {
          controller.set('selectedService.configGroups', managedConfigGroups);
        }
        //check whether selectedConfigGroup was selected
        if (selectedConfigGroup && controller.get('configGroups').someProperty('name', selectedConfigGroup.get('name'))) {
          controller.set('selectedConfigGroup', selectedConfigGroup);
        } else {
          controller.set('selectedConfigGroup', managedConfigGroups.findProperty('isDefault', true));
        }
      },

      updateButtons: function(){
        var modified = this.get('subViewController.isHostsModified');
        this.set('disablePrimary', !modified);
      }.observes('subViewController.isHostsModified'),
      secondary : Em.I18n.t('common.cancel'),
      didInsertElement: function () {}
    });
  },

  /**
   * If user chabges cfg group if some configs was changed popup with propose to save changes must be shown
   * @param {object} event - triggered event for selecting another config-group
   * @method selectConfigGroup
   */
  selectConfigGroup: function (event) {
    if (!this.get('isInit')) {
      if (this.hasUnsavedChanges()) {
        this.showSavePopup(null, event);
        return;
      }
    }
    this.set('selectedConfigGroup', event.context);
  },

  /**
   * Are some unsaved changes available
   * @returns {boolean}
   */
  hasUnsavedChanges: function() {
    return this.get('hash') != this.getHash();
  },

  /**
   * If some configs are changed and user navigates away or select another config-group, show this popup with propose to save changes
   * @param {String} path
   * @param {object} event - triggered event for selecting another config-group
   * @method showSavePopup
   */
  showSavePopup: function (path, event) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      body: Em.I18n.t('services.service.config.exitPopup.body'),
      footerClass: Ember.View.extend({
        templateName: require('templates/main/service/info/save_popup_footer'),
        isSaveDisabled: function() {
          return self.get('isSubmitDisabled');
        }.property()
      }),
      primary: Em.I18n.t('common.save'),
      secondary: Em.I18n.t('common.cancel'),
      onSave: function () {
        self.restartServicePopup();
        this.hide();
      },
      onDiscard: function () {
        if (path) {
          self.set('forceTransition', true);
          App.router.route(path);
        } else if (event) {
          // Prevent multiple popups
          self.set('hash', self.getHash());
          self.selectConfigGroup(event);
        }
        this.hide();
      },
      onCancel: function () {
        this.hide();
      }
    });
  }
});
