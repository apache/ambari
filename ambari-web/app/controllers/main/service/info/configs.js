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

App.MainServiceInfoConfigsController = Em.Controller.extend({
  name: 'mainServiceInfoConfigsController',
  isHostsConfigsPage: false,
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
  allConfigGroupsNames: [],
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
   * During page load time, we get the host overrides from the server.
   * The current host -> site:tag map is stored below. This will be
   * useful during save, so that removals can also be determined.
   *
   * Example:
   * {
   *  'hostname1':{
   *    'global': 'version1',
   *    'core-site': 'version1',
   *    'hdfs-site', 'tag3187261938'
   *  }
   * }
   *
   * @see savedHostToOverrideSiteToTagMap
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
   * During page save time, we set the host overrides to the server.
   * The new host -> site:tag map is stored below. This will be
   * useful during save, to update the host's host components. Also,
   * it will be useful in deletion of overrides.
   *
   * Example:
   * {
   *  'hostname1': {
   *    'global': {
   *      'tagName': 'tag3187261938_hostname1',
   *      'map': {
   *        'hadoop_heapsize': '2048m'
   *      }
   *    }
   *  }
   * }
   *
   * @see loadedHostToOverrideSiteToTagMap
   */
  savedHostToOverrideSiteToTagMap: {},

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
    this.set('dataIsLoaded', false);
    this.set('filter', '');
    this.get('filterColumns').setEach('selected', false);
    this.get('stepConfigs').clear();
    this.get('globalConfigs').clear();
    this.get('uiConfigs').clear();
    this.get('customConfig').clear();
    this.set('loadedGroupToOverrideSiteToTagMap', {});
    this.set('savedHostToOverrideSiteToTagMap', {});
    this.set('savedSiteNameToServerServiceConfigDataMap', {});
    if (this.get('serviceConfigTags')) {
      this.set('serviceConfigTags', null);
    }
  },

  serviceConfigProperties: function () {
    return App.db.getServiceConfigProperties();
  }.property('content'),

  /**
   * On load function
   */
  loadStep: function () {
    console.log("TRACE: Loading configure for service");
    this.clearStep();
    this.loadServiceConfigs();
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
    var currentService = this.get('content.serviceName');
    var restartData = {
      hostAndHostComponents: {},
      propertyToHostAndComponent: {}
    };
    var self = this;
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
        serviceConfigsDef: this.get('serviceConfigs').findProperty('serviceName', this.get('content.serviceName'))
      },
      success: 'loadServiceTagsSuccess'
    });
  },

  loadServiceTagsSuccess: function (data, opt, params) {
    var serviceConfigsDef = params.serviceConfigsDef;
    var serviceName = this.get('content.serviceName');
    console.debug("loadServiceConfigs(): data=", data);
    // Create default configuration group
    var defaultConfigGroupHosts = App.Host.find().mapProperty('hostName');
    var selectedConfigGroup;
    var allConfigGroupsNames = ['Default'];
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
          allConfigGroupsNames.push(item.group_name);
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
        this.set('allConfigGroupsNames', allConfigGroupsNames);
      }
      this.set('configGroups', configGroups);
    }
    var defaultConfigGroup = App.ConfigGroup.create({
      name: "Default",
      description: "Default cluster level " + serviceName + " configuration",
      isDefault: true,
      hosts: defaultConfigGroupHosts,
      parentConfigGroup: null,
      service: this.get('content'),
      configSiteTags: []
    });
    if (!selectedConfigGroup) {
      selectedConfigGroup = defaultConfigGroup;
    }
    this.get('configGroups').push(defaultConfigGroup);
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
          var tag = siteTag.get('tag');
          this.loadedGroupToOverrideSiteToTagMap[groupName][site] = tag;
        }, this);
      }, this);
    }
    //STEP 2: Create an array of objects defining tag names to be polled and new tag names to be set after submit
    this.setServiceConfigTags(this.loadedClusterSiteToTagMap);
    //STEP 3: Load advanced configs from server
    var advancedConfigs = App.config.loadAdvancedConfig(serviceName) || [];
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
    //STEP 9: Load and add host override configs
    this.loadServiceConfigHostsOverrides(allConfigs, this.loadedGroupToOverrideSiteToTagMap, this.get('configGroups'));
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
      this.loadComponentConfigs(allConfigs, serviceConfig, restartData);
      this.get('stepConfigs').pushObject(serviceConfig);
    }
    this.set('selectedService', this.get('stepConfigs').objectAt(0));
    this.checkForSecureConfig(this.get('selectedService'));
    this.set('dataIsLoaded', true);
  }.observes('selectedConfigGroup'),

  loadServiceConfigHostsOverrides: function (allConfigs, loadedGroupToOverrideSiteToTagMap, configGroups) {
    App.config.loadServiceConfigHostsOverrides(allConfigs, loadedGroupToOverrideSiteToTagMap, configGroups);
  },

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
    var restartHosts = Ember.A([]);
    if (restartData != null && restartData.hostAndHostComponents != null && !jQuery.isEmptyObject(restartData.hostAndHostComponents)) {
      serviceConfig.set('restartRequired', true);
      for (var host in restartData.hostAndHostComponents) {
        hostsCount++;
        var componentsArray = Ember.A([]);
        for (var component in restartData.hostAndHostComponents[host]) {
          componentsArray.push(Ember.Object.create({name: App.format.role(component)}));
          hostComponentCount++;
        }
        var hostObj = App.Host.find(host);
        restartHosts.push(Ember.Object.create({hostData: hostObj, components: componentsArray}))
      }
      serviceConfig.set('restartRequiredHostsAndComponents', restartHosts);
      serviceConfig.set('restartRequiredMessage', 'Service needs ' + hostComponentCount + ' components on ' + hostsCount + ' hosts to be restarted.')
    }
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

    var slaveComponentHosts = [];
    var slaves = App.HostComponent.find().filterProperty('isSlave', true).map(function(item) {
      return Em.Object.create({
        host: item.get('host.hostName'),
        componentName: item.get('componentName')
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

    var masterComponentHosts = App.HostComponent.find().filterProperty('isMaster', true).map(function(item) {
      return {
        component: item.get('componentName'),
        serviceId: item.get('service.serviceName'),
        host: item.get('host.hostName')
      }
    });
    var hosts = {};
    App.Host.find().map(function(host) {
      hosts[host.get('hostName')] = {
        name: host.get('hostName'),
        cpu: host.get('cpu'),
        memory: host.get('memory'),
        disk_info: host.get('diskInfo')
      };
    });

    return {
      masterComponentHosts: masterComponentHosts,
      slaveComponentHosts: slaveComponentHosts,
      hosts: hosts
    };
  },

  /**
   * Load child components to service config object
   * @param configs
   * @param componentConfig
   * @param restartData
   */
  loadComponentConfigs: function (configs, componentConfig, restartData) {

    var localDB = this.getInfoForDefaults();
    var recommendedDefaults = {};
    var s = this.get('serviceConfigsData').findProperty('serviceName', this.get('content.serviceName'));
    var defaultGroupSelected = this.get('selectedConfigGroup.isDefault');
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


    configs.forEach(function (_serviceConfigProperty) {
      console.log("config", _serviceConfigProperty);
      if (!_serviceConfigProperty) return;
      var overrides = _serviceConfigProperty.overrides;
      // we will populate the override properties below
      _serviceConfigProperty.overrides = null;

      if (_serviceConfigProperty.isOverridable === undefined) {
        _serviceConfigProperty.isOverridable = true;
      }
      var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);
      var propertyName = serviceConfigProperty.get('name');
      if (restartData != null && propertyName in restartData.propertyToHostAndComponent) {
        serviceConfigProperty.set('isRestartRequired', true);
        var message = '<ul>';
        for (var host in restartData.propertyToHostAndComponent[propertyName]) {
          var appHost = App.Host.find(host);
          message += "<li>" + appHost.get('publicHostName');
          message += "<ul>";
          restartData.propertyToHostAndComponent[propertyName][host].forEach(function (comp) {
            message += "<li>" + App.format.role(comp) + "</li>"
          });
          message += "</ul></li>";
        }
        message += "</ul>";
        serviceConfigProperty.set('restartRequiredMessage', message);
      }
      if (serviceConfigProperty.get('serviceName') === this.get('content.serviceName')) {

        // Do not reset values when reconfiguring.
        // This might be useful to setting better descriptions
        // or default values sometime in the future.
        // defaults.forEach(function(defaults) {
        //   for(var name in defaults) {
        //    if (serviceConfigProperty.name == name) {
        //       serviceConfigProperty.set('value', defaults[name]);
        //       serviceConfigProperty.set('defaultValue', defaults[name]);
        //     }
        //   }
        // });

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
        for (var overridenValue in overrides) {
          var newSCP = App.ServiceConfigProperty.create(_serviceConfigProperty);
          newSCP.set('value', overridenValue);
          newSCP.set('isOriginalSCP', false); // indicated this is overridden value,
          newSCP.set('parentSCP', serviceConfigProperty);
          if (App.supports.hostOverrides && defaultGroupSelected) {
            newSCP.set('group', overrides[overridenValue]);
            newSCP.set('isEditable', false);
          }
          var parentOverridesArray = serviceConfigProperty.get('overrides');
          if (parentOverridesArray == null) {
            parentOverridesArray = Ember.A([]);
            serviceConfigProperty.set('overrides', parentOverridesArray);
          }
          parentOverridesArray.pushObject(newSCP);
          console.debug("createOverrideProperty(): Added:", newSCP, " to main-property:", serviceConfigProperty)
        }
      }
      // serviceConfigProperty.serviceConfig = componentConfig;
      if (App.supports.hostOverrides) {
        serviceConfigProperty.set('isEditable', defaultGroupSelected && !this.get('isHostsConfigsPage'));
      } else {
        if (App.get('isAdmin')) {
          serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isReconfigurable'));
        } else {
          serviceConfigProperty.set('isEditable', false);
        }
      }
      componentConfig.configs.pushObject(serviceConfigProperty);
      serviceConfigProperty.validate();
    }, this);
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
   * Determines which host components are running on each host.
   * @param status 'running' or 'unknown'
   * @return Returned in the following format:
   * {
   *  runningHosts: {
   *    'hostname1': 'NameNode, DataNode, JobTracker',
   *    'hostname2': 'DataNode',
   *  },
   *  runningComponentCount: 5
   * }
   */
  getHostComponentsByStatus: function (services, status) {
    var hosts = [];
    var componentCount = 0;
    var hostToIndexMap = {};
    services.forEach(function (service) {
      var hostComponents = (status == App.HostComponentStatus.started) ? service.get('runningHostComponents') : service.get('unknownHostComponents');
      if (hostComponents != null) {
        hostComponents.forEach(function (hc) {
          var hostName = hc.get('host.publicHostName');
          var componentName = hc.get('displayName');
          componentCount++;
          if (!(hostName in hostToIndexMap)) {
            hosts.push({
              name: hostName,
              components: ""
            });
            hostToIndexMap[hostName] = hosts.length - 1;
          }
          var hostObj = hosts[hostToIndexMap[hostName]];
          if (hostObj.components.length > 0)
            hostObj.components += ", " + componentName;
          else
            hostObj.components += componentName;
        });
        hosts.sort(function (a, b) {
          return a.name.localeCompare(b.name);
        });
      }
    });
    return {
      hosts: hosts,
      componentCount: componentCount
    };
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
    var runningHosts = null;
    var runningComponentCount = 0;
    var unknownHosts = null;
    var unknownComponentCount = 0;

    var dfd = $.Deferred();
    var self = this;
    var serviceName = this.get('content.serviceName');
    var displayName = this.get('content.displayName');

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
          var uhc;
          if (self.get('content.serviceName') !== 'HDFS' || (self.get('content.serviceName') === 'HDFS' && !App.Service.find().someProperty('id', 'MAPREDUCE'))) {
            uhc = self.getHostComponentsByStatus([self.get('content')], App.HostComponentStatus.unknown);
          } else {
            uhc = self.getHostComponentsByStatus([self.get('content'), App.Service.find('MAPREDUCE')], App.HostComponentStatus.unknown);
          }
          unknownHosts = uhc.hosts;
          unknownComponentCount = uhc.componentCount;
        } else {
          header = Em.I18n.t('common.failure');
          message = result.message;
          messageClass = 'alert alert-error';
          value = result.value;
        }
      });
    } else {
      var rhc;
      if (this.get('content.serviceName') !== 'HDFS' || (this.get('content.serviceName') === 'HDFS' && !App.Service.find().someProperty('id', 'MAPREDUCE'))) {
        rhc = this.getHostComponentsByStatus([this.get('content')], App.HostComponentStatus.started);
        header = Em.I18n.t('services.service.config.notSaved');
        message = Em.I18n.t('services.service.config.msgServiceStop');
      } else {
        rhc = this.getHostComponentsByStatus([this.get('content'), App.Service.find('MAPREDUCE')], App.HostComponentStatus.started);
        header = Em.I18n.t('services.service.config.notSaved');
        message = Em.I18n.t('services.service.config.msgHDFSMapRServiceStop');
      }
      messageClass = 'alert alert-error';
      runningHosts = rhc.hosts;
      runningComponentCount = rhc.componentCount;
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
          runningHosts: runningHosts,
          runningComponentCount: runningComponentCount,
          unknownHosts: unknownHosts,
          unknownComponentCount: unknownComponentCount,
          siteProperties: value,
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
        overridenConfigs = overridenConfigs.concat(config.get('overrides'));
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
   * @return {Array}
   */
  buildGroupDesiredConfigs: function (configs) {
    var sites = [];
    var time = (new Date).getTime();
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
      } else if (hiveDb.value === 'Existing MySQL Database') {
        var existingMySqlHost = globals.findProperty('name', 'hive_existing_mysql_host');
        if (existingMySqlHost) {
          existingMySqlHost.name = 'hive_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'hive_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'hive_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_oracle_database'));
      } else { //existing oracle database
        var existingOracleHost = globals.findProperty('name', 'hive_existing_oracle_host');
        if (existingOracleHost) {
          existingOracleHost.name = 'hive_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'hive_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'hive_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_mysql_database'));
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
      } else { //existing oracle database
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
    var allUiConfigs = this.loadUiSideConfigs(this.get('configMapping').all());
    this.set('uiConfigs', storedConfigs.concat(allUiConfigs));
  },

  formatConfigValues: function(serviceConfigProperties){
    serviceConfigProperties.forEach(function (_config) {
      if (typeof _config.get('value') === "boolean") _config.set('value', _config.value.toString());
      _config.set('value', App.config.trimProperty(_config), true);
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
      var valueWithOverrides = this.getGlobConfigValueWithOverrides(_config.templateName, _config.value, _config.name);
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
   * @param name
   * @return {
   *   value: '...',
   *   overrides: {
   *    'value1': [h1, h2],
   *    'value2': [h3]
   *   }
   * }
   */
  getGlobConfigValueWithOverrides: function (templateName, expression, name) {
    var express = expression.match(/<(.*?)>/g);
    var value = expression;
    if (express == null) {
      return { value: expression, overrides: {}};      // if site property do not map any global property then return the value
    }
    var overrideHostToValue = {};
    express.forEach(function (_express) {
      //console.log("The value of template is: " + _express);
      var index = parseInt(_express.match(/\[([\d]*)(?=\])/)[1]);
      if (this.get('globalConfigs').someProperty('name', templateName[index])) {
        //console.log("The name of the variable is: " + this.get('content.serviceConfigProperties').findProperty('name', templateName[index]).name);
        var globalObj = this.get('globalConfigs').findProperty('name', templateName[index]);
        var globValue = globalObj.value;
        // Hack for templeton.zookeeper.hosts
        var preReplaceValue = null;
        if (value !== null) {   // if the property depends on more than one template name like <templateName[0]>/<templateName[1]> then don't proceed to the next if the prior is null or not found in the global configs
          preReplaceValue = value;
          value = this._replaceConfigValues(name, _express, value, globValue);
        }
        if (globalObj.overrides != null) {
          for (var ov in globalObj.overrides) {
            var hostsArray = globalObj.overrides[ov];
            hostsArray.forEach(function (host) {
              if (!(host in overrideHostToValue)) {
                overrideHostToValue[host] = this._replaceConfigValues(name, _express, preReplaceValue, ov);
              } else {
                overrideHostToValue[host] = this._replaceConfigValues(name, _express, overrideHostToValue[host], ov);
              }
            }, this);
          }
        }
      } else {
        /*
         console.log("ERROR: The variable name is: " + templateName[index]);
         console.log("ERROR: mapped config from configMapping file has no corresponding variable in " +
         "content.serviceConfigProperties. Two possible reasons for the error could be: 1) The service is not selected. " +
         "and/OR 2) The service_config metadata file has no corresponding global var for the site property variable");
         */
        value = null;
      }
    }, this);

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

  _replaceConfigValues: function (name, express, value, globValue) {
    if (name === "templeton.zookeeper.hosts" || name === 'hbase.zookeeper.quorum') {
      var zooKeeperPort = '2181';
      if (typeof globValue === 'string') {
        var temp = [];
        temp.push(globValue);
        globValue = temp;
      }
      if (name === "templeton.zookeeper.hosts") {
        var temp = [];
        globValue.forEach(function (_host, index) {
          temp.push(globValue[index] + ':' + zooKeeperPort);
        }, this);
        globValue = temp;
      }
      value = value.replace(express, globValue.toString());
    } else {
      value = value.replace(express, globValue);
    }
    return value;
  },

  /**
   * Saves cluster level configurations for all necessary sites.
   * PUT calls are made to /api/v1/clusters/clusterName for each site.
   *
   * @return {Boolean}
   */
  doPUTClusterConfigurations: function () {
    var result = true;
    var serviceConfigTags = this.get('serviceConfigTags');
    this.setNewTagNames(serviceConfigTags);
    var siteNameToServerDataMap = {};
    var configController = App.router.get('configurationController');

    serviceConfigTags.forEach(function (_serviceTags) {
      if (_serviceTags.siteName === 'global') {
        console.log("TRACE: Inside global");
        var serverGlobalConfigs = this.createGlobalSiteObj(_serviceTags.newTagName, this.get('globalConfigs'));
        siteNameToServerDataMap['global'] = serverGlobalConfigs;
        var loadedProperties = configController.getConfigsByTags([{siteName: 'global', tagName: this.loadedClusterSiteToTagMap['global']}]);
        if (loadedProperties && loadedProperties[0]) {
          loadedProperties = loadedProperties[0].properties;
        }
        if (this.isConfigChanged(loadedProperties, serverGlobalConfigs.properties)) {
          result = result && this.doPUTClusterConfigurationSite(serverGlobalConfigs);
        }
      } else if (_serviceTags.siteName === 'core-site') {
        console.log("TRACE: Inside core-site");
        if (this.get('content.serviceName') === 'HDFS' || this.get('content.serviceName') === 'HCFS') {
          var coreSiteConfigs = this.createCoreSiteObj(_serviceTags.newTagName);
          siteNameToServerDataMap['core-site'] = coreSiteConfigs;
          var loadedProperties = configController.getConfigsByTags([{siteName: 'core-site', tagName: this.loadedClusterSiteToTagMap['core-site']}]);
          if (loadedProperties && loadedProperties[0]) {
            loadedProperties = loadedProperties[0].properties;
          }
          if (this.isConfigChanged(loadedProperties, coreSiteConfigs.properties)) {
            result = result && this.doPUTClusterConfigurationSite(coreSiteConfigs);
          }
        }
      } else {
        var siteConfigs = this.get('uiConfigs').filterProperty('filename', _serviceTags.siteName + '.xml');
        var serverConfigs = this.createSiteObj(_serviceTags.siteName, _serviceTags.newTagName, siteConfigs);
        siteNameToServerDataMap[_serviceTags.siteName] = serverConfigs;
        var loadedProperties = configController.getConfigsByTags([{siteName: _serviceTags.siteName, tagName: this.loadedClusterSiteToTagMap[_serviceTags.siteName]}]);
        if (loadedProperties && loadedProperties[0]) {
          loadedProperties = loadedProperties[0].properties;
        }
        if (this.isConfigChanged(loadedProperties, serverConfigs.properties)) {
          result = result && this.doPUTClusterConfigurationSite(serverConfigs);
        }
      }
    }, this);
    this.savedSiteNameToServerServiceConfigDataMap = siteNameToServerDataMap;
    return result;
  },


  /**
   * Compares the loaded config values with the saving config values.
   */
  isConfigChanged: function (loadedConfig, savingConfig) {
    var changed = false;
    if (loadedConfig != null && savingConfig != null) {
      var seenLoadKeys = [];
      for (var loadKey in loadedConfig) {
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
   */
  doPUTClusterConfigurationSite: function (data) {
    var result;
    var url = this.getUrl('', '');
    var clusterData = {
      Clusters: {
        desired_config: data
      }
    };
    console.log("applyClusterConfigurationToSite(): PUTting data:", clusterData);
    $.ajax({
      type: 'PUT',
      url: url,
      async: false,
      dataType: 'text',
      data: JSON.stringify(clusterData),
      timeout: 5000,
      success: function (data) {
        console.log("applyClusterConfigurationToSite(): In success for data:", data);
        result = true;
      },
      error: function (request, ajaxOptions, error) {
        console.log('applyClusterConfigurationToSite(): ERROR:', request.responseText, ", error=", error);
        result = false;
      },
      statusCode: require('data/statusCodes')
    });
    console.log("applyClusterConfigurationToSite(): Exiting with result=" + result);
    return result;
  },


  /**
   * Creates host level overrides for service configuration.
   *
   */
  doPUTHostOverridesConfigurationSites: function () {
    var singlePUTHostData = [];
    var savedHostSiteArray = [];
    for (var host in this.savedHostToOverrideSiteToTagMap) {
      for (var siteName in this.savedHostToOverrideSiteToTagMap[host]) {
        var tagName = this.savedHostToOverrideSiteToTagMap[host][siteName].tagName;
        var map = this.savedHostToOverrideSiteToTagMap[host][siteName].map;
        savedHostSiteArray.push(host + "///" + siteName);
        singlePUTHostData.push({
          RequestInfo: {
            query: 'Hosts/host_name=' + host
          },
          Body: {
            Hosts: {
              desired_config: {
                type: siteName,
                tag: tagName,
                properties: map
              }
            }
          }
        });
      }
    }
    // Now cleanup removed overrides
    for (var loadedHost in this.loadedGroupToOverrideSiteToTagMap) {
      for (var loadedSiteName in this.loadedGroupToOverrideSiteToTagMap[loadedHost]) {
        if (!(savedHostSiteArray.contains(loadedHost + "///" + loadedSiteName))) {
          // This host-site combination was loaded, but not saved.
          // Meaning it is not needed anymore. Hence send a DELETE command.
          singlePUTHostData.push({
            RequestInfo: {
              query: 'Hosts/host_name=' + loadedHost
            },
            Body: {
              Hosts: {
                desired_config: {
                  type: loadedSiteName,
                  tag: this.loadedGroupToOverrideSiteToTagMap[loadedHost][loadedSiteName],
                  selected: false
                }
              }
            }
          });
        }
      }
    }
    console.debug("createHostOverrideConfigSites(): PUTting host-overrides. Data=", singlePUTHostData);
    if (singlePUTHostData.length > 0) {
      var url = this.getUrl('', '/hosts');
      var hostOverrideResult = true;
      $.ajax({
        type: 'PUT',
        url: url,
        data: JSON.stringify(singlePUTHostData),
        async: false,
        dataType: 'text',
        timeout: 5000,
        success: function (data) {
          var jsonData = jQuery.parseJSON(data);
          hostOverrideResult = true;
          console.log("createHostOverrideConfigSites(): SUCCESS:", url, ". RESPONSE:", jsonData);
        },
        error: function (request, ajaxOptions, error) {
          hostOverrideResult = false;
          console.log("createHostOverrideConfigSites(): ERROR:", url, ". RESPONSE:", request.responseText);
        },
        statusCode: require('data/statusCodes')
      });
      return hostOverrideResult;
    }
    return true;
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
   * @param tagName
   * @return {Object}
   */
  createGlobalSiteObj: function (tagName, globalConfigs) {
    var heapsizeException = ['hadoop_heapsize', 'yarn_heapsize', 'nodemanager_heapsize', 'resourcemanager_heapsize'];
    var globalSiteProperties = {};
    globalConfigs.forEach(function (_globalSiteObj) {
      // do not pass any globalConfigs whose name ends with _host or _hosts
      if (!/_hosts?$/.test(_globalSiteObj.name)) {
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

  recordHostOverride: function (serviceConfigObj, siteName, tagName, self) {
    var overrides = null;
    var name = '';
    if ('get' in serviceConfigObj) {
      overrides = serviceConfigObj.get('overrides');
      name = serviceConfigObj.get('name');
    } else {
      overrides = serviceConfigObj.overrides;
      name = serviceConfigObj.name;
    }
    if(overrides){
      if('get' in overrides) {
        overrides.forEach(function (override) {
          override.get('selectedHostOptions').forEach(function (host) {
            var value = override.get('value');
            self._recordHostOverride(value, host, name, siteName, tagName, self);
          });
        });
      } else {
        for (var value in overrides) {
          overrides[value].forEach(function (host) {
            self._recordHostOverride(value, host, name, siteName, tagName, self);
          });
        }
      }
    }
  },

  /**
   * Records all the host overrides per site/tag
   */

  _recordHostOverride: function(value, host, serviceConfigObjName, siteName, tagName, self) {
    if (!(host in self.savedHostToOverrideSiteToTagMap)) {
      self.savedHostToOverrideSiteToTagMap[host] = {};
    }
    if (!(siteName in self.savedHostToOverrideSiteToTagMap[host])) {
      self.savedHostToOverrideSiteToTagMap[host][siteName] = {};
      self.savedHostToOverrideSiteToTagMap[host][siteName].map = {};
    }
    var finalTag = tagName + '_' + host;
    console.log("recordHostOverride(): Saving host override for host=" + host + ", site=" + siteName + ", tag=" + finalTag + ", (key,value)=(" + serviceConfigObjName + "," + value + ")");
    self.savedHostToOverrideSiteToTagMap[host][siteName].tagName = finalTag;
    self.savedHostToOverrideSiteToTagMap[host][siteName].map[serviceConfigObjName] = value;
  },

  /**
   * create core site object
   * @param tagName
   * @return {Object}
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
   * @return {Object}
   */
  createSiteObj: function (siteName, tagName, siteObj) {
    var siteProperties = {};
    siteObj.forEach(function (_siteObj) {
      siteProperties[_siteObj.name] = App.config.escapeXMLCharacters(_siteObj.value);
      //this.recordHostOverride(_siteObj, siteName, tagName, this);
    }, this);
    return {"type": siteName, "tag": tagName, "properties": siteProperties};
  },

  /**
   * Set display names of the property from the puppet/global names
   * @param: displayNames: a field to be set with displayNames
   * @param names: array of property puppet/global names
   */
  setPropertyDisplayNames: function (displayNames, names) {
    var stepConfigs = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).configs;
    names.forEach(function (_name, index) {
      if (stepConfigs.someProperty('name', _name)) {
        displayNames.push(stepConfigs.findProperty('name', _name).displayName);
      }
    }, this);
  },

  /**
   * Set property of the site variable
   */
  setSiteProperty: function (key, value, filename) {
    if (filename === 'core-site.xml' && this.get('uiConfigs').filterProperty('filename', 'core-site.xml').someProperty('name', key)) {
      this.get('uiConfigs').filterProperty('filename', 'core-site.xml').findProperty('name', key).value = value;
      return;
    }
    this.get('uiConfigs').pushObject({
      "id": "site property",
      "name": key,
      "value": value,
      "filename": filename
    });
  },

  /**
   * return either specific url for request if testMode is false or testUrl
   * @param testUrl
   * @param url
   * @return {*}
   */
  getUrl: function (testUrl, url) {
    return (App.testMode) ? testUrl : App.apiPrefix + '/clusters/' + App.router.getClusterName() + url;
  },

  /**
   * Adds host name of master component to global config;
   */
  addHostNamesToGlobalConfig: function () {
    var serviceName = this.get('content.serviceName');
    var globalConfigs = this.get('globalConfigs');
    var serviceConfigs = this.get('serviceConfigs').findProperty('serviceName', serviceName).configs;
    //namenode_host is required to derive "fs.default.name" a property of core-site
    var nameNodeHost = this.get('serviceConfigs').findProperty('serviceName', 'HDFS').configs.findProperty('name', 'namenode_host');
    try {
      nameNodeHost.defaultValue = App.Service.find('HDFS').get('hostComponents').filterProperty('componentName', 'NAMENODE').mapProperty('host.hostName');
      globalConfigs.push(nameNodeHost);
    } catch (err) {
      console.log("No NameNode Host available.  This is expected if you're using HCFS rather than HDFS.");
    }

    //zooKeeperserver_host
    var zooKeperHost = this.get('serviceConfigs').findProperty('serviceName', 'ZOOKEEPER').configs.findProperty('name', 'zookeeperserver_hosts');
    if (serviceName === 'ZOOKEEPER' || serviceName === 'HBASE' || serviceName === 'WEBHCAT') {
      zooKeperHost.defaultValue = App.Service.find('ZOOKEEPER').get('hostComponents').filterProperty('componentName', 'ZOOKEEPER_SERVER').mapProperty('host.hostName');
      globalConfigs.push(zooKeperHost);
    }

    switch (serviceName) {
      case 'HDFS':
        if (this.get('content.hostComponents').findProperty('componentName', 'SECONDARY_NAMENODE') && this.get('content.hostComponents').findProperty('componentName', 'SECONDARY_NAMENODE').get('workStatus') != 'MAINTENANCE') {
          var sNameNodeHost = serviceConfigs.findProperty('name', 'snamenode_host');
          sNameNodeHost.defaultValue = this.get('content.hostComponents').findProperty('componentName', 'SECONDARY_NAMENODE').get('host.hostName');
          globalConfigs.push(sNameNodeHost);
        }
        break;
      case 'MAPREDUCE':
        var jobTrackerHost = serviceConfigs.findProperty('name', 'jobtracker_host');
        jobTrackerHost.defaultValue = this.get('content.hostComponents').findProperty('componentName', 'JOBTRACKER').get('host.hostName');
        globalConfigs.push(jobTrackerHost);
        break;
      case 'MAPREDUCE2':
        var historyServerHost = serviceConfigs.findProperty('name', 'hs_host');
        historyServerHost.defaultValue = this.get('content.hostComponents').findProperty('componentName', 'HISTORYSERVER').get('host.hostName');
        globalConfigs.push(historyServerHost);
        break;
      case 'YARN':
        var resourceManagerHost = serviceConfigs.findProperty('name', 'rm_host');
        resourceManagerHost.defaultValue = this.get('content.hostComponents').findProperty('componentName', 'RESOURCEMANAGER').get('host.hostName');
        globalConfigs.push(resourceManagerHost);
        //yarn.log.server.url config dependent on HistoryServer host
        if (App.HostComponent.find().someProperty('componentName', 'HISTORYSERVER')) {
          historyServerHost = this.get('serviceConfigs').findProperty('serviceName', 'MAPREDUCE2').configs.findProperty('name', 'hs_host');
          historyServerHost.defaultValue = App.HostComponent.find().findProperty('componentName', 'HISTORYSERVER').get('host.hostName');
          globalConfigs.push(historyServerHost);
        }
        break;
      case 'HIVE':
        var hiveMetastoreHost = serviceConfigs.findProperty('name', 'hivemetastore_host');
        hiveMetastoreHost.defaultValue = this.get('content.hostComponents').findProperty('componentName', 'HIVE_SERVER').get('host.hostName');
        globalConfigs.push(hiveMetastoreHost);
        var hiveDb = globalConfigs.findProperty('name', 'hive_database').value;
        if (['Existing MySQL Database', 'Existing Oracle Database'].contains(hiveDb)) {
          globalConfigs.findProperty('name', 'hive_hostname').isVisible = true;
        }
        break;

      case 'OOZIE':
        var oozieServerHost = serviceConfigs.findProperty('name', 'oozieserver_host');
        oozieServerHost.defaultValue = this.get('content.hostComponents').findProperty('componentName', 'OOZIE_SERVER').get('host.hostName');
        globalConfigs.push(oozieServerHost);
        var oozieDb = globalConfigs.findProperty('name', 'oozie_database').value;
        if (['Existing MySQL Database', 'Existing Oracle Database'].contains(oozieDb)) {
          globalConfigs.findProperty('name', 'oozie_hostname').isVisible = true;
        }
        break;
      case 'HBASE':
        var hbaseMasterHost = serviceConfigs.findProperty('name', 'hbasemaster_host');
        hbaseMasterHost.defaultValue = this.get('content.hostComponents').filterProperty('componentName', 'HBASE_MASTER').mapProperty('host.hostName');
        globalConfigs.push(hbaseMasterHost);
        break;
      case 'HUE':
        var hueServerHost = serviceConfigs.findProperty('name', 'hueserver_host');
        hueServerHost.defaultValue = this.get('content.hostComponents').findProperty('componentName', 'HUE_SERVER').get('host.hostName');
        globalConfigs.push(hueServerHost);
        break;
      case 'WEBHCAT':
        var webhcatMasterHost = serviceConfigs.findProperty('name', 'webhcatserver_host');
        webhcatMasterHost.defaultValue = this.get('content.hostComponents').filterProperty('componentName', 'WEBHCAT_SERVER').mapProperty('host.hostName');
        globalConfigs.push(webhcatMasterHost);
        var hiveMetastoreHost = this.get('serviceConfigs').findProperty('serviceName', 'HIVE').configs.findProperty('name', 'hivemetastore_host');
        hiveMetastoreHost.defaultValue = App.Service.find('HIVE').get('hostComponents').findProperty('componentName', 'HIVE_SERVER').get('host.hostName');
        globalConfigs.push(hiveMetastoreHost);
        break;
    }
  },

  /**
   * Provides service component name and display-name information for
   * the current selected service.
   */
  getCurrentServiceComponents: function () {
    var service = this.get('content');
    var components = service.get('hostComponents');
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

  getAllHosts: function () {
    return App.router.get('mainHostController.content');
  }.property('App.router.mainHostController.content'),

  doCancel: function () {
    this.loadStep();
  },

  restartComponents: function(e) {
    var commandName = "stop_component";
    if(e.context) {
      if(this.get('content.healthStatus') != 'green'){
        return;
      }
    }else {
      commandName = "start_component";
      if(this.get('content.healthStatus') != 'red'){
        return;
      }
    };
    var content = this;
    return App.ModalPopup.show({
      primary: Em.I18n.t('ok'),
      secondary: Em.I18n.t('common.cancel'),
      header: Em.I18n.t('popup.confirmation.commonHeader'),
      body: Em.I18n.t('question.sure'),
      content: content,
      onPrimary: function () {
        var selectedService = this.content.get('content.id');
        var hostComponents = App.HostComponent.find().filterProperty('service.id', selectedService).filterProperty('staleConfigs', true)
        hostComponents.forEach(function(item){
          var componentName = item.get('componentName');
          var hostName = item.get('host.hostName');
          App.ajax.send({
            name: 'config.stale.'+commandName,
            sender: this,
            data: {
              hostName: hostName,
              componentName: componentName,
              displayName: App.format.role(componentName)
            }
          });
        })
        this.hide();
        App.router.get('backgroundOperationsController').showPopup();
      },
      onSecondary: function () {
        this.hide();
      }
    });
  },

  showHostsShouldBeRestarted: function() {
    var hosts = [];
    for(var hostName in this.get('content.restartRequiredHostsAndComponents')) {
      hosts.push(hostName);
    }
    hosts = hosts.join(', ');
    this.showItemsShouldBeRestarted(hosts, Em.I18n.t('service.service.config.restartService.hostsShouldBeRestarted'));
  },
  showComponentsShouldBeRestarted: function() {
    var rhc = this.get('content.restartRequiredHostsAndComponents');
    var hostsComponets = [];
    for(var hostName in rhc) {
      rhc[hostName].forEach(function(hostComponent) {
        hostsComponets.push(hostComponent);
      })
    }
    hostsComponets = hostsComponets.join(', ');
    this.showItemsShouldBeRestarted(hostsComponets, Em.I18n.t('service.service.config.restartService.componentsShouldBeRestarted'));
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

  selectConfigGroup: function (event) {
    this.set('selectedConfigGroup', event.context);
  }
});
