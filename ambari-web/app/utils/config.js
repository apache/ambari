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

var serviceComponents = {};
var configGroupsByTag = [];
var globalPropertyToServicesMap = null;

App.config = Em.Object.create({

  preDefinedServiceConfigs: require('data/service_configs'),
  configMapping: require('data/config_mapping'),
  preDefinedConfigProperties: require('data/config_properties').configProperties,
  preDefinedCustomConfigs: require('data/custom_configs'),
  //categories which contain custom configs
  categoriesWithCustom: ['CapacityScheduler'],
  //configs with these filenames go to appropriate category not in Advanced
  customFileNames: function() {
    if (App.supports.capacitySchedulerUi) {
      return ['capacity-scheduler.xml', 'mapred-queue-acls.xml'];
    } else {
      return [];
    }
  }.property(''),
  /**
   * Cache of loaded configurations. This is useful in not loading
   * same configuration multiple times. It is populated in multiple
   * places.
   *
   * Example:
   * {
   *  'global_version1': {...},
   *  'global_version2': {...},
   *  'hdfs-site_version3': {...},
   * }
   */
  loadedConfigurationsCache: {},

  /**
   * Array of global "service/desired_tag/actual_tag" strings which
   * indicate different configurations. We cache these so that 
   * we dont have to recalculate if two tags are difference.
   */
  differentGlobalTagsCache:[],
  
  identifyCategory: function(config){
    var category = null;
    var serviceConfigMetaData = this.get('preDefinedServiceConfigs').findProperty('serviceName', config.serviceName);
    if (serviceConfigMetaData) {
      serviceConfigMetaData.configCategories.forEach(function (_category) {
        if (_category.siteFileNames && Array.isArray(_category.siteFileNames) && _category.siteFileNames.contains(config.filename)) {
          category = _category;
        }
      });
      category = (category == null) ? serviceConfigMetaData.configCategories.findProperty('siteFileName', config.filename) : category;
    }
    return category;
  },
  /**
   * additional handling for special properties such as
   * checkbox and digital which values with 'm' at the end
   * @param config
   */
  handleSpecialProperties: function(config){
    if (config.displayType === 'int' && /\d+m$/.test(config.value)) {
      config.value = config.value.slice(0, config.value.length - 1);
      config.defaultValue = config.value;
    }
    if (config.displayType === 'checkbox') {
      config.value = (config.value === 'true') ? config.defaultValue = true : config.defaultValue = false;
    }
  },
  /**
   * calculate config properties:
   * category, filename, isRequired, isUserProperty
   * @param config
   * @param isAdvanced
   * @param advancedConfigs
   */
  calculateConfigProperties: function(config, isAdvanced, advancedConfigs){
    if (!isAdvanced || this.get('customFileNames').contains(config.filename)) {
      var categoryMetaData = this.identifyCategory(config);
      if (categoryMetaData != null) {
        config.category = categoryMetaData.get('name');
        if(!isAdvanced) config.isUserProperty = true;
      }
    } else {
      config.category = 'Advanced';
      config.filename = isAdvanced && advancedConfigs.findProperty('name', config.name).filename;
      config.isRequired = true;
    }
  },
  /**
   * return:
   *   configs,
   *   globalConfigs,
   *   mappingConfigs
   *
   * @param configGroups
   * @param advancedConfigs
   * @param tags
   * @param serviceName
   * @return {object}
   */
  mergePreDefinedWithLoaded: function (configGroups, advancedConfigs, tags, serviceName) {
    var configs = [];
    var globalConfigs = [];
    var preDefinedConfigs = this.get('preDefinedConfigProperties');
    var mappingConfigs = [];

    tags.forEach(function (_tag) {
      var isAdvanced = null;
      var properties = configGroups.filter(function (serviceConfigProperties) {
        return _tag.tagName === serviceConfigProperties.tag && _tag.siteName === serviceConfigProperties.type;
      });

      properties = (properties.length) ? properties.objectAt(0).properties : {};
      for (var index in properties) {
        var configsPropertyDef = preDefinedConfigs.findProperty('name', index) || null;
        var serviceConfigObj = {
          name: index,
          value: properties[index],
          defaultValue: properties[index],
          filename: _tag.siteName + ".xml",
          isUserProperty: false,
          isOverridable: true,
          serviceName: serviceName
        };

        if (configsPropertyDef) {
          serviceConfigObj.displayType = configsPropertyDef.displayType;
          serviceConfigObj.isRequired = (configsPropertyDef.isRequired !== undefined) ? configsPropertyDef.isRequired : true;
          serviceConfigObj.isReconfigurable = (configsPropertyDef.isReconfigurable !== undefined) ? configsPropertyDef.isReconfigurable : true;
          serviceConfigObj.isVisible = (configsPropertyDef.isVisible !== undefined) ? configsPropertyDef.isVisible : true;
          serviceConfigObj.unit = (configsPropertyDef.unit !== undefined) ? configsPropertyDef.unit : undefined;
          serviceConfigObj.description = (configsPropertyDef.description !== undefined) ? configsPropertyDef.description : undefined;
          serviceConfigObj.isOverridable = configsPropertyDef.isOverridable === undefined ? true : configsPropertyDef.isOverridable;
          serviceConfigObj.serviceName = configsPropertyDef ? configsPropertyDef.serviceName : null;
          serviceConfigObj.index = configsPropertyDef.index;
        }
        // MAPREDUCE contains core-site properties but doesn't show them
        if(serviceConfigObj.serviceName === 'MAPREDUCE' && serviceConfigObj.filename === 'core-site.xml'){
          serviceConfigObj.isVisible = false;
        }
        if (_tag.siteName === 'global') {
          if (configsPropertyDef) {
            this.handleSpecialProperties(serviceConfigObj);
          }
          serviceConfigObj.id = 'puppet var';
          serviceConfigObj.displayName = configsPropertyDef ? configsPropertyDef.displayName : null;
          serviceConfigObj.category = configsPropertyDef ? configsPropertyDef.category : null;
          serviceConfigObj.options = configsPropertyDef ? configsPropertyDef.options : null;
          globalConfigs.push(serviceConfigObj);
        } else if (!this.get('configMapping').computed().someProperty('name', index)) {
          isAdvanced = advancedConfigs.someProperty('name', index);
          serviceConfigObj.id = 'site property';
          serviceConfigObj.displayType = 'advanced';
          serviceConfigObj.displayName = configsPropertyDef ? configsPropertyDef.displayName : index;
          this.calculateConfigProperties(serviceConfigObj, isAdvanced, advancedConfigs);
          configs.push(serviceConfigObj);
        } else {
          mappingConfigs.push(serviceConfigObj);
        }
      }
    }, this);
    return {
      configs: configs,
      globalConfigs: globalConfigs,
      mappingConfigs: mappingConfigs
    }
  },

  /**
   * merge stored configs with pre-defined
   * @param storedConfigs
   * @param advancedConfigs
   * @return {*}
   */
  mergePreDefinedWithStored: function (storedConfigs, advancedConfigs) {
    var mergedConfigs = [];
    var preDefinedConfigs = $.extend(true, [], this.get('preDefinedConfigProperties'));
    var preDefinedNames = [];
    var storedNames = [];
    var names = [];
    var categoryMetaData = null;
    storedConfigs = (storedConfigs) ? storedConfigs : [];

    preDefinedNames = this.get('preDefinedConfigProperties').mapProperty('name');
    storedNames = storedConfigs.mapProperty('name');
    names = preDefinedNames.concat(storedNames).uniq();
    names.forEach(function (name) {
      var stored = storedConfigs.findProperty('name', name);
      var preDefined = preDefinedConfigs.findProperty('name', name);
      var configData = {};
      var isAdvanced = advancedConfigs.someProperty('name', name);
      if (preDefined && stored) {
        configData = preDefined;
        configData.value = stored.value;
        configData.overrides = stored.overrides;
      } else if (!preDefined && stored) {
        configData = {
          id: stored.id,
          name: stored.name,
          displayName: stored.name,
          serviceName: stored.serviceName,
          value: stored.value,
          defaultValue: stored.defaultValue,
          displayType: "advanced",
          filename: stored.filename,
          category: 'Advanced',
          isUserProperty: stored.isUserProperty === true,
          isOverridable: true,
          overrides: stored.overrides,
          isRequired: true
        };
        this.calculateConfigProperties(configData, isAdvanced, advancedConfigs);
      } else if (preDefined && !stored) {
        configData = preDefined;
        if (isAdvanced) {
          configData.filename = advancedConfigs.findProperty('name', configData.name).filename;
        }
      }
      mergedConfigs.push(configData);
    }, this);
    return mergedConfigs;
  },
  /**
   * look over advanced configs and add missing configs to serviceConfigs
   * filter fetched configs by service if passed
   * @param serviceConfigs
   * @param advancedConfigs
   * @param serviceName
   */
  addAdvancedConfigs: function (serviceConfigs, advancedConfigs, serviceName) {
    serviceConfigs = (serviceName) ? serviceConfigs.filterProperty('serviceName', serviceName) : serviceConfigs;
    advancedConfigs.forEach(function (_config) {
      var configCategory = 'Advanced';
      var categoryMetaData = null;
      if (_config) {
        if (this.get('configMapping').computed().someProperty('name', _config.name)) {
        } else if (!(serviceConfigs.someProperty('name', _config.name))) {
          if(this.get('customFileNames').contains(_config.filename)){
            categoryMetaData = this.identifyCategory(_config);
            if (categoryMetaData != null) {
              configCategory = categoryMetaData.get('name');
            }
          }
          _config.id = "site property";
          _config.category = configCategory;
          _config.displayName = _config.name;
          _config.defaultValue = _config.value;
          // make all advanced configs optional and populated by default
          /*
           * if (/\${.*}/.test(_config.value) || (service.serviceName !==
           * 'OOZIE' && service.serviceName !== 'HBASE')) { _config.isRequired =
           * false; _config.value = ''; } else if
           * (/^\s+$/.test(_config.value)) { _config.isRequired = false; }
           */
          _config.isRequired = true;
          _config.isVisible = true;
          _config.displayType = 'advanced';
          serviceConfigs.push(_config);
        }
      }
    }, this);
  },
  /**
   * Render a custom conf-site box for entering properties that will be written in *-site.xml files of the services
   */
  addCustomConfigs: function (configs) {
    var preDefinedCustomConfigs = $.extend(true, [], this.get('preDefinedCustomConfigs'));
    var stored = configs.filter(function (_config) {
      if (this.get('categoriesWithCustom').contains(_config.category)) return true;
    }, this);
    var queueProperties = stored.filter(function (_config) {
      if ((_config.name.indexOf('mapred.capacity-scheduler.queue.') !== -1) ||
        (/mapred.queue.[a-z]([\_\-a-z0-9]{0,50}).acl-administer-jobs/i.test(_config.name)) ||
        (/mapred.queue.[a-z]([\_\-a-z0-9]{0,50}).acl-submit-job/i.test(_config.name))) {
        return true;
      }
    });
    if (queueProperties.length) {
      queueProperties.setEach('isQueue', true);
    } else {
      queueProperties = preDefinedCustomConfigs.filterProperty('isQueue');
      queueProperties.forEach(function (customConfig) {
        this.setDefaultQueue(customConfig, 'default');
        configs.push(customConfig);
      }, this);
    }
  },
  /**
   * set values to properties of queue
   * @param customConfig
   * @param queueName
   */
  setDefaultQueue: function (customConfig, queueName) {
    customConfig.name = customConfig.name.replace(/<queue-name>/, queueName);
    //default values of queue
    switch (customConfig.name) {
      case 'mapred.capacity-scheduler.queue.' + queueName + '.capacity':
        customConfig.value = '100';
        break;
      case 'mapred.capacity-scheduler.queue.' + queueName + '.maximum-capacity':
        customConfig.value = '100';
        break;
      case 'mapred.capacity-scheduler.queue.' + queueName + '.minimum-user-limit-percent':
        customConfig.value = '100';
        break;
      case 'mapred.capacity-scheduler.queue.' + queueName + '.user-limit-factor':
        customConfig.value = '1';
        break;
      case 'mapred.capacity-scheduler.queue.' + queueName + '.maximum-initialized-active-tasks':
        customConfig.value = '200000';
        break;
      case 'mapred.capacity-scheduler.queue.' + queueName + '.maximum-initialized-active-tasks-per-user':
        customConfig.value = '100000';
        break;
      case 'mapred.capacity-scheduler.queue.' + queueName + '.init-accept-jobs-factor':
        customConfig.value = '10';
        break;
      case 'mapred.capacity-scheduler.queue.' + queueName + '.supports-priority':
        customConfig.value = 'false';
        break;
      case 'mapred.queue.' + queueName + '.acl-submit-job':
        customConfig.value = '*';
        break;
      case 'mapred.queue.' + queueName + '.acl-administer-jobs':
        customConfig.value = '*';
        break;
    }
  },
  /**
   * render configs, distribute them by service
   * and wrap each in ServiceConfigProperty object
   * @param configs
   * @param allInstalledServiceNames
   * @param selectedServiceNames
   * @return {Array}
   */
  renderConfigs: function (configs, allInstalledServiceNames, selectedServiceNames) {
    var renderedServiceConfigs = [];
    var localDB = {
      hosts: App.db.getHosts(),
      masterComponentHosts: App.db.getMasterComponentHosts(),
      slaveComponentHosts: App.db.getSlaveComponentHosts()
    };
    var services = [];

    this.get('preDefinedServiceConfigs').forEach(function (serviceConfig) {
      if (allInstalledServiceNames.contains(serviceConfig.serviceName) || serviceConfig.serviceName === 'MISC') {
        console.log('pushing ' + serviceConfig.serviceName, serviceConfig);
        if (selectedServiceNames.contains(serviceConfig.serviceName) || serviceConfig.serviceName === 'MISC') {
          serviceConfig.showConfig = true;
        }
        services.push(serviceConfig);
      }
    });
    services.forEach(function (service) {
      var serviceConfig = {};
      var configsByService = [];
      var serviceConfigs = configs.filterProperty('serviceName', service.serviceName);
      serviceConfigs.forEach(function (_config) {
        var serviceConfigProperty = {};
        _config.isOverridable = (_config.isOverridable === undefined) ? true : _config.isOverridable;
        serviceConfigProperty = App.ServiceConfigProperty.create(_config);
        this.updateHostOverrides(serviceConfigProperty, _config);
        serviceConfigProperty.initialValue(localDB);
        serviceConfigProperty.validate();
        configsByService.pushObject(serviceConfigProperty);
      }, this);
      serviceConfig = this.createServiceConfig(service.serviceName);
      serviceConfig.set('showConfig', service.showConfig);
      serviceConfig.set('configs', configsByService);
      renderedServiceConfigs.push(serviceConfig);
    }, this);
    return renderedServiceConfigs;
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
        var hostsArray = Ember.A([]);
        overrideEntry.hosts.forEach(function (host) {
          hostsArray.push(host);
        });
        newSCP.set('selectedHostOptions', hostsArray);
        overrides.pushObject(newSCP);
      });
      configProperty.set('overrides', overrides);
    }
  },
  /**
   * create new ServiceConfig object by service name
   * @param serviceName
   */
  createServiceConfig: function (serviceName) {
    var preDefinedServiceConfig = App.config.preDefinedServiceConfigs.findProperty('serviceName', serviceName);
    var serviceConfig = App.ServiceConfig.create({
      filename: preDefinedServiceConfig.filename,
      serviceName: preDefinedServiceConfig.serviceName,
      displayName: preDefinedServiceConfig.displayName,
      configCategories: preDefinedServiceConfig.configCategories,
      configs: []
    });
    serviceConfig.configCategories.filterProperty('isCustomView', true).forEach(function (category) {
      switch (category.name) {
        case 'CapacityScheduler':
          category.set('customView', App.ServiceConfigCapacityScheduler);
          break;
      }
    }, this);
    return serviceConfig;
  },
  /**
   * GETs all cluster level sites in one call.
   *
   * @return Array of all site configs
   */
  loadConfigsByTags: function (tags) {
    var urlParams = [];
    tags.forEach(function (_tag) {
      urlParams.push('(type=' + _tag.siteName + '&tag=' + _tag.tagName + ')');
    });
    var params = urlParams.join('|');
    App.ajax.send({
      name: 'config.on-site',
      sender: this,
      data: {
        params: params
      },
      success: 'loadConfigsByTagsSuccess'
    });
    return configGroupsByTag;
  },

  loadConfigsByTagsSuccess: function (data) {
    if (data.items) {
      configGroupsByTag = [];
      data.items.forEach(function (item) {
        this.loadedConfigurationsCache[item.type + "_" + item.tag] = item.properties;
        configGroupsByTag.push(item);
      }, this);
    }
  },
  /**
   * Generate serviceProperties save it to localDB
   * called form stepController step6WizardController
   *
   * @param serviceName
   * @return {*}
   */
  loadAdvancedConfig: function (serviceName) {
    App.ajax.send({
      name: 'config.advanced',
      sender: this,
      data: {
        serviceName: serviceName,
        stack2VersionUrl: App.get('stack2VersionURL')
      },
      success: 'loadAdvancedConfigSuccess'
    });
    return serviceComponents[serviceName];
    //TODO clean serviceComponents
  },

  loadAdvancedConfigSuccess: function (data, opt, params) {
    console.log("TRACE: In success function for the loadAdvancedConfig; url is ", opt.url);
    var properties = [];
    if (data.items.length) {
      data.items.forEach(function (item) {
        item = item.StackConfigurations;
        properties.push({
          serviceName: item.service_name,
          name: item.property_name,
          value: item.property_value,
          description: item.property_description,
          filename: item.filename || item.type
        });
      }, this);
      serviceComponents[data.items[0].StackConfigurations.service_name] = properties;
    }
  },

  /**
   * Determine the map which shows which services
   * each global property effects.
   *
   * @return {*}
   * Example:
   * {
   *  'hive_pid_dir': ['HIVE'],
   *  ...
   * }
   */
  loadGlobalPropertyToServicesMap: function () {
    if (globalPropertyToServicesMap == null) {
      App.ajax.send({
        name: 'config.advanced.global',
        sender: this,
        data: {
          stack2VersionUrl: App.get('stack2VersionURL')
        },
        success: 'loadGlobalPropertyToServicesMapSuccess'
      });
    }
    return globalPropertyToServicesMap;
  },
  
  loadGlobalPropertyToServicesMapSuccess: function (data) {
    globalPropertyToServicesMap = {};
    if(data.items!=null){
      data.items.forEach(function(service){
        service.configurations.forEach(function(config){
          if("global.xml" === config.StackConfigurations.type){
            if(!(config.StackConfigurations.property_name in globalPropertyToServicesMap)){
              globalPropertyToServicesMap[config.StackConfigurations.property_name] = [];
            }
            globalPropertyToServicesMap[config.StackConfigurations.property_name].push(service.StackServices.service_name);
          }
        });
      });
    }
  },
  
  /**
   * When global configuration changes, not all services are effected
   * by all properties. This method determines if a given service
   * is effected by the difference in desired and actual configs.
   * 
   * This method might make a call to server to determine the actual
   * key/value pairs involved.
   */
  isServiceEffectedByGlobalChange: function (service, desiredTag, actualTag) {
    var effected = false;
    if (service != null && desiredTag != null && actualTag != null) {
      if(this.differentGlobalTagsCache.indexOf(service+"/"+desiredTag+"/"+actualTag) < 0){
        this.loadGlobalPropertyToServicesMap();
        var desiredConfigs = this.loadedConfigurationsCache['global_' + desiredTag];
        var actualConfigs = this.loadedConfigurationsCache['global_' + actualTag];
        var requestTags = [];
        if (!desiredConfigs) {
          requestTags.push({
            siteName: 'global',
            tagName: desiredTag
          });
        }
        if (!actualConfigs) {
          requestTags.push({
            siteName: 'global',
            tagName: actualTag
          });
        }
        if (requestTags.length > 0) {
          this.loadConfigsByTags(requestTags);
          desiredConfigs = this.loadedConfigurationsCache['global_' + desiredTag];
          actualConfigs = this.loadedConfigurationsCache['global_' + actualTag];
        }
        if (desiredConfigs != null && actualConfigs != null) {
          for ( var property in desiredConfigs) {
            if (!effected) {
              var dpv = desiredConfigs[property];
              var apv = actualConfigs[property];
              if (dpv !== apv && globalPropertyToServicesMap[property] != null) {
                effected = globalPropertyToServicesMap[property].indexOf(service) > -1;
                if(effected){
                  this.differentGlobalTagsCache.push(service+"/"+desiredTag+"/"+actualTag);
                }
              }
            }
          }
        }
      }else{
        effected = true; // We already know they are different
      }
    }
    return effected;
  },

  /**
   * Hosts can override service configurations per property. This method GETs
   * the overriden configurations and sets only the changed properties into
   * the 'overrides' of serviceConfig.
   *
   *
   */
  loadServiceConfigHostsOverrides: function (serviceConfigs, loadedHostToOverrideSiteToTagMap) {
    var configKeyToConfigMap = {};
    serviceConfigs.forEach(function (item) {
      configKeyToConfigMap[item.name] = item;
    });
    var typeTagToHostMap = {};
    var urlParams = [];
    for (var hostname in loadedHostToOverrideSiteToTagMap) {
      var overrideTypeTags = loadedHostToOverrideSiteToTagMap[hostname];
      for (var type in overrideTypeTags) {
        var tag = overrideTypeTags[type];
        typeTagToHostMap[type + "///" + tag] = hostname;
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
          typeTagToHostMap: typeTagToHostMap
        },
        success: 'loadServiceConfigHostsOverridesSuccess'
      });
    }
  },
  loadServiceConfigHostsOverridesSuccess: function (data, opt, params) {
    console.debug("loadServiceConfigHostsOverrides: Data=", data);
    data.items.forEach(function (config) {
      App.config.loadedConfigurationsCache[config.type + "_" + config.tag] = config.properties;
      var hostname = params.typeTagToHostMap[config.type + "///" + config.tag];
      var properties = config.properties;
      for (var prop in properties) {
        var serviceConfig = params.configKeyToConfigMap[prop];
        var hostOverrideValue = properties[prop];
        if (serviceConfig && serviceConfig.displayType === 'int') {
          if (/\d+m$/.test(hostOverrideValue)) {
            hostOverrideValue = hostOverrideValue.slice(0, hostOverrideValue.length - 1);
          }
        } else if (serviceConfig && serviceConfig.displayType === 'checkbox') {
          switch (hostOverrideValue) {
            case 'true':
              hostOverrideValue = true;
              break;
            case 'false':
              hostOverrideValue = false;
              break;
          }
        }
        if (serviceConfig) {
          // Value of this property is different for this host.
          var overrides = 'overrides';
          if (!(overrides in serviceConfig)) {
            serviceConfig.overrides = {};
          }
          if (!(hostOverrideValue in serviceConfig.overrides)) {
            serviceConfig.overrides[hostOverrideValue] = [];
          }
          console.log("loadServiceConfigHostsOverrides(): [" + hostname + "] OVERRODE(" + serviceConfig.name + "): " + serviceConfig.value + " -> " + hostOverrideValue);
          serviceConfig.overrides[hostOverrideValue].push(hostname);
        }
      }
    });
    console.log("loadServiceConfigHostsOverrides(): Finished loading.");
  }

});