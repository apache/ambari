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

/**
 * Mixin for loading and setting secure configs
 *
 * @type {Ember.Mixin}
 */
App.AddSecurityConfigs = Em.Mixin.create({

  kerberosDescriptor: {},

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

  /**
   * Generate stack descriptor configs.
   *
   * @returns {$.Deferred}
   */
  getDescriptorConfigs: function () {
    return this.loadDescriptorConfigs().pipe(this.createServicesStackDescriptorConfigs.bind(this));
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
      
      configObject.displayName = self._getDisplayNameForConfig(configObject.name, configObject.filename);
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
    var c = App.config.get('allPreDefinedSiteProperties').findProperty('name', name);
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
        isSecureConfig: true
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
      var _identities = service.identities || [];
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
   * This function updates stack/service/component level configurations of the kerberos descriptor
   * with the values entered by the user on the rendered ui
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
        isConfigUpdated = true
      }
    }
    return isConfigUpdated;
  },


  /**
   * This function updates stack/service/component level kerberos descriptor identities (principal and keytab)
   * with the values entered by the user on the rendered ui
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

        // compare ui rendered config against identity with `configuration attribute` (Most of the identities have `configuration attribute`)
        var isIdentityWithConfig =  (prop.configuration && prop.configuration.split('/')[0] === config.filename && prop.configuration.split('/')[1] === config.name);

        // compare ui rendered config against identity without `configuration attribute` (For example spnego principal and keytab)
        var isIdentityWithoutConfig = (!prop.configuration && identity.name === config.name.split('_')[0] && item === config.name.split('_')[1]);

        if (isIdentityWithConfig || isIdentityWithoutConfig) {
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
