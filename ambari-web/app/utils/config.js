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
  filenameExceptions: [],

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

  preDefinedSiteProperties: function () {
    var sitePropertiesForCurrentStack = this.preDefinedConfigFile('site_properties');
    if (sitePropertiesForCurrentStack) {
      return sitePropertiesForCurrentStack.configProperties;
    }
    if (App.get('isHadoop23Stack')) {
      return require('data/HDP2.3/site_properties').configProperties;
    }
    if (App.get('isHadoop22Stack')) {
      return require('data/HDP2.2/site_properties').configProperties;
    }
    return require('data/HDP2/site_properties').configProperties;
  }.property('App.isHadoop22Stack', 'App.isHadoop23Stack'),

  preDefinedConfigFile: function(file) {
    try {
      return require('data/{0}/{1}'.format(App.get('currentStackName'), file));
    } catch (err) {
      // the file doesn't exist, which might be expected.
    }
  },

  /**
   * Create array of service properties for Log4j files
   * @returns {Array}
   */
  createContentProperties: function (configs) {
    var services = App.StackService.find();
    var contentProperties = [];
    if (configs) {
      services.forEach(function (service) {
        if (service.get('configTypes')) {
          Object.keys(service.get('configTypes')).forEach(function (type) {
            var contentProperty = configs.filterProperty('filename', type + '.xml').someProperty('name', 'content');
            if (contentProperty && (type.endsWith('-log4j') || type.endsWith('-env'))) {
              var property = {
                "id": "site property",
                "name": "content",
                "displayName": type.endsWith('-env') ? type + ' template' : "content",
                "value": "",
                "description": type + " properties",
                "displayType": "content",
                "isOverridable": true,
                "isRequired": false,
                "isVisible": true,
                "showLabel": type.endsWith('-env'),
                "serviceName": service.get('serviceName'),
                "filename": type + '.xml',
                "category": "Advanced " + type
              };
              contentProperties.pushObject(property);
            }
          }, this);
        }
      }, this);
    }
    return contentProperties;
  },

  //configs with these filenames go to appropriate category not in Advanced
  customFileNames: ['flume-conf.xml'],

  /**
   * Function should be used post-install as precondition check should not be done only after installer wizard
   * @param siteNames {String|Array}
   * @returns {Array}
   */
  getBySiteName: function (siteNames) {
    var computedConfigs = this.get('configMapping').computed();
    var siteProperties = [];
    if (typeof siteNames === "string") {
      siteProperties = computedConfigs.filterProperty('filename', siteNames);
    } else if (siteNames instanceof Array) {
      siteNames.forEach(function (_siteName) {
        siteProperties = siteProperties.concat(computedConfigs.filterProperty('filename', _siteName));
      }, this);
    }
    return siteProperties;
  },

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
   * @param advancedConfigs
   * @param serviceName
   * @param selectedConfigGroup
   * @param canEdit
   * @returns {Array}
   */
  mergePredefinedWithSaved: function (configCategories, advancedConfigs, serviceName, selectedConfigGroup, canEdit) {
    var configs = [];
    var contentProperties = this.createContentProperties(advancedConfigs);
    var preDefinedConfigs = this.get('preDefinedSiteProperties').concat(contentProperties);

    configCategories.forEach(function (siteConfig) {
      var service = this.getServiceByConfigType(siteConfig.type);
      if (service) {
        serviceName = service.get('serviceName');
      }
      var filename = App.config.getOriginalFileName(siteConfig.type);
      var attributes = siteConfig['properties_attributes'] || {};
      var finalAttributes = attributes.final || {};
      var properties = siteConfig.properties || {};

      for (var index in properties) {
        var configsPropertyDef = preDefinedConfigs.filterProperty('name', index).findProperty('filename', filename);
        var advancedConfig = advancedConfigs.filterProperty('name', index).findProperty('filename', filename);

        var serviceConfigObj = this.mergeStackConfigsWithUI(index, filename, properties[index], finalAttributes[index] === "true", service, advancedConfig, configsPropertyDef);

        if (serviceConfigObj.get('isRequiredByAgent') !== false) {
          var formattedValue = this.formatPropertyValue(serviceConfigObj);
          serviceConfigObj.setProperties({
            'value': formattedValue,
            'savedValue': formattedValue,
            'isEditable': this.getIsEditable(serviceConfigObj, selectedConfigGroup, canEdit)
          });
        }

        var serviceConfigProperty = App.ServiceConfigProperty.create(serviceConfigObj);
        serviceConfigProperty.validate();
        configs.push(serviceConfigProperty);
      }
    }, this);
    return configs;
  },

  /**
   * This method merge properties form <code>stackConfigProperty<code> which are taken from stack
   * with <code>UIConfigProperty<code> which are hardcoded on UI
   * @param name
   * @param fileName
   * @param value
   * @param isFinal
   * @param service
   * @param stackConfigProperty
   * @param UIConfigProperty
   */
  mergeStackConfigsWithUI: function(name, fileName, value, isFinal, service, stackConfigProperty, UIConfigProperty) {
    return Em.Object.create({
      /** core properties **/
      name: name,
      filename: fileName,
      value: value,
      savedValue: value,
      isFinal: isFinal,
      savedIsFinal: isFinal,
      /** UI and Stack properties **/
      recommendedValue: this.getPropertyIfExists('recommendedValue', null, stackConfigProperty, UIConfigProperty),
      recommendedIsFinal: this.getPropertyIfExists('recommendedIsFinal', null, stackConfigProperty, UIConfigProperty),
      displayName: this.getPropertyIfExists('displayName', name, stackConfigProperty, UIConfigProperty),
      description: this.getPropertyIfExists('description', null, stackConfigProperty, UIConfigProperty),
      supportsFinal: this.getPropertyIfExists('supportsFinal', false, stackConfigProperty, UIConfigProperty),
      /** properties with calculations **/
      displayType: this.getPropertyIfExists('displayType', this.getDefaultDisplayType(value), UIConfigProperty, stackConfigProperty),
      category: this.getPropertyIfExists('category', this.getDefaultCategory(stackConfigProperty, fileName), UIConfigProperty),
      isSecureConfig: this.getPropertyIfExists('isSecureConfig', this.getIsSecure(name), UIConfigProperty),
      serviceName: this.getPropertyIfExists('serviceName', service ? service.get('serviceName') : 'MISC', stackConfigProperty, UIConfigProperty),
      isVisible: this.getPropertyIfExists('isVisible', !!service, UIConfigProperty),
      isUserProperty: this.getPropertyIfExists('isUserProperty', !stackConfigProperty, UIConfigProperty),
      isRequired: this.getPropertyIfExists('isRequired', !!stackConfigProperty, UIConfigProperty),
      /** UI properties **/
      id: this.getPropertyIfExists('id', 'site property', UIConfigProperty),
      isRequiredByAgent: this.getPropertyIfExists('isRequiredByAgent', true, UIConfigProperty),
      isReconfigurable: this.getPropertyIfExists('isReconfigurable', true, UIConfigProperty),
      unit: this.getPropertyIfExists('unit', null, UIConfigProperty),
      isOverridable: this.getPropertyIfExists('isOverridable', true, UIConfigProperty),
      index: this.getPropertyIfExists('index', null, UIConfigProperty),
      showLabel: this.getPropertyIfExists('showLabel', true, UIConfigProperty),
      dependentConfigPattern: this.getPropertyIfExists('dependentConfigPattern', null, UIConfigProperty),
      options: this.getPropertyIfExists('options', null, UIConfigProperty),
      radioName: this.getPropertyIfExists('radioName', null, UIConfigProperty),
      belongsToService: this.getPropertyIfExists('belongsToService', [], UIConfigProperty)
    });
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
   * @param value
   * @returns {string}
   */
  getDefaultDisplayType: function(value) {
    return stringUtils.isSingleLine(value) ? 'advanced' : 'multiLine';
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
   * @param {Em.Object} serviceConfigProperty
   * @param {Em.Object} selectedConfigGroup
   * @param {boolean} canEdit
   * @returns {boolean}
   */
  getIsEditable: function(serviceConfigProperty, selectedConfigGroup, canEdit) {
    return canEdit && selectedConfigGroup.get('isDefault') && serviceConfigProperty.get('isReconfigurable')
  },

  /**
   * format property value depending on displayType
   * and one exception for 'kdc_type'
   * @param serviceConfigProperty
   * @returns {*}
   */
  formatPropertyValue: function(serviceConfigProperty) {
    var value = serviceConfigProperty.get('value'), displayType = serviceConfigProperty.get('displayType') || serviceConfigProperty.get('valueAttributes.type'), category = serviceConfigProperty.get('category');
    switch (displayType) {
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
    if (serviceConfigProperty.get('name') === 'kdc_type') {
      return App.router.get('mainAdminKerberosController.kdcTypesValues')[value];
    }
    return value;
  },

  /**
   * return:
   *   configs,
   *   mappingConfigs
   *
   * @param configCategories
   * @param advancedConfigs
   * @param tags
   * @param serviceName
   * @return {object}
   */
  mergePreDefinedWithLoaded: function (configCategories, advancedConfigs, tags, serviceName) {
    var configs = [];
    var contentProperties = this.createContentProperties(advancedConfigs);
    var preDefinedConfigs = this.get('preDefinedSiteProperties').concat(contentProperties);
    var mappingConfigs = [];
    var filenameExceptions = this.get('filenameExceptions');
    tags.forEach(function (_tag) {
      var service = this.getServiceByConfigType(_tag.siteName);
      if (service) {
        serviceName = service.get('serviceName');
      }

      var filename = App.config.getOriginalFileName(_tag.siteName);
      var siteConfig = configCategories.filter(function (serviceConfigProperties) {
        return _tag.tagName === serviceConfigProperties.tag && _tag.siteName === serviceConfigProperties.type;
      });
      siteConfig = siteConfig[0] || {};

      var attributes = siteConfig['properties_attributes'] || {};
      var finalAttributes = attributes.final || {};
      var properties = siteConfig.properties || {};
      for (var index in properties) {
        var configsPropertyDef = preDefinedConfigs.filterProperty('name', index).findProperty('filename', filename);
        var advancedConfig = advancedConfigs.filterProperty('name', index).findProperty('filename', filename);
        var isAdvanced = Boolean(advancedConfig);
        if (!configsPropertyDef) {
          configsPropertyDef = advancedConfig;
        }
        var value = this.parseValue(properties[index], configsPropertyDef, advancedConfig);
        var serviceConfigObj = App.ServiceConfig.create({
          name: index,
          value: value,
          savedValue: value,
          recommendedValue: advancedConfig ? Em.get(advancedConfig, 'recommendedValue') : null,
          filename: filename,
          isUserProperty: !advancedConfig,
          isVisible: !!service,
          isOverridable: true,
          isReconfigurable: true,
          isRequired: isAdvanced,
          isFinal: finalAttributes[index] === "true",
          savedIsFinal: finalAttributes[index] === "true",
          recommendedIsFinal: advancedConfig ? Em.get(advancedConfig, 'recommendedIsFinal') : null,
          showLabel: true,
          serviceName: serviceName,
          belongsToService: [],
          supportsFinal: advancedConfig ? Em.get(advancedConfig, 'supportsFinal') : this.shouldSupportFinal(serviceName, _tag.siteName)

        });
        if (configsPropertyDef) {
          this.setServiceConfigUiAttributes(serviceConfigObj, configsPropertyDef);
          // check if defined UI config present in config list obtained from server.
          // in case when config is absent on server and defined UI config is required
          // by server, this config should be ignored
          var serverProperty = properties[serviceConfigObj.get('name')];
          if (Em.isNone(serverProperty) && serviceConfigObj.get('isRequiredByAgent')) {
            continue;
          }
        }

        this.tweakConfigVisibility(serviceConfigObj, properties);
        if (!this.getBySiteName(serviceConfigObj.get('filename')).someProperty('name', index)) {
          if (configsPropertyDef) {
            if (Em.get(configsPropertyDef, 'isRequiredByAgent') === false) {
              configs.push(serviceConfigObj);
              continue;
            }
            this.handleSpecialProperties(serviceConfigObj);
          } else {
            serviceConfigObj.set('displayType', stringUtils.isSingleLine(serviceConfigObj.get('value')) ? 'advanced' : 'multiLine');
          }
          serviceConfigObj.setProperties({
            'id': 'site property',
            'displayName': configsPropertyDef && Em.get(configsPropertyDef, 'displayName') ? Em.get(configsPropertyDef, 'displayName') : index,
            'options': configsPropertyDef ? Em.get(configsPropertyDef, 'options') : null,
            'radioName': configsPropertyDef ? Em.get(configsPropertyDef, 'radioName') : null,
            'serviceName': configsPropertyDef && Em.get(configsPropertyDef, 'serviceName') ? Em.get(configsPropertyDef, 'serviceName') : serviceName,
            'belongsToService': configsPropertyDef && Em.get(configsPropertyDef, 'belongsToService') ? Em.get(configsPropertyDef, 'belongsToService') : []
          });
          this.calculateConfigProperties(serviceConfigObj, isAdvanced, advancedConfig);
          this.setValueByDisplayType(serviceConfigObj);
          configs.push(serviceConfigObj);
        } else {
          mappingConfigs.push(serviceConfigObj);
        }
      }
    }, this);
    return {
      configs: configs,
      mappingConfigs: mappingConfigs
    }
  },

  /**
   * additional parsing when value is int of float
   * ex: if value is "0.40" result will be "0.4"
   * @param value
   * @param predefinedConfig
   * @param advancedConfig
   * @returns {String}
   */
  parseValue: function(value, predefinedConfig, advancedConfig) {
    var type = predefinedConfig ? Em.get(predefinedConfig, 'displayType') :
      advancedConfig && Em.get(advancedConfig, 'valueAttributes.type');
    switch (type) {
      case 'int':
        var res = parseInt(value);
        return isNaN(res) ? "" : res.toString();
      case 'float':
        var res = parseFloat(value);
        return isNaN(res) ? "" : res.toString();
      default:
        return value;
    }
  },


  tweakConfigVisibility: function (config, allSiteConfigs) {
    var kdcType = allSiteConfigs['kdc_type'];
    if (kdcType === 'active-directory' && ['container_dn', 'ldap_url'].contains(Em.get(config, 'name'))) {
      Em.set(config, 'isVisible', true);
    }
  },

  setValueByDisplayType: function (serviceConfigObj) {
    if (serviceConfigObj.get('displayType') == 'directories' && (serviceConfigObj.get('category') == 'DataNode' || serviceConfigObj.get('category') == 'NameNode')) {
      var dirs = serviceConfigObj.get('value').split(',').sort();
      serviceConfigObj.set('value', dirs.join(','));
      serviceConfigObj.set('savedValue', dirs.join(','));
    }

    if (serviceConfigObj.get('displayType') == 'directory' && serviceConfigObj.get('category') == 'SNameNode') {
      var dirs = serviceConfigObj.get('value').split(',').sort();
      serviceConfigObj.set('value', dirs[0]);
      serviceConfigObj.set('savedValue', dirs[0]);
    }

    if (serviceConfigObj.get('displayType') == 'masterHosts') {
      if (typeof(serviceConfigObj.get('value')) == 'string') {
        var value = serviceConfigObj.get('value').replace(/\[|]|'|&apos;/g, "").split(',');
        serviceConfigObj.set('value', value);
        serviceConfigObj.set('savedValue', value);
      }
    }
  },

  /**
   * @param serviceConfigObj : Object
   * @param configsPropertyDef : Object
   */
  setServiceConfigUiAttributes: function (serviceConfigObj, configsPropertyDef) {
    serviceConfigObj.setProperties({
      'displayType': Em.get(configsPropertyDef, 'displayType'),
      'isRequired': (Em.get(configsPropertyDef, 'isRequired') !== undefined) ? Em.get(configsPropertyDef, 'isRequired') : true,
      'isRequiredByAgent': (Em.get(configsPropertyDef, 'isRequiredByAgent') !== undefined) ? Em.get(configsPropertyDef, 'isRequiredByAgent') : true,
      'isReconfigurable': (Em.get(configsPropertyDef, 'isReconfigurable') !== undefined) ? Em.get(configsPropertyDef, 'isReconfigurable') : true,
      'isVisible': (Em.get(configsPropertyDef, 'isVisible') !== undefined) ? Em.get(configsPropertyDef, 'isVisible') : true,
      'unit': Em.get(configsPropertyDef, 'unit'),
      'description': Em.get(configsPropertyDef, 'description'),
      'isOverridable': Em.get(configsPropertyDef, 'isOverridable') === undefined ? true : Em.get(configsPropertyDef, 'isOverridable'),
      'serviceName': configsPropertyDef ? Em.get(configsPropertyDef, 'serviceName') : serviceConfigObj.get('serviceName'),
      'index': Em.get(configsPropertyDef, 'index'),
      'isSecureConfig': Em.get(configsPropertyDef, 'isSecureConfig') === undefined ? false : Em.get(configsPropertyDef, 'isSecureConfig'),
      'belongsToService': Em.get(configsPropertyDef, 'belongsToService'),
      'category': Em.get(configsPropertyDef, 'category'),
      'showLabel': Em.get(configsPropertyDef, 'showLabel') !== false,
      'dependentConfigPattern': Em.get(configsPropertyDef, 'dependentConfigPattern')
    });
  },

  /**
   * synchronize order of config properties with order, that on UI side
   *
   * @method syncOrderWithPredefined
   * @param {Object[]} siteConfigs
   * @return {Object[]}
   */
  syncOrderWithPredefined: function (siteConfigs) {
    var siteStart = [];
    var preDefinedSiteProperties = this.get('preDefinedSiteProperties').mapProperty('name');
    var contentProperties = this.createContentProperties(siteConfigs).mapProperty('name');
    var siteProperties = preDefinedSiteProperties.concat(contentProperties);
    siteProperties.forEach(function (name) {
      var _site = siteConfigs.filterProperty('name', name);
      if (_site.length == 1) {
        siteStart.push(_site[0]);
        siteConfigs = siteConfigs.without(_site[0]);
      } else if (_site.length > 1) {
        _site.forEach(function (site) {
          siteStart.push(site);
          siteConfigs = siteConfigs.without(site);
        }, this);
      }
    }, this);

    return siteStart.concat(siteConfigs.sortProperty('name'))
  },

  /**
   * merge stored configs with pre-defined
   * @param storedConfigs
   * @param advancedConfigs
   * @param selectedServiceNames
   * @return {Array}
   */
  mergePreDefinedWithStored: function (storedConfigs, advancedConfigs, selectedServiceNames) {
    var mergedConfigs = [];
    var contentProperties = advancedConfigs ? this.createContentProperties(advancedConfigs) : [];
    var preDefinedConfigs = this.get('preDefinedSiteProperties').concat(contentProperties);
    var self = this;
    storedConfigs = (storedConfigs) ? storedConfigs : [];

    var preDefinedNames = preDefinedConfigs.mapProperty('name');
    var storedNames = storedConfigs.mapProperty('name');
    var names = preDefinedNames.concat(storedNames).uniq();
    var configTypes = App.StackService.find().filter(function(service) {
      return selectedServiceNames.contains(service.get('serviceName'));
    }).map(function(item) {
      return Em.keys(item.get('configTypes'));
    }).reduce(function(p,c) { return p.concat(c); })
    .uniq().compact().filter(function(configType) { return !!configType; });

    names.forEach(function (name) {
      var storedCfgs = storedConfigs.filterProperty('name', name);
      var preDefinedCfgs = [];
      var preDefinedConfig = preDefinedConfigs.filterProperty('name', name);
      preDefinedConfig.forEach(function (_preDefinedConfig) {
        if (selectedServiceNames.contains(_preDefinedConfig.serviceName) || _preDefinedConfig.serviceName === 'MISC') {
          if (_preDefinedConfig.serviceName != 'MISC' && _preDefinedConfig.filename && !configTypes.contains(self.getConfigTagFromFileName(_preDefinedConfig.filename))) {
            return;
          }
          preDefinedCfgs.push($.extend(true, {}, _preDefinedConfig));
        }
      }, this);

      var configData = {};
      var isAdvanced = advancedConfigs && advancedConfigs.someProperty('name', name);
      if (storedCfgs.length <= 1 && preDefinedCfgs.length <= 1) {
        var stored = storedCfgs[0];
        var preDefined = preDefinedCfgs[0];

        if (preDefined && stored) {
          configData = preDefined;
          configData.value = stored.value;
          configData.savedValue = stored.savedValue;
          configData.recommendedValue = stored.recommendedValue;
          configData.overrides = stored.overrides;
          configData.displayName = stored.displayName;
          configData.name = stored.name;
          configData.filename = stored.filename;
          configData.description = stored.description;
          configData.isVisible = stored.isVisible;
          configData.isFinal = stored.isFinal;
          configData.supportsFinal = stored.supportsFinal;
          configData.isRequired = (configData.isRequired !== undefined) ? configData.isRequired : true;
          configData.isRequiredByAgent = (configData.isRequiredByAgent !== undefined) ? configData.isRequiredByAgent : true;
          configData.showLabel = !!stored.showLabel;
        }
        else if (!preDefined && stored) {
          configData = this.addUserProperty(stored, isAdvanced, advancedConfigs);
        }
        else if (preDefined && !stored) {
          configData = preDefined;
          // skip property if predefined config doesn't exist or ignored in stack property definition for current stack.
          // if `isRequiredByAgent` is set to `false` then this property used by UI only to display properties like
          // host names or some misc properties that won't be persisted.
          var isPresentInConfigApi = advancedConfigs && advancedConfigs.filterProperty('filename', preDefined.filename).someProperty('name', name);
          if (Em.get(preDefined, 'isRequiredByAgent') !== false && !isPresentInConfigApi &&
              Em.get(preDefined, 'filename') != 'alert_notification' &&
              ![
                'hive_hostname',
                'oozie_hostname',
                'hive_existing_oracle_host',
                'hive_existing_postgresql_host',
                'hive_existing_mysql_host',
                'hive_existing_mssql_server_host',
                'hive_existing_mssql_server_2_host',
                'oozie_existing_oracle_host',
                'oozie_existing_postgresql_host',
                'oozie_existing_mysql_host',
                'oozie_existing_mssql_server_host',
                'oozie_existing_mssql_server_2_host'
              ].contains(Em.get(preDefined, 'name'))) {
            return;
          }
          configData.isRequiredByAgent = (configData.isRequiredByAgent !== undefined) ? configData.isRequiredByAgent : true;
          if (isAdvanced) {
            var advanced = advancedConfigs.filterProperty('filename', configData.filename).findProperty('name', configData.name);
            this.setPropertyFromStack(configData, advanced);
          }
        }

        mergedConfigs.push(configData);
      } else {
        preDefinedCfgs.forEach(function (cfg) {
          configData = cfg;
          configData.isRequiredByAgent = (configData.isRequiredByAgent !== undefined) ? configData.isRequiredByAgent : true;
          var storedCfg = storedCfgs.findProperty('filename', cfg.filename);
          if (storedCfg) {
            configData.value = storedCfg.value;
            configData.recommendedValue = storedCfg.recommendedValue;
            configData.savedValue = storedCfg.savedValue;
            configData.overrides = storedCfg.overrides;
            configData.filename = storedCfg.filename;
            configData.description = storedCfg.description;
            configData.isFinal = storedCfg.isFinal;
            configData.supportsFinal = storedCfg.supportsFinal;
            configData.showLabel = !!storedCfg.showLabel;
            configData.displayName = storedCfg.displayName;
          } else if (isAdvanced) {
            advanced = advancedConfigs.filterProperty('filename', configData.filename).findProperty('name', configData.name);
            this.setPropertyFromStack(configData, advanced);
          }
          mergedConfigs.push(configData);
        }, this);
      }
    }, this);
    return mergedConfigs;
  },

  /**
   *
   * @param configData {Object} Configs that will be bound to the view on step-7 of installer wizard
   * @param advanced {Object} Config property loaded from Server side stack definition
   */
  setPropertyFromStack: function (configData, advanced) {

    // Password fields should be made blank by default in installer wizard
    // irrespective of whatever value is sent from stack definition.
    // This forces the user to fill the password field.
    if (configData.displayType == 'password') {
      configData.value = '';
    } else {
      configData.value = advanced ? advanced.value : configData.value;
    }
    configData.recommendedValue = configData.value;
    configData.filename = advanced ? advanced.filename : configData.filename;
    configData.displayName = advanced && advanced.displayName ? advanced.displayName : configData.displayName;
    configData.name = advanced && advanced.name ? advanced.name : configData.name;
    configData.description = advanced ? advanced.description : configData.description;
    configData.isFinal = !!(advanced && (advanced.isFinal === "true"));
    configData.supportsFinal = !!(advanced && advanced.supportsFinal);
  },

  /**
   * look over advanced configs and add missing configs to serviceConfigs
   * filter fetched configs by service if passed
   * @param serviceConfigs
   * @param advancedConfigs
   * @param serviceName
   */
  addAdvancedConfigs: function (serviceConfigs, advancedConfigs, serviceName) {
    var miscConfigs = serviceConfigs.filterProperty('serviceName', 'MISC');
    var configsToVerifying = (serviceName) ? serviceConfigs.filterProperty('serviceName', serviceName).concat(miscConfigs) : serviceConfigs.slice();
    var definedService = this.get('preDefinedServiceConfigs').findProperty('serviceName', serviceName);
    if (definedService) {
      var definedConfigs = (serviceName) ? definedService.get('configs') : [];

      if (definedConfigs.length) {
        advancedConfigs = advancedConfigs.filter(function (property) {
          return !(definedConfigs.someProperty('name', property.name) && !serviceConfigs.someProperty('name', property.name));
        }, this);
      }
    }
    if (advancedConfigs) {
      advancedConfigs.forEach(function (_config) {
        var configType = this.getConfigTagFromFileName(_config.filename);
        var configCategory = _config.category || 'Advanced ' + configType;
        var categoryMetaData = null;
        if (_config) {
          if (!(this.get('configMapping').computed().someProperty('name', _config.name) ||
            configsToVerifying.filterProperty('name', _config.name).someProperty('filename', _config.filename))) {
            if (this.get('customFileNames').contains(_config.filename)) {
              categoryMetaData = this.identifyCategory(_config);
              if (categoryMetaData != null) {
                configCategory = categoryMetaData.get('name');
              }
            }
            _config.id = "site property";
            _config.category = configCategory;
            _config.displayName = _config.displayName || _config.name;
            _config.recommendedValue = _config.value;
            // make all advanced configs optional and populated by default
            /*
             * if (/\${.*}/.test(_config.value) || (service.serviceName !==
             * 'OOZIE' && service.serviceName !== 'HBASE')) { _config.isRequired =
             * false; _config.value = ''; } else if
             * (/^\s+$/.test(_config.value)) { _config.isRequired = false; }
             */
            _config.isRequired = true;
            _config.displayType = _config.displayType ? _config.displayType : stringUtils.isSingleLine(_config.value) ? 'advanced' : 'multiLine';
            serviceConfigs.push(_config);
          }
        }
      }, this);
    }
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
   * Fetch cluster configs from server
   *
   * @param callback
   * @return {$.ajax}
   */
  loadClusterConfig: function (callback) {
    return App.ajax.send({
      name: 'config.cluster',
      sender: this,
      data: {
        stackVersionUrl: App.get('stackVersionURL'),
        callback: callback
      },
      success: 'loadClusterConfigSuccess',
      error: 'loadClusterConfigError'
    });
  },

  loadClusterConfigSuccess: function (data, opt, params) {
    console.log("TRACE: In success function for the loadClusterConfigSuccess; url is ", opt.url);
    var properties = [];
    if (data.items.length) {
      data.items.forEach(function (item) {
        item.StackLevelConfigurations.property_type = item.StackLevelConfigurations.property_type || [];
        item.StackLevelConfigurations.service_name = 'MISC';
        var property = this.createAdvancedPropertyObject(item.StackLevelConfigurations);
        if (property) properties.push(property);
      }, this);
    }
    params.callback(properties);
  },

  loadClusterConfigError: function (request, ajaxOptions, error, opt, params) {
    console.log('ERROR: Failed to load cluster-env configs');
    params.callback([]);
  },


  /**
   * Generate serviceProperties save it to localDB
   * called from stepController step6WizardController
   *
   * @method loadAdvancedConfig
   * @param serviceName
   * @param callback
   * @return {object|null}
   */
  loadAdvancedConfig: function (serviceName, callback) {
    return App.ajax.send({
      name: 'config.advanced',
      sender: this,
      data: {
        serviceName: serviceName,
        stackVersionUrl: App.get('stackVersionURL'),
        stackVersion: App.get('currentStackVersionNumber'),
        callback: callback
      },
      success: 'loadAdvancedConfigSuccess',
      error: 'loadAdvancedConfigError'
    });
  },

  /**
   * Generate serviceProperties save it to localDB
   * called from stepController step6WizardController
   *
   * @method loadAdvancedConfig
   * @param {Array} serviceNames
   * @param callback
   * @return {object|null}
   */
  loadAdvancedConfigAll: function (serviceNames, callback) {
    return App.ajax.send({
      name: 'config.advanced.multiple.services',
      sender: this,
      data: {
        serviceNames: serviceNames.join(','),
        stackVersionUrl: App.get('stackVersionURL'),
        stackVersion: App.get('currentStackVersionNumber'),
        callback: callback
      },
      success: 'loadAdvancedConfigAllSuccess',
      error: 'loadAdvancedConfigAllError'
    });
  },

  loadAdvancedConfigAllSuccess: function (data, opt, params, request) {
    console.log("TRACE: In success function for the loadAdvancedConfig; url is ", opt.url);
    var serviceConfigMap = {};
    if (data.items.length) {
      data.items.forEach(function (service) {
        var properties = [];
        service.configurations.forEach(function(item){
          properties.push(this.createAdvancedPropertyObject(item.StackConfigurations));
        }, this);
        serviceConfigMap[service.StackServices.service_name] = properties;
      }, this);
    }
    params.callback(serviceConfigMap, request);
  },

  loadAdvancedConfigAllError: function (request, ajaxOptions, error, opt, params) {
    console.log('ERROR: failed to load stack configs for', params.serviceNames);
    params.callback([], request);
  },

  /**
   * Load advanced configs by service names etc.
   * Use this method when you need to get configs for
   * particular services by single request
   *
   * @method loadAdvancedConfigPartial
   * @param {String[]} serviceNames
   * @param {Object} opt
   * @param {Function} callback
   * @returns {$.ajax}
   */
  loadAdvancedConfigPartial: function (serviceNames, opt, callback) {
    var data = {
      serviceList: serviceNames.join(','),
      stackVersionUrl: App.get('stackVersionURL'),
      stackVersion: App.get('currentStackVersionNumber'),
      queryFilter: ('&' + opt.queryFilter) || '',
      callback: callback
    };
    return App.ajax.send({
      name: 'config.advanced.partial',
      sender: this,
      data: data,
      success: 'loadAdvancedConfigPartialSuccess',
      error: 'loadAdvancedConfigError'
    });
  },

  loadAdvancedConfigSuccess: function (data, opt, params, request) {
    console.log("TRACE: In success function for the loadAdvancedConfig; url is ", opt.url);
    var properties = [];
    if (data.items.length) {
      data.items.forEach(function (item) {
        var property = this.createAdvancedPropertyObject(item.StackConfigurations);
        if (property) properties.push(property);
      }, this);
    }
    params.callback(properties, request);
  },

  loadAdvancedConfigError: function (request, ajaxOptions, error, opt, params) {
    console.log('ERROR: failed to load stack configs for', params.serviceName);
    params.callback([], request);
  },

  loadAdvancedConfigPartialSuccess: function (data, opt, params, request) {
    var properties = [];
    if (data.items.length && data.items.mapProperty('configurations').length) {
      var configurations = data.items.mapProperty('configurations').reduce(function (p, c) {
        return p.concat(c);
      });
      configurations.forEach(function (item) {
        var property = this.createAdvancedPropertyObject(item.StackConfigurations);
        if (property) properties.push(property);
      }, this);
    }
    params.callback(properties, request);
  },

  /**
   * Bootstrap configuration property object according to
   * format that we using in our application.
   *
   * @method createAdvancedPropertyObject
   * @param {Object} item
   * @returns {Object|Boolean}
   */
  createAdvancedPropertyObject: function (item) {
    var serviceName = item.service_name;
    var fileName = item.type;
    /**
     * Properties from mapred-queue-acls.xml are ignored
     * Properties from capacity-scheduler.xml are ignored unless HDP stack version is 2.x or
     * HDP stack version is 1.x
     */
    if (fileName == 'mapred-queue-acls.xml') return false;
    item.isVisible = fileName != 'cluster-env.xml';
    var property = {
      serviceName: serviceName,
      name: item.property_name,
      value: item.property_value,
      description: item.property_description,
      isVisible: item.isVisible,
      isFinal: item.final === "true",
      recommendedIsFinal: item.final === "true",
      filename: item.filename || fileName
    };

    return $.extend(property, this.advancedConfigIdentityData(item));
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
      'override_hbase_uid': 'Have Ambari manage UIDs'
    };
    var checkboxProperties = ['ignore_groupsusers_create', 'override_hbase_uid'];
    if (Em.isArray(config.property_type)) {
      if (config.property_type.contains('USER') || config.property_type.contains('ADDITIONAL_USER_PROPERTY') || config.property_type.contains('GROUP')) {
        propertyData.id = "puppet var";
        propertyData.category = 'Users and Groups';
        propertyData.isVisible = !App.get('isHadoopWindowsStack');
        propertyData.serviceName = 'MISC';
        propertyData.isOverridable = false;
        propertyData.isReconfigurable = false;
        propertyData.displayName = nameToDisplayNameMap[config.property_name] || App.format.normalizeName(config.property_name);
        propertyData.displayType = checkboxProperties.contains(config.property_name) ? 'checkbox' : 'user';
        if (config.service_name && !config.property_type.contains('ADDITIONAL_USER_PROPERTY')) {
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
   * @return {Object}
   **/
  createCustomGroupConfig: function (propertyName, config, group) {
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
          this.getOriginalConfigAttribute(stored, 'displayName', advancedConfigs) : App.format.normalizeName(stored.name),
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
        supportsFinal: stored.supportsFinal,
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

  getOriginalConfigAttribute: function (stored, key, advancedConfigs) {
    return advancedConfigs.findProperty('name', stored.name) ?
      advancedConfigs.findProperty('name', stored.name)[key] : stored[key];
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
   * @param {Boolean} isEmberObject
   * @returns {*}
   */
  trimProperty: function (property, isEmberObject) {
    var displayType = (isEmberObject) ? property.get('displayType') : property.displayType;
    var value = (isEmberObject) ? property.get('value') : property.value;
    var name = (isEmberObject) ? property.get('name') : property.name;
    var rez;
    switch (displayType) {
      case 'directories':
      case 'directory':
      case 'datanodedirs':
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
        name: item,
        displayName: item,
        isVisible: true,
        isReconfigurable: true
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
    if (!serviceName || serviceName == 'MISC' || !filename) {
      return false;
    } else {
      var stackService = App.StackService.find().findProperty('serviceName', serviceName);
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
        var sp = stored.findProperty("name", p.name);
        if (sp) {
          p.set("value", sp.value);
        }
      });
    }
  }
});
