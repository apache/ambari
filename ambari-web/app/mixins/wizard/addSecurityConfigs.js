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
var objectUtils = require('utils/object_utils');

/**
 * Mixin for loading and setting secure configs
 *
 * @type {Ember.Mixin}
 */
App.AddSecurityConfigs = Em.Mixin.create({

  kerberosDescriptor: {},

  secureProperties: require('data/HDP2/secure_properties').configProperties,

  kerberosDescriptorProperties: require('data/HDP2/kerberos_descriptor_properties'),

  secureMapping: require('data/HDP2/secure_mapping'),

  serviceUsersBinding: 'App.router.mainAdminSecurityController.serviceUsers',

  componentsConfig: [
    {
      serviceName: 'OOZIE',
      componentName: 'OOZIE_SERVER',
      configName: 'oozieserver_host'
    },
    {
      serviceName: 'HIVE',
      componentName: 'WEBHCAT_SERVER',
      configName: 'webhcat_server'
    }
  ],

  /**
   * mock users used in testMode
   */
  testModeUsers: [
    {
      name: 'hdfs_user',
      value: 'hdfs'
    },
    {
      name: 'mapred_user',
      value: 'mapred'
    },
    {
      name: 'hbase_user',
      value: 'hbase'
    },
    {
      name: 'hive_user',
      value: 'hive'
    }
  ],

  /**
   * security configs, which values should be modified after APPLY CONFIGURATIONS stage
   */
  secureConfigs: function () {
    var configs = [
      {
        name: 'zookeeper_principal_name',
        serviceName: 'ZOOKEEPER'
      },
      {
        name: 'knox_principal_name',
        serviceName: 'KNOX'
      },
      {
        name: 'storm_principal_name',
        serviceName: 'STORM'
      }
    ];
    if (App.get('isHadoop22Stack')) {
      configs.push({
        name: 'nimbus_principal_name',
        serviceName: 'STORM'
      });
    }
    return configs;
  }.property('App.isHadoop22Stack'),

  secureServices: function () {
    return this.get('content.services');
  }.property('content.services'),

  /**
   * prepare secure configs
   */
  prepareSecureConfigs: function () {
    var configs = this.get('content.serviceConfigProperties');
    this.set('configs', configs);
    this.loadStaticConfigs(); //Hack for properties which are declared in site_properties.js and not able to retrieve values declared in secure_properties.js
    this.loadUsersToConfigs();
    this.loadHostNames();
    this.loadPrimaryNames();
    var uiConfigs = this.loadUiSideSecureConfigs();
    this.set('configs', this.get('configs').concat(uiConfigs));
  },


  /**
   * push users to configs
   */
  loadUsersToConfigs: function () {
    if (!this.get('serviceUsers').length) {
      this.loadUsersFromServer();
    }
    App.router.get('mainAdminSecurityController.serviceUsers').forEach(function (_user) {
      this.get('configs').pushObject(_user);
    }, this);
  },

  /**
   * add component config that contain host name as value
   * @param serviceName
   * @param componentName
   * @param configName
   * @return {Boolean}
   */
  addHostConfig: function (serviceName, componentName, configName) {
    var service = App.Service.find(serviceName);
    var isServiceSecure = this.get('secureServices').someProperty('serviceName', serviceName);

    if (service.get('isLoaded') && isServiceSecure) {
      var hostComponent = service.get('hostComponents').findProperty('componentName', componentName);
      if (hostComponent) {
        var hostName = hostComponent.get('hostName');
        this.get('configs').push({
          name: configName,
          value: hostName
        });
        return true;
      }
    }
    return false;
  },

  /**
   * add hosts' names to configs
   */
  loadHostNames: function () {
    var componentsConfig = this.get('componentsConfig');
    componentsConfig.forEach(function (host) {
      this.addHostConfig(host.serviceName, host.componentName, host.configName);
    }, this);
  },

  /**
   * load static configs
   */
  loadStaticConfigs: function () {
    this.get('configs').forEach(function (_property) {
      switch (_property.name) {
        case 'security_enabled':
          _property.value = 'true';
          break;
      }
    }, this);
  },

  /**
   * add principals to properties
   */
  loadPrimaryNames: function () {
    var principalProperties = this.getPrincipalNames();
    principalProperties.forEach(function (_principalProperty) {
      var name = _principalProperty.name.replace('principal', 'primary');
      var value = _principalProperty.value.split('/')[0];
      this.get('configs').push({name: name, value: value});
    }, this);
  },

  /**
   * gather and return properties with "principal_name"
   * @return {Array}
   */
  getPrincipalNames: function () {
    var principalNames = [];
    this.get('configs').forEach(function (_property) {
      if (/principal_name?$/.test(_property.name)) {
        principalNames.push(_property);
      }
    }, this);
    this.get('secureProperties').forEach(function (_secureProperty) {
      if (/principal_name?$/.test(_secureProperty.name)) {
        var principalName = principalNames.findProperty('name', _secureProperty.name);
        if (!principalName) {
          _secureProperty.value = _secureProperty.defaultValue;
          principalNames.push(_secureProperty);
        }
      }
    }, this);
    return principalNames;
  },

  /**
   * load users from server
   */
  loadUsersFromServer: function () {
    if (App.get('testMode')) {
      var serviceUsers = this.get('serviceUsers');
      this.get('testModeUsers').forEach(function (user) {
        serviceUsers.push(user);
      }, this);
    } else {
      App.router.set('mainAdminSecurityController.serviceUsers', App.db.getSecureUserInfo());
    }
  },

  /**
   * load configs from UI side
   * @return {Array}
   */
  loadUiSideSecureConfigs: function () {
    var uiConfig = [];
    var configs = this.get('secureMapping').filterProperty('foreignKey', null).filter(function (_configProperty) {
      return (App.Service.find().mapProperty('serviceName').contains(_configProperty.serviceName));
    }, this);
    configs.forEach(function (_config) {
      var value = _config.value;
      if (_config.hasOwnProperty('dependedServiceName')) {
        value = this.checkServiceForConfigValue(value, _config.dependedServiceName);
      }
      value = this.getConfigValue(_config.templateName, value, _config.name);
      uiConfig.push({
        "name": _config.name,
        "value": value,
        "filename": _config.filename
      });
    }, this);
    var dependentConfig = this.get('secureMapping').filterProperty('foreignKey');
    dependentConfig.forEach(function (_config) {
      if (App.Service.find().mapProperty('serviceName').contains(_config.serviceName)) {
        this.setConfigValue(_config);
        this.formatConfigName(uiConfig, _config);
        uiConfig.push({
          "name": _config._name || _config.name,
          "value": _config.value,
          "filename": _config.filename
        });
      }
    }, this);
    return uiConfig;
  },

  /**
   * erase template rules from config value if service is not loaded
   * @param value
   * @param services
   * @return {*}
   */
  checkServiceForConfigValue: function (value, services) {
    services.forEach(function (_service) {
      if (!App.Service.find(_service.name).get('isLoaded')) {
        value = value.replace(_service.replace, '');
      }
    }, this);
    return value;
  },

  /**
   * Set all property that are derived from other puppet-variable
   * @param templateName
   * @param expression
   * @param name
   * @return {String|null}
   */
  getConfigValue: function (templateName, expression, name) {
    var express = expression.match(/<(.*?)>/g);
    var value = expression;
    if (Em.isNone(express)) return expression;

    express.forEach(function (_express) {
      var index = parseInt(_express.match(/\[([\d]*)(?=\])/)[1]);
      var configs = this.get('configs').findProperty('name', templateName[index]);
      var configValue = templateName[index] == 'hive_metastore' ?
        configs.value.map(function (hostName) {
          return 'thrift://' + hostName + ':9083';
        }).join(',') : configs.value;

      if (!!value) {
        value = (configs) ? App.config.replaceConfigValues(name, _express, value, configValue) : null;
      }
    }, this);
    return value;
  },

  /**
   * format name of config values of configs which match foreignKey
   * @param uiConfig
   * @param config
   * @return {Boolean}
   */
  formatConfigName: function (uiConfig, config) {
    if (Em.isNone(config.value)) return false;

    var fkValue = config.name.match(/<(foreignKey.*?)>/g);
    if (fkValue) {
      fkValue.forEach(function (_fkValue) {
        var index = parseInt(_fkValue.match(/\[([\d]*)(?=\])/)[1]);
        var value;
        if (uiConfig.someProperty('name', config.foreignKey[index])) {
          value = uiConfig.findProperty('name', config.foreignKey[index]).value;
          config._name = config.name.replace(_fkValue, value);
        } else if (this.get('configs').someProperty('name', config.foreignKey[index])) {
          value = this.get('configs').findProperty('name', config.foreignKey[index]).value;
          config._name = config.name.replace(_fkValue, value);
        }
      }, this);
      return true;
    }
    return false;
  },

  /**
   * Set config value with values of configs which match template
   * @param config
   * @return {Boolean}
   */
  setConfigValue: function (config) {
    if (Em.isNone(config.value)) return false;

    //For properties in the configMapping file having foreignKey and templateName properties.
    var templateValue = config.value.match(/<(templateName.*?)>/g);
    if (templateValue) {
      templateValue.forEach(function (_value) {
        var index = parseInt(_value.match(/\[([\d]*)(?=\])/)[1]);
        var cfg = this.get('configs').findProperty('name', config.templateName[index]);

        if (cfg) {
          var cfgValue = config.templateName[index] == 'hive_metastore' ? cfg.value.join(',') : cfg.value;
          config.value = config.value.replace(_value, cfgValue);
        } else {
          config.value = null;
        }
      }, this);
      return true;
    }
    return false;
  },

  /**
   * set value of principal property
   * @param serviceName
   * @param principalName
   * @return {Boolean}
   */
  setPrincipalValue: function (serviceName, principalName) {
    var siteProperties = this.get('configs');

    var realmName = siteProperties.findProperty('name', 'kerberos_domain');

    if (this.get('secureServices').someProperty('serviceName', serviceName)) {
      var principalProperty = siteProperties.findProperty('name', principalName);
      principalProperty.value = principalProperty.value + '@' + realmName.value;
      return true;
    }
    return false;
  },

  /**
   * Generate stack descriptor configs.
   *  - Load kerberos artifacts from stack endpoint
   *  - Load kerberos artifacts from cluster resource and merge them with stack descriptor.
   * When cluster descriptor is absent then stack artifacts used.
   *
   * @returns {$.Deferred}
   */
  getDescriptorConfigs: function () {
    var dfd = $.Deferred();
    var self = this;
    this.loadStackDescriptorConfigs().then(function(data) {
      var stackArtifacts = data;
      self.loadClusterDescriptorConfigs().then(function(clusterArtifacts) {
        self.storeClusterDescriptorStatus(true);
        dfd.resolve(self.createServicesStackDescriptorConfigs(objectUtils.deepMerge(data, clusterArtifacts)));
      }, function() {
        self.storeClusterDescriptorStatus(false);
        dfd.resolve(self.createServicesStackDescriptorConfigs(stackArtifacts));
      });
    }, function() {
      dfd.reject();
    });
    return dfd.promise();
  },


  /**
   * Store status of kerberos descriptor located in cluster artifacts.
   * This status needed for Add Service Wizard to select appropriate method to create
   * or update descriptor.
   *
   * @param  {Boolean} isExists <code>true</code> if cluster descriptor present
   */
  storeClusterDescriptorStatus: function(isExists) {
    if (this.get('isWithinAddService')) {
      this.get('wizardController').setDBProperty('isClusterDescriptorExists', isExists);
    }
  },

  /**
   *
   * @param {object[]} items - stack descriptor json response
   * @returns {App.ServiceConfigProperty[]}
   */
  createServicesStackDescriptorConfigs: function (items) {
    var self = this;
    var configs = [];
    var clusterConfigs = [];
    var kerberosDescriptor = items.artifact_data;
    this.set('kerberosDescriptor', kerberosDescriptor);
    // generate configs for root level properties object, currently realm, keytab_dir
    clusterConfigs = clusterConfigs.concat(this.expandKerberosStackDescriptorProps(kerberosDescriptor.properties, 'Cluster', 'stackConfigs'));
    // generate configs for root level identities object, currently spnego property
    clusterConfigs = clusterConfigs.concat(this.createConfigsByIdentities(kerberosDescriptor.identities, 'Cluster'));
    kerberosDescriptor.services.forEach(function (service) {
      var serviceName = service.name;
      // generate configs for service level identity objects
      configs = configs.concat(self.createResourceConfigs(service, serviceName));
      // generate configs for service component level identity  object
      service.components.forEach(function (component) {
        configs = configs.concat(self.createResourceConfigs(component, serviceName));
      });
    });
    // unite cluster, service and component configs
    configs = configs.concat(clusterConfigs);
    self.processConfigReferences(kerberosDescriptor, configs);
    return configs;
  },

  /**
   *
   * @param {Object} resource
   * @param {String} serviceName
   * @return {Array}
   */
  createResourceConfigs: function (resource, serviceName) {
    var identityConfigs = [];
    var resourceConfigs = [];
    if (resource.identities) {
      identityConfigs = this.createConfigsByIdentities(resource.identities, serviceName);
    }
    if (resource.configurations) {
      resource.configurations.forEach(function (_configuration) {
        for (var key in _configuration) {
          resourceConfigs = resourceConfigs.concat(this.expandKerberosStackDescriptorProps(_configuration[key], serviceName, key));
        }
      }, this);
    }
    return identityConfigs.concat(resourceConfigs);
  },

  /**
   * Create service properties based on component identity
   *
   * @param {object[]} identities
   * @param {string} serviceName
   * @returns {App.ServiceConfigProperty[]}
   */
  createConfigsByIdentities: function (identities, serviceName) {
    var self = this;
    var configs = [];
    identities.forEach(function (identity) {
      var defaultObject = {
        isOverridable: false,
        isVisible: true,
        isSecureConfig: true,
        serviceName: serviceName,
        name: identity.name,
        identityType: identity.principal && identity.principal.type
      };
      self.parseIdentityObject(identity).forEach(function (item) {
        configs.push(App.ServiceConfigProperty.create($.extend({}, defaultObject, item)));
      });
    });

    return configs;
  },

  /**
   * Bootstrap base object according to identity info. Generate objects will be converted to
   * App.ServiceConfigProperty model class instances.
   *
   * @param {object} identity
   * @returns {object[]}
   */
  parseIdentityObject: function (identity) {
    var result = [];
    var name = identity.name;
    var self = this;
    Em.keys(identity).without('name').forEach(function (item) {
      var configObject = {};
      var prop = identity[item];
      var itemValue = prop[{keytab: 'file', principal: 'value'}[item]];
      var predefinedProperty;
      // skip inherited property without `configuration` and `keytab` or `file` values
      if (!prop.configuration && !itemValue) return;
      // inherited property with value should not observe value from reference
      if (name.startsWith('/') && !itemValue) {
        configObject.referenceProperty = name.substring(1) + ':' + item;
        configObject.isEditable = false;
      }
      configObject.defaultValue = configObject.savedValue = configObject.value = itemValue;
      configObject.filename = prop.configuration ? prop.configuration.split('/')[0] : 'cluster-env';
      configObject.name = prop.configuration ? prop.configuration.split('/')[1] : name + '_' + item;

      predefinedProperty = self.get('kerberosDescriptorProperties').findProperty('name', configObject.name);
      configObject.displayName = self._getDisplayNameForConfig(configObject.name, configObject.filename);
      configObject.index = predefinedProperty && !Em.isNone(predefinedProperty.index) ? predefinedProperty.index : Infinity;
      result.push(configObject);
    });
    return result;
  },

  /**
   * Get new config display name basing on its name and filename
   * If config <code>fileName</code> is `cluster-env`, normalizing for its <code>name</code> is used (@see App.format.normalizeName)
   * If config is predefined in the <code>secureProperties</code> (and it's displayName isn't empty there), predefined displayName is used
   * Otherwise - config <code>name</code> is returned
   *
   * @param {string} name config name
   * @param {string} fileName config filename
   * @returns {String} new config display name
   * @method _getDisplayNameForConfig
   * @private
   */
  _getDisplayNameForConfig: function(name, fileName) {
    var c = this.get('secureProperties').findProperty('name', name);
    var dName = c ? Em.get(c, 'displayName') : '';
    dName = Em.isEmpty(dName) ? name : dName;
    return fileName == 'cluster-env' ? App.format.normalizeName(name) : dName;
  },

  /**
   * Wrap kerberos properties to App.ServiceConfigProperty model class instances.
   *
   * @param {object} kerberosProperties
   * @param {string} serviceName
   * @param {string} filename
   * @returns {App.ServiceConfigProperty[]}
   */
  expandKerberosStackDescriptorProps: function (kerberosProperties, serviceName, filename) {
    var configs = [];

    for (var propertyName in kerberosProperties) {
      var predefinedProperty = this.get('kerberosDescriptorProperties').findProperty('name', propertyName);
      var propertyObject = {
        name: propertyName,
        value: kerberosProperties[propertyName],
        defaultValue: kerberosProperties[propertyName],
        savedValue: kerberosProperties[propertyName],
        serviceName: serviceName,
        filename: filename,
        displayName: serviceName == "Cluster" ? App.format.normalizeName(propertyName) : propertyName,
        isOverridable: false,
        isEditable: propertyName != 'realm',
        isRequired: propertyName != 'additional_realms',
        isSecureConfig: true,
        placeholderText: predefinedProperty && !Em.isNone(predefinedProperty.index) ? predefinedProperty.placeholderText : '',
        index: predefinedProperty && !Em.isNone(predefinedProperty.index) ? predefinedProperty.index : Infinity
      };
      configs.push(App.ServiceConfigProperty.create(propertyObject));
    }

    return configs;
  },


  /**
   * Take care about configs that should observe value from referenced configs.
   * Reference is set with `referenceProperty` key.
   *
   * @param {object[]} kerberosDescriptor
   * @param {App.ServiceConfigProperty[]} configs
   */
  processConfigReferences: function (kerberosDescriptor, configs) {
    var identities = kerberosDescriptor.identities;
    identities = identities.concat(kerberosDescriptor.services.map(function (service) {
      if (service.components && !!service.components.length) {
        identities = identities.concat(service.components.mapProperty('identities').reduce(function (p, c) {
          return p.concat(c);
        }, []));
        return identities;
      }
    }).reduce(function (p, c) {
      return p.concat(c);
    }, []));
    // clean up array
    identities = identities.compact().without(undefined);
    configs.forEach(function (item) {
      var reference = item.get('referenceProperty');
      if (!!reference) {
        var identity = identities.findProperty('name', reference.split(':')[0])[reference.split(':')[1]];
        if (identity && !!identity.configuration) {
          item.set('observesValueFrom', identity.configuration.split('/')[1]);
        } else {
          item.set('observesValueFrom', reference.replace(':', '_'));
        }
      }
    });
  },

  /**
   * update the kerberos descriptor to be put on cluster resource with user customizations
   * @param kerberosDescriptor {Object}
   * @param configs {Object}
   */
  updateKerberosDescriptor: function (kerberosDescriptor, configs) {
    configs.forEach(function (_config) {
      var isConfigUpdated;
      var isStackResouce = true;
      isConfigUpdated = this.updateResourceIdentityConfigs(kerberosDescriptor, _config, isStackResouce);
      if (!isConfigUpdated) {
        kerberosDescriptor.services.forEach(function (_service) {
          isConfigUpdated = this.updateResourceIdentityConfigs(_service, _config);
          if (!isConfigUpdated) {
            _service.components.forEach(function (_component) {
              isConfigUpdated = this.updateResourceIdentityConfigs(_component, _config);
            }, this);
          }
        }, this);
      }
    }, this);
  },

  /**
   * Updates the identity configs or configurations at a resource. A resource could be
   * 1) Stack
   * 2) Service
   * 3) Component
   * @param resource
   * @param config
   * @param isStackResource
   * @return boolean
   */
  updateResourceIdentityConfigs: function (resource, config, isStackResource) {
    var isConfigUpdated;
    var identities = resource.identities;
    var properties = !!isStackResource ? resource.properties : resource.configurations;
    isConfigUpdated = this.updateDescriptorConfigs(properties, config);
    if (!isConfigUpdated) {
      if (identities) {
        isConfigUpdated = this.updateDescriptorIdentityConfig(identities, config);
      }
    }
    return isConfigUpdated;
  },

  /**
   *
   * @param configurations
   * @param config
   * @return boolean
   */
  updateDescriptorConfigs: function (configurations, config) {
    var isConfigUpdated;
    if (!!configurations) {
      if (Array.isArray(configurations)) {
        configurations.forEach(function (_configuration) {
          for (var key in _configuration) {
            if (Object.keys(_configuration[key]).contains(config.name) && config.filename === key) {
              _configuration[key][config.name] = config.value;
              isConfigUpdated = true
            }
          }
        }, this);
      } else if (Object.keys(configurations).contains(config.name) && config.filename === 'stackConfigs') {
        configurations[config.name] = config.value;
        isConfigUpdated = true;
      }
    }
    return isConfigUpdated;
  },


  /**
   *
   * @param identities
   * @param config
   * @return boolean
   */
  updateDescriptorIdentityConfig: function (identities, config) {
    var isConfigUpdated = false;
    identities.forEach(function (identity) {
      var keys = Em.keys(identity).without('name');
      keys.forEach(function (item) {
        var prop = identity[item];
        if (prop.configuration && prop.configuration.split('/')[0] === config.filename &&
          prop.configuration.split('/')[1] === config.name) {
          prop[{keytab: 'file', principal: 'value'}[item]] = config.value;
          isConfigUpdated = true;
        }
      });
    }, this);
    return isConfigUpdated;
  },

  /**
   * Make request for stack descriptor configs if cluster  is not secure
   * or cluster descriptor configs if cluster is secure
   * @returns {$.ajax}
   * @method loadStackDescriptorConfigs
   */
  loadDescriptorConfigs: function() {
    if (this.get('shouldLoadClusterDescriptor')) {
      return this.loadClusterDescriptorConfigs();
    } else {
      return this.loadStackDescriptorConfigs();
    }
  },

  /**
   * Check if cluster descriptor should be loaded
   * @returns {Boolean}
   */
  shouldLoadClusterDescriptor: function() {
    return App.get('isKerberosEnabled') && !App.router.get('mainAdminKerberosController.defaultKerberosLoaded');
  }.property('App.isKerberosEnabled', 'App.router.mainAdminKerberosController.defaultKerberosLoaded'),

  /**
   * Make request for stack descriptor configs.
   * @returns {$.ajax}
   * @method loadStackDescriptorConfigs
   */
  loadStackDescriptorConfigs: function () {
    return App.ajax.send({
      sender: this,
      name: 'admin.kerberize.stack_descriptor',
      data: {
        stackName: App.get('currentStackName'),
        stackVersionNumber: App.get('currentStackVersionNumber')
      }
    });
  },

  /**
   * Make request for cluster descriptor configs.
   * @returns {$.ajax}
   * @method loadClusterDescriptorConfigs
   */
  loadClusterDescriptorConfigs: function () {
    return App.ajax.send({
      sender: this,
      name: 'admin.kerberize.cluster_descriptor'
    });
  }
});
