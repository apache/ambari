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

  preDefinedSiteProperties: function () {
    var sitePropertiesForCurrentStack = this.preDefinedConfigFile(this.mapCustomStack(), 'site_properties');
    var serviceNames = App.StackService.find().mapProperty('serviceName').concat('MISC');
    var properties = [];
    if (sitePropertiesForCurrentStack) {
      properties =  sitePropertiesForCurrentStack.configProperties;
    } else if (App.get('isHadoop23Stack')) {
      properties = require('data/HDP2.3/site_properties').configProperties;
    } else if (App.get('isHadoop22Stack')) {
      properties = require('data/HDP2.2/site_properties').configProperties;
    } else {
      properties = require('data/HDP2/site_properties').configProperties;
    }
    return properties.filter(function(p) {
      return serviceNames.contains(p.serviceName);
    });
  }.property('App.isHadoop22Stack', 'App.isHadoop23Stack'),

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

  /**
   *
   * @param name
   * @param fileName
   * @returns {string}
   */
  configId: function(name, fileName) {
    return name + "_" + App.config.getConfigTagFromFileName(fileName);
  },

  preDefinedConfigFile: function(folder, file) {
    try {
      return require('data/{0}/{1}'.format(folder, file));
    } catch (err) {
      // the file doesn't exist, which might be expected.
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

  serviceByConfigTypeMap: function () {
    var ret = {};
    App.StackService.find().forEach(function(s) {
      Object.keys(s.get('configTypes')).forEach(function (ct) {
        ret[ct] = s;
      });
    });
    return ret;
  }.property(),

  /**
   * generates config objects
   * @param configGroups
   * @param serviceName
   * @param selectedConfigGroup
   * @param canEdit
   * @returns {Array}
   */
  mergePredefinedWithSaved: function (configGroups, serviceName, selectedConfigGroup, canEdit) {
    var configs = [];
    var serviceConfigProperty;
    var serviceByConfigTypeMap = this.get('serviceByConfigTypeMap');

    configGroups.forEach(function (siteConfig) {
      var service = serviceByConfigTypeMap[siteConfig.type];
      if (service && serviceName != 'MISC') {
        serviceName = service.get('serviceName');
      }
      var filename = App.config.getOriginalFileName(siteConfig.type);
      var attributes = siteConfig['properties_attributes'] || {};
      var finalAttributes = attributes.final || {};
      var properties = siteConfig.properties || {};

      var uiOnlyConfigsObj = {};
      var uiOnlyConfigDerivedFromTheme = App.uiOnlyConfigDerivedFromTheme.toArray();
      uiOnlyConfigDerivedFromTheme.forEach(function(item) {
        if (filename === item.filename) {
          uiOnlyConfigsObj[item.name] = item.value;
        }
      });
      properties = $.extend({}, properties, uiOnlyConfigsObj);

      for (var index in properties) {
        var id = this.configId(index, siteConfig.type);
        var preDefinedPropertyDef = this.get('preDefinedSitePropertiesMap')[id];
        var uiOnlyConfigFromTheme = uiOnlyConfigDerivedFromTheme.findProperty('name', index);
        var configsPropertyDef =  preDefinedPropertyDef  || uiOnlyConfigFromTheme;
        var advancedConfig = App.StackConfigProperty.find(id);
        var isStackProperty = !!advancedConfig.get('id') || !!preDefinedPropertyDef;
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
   * These property values has the lowest priority and can be overridden be stack/UI
   * config property but is used when such properties are absent in stack/UI configs
   * @param {string} name
   * @param {string} serviceName
   * @param {string} fileName
   * @param {boolean} definedInStack
   * @param {Object} [coreObject]
   * @returns {Object}
   */
  createDefaultConfig: function(name, serviceName, fileName, definedInStack, coreObject) {
    var tpl = {
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
      displayType: this.getDefaultDisplayType(name, fileName, coreObject ? coreObject.value : '', serviceName),
      description: null,
      category: this.getDefaultCategory(definedInStack, fileName),
      isSecureConfig: this.getIsSecure(name),
      showLabel: this.getDefaultIsShowLabel(name, fileName),
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
      belongsToService: [],
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
   * @param name
   * @param type
   * @param value
   * @param serviceName
   * @returns {string}
   */
  getDefaultDisplayType: function(name, type, value, serviceName) {
    if (this.isContentProperty(name, type)) {
      return 'content';
    } else if (serviceName && serviceName == 'FALCON' && this.getConfigTagFromFileName(type) == 'oozie-site') {
      /**
       * This specific type for 'oozie-site' configs of FALCON service.
       * After this type will be moved to stack definition this hard-code should be removed
       */
      return 'custom';
    }
    return value && !stringUtils.isSingleLine(value) ? 'multiLine' : 'string';
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
    var value = Em.isNone(originalValue) ? Em.get(serviceConfigProperty, 'value') : originalValue,
        displayType = Em.get(serviceConfigProperty, 'displayType') || Em.get(serviceConfigProperty, 'valueAttributes.type'),
        category = Em.get(serviceConfigProperty, 'category');
    switch (displayType) {
      case 'content':
      case 'string':
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
      case 'componentHosts':
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
    if ( /^\s+$/.test("" + value)) {
      value = " ";
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
      if (Em.get(a, 'index') > Em.get(b, 'index')) return 1;
      if (Em.get(a, 'index') < Em.get(b, 'index')) return -1;
      if (Em.get(a, 'name') > Em.get(b, 'index')) return 1;
      if (Em.get(a, 'name') < Em.get(b, 'index')) return -1;
      return 0;
    });
  },

  /**
   * merge stored configs with pre-defined
   * @return {Array}
   */
  mergePreDefinedWithStack: function (selectedServiceNames) {
    var mergedConfigs = [];

    var uiPersistentProperties = [
      this.configId('oozie_hostname', 'oozie-env.xml')
    ];
    var configTypesMap = {};
    App.StackService.find().filter(function (service) {
      return selectedServiceNames.contains(service.get('serviceName'));
    }).map(function (item) {
      return Em.keys(item.get('configTypes'));
    }).reduce(function (p, c) {
      return p.concat(c);
    }).concat(['cluster-env', 'alert_notification'])
      .uniq().compact().filter(function (configType) {
        return !!configType;
      }).forEach(function (c) {
        configTypesMap[c] = true;
      });
    var predefinedIds = Object.keys(this.get('preDefinedSitePropertiesMap'));
    var uiOnlyConfigDerivedFromTheme =  App.uiOnlyConfigDerivedFromTheme.mapProperty('name');
    // ui only required configs from theme are required to show configless widgets (widget that are not related to a config)
    var stackIds = [];
    var stackConfigPropertyMap = {};

    App.StackConfigProperty.find().forEach(function (scp) {
      var id = scp.get('id');
      if(scp.get('isValueDefined')) {
        stackIds.push(id);
      }
      stackConfigPropertyMap[id] = scp;
    });
    var configIds = stackIds.concat(predefinedIds).concat(uiOnlyConfigDerivedFromTheme).uniq();

    configIds.forEach(function(id) {

      var preDefined = this.get('preDefinedSitePropertiesMap')[id];
      var isUIOnlyFromTheme = App.uiOnlyConfigDerivedFromTheme.findProperty('name',id);
      var advanced = stackConfigPropertyMap[id] || Em.Object.create({});

      var name = preDefined ? preDefined.name : isUIOnlyFromTheme ? isUIOnlyFromTheme.get('name') : advanced.get('name');
      var filename = preDefined ? preDefined.filename : isUIOnlyFromTheme ? isUIOnlyFromTheme.get('filename') : advanced.get('filename');
      var isUIOnly = (Em.getWithDefault(preDefined || {}, 'isRequiredByAgent', true) === false) || isUIOnlyFromTheme;
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
      var serviceName = preDefined ? preDefined.serviceName : isUIOnlyFromTheme ? isUIOnlyFromTheme.get('serviceName') : advanced.get('serviceName');
      if (configTypesMap[this.getConfigTagFromFileName(filename)]) {
        var configData = this.createDefaultConfig(name, serviceName, filename, true, preDefined || isUIOnlyFromTheme || {});
        if (configData.recommendedValue) {
          configData.value = configData.recommendedValue;
        }
        if (advanced.get('id')) {
          configData = this.mergeStaticProperties(configData, advanced, null, ['name', 'filename']);
          configData.value = this.formatPropertyValue(advanced, advanced.get('value'));
          configData.widgetType = advanced.get('widget.type');
        }

        mergedConfigs.push(configData);
      }

    }, this);
    return mergedConfigs;
  },

  /**
   *
   * @param {string} ifStatement
   * @param {Array} serviceConfigs
   * @returns {boolean}
   */
  calculateConfigCondition: function(ifStatement, serviceConfigs) {
    // Split `if` statement if it has logical operators
    var ifStatementRegex = /(&&|\|\|)/;
    var IfConditions = ifStatement.split(ifStatementRegex);
    var allConditionResult = [];
    IfConditions.forEach(function(_condition){
      var condition = _condition.trim();
      if (condition === '&&' || condition === '||') {
        allConditionResult.push(_condition);
      }  else {
        var splitIfCondition = condition.split('===');
        var ifCondition = splitIfCondition[0];
        var result = splitIfCondition[1] || "true";
        var parseIfConditionVal = ifCondition;
        var regex = /\$\{.*?\}/g;
        var configStrings = ifCondition.match(regex);
        configStrings.forEach(function (_configString) {
          var configObject = _configString.substring(2, _configString.length - 1).split("/");
          var config = serviceConfigs.filterProperty('filename', configObject[0] + '.xml').findProperty('name', configObject[1]);
          if (config) {
            var configValue = config.value;
            parseIfConditionVal = parseIfConditionVal.replace(_configString, configValue);
          }
        }, this);
        var conditionResult = window.eval(JSON.stringify(parseIfConditionVal.trim())) === result.trim();
        allConditionResult.push(conditionResult);
      }
    }, this);
    return Boolean(window.eval(allConditionResult.join('')));
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
    var checkboxProperties = ['ignore_groupsusers_create', 'override_uid'];
    if (Em.isArray(config.property_type)) {
      if (config.property_type.contains('USER') || config.property_type.contains('ADDITIONAL_USER_PROPERTY') || config.property_type.contains('GROUP')) {
        propertyData.category = 'Users and Groups';
        propertyData.isVisible = !App.get('isHadoopWindowsStack');
        propertyData.serviceName = 'MISC';
        propertyData.displayType = checkboxProperties.contains(config.property_name) ? 'boolean' : 'user';
        if (config.property_type.contains('ADDITIONAL_USER_PROPERTY')) {
          propertyData.index = 999;//it means these configs should be shown last (if somehow we will have more that 1000 configs in tab - it wouldn't work)
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
    configTypesInfoMap[service] = configTypesInfo;
    this.set('configTypesInfoMap', configTypesInfoMap);
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
      var hostOverrideValue, hostOverrideIsFinal;
      var group = params.typeTagToGroupMap[config.type + "///" + config.tag];
      var properties = config.properties;
      for (var prop in properties) {
        var fileName = this.getOriginalFileName(config.type);
        var serviceConfig = !!params.configKeyToConfigMap[fileName] ? params.configKeyToConfigMap[fileName][prop] : false;
        if (serviceConfig) {
          // Value of this property is different for this host.
          hostOverrideValue = this.formatPropertyValue(serviceConfig, properties[prop]);
          hostOverrideIsFinal = !!(config.properties_attributes && config.properties_attributes.final && config.properties_attributes.final[prop]);
          if (!Em.get(serviceConfig, 'overrides')) Em.set(serviceConfig, 'overrides', []);
          serviceConfig.overrides.pushObject({value: hostOverrideValue, group: group, isFinal: hostOverrideIsFinal});
        } else {
          params.serviceConfigs.push(this.createCustomGroupConfig(prop, config.type, config.properties[prop], group));
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
   * @param {String} filename - config filename
   * @param {String} value - property value
   * @param {Em.Object} group - config group to set
   * @param {Boolean} isEditable
   * @return {Object}
   **/
  createCustomGroupConfig: function (propertyName, filename, value, group, isEditable) {
    var propertyObject = this.createDefaultConfig(propertyName, group.get('service.serviceName'), this.getOriginalFileName(filename), false, {
      savedValue: value,
      value: value,
      group: group,
      isEditable: isEditable !== false,
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

    var cs = App.config.createDefaultConfig('capacity-scheduler', 'YARN', 'capacity-scheduler.xml', true, {
      'value': value,
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
            var configIds = ss.get('configProperties').map(function(p) {
              return App.config.configId(p.get('name'), p.get('filename'));
            });
            properties = properties.concat(configIds);
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
  }
});
