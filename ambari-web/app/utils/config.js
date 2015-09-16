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
var stringUtils = require('utils/string_utils');
var configPropertyHelper = require('utils/configs/config_property_helper');

App.config = Em.Object.create({

  CONFIG_GROUP_NAME_MAX_LENGTH: 18,

  /**
   * filename exceptions used to support substandard sitenames which don't have "xml" extension
   * @type {string[]}
   */
  filenameExceptions: ['alert_notification'],

  preDefinedServiceConfigs: [],

  /**
   *
   * Returns file name version that stored on server.
   *
   * Example:
   *   App.config.getOriginalFileName('core-site') // returns core-site.xml
   *   App.config.getOriginalFileName('zoo.cfg') // returns zoo.cfg
   *
   * @param {String} fileName
   * @method getOriginalFileName
   **/
  getOriginalFileName: function (fileName) {
    if (/\.xml$/.test(fileName)) return fileName;
    return this.get('filenameExceptions').contains(fileName) ? fileName : fileName + '.xml';
  },

  /**
   * Check if Hive installation with new MySQL database created via Ambari is allowed
   * @param osFamily
   * @returns {boolean}
   */
  isManagedMySQLForHiveAllowed: function (osFamily) {
    var osList = ['redhat5', 'suse11'];
    return !osList.contains(osFamily);
  },

  /**
   *
   * Returns the configuration tagName from supplied filename
   *
   * Example:
   *   App.config.getConfigTagFromFileName('core-site.xml') // returns core-site
   *   App.config.getConfigTagFromFileName('zoo.cfg') // returns zoo.cfg
   *
   * @param {String} fileName
   * @method getConfigTagFromFileName
   **/
  getConfigTagFromFileName: function (fileName) {
    return fileName.endsWith('.xml') ? fileName.slice(0, -4) : fileName;
  },

  /**
   *
   * @param name
   * @param fileName
   * @returns {string}
   */
  configId: function(name, fileName) {
    return name + "__" + App.config.getConfigTagFromFileName(fileName);
  },

  setPreDefinedServiceConfigs: function (isMiscTabToBeAdded) {
    var configs = this.get('preDefinedSiteProperties');
    var services = [];
    var self = this;
    var stackServices = App.StackService.find().filterProperty('id');
    // Only include services that has configTypes related to them for service configuration page
    var servicesWithConfigTypes = stackServices.filter(function (service) {
      var configtypes = service.get('configTypes');
      return configtypes && !!Object.keys(configtypes).length;
    }, this);

    var allTabs;
    if (isMiscTabToBeAdded) {
      var nonServiceTab = require('data/service_configs');
      var miscService = nonServiceTab.findProperty('serviceName', 'MISC');
      var tagTypes = {};
      servicesWithConfigTypes.mapProperty('configTypes').forEach(function (configTypes) {
        for (var fileName in configTypes) {
          if (fileName.endsWith('-env') && !miscService.get('configTypes')[fileName]) {
            tagTypes[fileName] = configTypes[fileName];
          }
        }
      });
      miscService.set('configTypes', $.extend(miscService.get('configTypes'), tagTypes));
      allTabs = servicesWithConfigTypes.concat(nonServiceTab);
    } else {
      allTabs = servicesWithConfigTypes;
    }

    allTabs.forEach(function (service) {
      var configTypes = Em.keys(service.get('configTypes'));
      // filter properties by service name and service config types
      var serviceConfigs = configs.filterProperty('serviceName', service.get('serviceName')).filter(function(property) {
        var propFilename = self.getConfigTagFromFileName(Em.getWithDefault(property, 'filename', ''));
        if (propFilename && service.get('serviceName') != 'MISC') {
          return configTypes.contains(propFilename);
        }
        return true;
      });
      service.set('configs', serviceConfigs);
      services.push(service);
    });
    this.set('preDefinedServiceConfigs', services);
  },

  secureConfigs: require('data/HDP2/secure_mapping'),

  configMapping: require('data/HDP2/config_mapping'),

  customStackMapping: require('data/custom_stack_map'),

  mapCustomStack: function () {
    var
      baseStackFolder = App.get('currentStackName'),
      singMap = {
        "1": ">",
        "-1": "<",
        "0": "="
      };

    this.get('customStackMapping').every(function (stack) {
      if(stack.stackName == App.get('currentStackName')){
        var versionCompare = Em.compare(App.get('currentStackVersionNumber'), stack.stackVersionNumber);
        if(singMap[versionCompare+""] === stack.sign){
          baseStackFolder = stack.baseStackFolder;
          return false;
        }
      }
      return true;
    });

    return baseStackFolder;
  },

  allPreDefinedSiteProperties: function() {
    var sitePropertiesForCurrentStack = this.preDefinedConfigFile(this.mapCustomStack(), 'site_properties');
    if (sitePropertiesForCurrentStack) {
      return sitePropertiesForCurrentStack.configProperties;
    } else if (App.get('isHadoop23Stack')) {
      return require('data/HDP2.3/site_properties').configProperties;
    } else if (App.get('isHadoop22Stack')) {
      return require('data/HDP2.2/site_properties').configProperties;
    } else {
      return require('data/HDP2/site_properties').configProperties;
    }
  }.property('App.isHadoop22Stack', 'App.isHadoop23Stack'),

  preDefinedSiteProperties: function () {
    var serviceNames = App.StackService.find().mapProperty('serviceName').concat('MISC');
    return this.get('allPreDefinedSiteProperties').filter(function(p) {
      return serviceNames.contains(p.serviceName);
    });
  }.property('allPreDefinedSiteProperties'),

  /**
   * map of <code>preDefinedSiteProperties</code> provide search by index
   * @type {object}
   */
  preDefinedSitePropertiesMap: function () {
    var map = {};

    this.get('preDefinedSiteProperties').forEach(function (c) {
      map[this.configId(c.name, c.filename)] = c;
    }, this);
    return map;
  }.property('preDefinedSiteProperties'),

  preDefinedConfigFile: function(folder, file) {
    try {
      return require('data/{0}/{1}'.format(folder, file));
    } catch (err) {
      // the file doesn't exist, which might be expected.
    }
  },

  //configs with these filenames go to appropriate category not in Advanced
  customFileNames: ['flume-conf.xml'],

  /**
   * identify category by filename of config
   * @param config
   * @return {object|null}
   */
  identifyCategory: function (config) {
    var category = null,
      serviceConfigMetaData = this.get('preDefinedServiceConfigs').findProperty('serviceName', Em.get(config, 'serviceName')),
      configCategories = (serviceConfigMetaData && serviceConfigMetaData.get('configCategories')) || [];

    if (Em.get(config, 'filename') && Em.get(config, 'filename').contains("env")) {
      if (Em.get(config, 'category')) {
        category = configCategories.findProperty("name", Em.get(config, 'category'));
      } else {
        configCategories.forEach(function (_category) {
          if (_category.name.contains(this.getConfigTagFromFileName(Em.get(config, 'filename')))) {
            category = _category;
          }
        }, this);
      }
    } else {
      configCategories.forEach(function (_category) {
        if (_category.siteFileNames && Array.isArray(_category.siteFileNames) && _category.siteFileNames.contains(Em.get(config, 'filename'))) {
          category = _category;
        }
      });
      category = Em.isNone(category) ? configCategories.findProperty('siteFileName', this.getOriginalFileName(Em.get(config, 'filename'))) : category;
    }
    return category;
  },

  /**
   * additional handling for special properties such as
   * checkbox and digital which values with 'm' at the end
   * @param config
   */
  handleSpecialProperties: function (config) {
    if (Em.get(config, 'displayType') === 'int' && /\d+m$/.test(Em.get(config, 'value') )) {
      Em.set(config, 'value', Em.get(config, 'value').slice(0, Em.get(config, 'value.length') - 1));
      Em.set(config, 'savedValue', Em.get(config, 'value'));
    }
  },

  /**
   * calculate config properties:
   * category, filename, description
   * @param config
   * @param isAdvanced
   * @param advancedProperty
   */
  calculateConfigProperties: function (config, isAdvanced, advancedProperty) {
    if (!isAdvanced || this.get('customFileNames').contains(Em.get(config, 'filename'))) {
      var categoryMetaData = this.identifyCategory(config);
      if (categoryMetaData != null) {
        Em.set(config, 'category', categoryMetaData.get('name'));
      }
    } else {
      var configType = this.getConfigTagFromFileName(Em.get(config, 'filename'));
      Em.set(config, 'category', Em.get(config, 'category') ? Em.get(config, 'category') : 'Advanced ' + configType);
    }
    if (advancedProperty) {
      Em.set(config, 'description', Em.get(advancedProperty, 'description'));
    }
  },

  /**
   * get service for current config type
   * @param {String} configType - config fileName without xml
   * @return App.StackService
   */
  getServiceByConfigType: function(configType) {
    return App.StackService.find().find(function(s) {
      return Object.keys(s.get('configTypes')).contains(configType);
    });
  },


  /**
   * generates config objects
   * @param configCategories
   * @param serviceName
   * @param selectedConfigGroup
   * @param canEdit
   * @returns {Array}
   */
  mergePredefinedWithSaved: function (configCategories, serviceName, selectedConfigGroup, canEdit) {
    var configs = [];

    configCategories.forEach(function (siteConfig) {
      var service = this.getServiceByConfigType(siteConfig.type);
      if (service && serviceName != 'MISC') {
        serviceName = service.get('serviceName');
      }
      var filename = App.config.getOriginalFileName(siteConfig.type);
      var attributes = siteConfig['properties_attributes'] || {};
      var finalAttributes = attributes.final || {};
      var properties = siteConfig.properties || {};

      for (var index in properties) {
        var id = this.configId(index, siteConfig.type);
        var configsPropertyDef = this.get('preDefinedSitePropertiesMap')[id];
        var advancedConfig = App.StackConfigProperty.find(id);
        var isStackProperty = !!advancedConfig.get('id') || !!configsPropertyDef;
        var template = this.createDefaultConfig(index, serviceName, filename, isStackProperty, configsPropertyDef);
        var serviceConfigObj = isStackProperty ? this.mergeStaticProperties(template, advancedConfig) : template;

        if (serviceConfigObj.isRequiredByAgent !== false) {
          var formattedValue = this.formatPropertyValue(serviceConfigObj, properties[index]);
          serviceConfigObj.value = serviceConfigObj.savedValue = formattedValue;
          serviceConfigObj.isFinal = serviceConfigObj.savedIsFinal = finalAttributes[index] === "true";
          serviceConfigObj.isEditable = this.getIsEditable(serviceConfigObj, selectedConfigGroup, canEdit);
          serviceConfigObj.isVisible = serviceConfigObj.isVisible !== false || serviceName === 'MISC';
          if (serviceName!='MISC' && serviceConfigObj.category === "Users and Groups") {
            serviceConfigObj.category = this.getDefaultCategory(advancedConfig, filename);
          }
          serviceConfigObj.serviceName = serviceName;
        }

        var serviceConfigProperty = App.ServiceConfigProperty.create(serviceConfigObj);
        serviceConfigProperty.validate();
        configs.push(serviceConfigProperty);
      }
    }, this);
    return configs;
  },

  /**
   * This method sets default values for config property
   * These property values has the lowest priority and can be overriden be stack/UI
   * config property but is used when such properties are absent in stack/UI configs
   * @param {string} name
   * @param {string} serviceName
   * @param {string} fileName
   * @param {boolean} definedInStack
   * @param {Object} [coreObject]
   * @returns {Object}
   */
  createDefaultConfig: function(name, serviceName, fileName, definedInStack, coreObject) {
    return $.extend({
      /** core properties **/
      name: name,
      filename: fileName,
      value: '',
      savedValue: null,
      isFinal: false,
      savedIsFinal: null,
      /** UI and Stack properties **/
      recommendedValue: null,
      recommendedIsFinal: null,
      supportsFinal: this.shouldSupportFinal(serviceName, fileName),
      serviceName: serviceName,
      displayName: this.getDefaultDisplayName(name, fileName),
      displayType: this.getDefaultDisplayType(name, fileName, coreObject ? coreObject.value : ''),
      description: null,
      category: this.getDefaultCategory(definedInStack, fileName),
      isSecureConfig: this.getIsSecure(name),
      showLabel: this.getDefaultIsShowLabel(name, fileName),
      isVisible: true,
      isUserProperty: !definedInStack,
      isRequired: definedInStack,
      group: null,
      id: 'site property',
      isRequiredByAgent:  true,
      isReconfigurable: true,
      unit: null,
      hasInitialValue: false,
      isOverridable: true,
      index: null,
      dependentConfigPattern: null,
      options: null,
      radioName: null,
      belongsToService: []
    }, coreObject);
  },

  /**
   * This method merge properties form <code>stackConfigProperty<code> which are taken from stack
   * with <code>UIConfigProperty<code> which are hardcoded on UI
   * @param coreObject
   * @param stackProperty
   * @param preDefined
   * @param [propertiesToSkip]
   * @param [preDefinedOnly]
   */
  mergeStaticProperties: function(coreObject, stackProperty, preDefined, propertiesToSkip, preDefinedOnly) {
    propertiesToSkip = propertiesToSkip || ['name', 'filename', 'value', 'savedValue', 'isFinal', 'savedIsFinal'];
    preDefinedOnly = preDefinedOnly || ['id'];
    for (var k in coreObject) {
      if (!propertiesToSkip.contains(k)) {
        coreObject[k] = this.getPropertyIfExists(k, coreObject[k], !preDefinedOnly.contains(k) ? stackProperty : null, preDefined);
      }
    }
    return coreObject;
  },

  /**
   * This method using for merging some properties from two objects
   * if property exists in <code>firstPriority<code> result will be it's property
   * else if property exists in <code>secondPriority<code> result will be it's property
   * otherwise <code>defaultValue<code> will be returned
   * @param {String} propertyName
   * @param {*} defaultValue=null
   * @param {Em.Object|Object} firstPriority
   * @param {Em.Object|Object} [secondPriority=null]
   * @returns {*}
   */
  getPropertyIfExists: function(propertyName, defaultValue, firstPriority, secondPriority) {
    if (firstPriority && !Em.isNone(Em.get(firstPriority, propertyName))) {
      return Em.get(firstPriority, propertyName);
    } else if (secondPriority && !Em.isNone(Em.get(secondPriority, propertyName))) {
      return Em.get(secondPriority, propertyName);
    } else {
      return defaultValue;
    }
  },

  /**
   * Get displayType for properties that has not defined value
   * @param name
   * @param type
   * @param value
   * @returns {string}
   */
  getDefaultDisplayType: function(name, type, value) {
    if (this.isContentProperty(name, type)) {
      return 'content';
    }
    return value && !stringUtils.isSingleLine(value) ? 'multiLine' : 'advanced';
  },

  /**
   * Get the default value of displayName
   * @param name
   * @param fileName
   * @returns {*}
   */
  getDefaultDisplayName: function(name, fileName) {
    return this.isContentProperty(name, fileName, ['-env']) ? this.getConfigTagFromFileName(fileName) + ' template' : name
  },

  /**
   * Get category for properties that has not defined value
   * @param stackConfigProperty
   * @param fileName
   * @returns {string}
   */
  getDefaultCategory: function(stackConfigProperty, fileName) {
    return (stackConfigProperty ? 'Advanced ' : 'Custom ') + this.getConfigTagFromFileName(fileName);
  },

  /**
   * Get isSecureConfig for properties that has not defined value
   * @param propertyName
   * @returns {boolean}
   */
  getIsSecure: function(propertyName) {
    return this.get('secureConfigs').mapProperty('name').contains(propertyName);
  },

  /**
   * Calculate isEditable rely on controller state selected group and config restriction
   * @param {Object} serviceConfigProperty
   * @param {Object} selectedConfigGroup
   * @param {boolean} canEdit
   * @returns {boolean}
   */
  getIsEditable: function(serviceConfigProperty, selectedConfigGroup, canEdit) {
    return canEdit && Em.get(selectedConfigGroup, 'isDefault') && Em.get(serviceConfigProperty, 'isReconfigurable')
  },

  /**
   *
   * @param name
   * @param fileName
   */
  getDefaultIsShowLabel: function(name, fileName) {
    return !this.isContentProperty(name, fileName) || this.isContentProperty(name, fileName, ['-env']);
  },

  /**
   * format property value depending on displayType
   * and one exception for 'kdc_type'
   * @param serviceConfigProperty
   * @param [originalValue]
   * @returns {*}
   */
  formatPropertyValue: function(serviceConfigProperty, originalValue) {
    var value = originalValue || Em.get(serviceConfigProperty, 'value'),
        displayType = Em.get(serviceConfigProperty, 'displayType') || Em.get(serviceConfigProperty, 'valueAttributes.type'),
        category = Em.get(serviceConfigProperty, 'category');
    switch (displayType) {
      case 'content':
      case 'advanced':
      case 'multiLine':
        return this.trimProperty({ displayType: displayType, value: value });
        break;
      case 'directories':
        if (['DataNode', 'NameNode'].contains(category)) {
          return value.split(',').sort().join(',');//TODO check if this code is used
        }
        break;
      case 'directory':
        if (['SNameNode'].contains(category)) {
          return value.split(',').sort()[0];//TODO check if this code is used
        }
        break;
      case 'masterHosts':
        if (typeof(value) == 'string') {
          return value.replace(/\[|]|'|&apos;/g, "").split(',');
        }
        break;
      case 'int':
        if (/\d+m$/.test(value) ) {
          return value.slice(0, value.length - 1);
        } else {
          var int = parseInt(value);
          return isNaN(int) ? "" : int.toString();
        }
        break;
      case 'float':
        var float = parseFloat(value);
        return isNaN(float) ? "" : float.toString();
    }
    if (Em.get(serviceConfigProperty, 'name') === 'kdc_type') {
      return App.router.get('mainAdminKerberosController.kdcTypesValues')[value];
    }
    return value;
  },

  /**
   * defines if property with <code>name<code> and <code>fileName<code>
   * are special content property. By default result will be true if property name is 'content'
   * and tag ends on '-env' or '-log4j', but some other tag endings can be passed in <code>tagEnds<code>
   * @param {string} name
   * @param {string} fileName
   * @param {string[]} [tagEnds]
   * @returns {boolean}
   */
  isContentProperty: function(name, fileName, tagEnds) {
    if (tagEnds && tagEnds.length) {
      //tagEnds = tagEnds || ['-env', '-log4j'];
      var  type = this.getConfigTagFromFileName(fileName);
      return name == 'content' && tagEnds.some(function(tagEnd) { return type.endsWith(tagEnd)});
    } else {
      return name == 'content';
    }
  },

  /**
   *
   * @param configs
   * @returns {Object[]}
   */
  sortConfigs: function(configs) {
    return configs.sort(function(a, b) {
      return Em.get(a, 'index') == Em.get(b, 'index') ? Em.get(a, 'name') > Em.get(b, 'name') : Em.get(a, 'index') > Em.get(b, 'index');
    });
  },

  /**
   * merge stored configs with pre-defined
   * @return {Array}
   */
  mergePreDefinedWithStack: function (selectedServiceNames) {
    var mergedConfigs = [];

    var uiPersistentProperties = [
      this.configId('oozie_hostname', 'oozie-env.xml'),
      this.configId('oozie_ambari_database', 'oozie-env.xml')
    ];
    var configTypes = App.StackService.find().filter(function(service) {
      return selectedServiceNames.contains(service.get('serviceName'));
    }).map(function(item) {
      return Em.keys(item.get('configTypes'));
    }).reduce(function(p,c) { return p.concat(c); }).concat(['cluster-env', 'alert_notification'])
      .uniq().compact().filter(function(configType) { return !!configType; });

    var predefinedIds = Object.keys(this.get('preDefinedSitePropertiesMap'));
    var stackIds = App.StackConfigProperty.find().filterProperty('isValueDefined').mapProperty('id');

    var configIds = stackIds.concat(predefinedIds).uniq();

    configIds.forEach(function(id) {

      var preDefined = this.get('preDefinedSitePropertiesMap')[id];
      var advanced = App.StackConfigProperty.find(id);

      var name = preDefined ? preDefined.name : advanced.get('name');
      var filename = preDefined ? preDefined.filename : advanced.get('filename');
      var isUIOnly = Em.getWithDefault(preDefined || {}, 'isRequiredByAgent', true) === false;
      /*
        Take properties that:
          - UI specific only, marked with <code>isRequiredByAgent: false</code>
          - Present in stack definition, mapped to <code>App.StackConfigProperty</code>
          - Related to configuration for alerts notification, marked with <code>filename: "alert_notification"</code>
          - Property that filename supported by Service's config type, <code>App.StackService:configType</code>
          - Property that not defined in stack but should be saved, see <code>uiPersistentProperties</code>
       */
      if (!(uiPersistentProperties.contains(id) || isUIOnly || advanced.get('id')) && filename != 'alert_notification') {
        return;
      }
      var serviceName = preDefined ? preDefined.serviceName : advanced.get('serviceName');
      if (configTypes.contains(this.getConfigTagFromFileName(filename))) {
        var configData = this.createDefaultConfig(name, serviceName, filename, true, preDefined || {});
        if (configData.recommendedValue) {
          configData.value = configData.recommendedValue;
        }

        if (advanced.get('id')) {
          configData = this.mergeStaticProperties(configData, advanced, null, ['name', 'filename']);
          var configValue = this.formatPropertyValue(advanced, advanced.get('value'));
          // for property which value is single/multiple spaces set single space as well
          configData.value = configData.recommendedValue = /^\s+$/.test("" + configValue) ? " " : configValue;
        }

        mergedConfigs.push(configData);
      }

    }, this);
    return mergedConfigs;
  },

  miscConfigVisibleProperty: function (configs, serviceToShow) {
    configs.forEach(function (item) {
      if (item.get('isVisible') && item.belongsToService && item.belongsToService.length) {
        if (item.get('belongsToService').contains('Cluster') && item.get('displayType') == 'user') {
          item.set('isVisible', true);
          return;
        }
        item.set("isVisible", item.belongsToService.some(function (cur) {
          return serviceToShow.contains(cur)
        }));
      }
    });
    return configs;
  },

  /**
   * render configs, distribute them by service
   * and wrap each in ServiceConfigProperty object
   * @param configs
   * @param storedConfigs
   * @param allSelectedServiceNames
   * @param installedServiceNames
   * @param localDB
   * @return {App.ServiceConfig[]}
   */
  renderConfigs: function (configs, storedConfigs, allSelectedServiceNames, installedServiceNames, localDB) {
    var renderedServiceConfigs = [];
    var services = [];

    this.get('preDefinedServiceConfigs').forEach(function (serviceConfig) {
      var serviceName = serviceConfig.get('serviceName');
      if (allSelectedServiceNames.contains(serviceName) || serviceName === 'MISC') {
        console.log('pushing ' + serviceName, serviceConfig);
        if (!installedServiceNames.contains(serviceName) || serviceName === 'MISC') {
          serviceConfig.set('showConfig', true);
        }
        services.push(serviceConfig);
      }
    });
    services.forEach(function (service) {
      var configsByService = [];
      var serviceConfigs = configs.filterProperty('serviceName', service.get('serviceName'));
      serviceConfigs.forEach(function (_config) {
        _config.isOverridable = (_config.isOverridable === undefined) ? true : _config.isOverridable;
        var serviceConfigProperty = App.ServiceConfigProperty.create(_config);
        this.updateHostOverrides(serviceConfigProperty, _config);
        if (!storedConfigs && !serviceConfigProperty.get('hasInitialValue')) {
          var hiveMetastoreUrisConfig = configs.filterProperty('filename', 'hive-site.xml').findProperty('name', 'hive.metastore.uris');
          var clientPortConfig = configs.filterProperty('filename', 'zoo.cfg.xml').findProperty('name', 'clientPort');
          var dependencies = {
            'hive.metastore.uris': hiveMetastoreUrisConfig && hiveMetastoreUrisConfig.recommendedValue,
            'clientPort': clientPortConfig && clientPortConfig.recommendedValue
          };
          configPropertyHelper.initialValue(serviceConfigProperty, localDB, dependencies);
        }
        if (storedConfigs && storedConfigs.filterProperty('name', _config.name).length && !!_config.filename) {
          var storedConfig = storedConfigs.filterProperty('name', _config.name).findProperty('filename', _config.filename);
          if (storedConfig) {
            serviceConfigProperty.set('recommendedValue', storedConfig.recommendedValue);
            serviceConfigProperty.set('value', storedConfig.value);
          }
        }
        this.tweakDynamicDefaults(localDB, serviceConfigProperty, _config);
        serviceConfigProperty.validate();
        configsByService.pushObject(serviceConfigProperty);
      }, this);
      var serviceConfig = this.createServiceConfig(service.get('serviceName'));
      serviceConfig.set('showConfig', service.get('showConfig'));
      serviceConfig.set('configs', configsByService);
      renderedServiceConfigs.push(serviceConfig);
    }, this);
    return renderedServiceConfigs;
  },

  /**
   Takes care of the "dynamic defaults" for the GLUSTERFS configs.  Sets
   some of the config defaults to previously user-entered data.
   **/
  tweakDynamicDefaults: function (localDB, serviceConfigProperty, config) {
    var firstHost;
    for (var host in localDB.hosts) {
      firstHost = host;
      break;
    }
    try {
      if (typeof(config.recommendedValue) == "string" && config.recommendedValue.indexOf("{firstHost}") >= 0) {
        serviceConfigProperty.set('value', serviceConfigProperty.value.replace(new RegExp("{firstHost}"), firstHost));
        serviceConfigProperty.set('recommendedValue', serviceConfigProperty.recommendedValue.replace(new RegExp("{firstHost}"), firstHost));
      }
    } catch (err) {
      // Nothing to worry about here, most likely trying indexOf on a non-string
    }
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
   * @param {string} serviceName
   * @param {App.ServiceConfigGroup[]} configGroups
   * @param {App.ServiceConfigProperty[]} configs
   * @param {Number} initConfigsLength
   * @return {App.ServiceConfig}
   * @method createServiceConfig
   */
  createServiceConfig: function (serviceName, configGroups, configs, initConfigsLength) {
    var preDefinedServiceConfig = App.config.get('preDefinedServiceConfigs').findProperty('serviceName', serviceName);
    return App.ServiceConfig.create({
      serviceName: preDefinedServiceConfig.get('serviceName'),
      displayName: preDefinedServiceConfig.get('displayName'),
      configCategories: preDefinedServiceConfig.get('configCategories'),
      configs: configs || [],
      configGroups: configGroups || [],
      initConfigsLength: initConfigsLength || 0
    });
  },

  /**
   * GETs all cluster level sites in one call.
   *
   * @return {$.ajax}
   */
  loadConfigsByTags: function (tags) {
    var urlParams = [];
    tags.forEach(function (_tag) {
      urlParams.push('(type=' + _tag.siteName + '&tag=' + _tag.tagName + ')');
    });
    var params = urlParams.join('|');
    return App.ajax.send({
      name: 'config.on_site',
      sender: this,
      data: {
        params: params
      }
    });
  },

  /**
   * Add additional properties to advanced property config object.
   * Additional logic based on `property_type`.
   *
   * @method advancedConfigIdentityData
   * @param {Object} config
   * @return {Object}
   */
  advancedConfigIdentityData: function (config) {
    var propertyData = {};
    var proxyUserGroupServices = ['HIVE', 'OOZIE', 'FALCON'];
    var nameToDisplayNameMap = {
      'smokeuser': 'Smoke Test User',
      'user_group': 'Hadoop Group',
      'mapred_user': 'MapReduce User',
      'zk_user': 'ZooKeeper User',
      'metadata_user': 'Atlas User',
      'ignore_groupsusers_create': 'Skip group modifications during install',
      'override_uid': 'Have Ambari manage UIDs'
    };
    var checkboxProperties = ['ignore_groupsusers_create', 'override_uid'];
    if (Em.isArray(config.property_type)) {
      if (config.property_type.contains('USER') || config.property_type.contains('ADDITIONAL_USER_PROPERTY') || config.property_type.contains('GROUP')) {
        propertyData.category = 'Users and Groups';
        propertyData.isVisible = !App.get('isHadoopWindowsStack');
        propertyData.serviceName = 'MISC';
        propertyData.isOverridable = false;
        propertyData.isReconfigurable = false;
        propertyData.displayName = nameToDisplayNameMap[config.property_name] || App.format.normalizeName(config.property_name);
        propertyData.displayType = checkboxProperties.contains(config.property_name) ? 'checkbox' : 'user';
        if (config.property_type.contains('ADDITIONAL_USER_PROPERTY')) {
          propertyData.index = 999;
        } else if (config.service_name) {
          var propertyIndex = config.service_name == 'MISC' ? 30 : App.StackService.find().mapProperty('serviceName').indexOf(config.service_name);
          propertyData.belongsToService = [config.service_name];
          propertyData.index = propertyIndex;
        } else {
          propertyData.index = 30;
        }
        if (config.property_name == 'proxyuser_group') propertyData.belongsToService = proxyUserGroupServices;
      }
      if (config.property_type.contains('PASSWORD')) {
        propertyData.displayType = "password";
      }
    }

    return propertyData;
  },

  /**
   * Get config types and config type attributes from stack service
   *
   * @param service
   * @return {object}
   */
  getConfigTypesInfoFromService: function (service) {
    var configTypes = service.get('configTypes');
    var configTypesInfo = {
      items: [],
      supportsFinal: []
    };
    if (configTypes) {
      for (var key in configTypes) {
        if (configTypes.hasOwnProperty(key)) {
          configTypesInfo.items.push(key);
          if (configTypes[key].supports && configTypes[key].supports.final === "true") {
            configTypesInfo.supportsFinal.push(key);
          }
        }
      }
    }
    return configTypesInfo;
  },

  /**
   * Get properties from server by type and tag with properties, that belong to group
   * push them to common {serviceConfigs} and call callback function
   */
  loadServiceConfigGroupOverrides: function (serviceConfigs, loadedGroupToOverrideSiteToTagMap, configGroups, callback, sender) {
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
          callback: callback,
          sender: sender,
          serviceConfigs: serviceConfigs
        },
        success: 'loadServiceConfigGroupOverridesSuccess'
      });
    } else {
      callback.call(sender, serviceConfigs);
    }
  },

  loadServiceConfigGroupOverridesSuccess: function (data, opt, params) {
    data.items.forEach(function (config) {
      var group = params.typeTagToGroupMap[config.type + "///" + config.tag];
      var properties = config.properties;
      for (var prop in properties) {
        var fileName = this.getOriginalFileName(config.type);
        var serviceConfig = !!params.configKeyToConfigMap[fileName] ? params.configKeyToConfigMap[fileName][prop] : false;
        var hostOverrideValue = this.formatOverrideValue(serviceConfig, properties[prop]);
        var hostOverrideIsFinal = !!(config.properties_attributes && config.properties_attributes.final && config.properties_attributes.final[prop]);
        if (serviceConfig) {
          // Value of this property is different for this host.
          if (!Em.get(serviceConfig, 'overrides')) Em.set(serviceConfig, 'overrides', []);
          console.log("loadServiceConfigGroupOverridesSuccess(): [" + group + "] OVERRODE(" + serviceConfig.name + "): " + serviceConfig.value + " -> " + hostOverrideValue);
          serviceConfig.overrides.pushObject({value: hostOverrideValue, group: group, isFinal: hostOverrideIsFinal});
        } else {
          params.serviceConfigs.push(this.createCustomGroupConfig(prop, config, group));
        }
      }
    }, this);
    params.callback.call(params.sender, params.serviceConfigs);
  },

  /**
   * Create config with non default config group. Some custom config properties
   * can be created and assigned to non-default config group.
   *
   * @param {String} propertyName - name of the property
   * @param {Object} config - config info
   * @param {Em.Object} group - config group to set
   * @param {Boolean} isEditable
   * @return {Object}
   **/
  createCustomGroupConfig: function (propertyName, config, group, isEditable) {
    var propertyValue = config.properties[propertyName];
    var propertyObject = {
      name: propertyName,
      displayName: propertyName,
      savedValue: propertyValue,
      value: propertyValue,
      displayType: stringUtils.isSingleLine(propertyValue) ? 'advanced' : 'multiLine',
      isSecureConfig: false,
      group: group,
      id: 'site property',
      serviceName: group.get('service.serviceName'),
      filename: this.getOriginalFileName(config.type),
      isUserProperty: true,
      isVisible: true,
      isOverridable: false
    };
    propertyObject.category = this.identifyCategory(propertyObject).name;
    if(isEditable == false) {
      propertyObject.isEditable = isEditable;
    }
    group.set('switchGroupTextShort', Em.I18n.t('services.service.config_groups.switchGroupTextShort').format(group.get('name')));
    group.set('switchGroupTextFull', Em.I18n.t('services.service.config_groups.switchGroupTextFull').format(group.get('name')));
    return App.ServiceConfigProperty.create(propertyObject);
  },

  /**
   * format value of override of config
   * @param serviceConfig
   * @param hostOverrideValue
   */
  formatOverrideValue: function (serviceConfig, hostOverrideValue) {
    if (serviceConfig && serviceConfig.displayType === 'int') {
      if (/\d+m$/.test(hostOverrideValue)) {
        return hostOverrideValue.slice(0, hostOverrideValue.length - 1);
      }
    } else if (serviceConfig &&
               serviceConfig.displayType === 'masterHosts' &&
               typeof hostOverrideValue === 'string') {
      try {
        var value = JSON.parse(hostOverrideValue.replace(/'/g, "\""));
        if (typeof value === 'object') {
          return value;
        }
      } catch(err) {
        console.error(err);
      }

    }
    return hostOverrideValue;
  },

  /**
   * Set all site property that are derived from other site-properties
   * Replace <foreignKey[0]>, <foreignKey[1]>, ... (in the name and value) to values from configs with names in foreignKey-array
   * Replace <templateName[0]>, <templateName[1]>, ... (in the value) to values from configs with names in templateName-array
   * Example:
   * <code>
   *  config: {
   *    name: "name.<foreignKey[0]>.name",
   *    foreignKey: ["name1"],
   *    templateName: ["name2"],
   *    value: "<foreignKey[0]><templateName[0]>"
   *  }
   * </code>
   * "<foreignKey[0]>" in the name will be replaced with value from config with name "name1" (this config will be found
   * in the mappedConfigs or allConfigs). New name will be set to the '_name'-property. If config with name "name1" won't
   * be found, updated config will be marked as skipped (<code>noMatchSoSkipThisConfig</code>-property is set to true)
   * "<templateName[0]>" in the value will be replace with value from config with name "name2" (it also will be found
   * in the mappedConfigs or allConfigs).
   *
   * @param {object[]} mappedConfigs
   * @param {object[]} allConfigs
   * @param {object} config
   * @method setConfigValue
   */
  setConfigValue: function (mappedConfigs, allConfigs, config) {
    var globalValue;
    if (config.value == null) {
      return;
    }
    var fkValue = config.value.match(/<(foreignKey.*?)>/g);
    var fkName = config.name.match(/<(foreignKey.*?)>/g);
    var templateValue = config.value.match(/<(templateName.*?)>/g);

    if (fkValue) {
      fkValue.forEach(function (_fkValue) {

        var index = parseInt(_fkValue.match(/\[([\d]*)(?=\])/)[1]);
        var cfk = config.foreignKey[index];
        var cFromMapped = mappedConfigs.findProperty('name', cfk);
        if (Em.isNone(cFromMapped)) {
          var cFromAll = allConfigs.findProperty('name', cfk);
          if (!Em.isNone(cFromAll)) {
            globalValue = Em.get(cFromAll, 'value') === '' ? Em.get(cFromAll, 'recommendedValue') : Em.get(cFromAll, 'value');
            config.value = config.value.replace(_fkValue, globalValue);
          }
        }
        else {
          globalValue = Em.get(cFromMapped, 'value');
          config.value = config.value.replace(_fkValue, globalValue);
        }
      });
    }

    // config._name - formatted name from original config name
    if (fkName) {
      fkName.forEach(function (_fkName) {

        var index = parseInt(_fkName.match(/\[([\d]*)(?=\])/)[1]);
        var cfk = config.foreignKey[index];
        var cFromMapped = mappedConfigs.findProperty('name', cfk);

        if (Em.isNone(cFromMapped)) {
          var cFromAll = allConfigs.findProperty('name', cfk);
          if (Em.isNone(cFromAll)) {
            config.noMatchSoSkipThisConfig = true;
          }
          else {
            globalValue = Em.get(cFromAll, 'value') === '' ? Em.get(cFromAll, 'recommendedValue') : Em.get(cFromAll, 'value');
            config._name = config.name.replace(_fkName, globalValue);
          }
        }
        else {
          globalValue = Em.get(cFromMapped, 'value');
          config._name = config.name.replace(_fkName, globalValue);
        }
      });
    }

    //For properties in the configMapping file having foreignKey and templateName properties.
    if (templateValue) {
      templateValue.forEach(function (_value) {
        var index = parseInt(_value.match(/\[([\d]*)(?=\])/)[1]);
        var cfk = config.templateName[index];
        var cFromAll = allConfigs.findProperty('name', cfk);
        if (Em.isNone(cFromAll)) {
          config.value = null;
        }
        else {
          var globalValue = Em.get(cFromAll, 'value');
          config.value = config.value.replace(_value, globalValue);
        }
      });
    }
  },

  /**
   * identify service name of config by its config's type
   * @param type
   * @return {string|null}
   */
  getServiceNameByConfigType: function (type) {
    var preDefinedServiceConfigs = this.get('preDefinedServiceConfigs');
    var service = preDefinedServiceConfigs.find(function (serviceConfig) {
      return !!serviceConfig.get('configTypes')[type];
    }, this);
    return service && service.get('serviceName');
  },

  /**
   * add user property
   * @param stored
   * @param isAdvanced
   * @param advancedConfigs
   * @return {Object}
   */
  addUserProperty: function (stored, isAdvanced, advancedConfigs) {
    var
      skipAttributeChanges = {
        displayType: ['ignore_groupsusers_create'],
        displayName: ['ignore_groupsusers_create', 'smokeuser', 'user_group', 'mapred_user', 'zk_user']
      },
      configData = {
        id: stored.id,
        name: stored.name,
        displayName: skipAttributeChanges.displayName.contains(stored.name) ?
          this.getOriginalConfigAttribute(stored, 'displayName', advancedConfigs) : stored.name,
        serviceName: stored.serviceName,
        value: stored.value,
        savedValue: stored.savedValue,
        recommendedValue: stored.recommendedValue,
        displayType: skipAttributeChanges.displayType.contains(stored.name) ?
          this.getOriginalConfigAttribute(stored, 'displayType', advancedConfigs) :
          (stringUtils.isSingleLine(stored.value) ? 'advanced' : 'multiLine'),
        filename: stored.filename,
        isUserProperty: stored.isUserProperty === true,
        hasInitialValue: !!stored.hasInitialValue,
        isOverridable: true,
        overrides: stored.overrides,
        isRequired: false,
        isVisible: stored.isVisible,
        isFinal: stored.isFinal,
        savedIsFinal: stored.savedIsFinal,
        supportsFinal: this.shouldSupportFinal(stored.serviceName, stored.filename),
        showLabel: stored.showLabel !== false,
        category: stored.category
      };
    if (stored.category == 'Users and Groups') {
      configData.index = this.getOriginalConfigAttribute(stored, 'index', advancedConfigs);
    }
    var advancedConfig = advancedConfigs.filterProperty('name', stored.name).findProperty('filename', stored.filename);
    App.get('config').calculateConfigProperties(configData, isAdvanced, advancedConfig);
    return configData;
  },

  complexConfigsTemplate: [
    {
      "id": "site property",
      "name": "capacity-scheduler",
      "displayName": "Capacity Scheduler",
      "value": "",
      "description": "Capacity Scheduler properties",
      "displayType": "custom",
      "isOverridable": true,
      "isRequired": true,
      "isVisible": true,
      "isReconfigurable": true,
      "supportsFinal": false,
      "serviceName": "YARN",
      "filename": "capacity-scheduler.xml",
      "category": "CapacityScheduler"
    }
  ],

  /**
   * transform set of configs from file
   * into one config with textarea content:
   * name=value
   * @param {App.ServiceConfigProperty[]} configs
   * @param {String} filename
   * @param {App.ServiceConfigProperty[]} [configsToSkip=[]]
   * @return {*}
   */
  fileConfigsIntoTextarea: function (configs, filename, configsToSkip) {
    var fileConfigs = configs.filterProperty('filename', filename);
    var value = '', savedValue = '', recommendedValue = '';
    var template = this.get('complexConfigsTemplate').findProperty('filename', filename);
    var complexConfig = $.extend({}, template);
    if (complexConfig) {
      fileConfigs.forEach(function (_config) {
        if (!(configsToSkip && configsToSkip.someProperty('name', _config.name))) {
          value += _config.name + '=' + _config.value + '\n';
          if (!Em.isNone(_config.savedValue)) {
            savedValue += _config.name + '=' + _config.savedValue + '\n';
          }
          if (!Em.isNone(_config.recommendedValue)) {
            recommendedValue += _config.name + '=' + _config.recommendedValue + '\n';
          }
        }
      }, this);
      var isFinal = fileConfigs.someProperty('isFinal', true);
      var savedIsFinal = fileConfigs.someProperty('savedIsFinal', true);
      var recommendedIsFinal = fileConfigs.someProperty('recommendedIsFinal', true);
      complexConfig.value = value;
      if (savedValue) {
        complexConfig.savedValue = savedValue;
      }
      if (recommendedValue) {
        complexConfig.recommendedValue = recommendedValue;
      }
      complexConfig.isFinal = isFinal;
      complexConfig.savedIsFinal = savedIsFinal;
      complexConfig.recommendedIsFinal = recommendedIsFinal;
      configs = configs.filter(function (_config) {
        return _config.filename !== filename || (configsToSkip && configsToSkip.someProperty('name', _config.name));
      });
      configs.push(App.ServiceConfigProperty.create(complexConfig));
    }
    return configs;
  },

  /**
   * transform one config with textarea content
   * into set of configs of file
   * @param configs
   * @param filename
   * @return {*}
   */
  textareaIntoFileConfigs: function (configs, filename) {
    var complexConfigName = this.get('complexConfigsTemplate').findProperty('filename', filename).name;
    var configsTextarea = configs.findProperty('name', complexConfigName);
    if (configsTextarea && !App.get('testMode')) {
      var properties = configsTextarea.get('value').split('\n');

      properties.forEach(function (_property) {
        var name, value;
        if (_property) {
          _property = _property.split('=');
          name = _property[0];
          value = (_property[1]) ? _property[1] : "";
          configs.push(Em.Object.create({
            id: configsTextarea.get('id'),
            name: name,
            value: value,
            savedValue: value,
            serviceName: configsTextarea.get('serviceName'),
            filename: filename,
            isFinal: configsTextarea.get('isFinal'),
            isNotDefaultValue: configsTextarea.get('isNotDefaultValue'),
            isRequiredByAgent: configsTextarea.get('isRequiredByAgent'),
            group: null
          }));
        }
      });
      return configs.without(configsTextarea);
    }
    console.log('ERROR: textarea config - ' + complexConfigName + ' is missing');
    return configs;
  },

  /**
   * trim trailing spaces for all properties.
   * trim both trailing and leading spaces for host displayType and hive/oozie datebases url.
   * for directory or directories displayType format string for further using.
   * for password and values with spaces only do nothing.
   * @param {Object} property
   * @returns {*}
   */
  trimProperty: function (property) {
    var displayType = Em.get(property, 'displayType');
    var value = Em.get(property, 'value');
    var name = Em.get(property, 'name');
    var rez;
    switch (displayType) {
      case 'directories':
      case 'directory':
        rez = value.replace(/,/g, ' ').trim().split(/\s+/g).join(',');
        break;
      case 'host':
        rez = value.trim();
        break;
      case 'password':
        break;
      case 'advanced':
        if (name == 'javax.jdo.option.ConnectionURL' || name == 'oozie.service.JPAService.jdbc.url') {
          rez = value.trim();
        }
      default:
        rez = (typeof value == 'string') ? value.replace(/(\s+$)/g, '') : value;
    }
    return ((rez == '') || (rez == undefined)) ? value : rez;
  },

  /**
   * exclude configs that depends on services which are uninstalled
   * if config doesn't have serviceName or dependent service is installed then
   * config not excluded
   * @param {object[]} configs
   * @param {string[]} installedServices
   * @return {object[]}
   * @method excludeUnsupportedConfigs
   */
  excludeUnsupportedConfigs: function (configs, installedServices) {
    return configs.filter(function (config) {
      return !(config.serviceName && !installedServices.contains(config.serviceName));
    });
  },

  /**
   * Generate minimal config property object used in *_properties.js files.
   * Example:
   * <code>
   *   var someProperties = App.config.generateConfigPropertiesByName([
   *    'property_1', 'property_2', 'property_3'], { category: 'General', filename: 'myFileName'});
   *   // someProperties contains Object[]
   *   [
   *    {
   *      name: 'property_1',
   *      displayName: 'property_1',
   *      isVisible: true,
   *      isReconfigurable: true,
   *      category: 'General',
   *      filename: 'myFileName'
   *    },
   *    .......
   *   ]
   * </code>
   * @param {string[]} names
   * @param {Object} properties - additional properties which will merge with base object definition
   * @returns {object[]}
   * @method generateConfigPropertiesByName
   */
  generateConfigPropertiesByName: function (names, properties) {
    return names.map(function (item) {
      var baseObj = {
        name: item
      };
      if (properties) return $.extend(baseObj, properties);
      else return baseObj;
    });
  },

  /**
   * replace some values in config property
   * @param {string} name
   * @param {string} express
   * @param {string} value
   * @param {string} globValue
   * @return {string}
   * @private
   * @method replaceConfigValues
   */
  replaceConfigValues: function (name, express, value, globValue) {
    if (name == 'templeton.hive.properties') {
      globValue = globValue.replace(/,/g, '\\,');
    }
    return value.replace(express, globValue);
  },

  /**
   * load cluster stack configs from server and run mapper
   * @returns {$.ajax}
   * @method loadConfigsFromStack
   */
  loadClusterConfigsFromStack: function () {
    return App.ajax.send({
      name: 'configs.stack_configs.load.cluster_configs',
      sender: this,
      data: {
        stackVersionUrl: App.get('stackVersionURL')
      },
      success: 'saveConfigsToModel'
    });
  },

  /**
   * load stack configs from server and run mapper
   * @param {String[]} [serviceNames=null]
   * @returns {$.ajax}
   * @method loadConfigsFromStack
   */
  loadConfigsFromStack: function (serviceNames) {
    serviceNames = serviceNames || [];
    var name = serviceNames.length > 0 ? 'configs.stack_configs.load.services' : 'configs.stack_configs.load.all';
    return App.ajax.send({
      name: name,
      sender: this,
      data: {
        stackVersionUrl: App.get('stackVersionURL'),
        serviceList: serviceNames.join(',')
      },
      success: 'saveConfigsToModel'
    });
  },

  /**
   * Runs <code>stackConfigPropertiesMapper<code>
   * @param {object} data
   * @method saveConfigsToModel
   */
  saveConfigsToModel: function (data) {
    App.stackConfigPropertiesMapper.map(data);
  },

  /**
   * Check if config filename supports final attribute
   * @param serviceName
   * @param filename
   * @returns {boolean}
   */
  shouldSupportFinal: function (serviceName, filename) {
    var unsupportedServiceNames = ['MISC', 'Cluster'];
    if (!serviceName || unsupportedServiceNames.contains(serviceName) || !filename) {
      return false;
    } else {
      var stackService = App.StackService.find().findProperty('serviceName', serviceName);
      if (!stackService) {
        return false;
      }
      var supportsFinal = this.getConfigTypesInfoFromService(stackService).supportsFinal;
      var matchingConfigType = supportsFinal.find(function (configType) {
        return filename.startsWith(configType);
      });
      return !!matchingConfigType;
    }
  },

  /**
   * Remove all ranger-related configs, that should be available only if Ranger is installed
   * @param configs - stepConfigs object
   */
  removeRangerConfigs: function (configs) {
    configs.forEach(function (service) {
      var filteredConfigs = [];
      service.get('configs').forEach(function (config) {
        if (!/^ranger-/.test(config.get('filename'))) {
          filteredConfigs.push(config);
        }
      });
      service.set('configs', filteredConfigs);
      var filteredCategories = [];
      service.get('configCategories').forEach(function (category) {
        if (!/ranger-/.test(category.get('name'))) {
          filteredCategories.push(category);
        }
      });
      service.set('configCategories', filteredCategories);
    });
  },

  /**
   * @param {App.ServiceConfigProperty} serviceConfigProperty
   * @param {Object} override - plain object with properties that is different from parent SCP
   * @param {App.ServiceConfigGroup} configGroup
   * @returns {App.ServiceConfigProperty}
   */
  createOverride: function(serviceConfigProperty, override, configGroup) {
    Em.assert('serviceConfigProperty can\' be null', serviceConfigProperty);
    Em.assert('configGroup can\' be null', configGroup);

    if (Em.isNone(serviceConfigProperty.get('overrides'))) serviceConfigProperty.set('overrides', []);

    var newOverride = App.ServiceConfigProperty.create(serviceConfigProperty);

    if (!Em.isNone(override)) {
      for (var key in override) {
        newOverride.set(key, override[key]);
      }
    }

    newOverride.setProperties({
      'isOriginalSCP': false,
      'overrides': null,
      'group': configGroup,
      'parentSCP': serviceConfigProperty
    });

    serviceConfigProperty.get('overrides').pushObject(newOverride);
    serviceConfigProperty.set('overrideValues', serviceConfigProperty.get('overrides').mapProperty('value'));
    serviceConfigProperty.set('overrideIsFinalValues', serviceConfigProperty.get('overrides').mapProperty('isFinal'));

    newOverride.validate();
    return newOverride;
  },


  /**
   * Merge values in "stored" to "base" if name matches, it's a value only merge.
   * @param base {Array} Em.Object
   * @param stored {Array} Object
   */
  mergeStoredValue: function(base, stored) {
    if (stored) {
      base.forEach(function (p) {
        var sp = stored.filterProperty("filename", p.filename).findProperty("name", p.name);
        if (sp) {
          p.set("value", sp.value);
        }
      });
    }
  },

  /**
   * Update config property value based on its current value and list of zookeeper server hosts.
   * Used to prevent sort order issues.
   * <code>siteConfigs</code> object formatted according server's persist format e.g.
   *
   * <code>
   *   {
   *     'yarn-site': {
   *       'property_name1': 'property_value1'
   *       'property_name2': 'property_value2'
   *       .....
   *     }
   *   }
   * </code>
   *
   * @method updateHostsListValue
   * @param {Object} siteConfigs - prepared site config object to store
   * @param {String} propertyName - name of the property to update
   * @param {String} hostsList - list of ZooKeeper Server names to set as config property value
   * @return {String} - result value
   */
  updateHostsListValue: function(siteConfigs, propertyName, hostsList) {
    var value = hostsList;
    var propertyHosts = (siteConfigs[propertyName] || '').split(',');
    var hostsToSet = hostsList.split(',');

    if (!Em.isEmpty(siteConfigs[propertyName])) {
      var diffLength = propertyHosts.filter(function(hostName) {
        return !hostsToSet.contains(hostName);
      }).length;
      if (diffLength == 0 && propertyHosts.length == hostsToSet.length) {
        value = siteConfigs[propertyName];
      }
    }
    siteConfigs[propertyName] = value;
    return value;
  }
});
