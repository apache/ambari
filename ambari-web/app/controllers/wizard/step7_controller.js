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

  stepConfigs: [], //contains all field properties that are viewed in this step

  selectedService: null,

  slaveHostToGroup: null,

  secureConfigs: require('data/secure_mapping'),

  miscModalVisible: false, //If miscConfigChange Modal is shown

  gangliaAvailableSpace: null,

  gangliaMoutDir:'/',

  overrideToAdd: null,

  isInstaller: true,

  configGroups: [],

  groupsToDelete: [],

  selectedConfigGroup: null,
  /**
   * config tags of actually installed services
   */
  serviceConfigTags: [],

  serviceConfigsData: require('data/service_configs'),

  isAdvancedConfigLoaded: true,

  isSubmitDisabled: function () {
    return (!this.stepConfigs.filterProperty('showConfig', true).everyProperty('errorCount', 0) || this.get("miscModalVisible"));
  }.property('stepConfigs.@each.errorCount', 'miscModalVisible'),

  selectedServiceNames: function () {
    return this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false).mapProperty('serviceName');
  }.property('content.services').cacheable(),

  allSelectedServiceNames: function () {
    return this.get('content.services').filterProperty('isSelected').mapProperty('serviceName');
  }.property('content.services').cacheable(),

  installedServiceNames: function () {
    var serviceNames = this.get('content.services').filterProperty('isInstalled').mapProperty('serviceName');
    if(this.get('content.controllerName') !== 'installerController') {
      return serviceNames.without('SQOOP').without('HCATALOG');
    }
    return serviceNames;
  }.property('content.services').cacheable(),

  masterComponentHosts: function () {
    return this.get('content.masterComponentHosts');
  }.property('content.masterComponentHosts'),

  slaveComponentHosts: function () {
    return this.get('content.slaveGroupProperties');
  }.property('content.slaveGroupProperties', 'content.slaveComponentHosts'),

  customData: [],

  clearStep: function () {
    this.get('serviceConfigTags').clear();
    this.get('stepConfigs').clear();
    this.set('filter', '');
    this.get('filterColumns').setEach('selected', false);
  },
  /**
   *  Load config groups for installed services
   */
  loadInstalledServicesConfigGroups: function (servicesNames) {
    servicesNames.forEach(function(serviceName) {
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
  * Load config groups success callback
  */
  loadServiceTagsSuccess: function (data, opt, params) {
    var serviceConfigsDef = params.serviceConfigsDef;
    var serviceName = params.serviceName;
    var service = this.get('stepConfigs').findProperty('serviceName', serviceName);
    // Create default configuration group
    var defaultConfigGroupHosts = this.get('wizardController.allHosts').mapProperty('hostName');
    var selectedConfigGroup;
    var siteToTagMap = {};
    for (var site in data.Clusters.desired_configs) {
      if (serviceConfigsDef.sites.indexOf(site) > -1) {
        siteToTagMap[site] = data.Clusters.desired_configs[site].tag;
      }
    }
    this.loadedClusterSiteToTagMap = siteToTagMap;
    //parse loaded config groups
    if (App.supports.hostOverrides) {
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
    if (App.supports.hostOverrides) {
      service.set('configGroups', configGroups);
      var loadedGroupToOverrideSiteToTagMap = {};
      if (App.supports.hostOverrides) {
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

  loadComponentConfigs: function (configs, componentConfig, component) {
    var localDB = App.router.get('mainServiceInfoConfigsController').getInfoForDefaults();
    var recommendedDefaults = {};
    var s = this.get('serviceConfigsData').findProperty('serviceName', component.get('serviceName'));
    var defaultGroupSelected = component.get('selectedConfigGroup.isDefault');
    var defaults = [];
    if (s.defaultsProviders) {
      s.defaultsProviders.forEach(function(defaultsProvider) {
        var d = defaultsProvider.getDefaults(localDB);
        defaults.push(d);
        for (var name in d) {
          recommendedDefaults[name] = d[name];
        }
      });
    }
    if (s.configsValidator) {
      s.configsValidator.set('recommendedDefaults', recommendedDefaults);
    }
    configs.forEach(function (serviceConfigProperty) {
      console.log("config", serviceConfigProperty);
      if (!serviceConfigProperty) return;
      var overrides = serviceConfigProperty.get('overrides');
      // we will populate the override properties below
      serviceConfigProperty.set('overrides', null);

      if (serviceConfigProperty.isOverridable === undefined) {
        serviceConfigProperty.set('isOverridable', true);
      }
      if (serviceConfigProperty.displayType === 'checkbox') {
        switch (serviceConfigProperty.value) {
          case 'true':
            serviceConfigProperty.set('value', true);
            serviceConfigProperty.set('defaultValue', true);
            break;
          case 'false':
            serviceConfigProperty.set('value', false);
            serviceConfigProperty.set('defaultValue', false);
            break;
        }
      }
      if (serviceConfigProperty.get('serviceName') === component.get('serviceName')) {
        if (s.configsValidator) {
          var validators = s.configsValidator.get('configValidators');
          for (var validatorName in validators) {
            if (serviceConfigProperty.name == validatorName) {
              serviceConfigProperty.set('serviceValidator', s.configsValidator);
            }
          }
        }
        console.log("config result", serviceConfigProperty);
      } else {
        serviceConfigProperty.set('isVisible', false);
      }
      if (overrides != null) {
        overrides.forEach(function (override) {
          var newSCP = App.ServiceConfigProperty.create(serviceConfigProperty);
          newSCP.set('value', override.value);
          newSCP.set('isOriginalSCP', false); // indicated this is overridden value,
          newSCP.set('parentSCP', serviceConfigProperty);
          if (App.supports.hostOverrides && defaultGroupSelected) {
            var group = component.get('configGroups').findProperty('name', override.group.get('name'));
            // prevent cycle in proto object, clean link
            if (group.get('properties').length == 0)
              group.set('properties', Em.A([]));
            group.get('properties').push(newSCP);
            newSCP.set('group', override.group);
            newSCP.set('isEditable', false);
          }
          var parentOverridesArray = serviceConfigProperty.get('overrides');
          if (parentOverridesArray == null) {
            parentOverridesArray = Ember.A([]);
            serviceConfigProperty.set('overrides', parentOverridesArray);
          }
          serviceConfigProperty.get('overrides').pushObject(newSCP);
          console.debug("createOverrideProperty(): Added:", newSCP, " to main-property:", serviceConfigProperty)
        }, this);
      } else {
        serviceConfigProperty.set('overrides', Ember.A([]));
      }
      if (App.get('isAdmin')) {
        if(defaultGroupSelected && !this.get('isHostsConfigsPage')){
          serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isReconfigurable'));
        } else {
          serviceConfigProperty.set('isEditable', false);
        }
      } else {
        serviceConfigProperty.set('isEditable', false);
      }
      componentConfig.get('configs').pushObject(serviceConfigProperty);
      serviceConfigProperty.validate();
    }, this);
    var overrideToAdd = this.get('overrideToAdd');
    if (overrideToAdd) {
      overrideToAdd = componentConfig.configs.findProperty('name', overrideToAdd.name);
      if (overrideToAdd) {
        this.addOverrideProperty(overrideToAdd);
        component.set('overrideToAdd', null);
      }
    }
  },
  /**
   *  Resolve dependency between configs.
   *  @param serviceName {String}
   *  @param configs {Mixed}
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

  resolveStormConfigs: function(configs) {
    var dependentConfigs, gangliaServerHost;
    dependentConfigs = ['nimbus.childopts', 'supervisor.childopts', 'worker.childopts'];
    // if Ganglia selected or installed, set ganglia host to configs
    if (this.get('installedServiceNames').contains('STORM') && this.get('installedServiceNames').contains('GANGLIA')) return;
    if (this.get('allSelectedServiceNames').contains('GANGLIA') || this.get('installedServiceNames').contains('GANGLIA')) {
      gangliaServerHost = this.get('wizardController').getDBProperty('masterComponentHosts').findProperty('component', 'GANGLIA_SERVER').hostName;
      dependentConfigs.forEach(function(configName) {
        var config = configs.findProperty('name', configName);
        var replaceStr = '.jar=host=';
        config.value = config.defaultValue = config.value.replace(replaceStr, replaceStr+gangliaServerHost);
        config.forceUpdate = true;
      }, this);
    }
  },

  /**
   * On load function
   */
  loadStep: function () {
    console.log("TRACE: Loading step7: Configure Services");
    if (!this.get('isAdvancedConfigLoaded')) {
      return;
    }
    this.clearStep();
    //STEP 1: Load advanced configs
    var advancedConfigs = this.get('content.advancedServiceConfig');
    //STEP 2: Load on-site configs by service from local DB
    var storedConfigs = this.get('content.serviceConfigProperties');
    //STEP 3: Merge pre-defined configs with loaded on-site configs
    var configs = App.config.mergePreDefinedWithStored(storedConfigs, advancedConfigs, this.get('selectedServiceNames').concat(this.get('installedServiceNames')));
    //STEP 4: Add advanced configs
    App.config.addAdvancedConfigs(configs, advancedConfigs);
    //STEP 5: Add custom configs
    App.config.addCustomConfigs(configs);
    //put properties from capacity-scheduler.xml into one config with textarea view
    if (this.get('allSelectedServiceNames').contains('YARN') && !App.supports.capacitySchedulerUi) {
      configs = App.config.fileConfigsIntoTextarea(configs, 'capacity-scheduler.xml');
    }
    this.set('groupsToDelete', this.get('wizardController').getDBProperty('groupsToDelete') || []);
    var localDB = {
      hosts: this.get('wizardController').getDBProperty('hosts'),
      masterComponentHosts: this.get('wizardController').getDBProperty('masterComponentHosts'),
      slaveComponentHosts: this.get('wizardController').getDBProperty('slaveComponentHosts')
    };
    if (this.get('wizardController.name') === 'addServiceController') {
      this.getConfigTags();
      this.setInstalledServiceConfigs(this.get('serviceConfigTags'), configs);
    }
    if (this.get('allSelectedServiceNames').contains('STORM') || this.get('installedServiceNames').contains('STORM')) {
      this.resolveServiceDependencyConfigs('STORM', configs);
    }
    //STEP 6: Distribute configs by service and wrap each one in App.ServiceConfigProperty (configs -> serviceConfigs)
    var serviceConfigs = App.config.renderConfigs(configs, storedConfigs, this.get('allSelectedServiceNames'), this.get('installedServiceNames'), localDB);
    if (this.get('wizardController.name') === 'addServiceController') {
      serviceConfigs.setEach('showConfig', true);
      serviceConfigs.setEach('selected', false);
      this.get('selectedServiceNames').forEach(function(serviceName) {
        if(!serviceConfigs.findProperty('serviceName', serviceName)) return;
        serviceConfigs.findProperty('serviceName', serviceName).set('selected', true);
      });

      // Remove SNameNode if HA is enabled
      if (App.get('isHaEnabled')) {
        configs = serviceConfigs.findProperty('serviceName', 'HDFS').configs;
        var removedConfigs = configs.filterProperty('category', 'SNameNode');
        removedConfigs.map(function(config) {
          configs = configs.without(config);
        });
        serviceConfigs.findProperty('serviceName', 'HDFS').configs = configs;
      }
    }

    this.set('stepConfigs', serviceConfigs);
    if (App.supports.hostOverridesInstaller) {
      this.loadConfigGroups(this.get('content.configGroups'));
      if (this.get('installedServiceNames').length > 0)
        this.loadInstalledServicesConfigGroups(this.get('installedServiceNames'));
    }
    this.activateSpecialConfigs();
    if (this.get('wizardController.name') === 'addServiceController') {
      this.set('selectedService', this.get('stepConfigs').filterProperty('selected', true).get('firstObject'));
    }
    else {
      this.set('selectedService', this.get('stepConfigs').filterProperty('showConfig', true).objectAt(0));
    }
    if (this.get('content.skipConfigStep')) {
      App.router.send('next');
    }
  },
  getConfigTags: function() {
    App.ajax.send({
      name: 'config.tags.sync',
      sender: this,
      success: 'getConfigTagsSuccess'
    });
  },

  getConfigTagsSuccess: function (data, opt, params) {
    var installedServiceSites = [];
    this.get('serviceConfigsData').filter(function (service) {
      if (this.get('installedServiceNames').contains(service.serviceName)){
        installedServiceSites = installedServiceSites.concat(service.sites);
      }
    }, this);
    installedServiceSites = installedServiceSites.uniq();
    var serviceConfigTags = [];
    for (var site in data.Clusters.desired_configs) {
      if (installedServiceSites.contains(site)) {
        serviceConfigTags.push({
          siteName: site,
          tagName: data.Clusters.desired_configs[site].tag,
          newTagName: null
        });
      }
    }
    this.set('serviceConfigTags', serviceConfigTags);
  },

  /**
   * set configs actual values from server
   * @param serviceConfigTags
   * @param configs
   */
  setInstalledServiceConfigs: function (serviceConfigTags, configs) {
    var configsMap = {};
    App.router.get('configurationController').getConfigsByTags(serviceConfigTags).forEach(function(configSite){
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

  setGroupsToDelete: function(groups) {
    var groupsToDelete = this.get('groupsToDelete');
    groups.forEach(function(group) {
      if (group.get('id'))
        groupsToDelete.push({
          id: group.get('id')
        });
    });
    this.get('wizardController').setDBProperty('groupsToDelete', groupsToDelete);
  },
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
      } else {
        var defaultGroup = App.ConfigGroup.create(serviceRawGroups.findProperty('isDefault'));
        var serviceGroups = service.get('configGroups');
        serviceRawGroups.filterProperty('isDefault', false).forEach(function (configGroup) {
          var readyGroup = App.ConfigGroup.create(configGroup);
          var wrappedProperties = [];
          readyGroup.get('properties').forEach(function(property){
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

  selectConfigGroup: function (event) {
    this.set('selectedConfigGroup', event.context);
  },

  /**
   * rebuild list of configs switch of config group:
   * on default - display all configs from default group and configs from non-default groups as disabled
   * on non-default - display all from default group as disabled and configs from selected non-default group
   */
  switchConfigGroupConfigs: function () {
    var serviceConfigs = this.get('selectedService.configs');
    var selectedGroup = this.get('selectedConfigGroup');
    var overrideToAdd = this.get('overrideToAdd');
    var isServiceInstalled = this.get('installedServiceNames').contains(this.get('selectedService.serviceName'));
    if(!selectedGroup) return;
    var displayedConfigGroups = (selectedGroup.get('isDefault')) ?
        this.get('selectedService.configGroups').filterProperty('isDefault', false) :
        [this.get('selectedConfigGroup')];
    var overrides = [];

    displayedConfigGroups.forEach(function (group) {
      overrides.pushObjects(group.get('properties'));
    });
    serviceConfigs.forEach(function (config) {
      var configOverrides = overrides.filterProperty('name', config.get('name'));
      var isEditable = config.get('isEditable');
      if (isServiceInstalled) {
        isEditable = (!isEditable && !config.get('isReconfigurable')) ? false : selectedGroup.get('isDefault');
      } else {
        isEditable = selectedGroup.get('isDefault');
      }
      config.set('isEditable', isEditable);
      if (overrideToAdd && overrideToAdd.get('name') === config.get('name')) {
        configOverrides.push(this.addOverrideProperty(config));
        this.set('overrideToAdd', null);
      }
      configOverrides.setEach('isEditable', !selectedGroup.get('isDefault'));
      configOverrides.setEach('parentSCP', config);
      config.set('overrides', configOverrides);
    }, this);
  }.observes('selectedConfigGroup'),
  /**
   * create overriden property and push it into Config group
   * @param serviceConfigProperty
   * @return {*}
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

  manageConfigurationGroup: function () {
    App.router.get('mainServiceInfoConfigsController').manageConfigurationGroups(this);
  },
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
   * make some configs visible depending on active services
   */
  activateSpecialConfigs: function () {
    var miscConfigs = this.get('stepConfigs').findProperty('serviceName', 'MISC').configs;
    miscConfigs = App.config.miscConfigVisibleProperty(miscConfigs, this.get('selectedServiceNames'));
  },

  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      App.router.send('next');
    }
  }, 
  
  /**
   * Provides service component name and display-name information for 
   * the current selected service. 
   */
  getCurrentServiceComponents: function () {
    var selectedServiceName = this.get('selectedService.serviceName');
    var masterComponents = this.get('content.masterComponentHosts');
    var slaveComponents = this.get('content.slaveComponentHosts');
    var scMaps = require('data/service_components');
    
    var validComponents = Ember.A([]);
    var seenComponents = {};
    masterComponents.forEach(function(component){
      var cn = component.component;
      var cdn = component.display_name;
      if(component.serviceId===selectedServiceName && !seenComponents[cn]){
        validComponents.pushObject(Ember.Object.create({
          componentName: cn,
          displayName: cdn,
          selected: false
        }));
        seenComponents[cn] = cn;
      }
    });
    slaveComponents.forEach(function(component){
      var cn = component.componentName;
      var cdn = component.displayName;
      var componentDef = scMaps.findProperty('component_name', cn);
      if(componentDef!=null && selectedServiceName===componentDef.service_name && !seenComponents[cn]){
        validComponents.pushObject(Ember.Object.create({
          componentName: cn,
          displayName: cdn,
          selected: false
        }));
        seenComponents[cn] = cn;
      }
    });
    return validComponents;
  }.property('content')

});
