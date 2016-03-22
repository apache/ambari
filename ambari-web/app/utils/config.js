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
require('utils/configs_collection');
var stringUtils = require('utils/string_utils');
var validator = require('utils/validator');

var configTagFromFileNameMap = {};

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
   * truncate Config Group name to <CONFIG_GROUP_NAME_MAX_LENGTH> length and paste "..." in the middle
   */
  truncateGroupName: function (name) {
    if (name && name.length > App.config.CONFIG_GROUP_NAME_MAX_LENGTH) {
      var middle = Math.floor(App.config.CONFIG_GROUP_NAME_MAX_LENGTH / 2);
      name = name.substring(0, middle) + "..." + name.substring(name.length - middle);
    }
    return name;
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
    if (configTagFromFileNameMap[fileName]) {
      return configTagFromFileNameMap[fileName];
    }
    var ret = fileName.endsWith('.xml') ? fileName.slice(0, -4) : fileName;
    configTagFromFileNameMap[fileName] = ret;
    return ret;
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
    this.set('preDefinedServiceConfigs', allTabs);
  },

  secureConfigs: require('data/HDP2/secure_mapping'),

  secureConfigsMap: function () {
    var ret = {};
    this.get('secureConfigs').forEach(function (sc) {
      ret[sc.name] = true;
    });
    return ret;
  }.property('secureConfigs.[]'),

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

  serviceByConfigTypeMap: function () {
    var ret = {};
    App.StackService.find().forEach(function(s) {
      s.get('configTypeList').forEach(function (ct) {
        ret[ct] = s;
      });
    });
    return ret;
  }.property(),

  /**
   * Generate configs collection with Ember or plain config objects
   * from config JSON
   *
   * @param configJSON
   * @param useEmberObject
   * @returns {Array}
   */
  getConfigsFromJSON: function(configJSON, useEmberObject) {
    var configs = [],
      filename = App.config.getOriginalFileName(configJSON.type),
      properties = configJSON.properties,
      finalAttributes = Em.get(configJSON, 'properties_attributes.final') || {};

    for (var index in properties) {
      var serviceConfigObj = this.getDefaultConfig(index, filename);

      if (serviceConfigObj.isRequiredByAgent !== false) {
        serviceConfigObj.value = serviceConfigObj.savedValue = this.formatPropertyValue(serviceConfigObj, properties[index]);
        serviceConfigObj.isFinal = serviceConfigObj.savedIsFinal = finalAttributes[index] === "true";
        serviceConfigObj.isEditable = serviceConfigObj.isReconfigurable;
      }

      if (useEmberObject) {
        var serviceConfigProperty = App.ServiceConfigProperty.create(serviceConfigObj);
        serviceConfigProperty.validate();
        configs.push(serviceConfigProperty);
      } else {
        configs.push(serviceConfigObj);
      }
    }
    return configs;
  },

  /**
   * Get config from configsCollections or
   * generate new default config in collection does not contain
   * such config
   *
   * @param name
   * @param fileName
   * @param coreObject
   * @returns {*|Object}
   */
  getDefaultConfig: function(name, fileName, coreObject) {
    var cfg = App.configsCollection.getConfigByName(name, fileName) ||
      App.config.createDefaultConfig(name, fileName, false);
    if (Em.typeOf(coreObject) === 'object') {
      Em.setProperties(cfg, coreObject);
    }
    return cfg;
  },

  /**
   * This method sets default values for config property
   * These property values has the lowest priority and can be overridden be stack/UI
   * config property but is used when such properties are absent in stack/UI configs
   * @param {string} name
   * @param {string} fileName
   * @param {boolean} definedInStack
   * @param {Object} [coreObject]
   * @returns {Object}
   */
  createDefaultConfig: function(name, fileName, definedInStack, coreObject) {
    var service = this.get('serviceByConfigTypeMap')[App.config.getConfigTagFromFileName(fileName)];
    var serviceName = service ? service.get('serviceName') : 'MISC';
    var tpl = {
      /** core properties **/
      id: this.configId(name, fileName),
      name: name,
      filename: this.getOriginalFileName(fileName),
      value: '',
      savedValue: null,
      isFinal: false,
      savedIsFinal: null,
      /** UI and Stack properties **/
      recommendedValue: null,
      recommendedIsFinal: null,
      supportsFinal: this.shouldSupportFinal(serviceName, fileName),
      supportsAddingForbidden: this.shouldSupportAddingForbidden(serviceName, fileName),
      serviceName: serviceName,
      displayName: name,
      displayType: this.getDefaultDisplayType(coreObject ? coreObject.value : ''),
      description: '',
      category: this.getDefaultCategory(definedInStack, fileName),
      isSecureConfig: this.getIsSecure(name),
      showLabel: true,
      isVisible: true,
      isUserProperty: !definedInStack,
      isRequired: definedInStack,
      group: null,
      isRequiredByAgent:  true,
      isReconfigurable: true,
      unit: null,
      hasInitialValue: false,
      isOverridable: true,
      index: Infinity,
      dependentConfigPattern: null,
      options: null,
      radioName: null,
      widgetType: null
    };
    return Object.keys(coreObject|| {}).length ?
      $.extend(tpl, coreObject) : tpl;
  },

  /**
   * This method creates host name properties
   * @param serviceName
   * @param componentName
   * @param value
   * @param stackComponent
   * @returns Object
   */
  createHostNameProperty: function(serviceName, componentName, value, stackComponent) {
    var hostOrHosts = stackComponent.get('isMultipleAllowed') ? 'hosts' : 'host';
    return {
      "name": componentName.toLowerCase() + '_' + hostOrHosts,
      "displayName":  stackComponent.get('displayName') + ' ' + (value.length > 1 ? 'hosts' : 'host'),
      "value": value,
      "recommendedValue": value,
      "description": "The " + hostOrHosts + " that has been assigned to run " + stackComponent.get('displayName'),
      "displayType": "component" + hostOrHosts.capitalize(),
      "isOverridable": false,
      "isRequiredByAgent": false,
      "serviceName": serviceName,
      "filename": serviceName.toLowerCase() + "-site.xml",
      "category": componentName,
      "index": 0
    }
  },

  /**
   * This method merge properties form <code>stackConfigProperty<code> which are taken from stack
   * with <code>UIConfigProperty<code> which are hardcoded on UI
   * @param coreObject
   * @param stackProperty
   * @param preDefined
   * @param [propertiesToSkip]
   */
  mergeStaticProperties: function(coreObject, stackProperty, preDefined, propertiesToSkip) {
    propertiesToSkip = propertiesToSkip || ['name', 'filename', 'value', 'savedValue', 'isFinal', 'savedIsFinal'];
    for (var k in coreObject) {
      if (coreObject.hasOwnProperty(k)) {
        if (!propertiesToSkip.contains(k)) {
          coreObject[k] = this.getPropertyIfExists(k, coreObject[k], stackProperty, preDefined);
        }
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
    firstPriority = firstPriority || {};
    secondPriority = secondPriority || {};
    var fp = Em.get(firstPriority, propertyName);
    if (firstPriority && !Em.isNone(fp)) {
      return fp;
    }
    else {
      var sp = Em.get(secondPriority, propertyName);
      if (secondPriority && !Em.isNone(sp)) {
        return sp;
      } else {
        return defaultValue;
      }
    }
  },

  /**
   * Get displayType for properties that has not defined value
   * @param value
   * @returns {string}
   */
  getDefaultDisplayType: function(value) {
    return value && !stringUtils.isSingleLine(value) ? 'multiLine' : 'string';
  },

  /**
   * Get category for properties that has not defined value
   * @param stackConfigProperty
   * @param fileName
   * @returns {string}
   */
  getDefaultCategory: function(stackConfigProperty, fileName) {
    var tag = this.getConfigTagFromFileName(fileName);
    switch (tag) {
      case 'capacity-scheduler':
        return 'CapacityScheduler';
      default :
        return (stackConfigProperty ? 'Advanced ' : 'Custom ') + tag;
    }
  },

  /**
   * Get isSecureConfig for properties that has not defined value
   * @param propertyName
   * @returns {boolean}
   */
  getIsSecure: function(propertyName) {
    return !!this.get('secureConfigsMap')[propertyName];
  },

  /**
   * format property value depending on displayType
   * and one exception for 'kdc_type'
   * @param serviceConfigProperty
   * @param [originalValue]
   * @returns {*}
   */
  formatPropertyValue: function(serviceConfigProperty, originalValue) {
    var value = Em.isNone(originalValue) ? Em.get(serviceConfigProperty, 'value') : originalValue,
        displayType = Em.get(serviceConfigProperty, 'displayType') || Em.get(serviceConfigProperty, 'valueAttributes.type');
    if (Em.get(serviceConfigProperty, 'name') === 'kdc_type') {
      return App.router.get('mainAdminKerberosController.kdcTypesValues')[value];
    }
    if ( /^\s+$/.test("" + value)) {
      return " ";
    }
    switch (displayType) {
      case 'int':
        if (/\d+m$/.test(value) ) {
          return value.slice(0, value.length - 1);
        } else {
          var int = parseInt(value);
          return isNaN(int) ? "" : int.toString();
        }
      case 'float':
        var float = parseFloat(value);
        return isNaN(float) ? "" : float.toString();
      case 'componentHosts':
        if (typeof(value) == 'string') {
          return value.replace(/\[|]|'|&apos;/g, "").split(',');
        }
        return value;
      case 'content':
      case 'string':
      case 'multiLine':
      case 'directories':
      case 'directory':
        return this.trimProperty({ displayType: displayType, value: value });
      default:
        return value;
    }

  },

  /**
   * Format float value
   *
   * @param {*} value
   * @returns {string|*}
   */
  formatValue: function(value) {
    return validator.isValidFloat(value) ? parseFloat(value).toString() : value;
  },

  /**
   * Get step config by file name
   *
   * @param stepConfigs
   * @param fileName
   * @returns {Object|null}
   */
  getStepConfigForProperty: function (stepConfigs, fileName) {
    return stepConfigs.find(function (s) {
      return s.get('configTypes').contains(App.config.getConfigTagFromFileName(fileName));
    });
  },

  /**
   *
   * @param configs
   * @returns {Object[]}
   */
  sortConfigs: function(configs) {
    return configs.sort(function(a, b) {
      if (Em.get(a, 'index') > Em.get(b, 'index')) return 1;
      if (Em.get(a, 'index') < Em.get(b, 'index')) return -1;
      if (Em.get(a, 'name') > Em.get(b, 'index')) return 1;
      if (Em.get(a, 'name') < Em.get(b, 'index')) return -1;
      return 0;
    });
  },

  /**
   * create new ServiceConfig object by service name
   * @param {string} serviceName
   * @param {App.ServiceConfigGroup[]} [configGroups]
   * @param {App.ServiceConfigProperty[]} [configs]
   * @param {Number} [initConfigsLength]
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

  configTypesInfoMap: {},

  /**
   * Get config types and config type attributes from stack service
   *
   * @param service
   * @return {object}
   */
  getConfigTypesInfoFromService: function (service) {
    var configTypesInfoMap = this.get('configTypesInfoMap');
    if (configTypesInfoMap[service]) {
      // don't recalculate
      return configTypesInfoMap[service];
    }
    var configTypes = service.get('configTypes');
    var configTypesInfo = {
      items: [],
      supportsFinal: [],
      supportsAddingForbidden: []
    };
    if (configTypes) {
      for (var key in configTypes) {
        if (configTypes.hasOwnProperty(key)) {
          configTypesInfo.items.push(key);
          if (configTypes[key].supports && configTypes[key].supports.final === "true") {
            configTypesInfo.supportsFinal.push(key);
          }
          if (configTypes[key].supports && configTypes[key].supports.adding_forbidden === "true"){
            configTypesInfo.supportsAddingForbidden.push(key);
          }
        }
      }
    }
    configTypesInfoMap[service] = configTypesInfo;
    this.set('configTypesInfoMap', configTypesInfoMap);
    return configTypesInfo;
  },

  /**
   * Create config with non default config group. Some custom config properties
   * can be created and assigned to non-default config group.
   *
   * @param {String} propertyName - name of the property
   * @param {String} fileName - file name of the property
   * @param {String} value - config value
   * @param {Em.Object} group - config group to set
   * @param {Boolean} [isEditable]
   * @param {Boolean} [isInstaller]
   * @return {Object}
   **/
  createCustomGroupConfig: function (propertyName, fileName, value, group, isEditable, isInstaller) {
    var propertyObject = this.createDefaultConfig(propertyName, this.getOriginalFileName(fileName), false, {
      savedValue: isInstaller ? null : value,
      value: value,
      group: group,
      isEditable: !!isEditable,
      isOverridable: false
    });
    group.set('switchGroupTextShort', Em.I18n.t('services.service.config_groups.switchGroupTextShort').format(group.get('name')));
    group.set('switchGroupTextFull', Em.I18n.t('services.service.config_groups.switchGroupTextFull').format(group.get('name')));
    return App.ServiceConfigProperty.create(propertyObject);
  },

  /**
   *
   * @param configs
   */
  addYarnCapacityScheduler: function(configs) {
    var value = '', savedValue = '', recommendedValue = '',
      excludedConfigs = App.config.getPropertiesFromTheme('YARN');

    var connectedConfigs = configs.filter(function(config) {
      return !excludedConfigs.contains(App.config.configId(config.get('name'), config.get('filename'))) && (config.get('filename') === 'capacity-scheduler.xml');
    });
    var names = connectedConfigs.mapProperty('name');

    connectedConfigs.forEach(function (config) {
      value += config.get('name') + '=' + config.get('value') + '\n';
      if (!Em.isNone(config.get('savedValue'))) {
        savedValue += config.get('name') + '=' + config.get('savedValue') + '\n';
      }
      if (!Em.isNone(config.get('recommendedValue'))) {
        recommendedValue += config.get('name') + '=' + config.get('recommendedValue') + '\n';
      }
    }, this);

    var isFinal = connectedConfigs.someProperty('isFinal', true);
    var savedIsFinal = connectedConfigs.someProperty('savedIsFinal', true);
    var recommendedIsFinal = connectedConfigs.someProperty('recommendedIsFinal', true);

    var cs = App.config.createDefaultConfig('capacity-scheduler', 'capacity-scheduler.xml', true, {
      'value': value,
      'serviceName': 'YARN',
      'savedValue': savedValue || null,
      'recommendedValue': recommendedValue || null,
      'isFinal': isFinal,
      'savedIsFinal': savedIsFinal,
      'recommendedIsFinal': recommendedIsFinal,
      'displayName': 'Capacity Scheduler',
      'description': 'Capacity Scheduler properties',
      'displayType': 'capacityScheduler'
    });

    configs = configs.filter(function(c) {
      return !(names.contains(c.get('name')) && (c.get('filename') === 'capacity-scheduler.xml'));
    });
    configs.push(App.ServiceConfigProperty.create(cs));
    return configs;
  },

  /**
   *
   * @param serviceName
   * @returns {Array}
   */
  getPropertiesFromTheme: function (serviceName) {
    var properties = [];
    App.Tab.find().forEach(function (t) {
      if (!t.get('isAdvanced') && t.get('serviceName') === serviceName) {
        t.get('sections').forEach(function (s) {
          s.get('subSections').forEach(function (ss) {
            properties = properties.concat(ss.get('configProperties'));
          });
        });
      }
    }, this);
    return properties;
  },

  /**
   * transform one config with textarea content
   * into set of configs of file
   * @param configs
   * @param filename
   * @return {*}
   */
  textareaIntoFileConfigs: function (configs, filename) {
    var configsTextarea = configs.findProperty('name', 'capacity-scheduler');
    if (configsTextarea && !App.get('testMode')) {
      var properties = configsTextarea.get('value').split('\n');

      properties.forEach(function (_property) {
        var name, value;
        if (_property) {
          _property = _property.split('=');
          name = _property[0];
          value = (_property[1]) ? _property[1] : "";
          configs.push(Em.Object.create({
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
      default:
        if (name == 'javax.jdo.option.ConnectionURL' || name == 'oozie.service.JPAService.jdbc.url') {
          rez = value.trim();
        }
        rez = (typeof value == 'string') ? value.replace(/(\s+$)/g, '') : value;
    }
    return ((rez == '') || (rez == undefined)) ? value : rez;
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
      var stackService = App.StackService.find(serviceName);
      if (!stackService) {
        return false;
      }
      return !!this.getConfigTypesInfoFromService(stackService).supportsFinal.find(function (configType) {
        return filename.startsWith(configType);
      });
    }
  },

  shouldSupportAddingForbidden: function(serviceName, filename) {
    var unsupportedServiceNames = ['MISC', 'Cluster'];
    if (!serviceName || unsupportedServiceNames.contains(serviceName) || !filename) {
      return false;
    } else {
      var stackServiceName = App.StackService.find().findProperty('serviceName', serviceName);
      if (!stackServiceName) {
        return false;
      }
      var stackService = App.StackService.find(serviceName);
      return !!this.getConfigTypesInfoFromService(stackService).supportsAddingForbidden.find(function (configType) {
        return filename.startsWith(configType);
      });
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
   * @param {boolean} [updateGroup]
   * @returns {App.ServiceConfigProperty}
   */
  createOverride: function(serviceConfigProperty, override, configGroup, updateGroup) {
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

    if (updateGroup) {
      if (!configGroup.get('properties.length')) {
        configGroup.set('properties', Em.A([]));
      }
      configGroup.get('properties').push(newOverride);
    }

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
   * Helper method to get property from the <code>stepConfigs</code>
   *
   * @param {String} name - config property name
   * @param {String} fileName - config property filename
   * @param {Object[]} stepConfigs
   * @return {App.ServiceConfigProperty|Boolean} - App.ServiceConfigProperty instance or <code>false</code> when property not found
   */
  findConfigProperty: function(stepConfigs, name, fileName) {
    if (!name && !fileName) return false;
    if (stepConfigs && stepConfigs.length) {
      return stepConfigs.mapProperty('configs').filter(function(item) {
        return item.length;
      }).reduce(function(p, c) {
        if (p) {
          return p.concat(c);
        }
      }).filterProperty('filename', fileName).findProperty('name', name);
    }
    return false;
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
