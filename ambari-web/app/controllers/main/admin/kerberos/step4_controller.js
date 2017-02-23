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
var blueprintUtils = require('utils/blueprint');
require('controllers/wizard/step7_controller');

App.KerberosWizardStep4Controller = App.WizardStep7Controller.extend(App.AddSecurityConfigs, App.ToggleIsRequiredMixin, App.KDCCredentialsControllerMixin, {
  name: 'kerberosWizardStep4Controller',
  isWithinAddService: Em.computed.equal('wizardController.name', 'addServiceController'),

  // stores configurations loaded by ConfigurationsController.getConfigsByTags
  servicesConfigurations: null,

  clearStep: function() {
    this.set('isRecommendedLoaded', false);
    this.set('submitButtonClicked', false);
    this.set('selectedService', null);
    this.set('stepConfigs', []);
  },

  loadStep: function() {
    var self, stored;
    if (this.get('wizardController.skipConfigureIdentitiesStep')) {
      App.router.send('next');
      return;
    }
    self = this;
    this.clearStep();
    stored = this.get('wizardController').loadCachedStepConfigValues(this) || [];
    this.getDescriptor().then(function (kerberosDescriptor) {
      var stepConfigs = self.setStepConfigs(self.createServicesStackDescriptorConfigs(kerberosDescriptor));
      self.set('stepConfigs', stepConfigs);
      // when configurations were stored no need to apply recommendations again
      if (App.get('supports.kerberosStackAdvisor') && !stored.length) {
        self.bootstrapRecommendationPayload(kerberosDescriptor).then(function(recommendations) {
          self.loadServerSideConfigsRecommendations(recommendations).always(function() {
            self.applyServiceConfigs(stepConfigs);
          });
        });
      } else {
        self.applyServiceConfigs(stepConfigs);
      }
    }, function() {
      self.set('isRecommendedLoaded', true);
    });
  },

  /**
   * Get descriptor configs from API endpoint.
   * On <b>Enable Kerberos</b> loads descriptor from cluster STACK resource.
   * On <b>Add Service Wizard</b> first check for cluster's artifacts descriptor and
   * save it presence status, then loads from cluster COMPOSITE resource.
   * Check for cluster/artifacts/kerberos_descriptor is necessary to determine updating or creation
   * kerberos descriptor.
   *
   * @returns {$.Deferred}
   */
  getDescriptor: function() {
    var self = this;
    var dfd = $.Deferred();
    var successCallback = function(data) {
      dfd.resolve(data);
    };
    var checkDescriptor = function() {
      if (self.get('isWithinAddService')) {
        return App.ajax.send({
          sender: self,
          name: 'admin.kerberize.cluster_descriptor_artifact'
        });
      }
      return $.Deferred().resolve().promise();
    };

    checkDescriptor().always(function(data, status) {
      self.storeClusterDescriptorStatus(status === 'success');
      self.loadClusterDescriptorConfigs(self.get('isWithinAddService') ? self.get('selectedServiceNames') : false).then(successCallback);
    });
    return dfd.promise();
  },

  /**
   * Create service config object for Kerberos service.
   *
   * @param {App.ServiceConfigProperty[]} configs
   * @returns {Em.Object}
   */
  createServiceConfig: function(configs) {
    // Identity configs related to user principal
    var clusterConfigs = configs.filterProperty('serviceName','Cluster');
    // storm user principal is not required for ambari operation
    var userConfigs = configs.filterProperty('identityType','user');
    var generalConfigs = clusterConfigs.concat(userConfigs).uniq('name');
    var advancedConfigs = configs.filter(function(element){
      return !generalConfigs.findProperty('name', element.get('name'));
    });
    var categoryForGeneralConfigs = [
      App.ServiceConfigCategory.create({ name: 'Global', displayName: 'Global'}),
      App.ServiceConfigCategory.create({ name: 'Ambari Principals', displayName: 'Ambari Principals'})
    ];
    var categoryForAdvancedConfigs = this.createCategoryForServices();
    return [
      App.ServiceConfig.create({
      displayName: 'General',
      name: 'GENERAL',
      serviceName: 'KERBEROS_GENERAL',
      configCategories: categoryForGeneralConfigs,
      configs: generalConfigs,
      configGroups: [],
      showConfig: true
    }),
      App.ServiceConfig.create({
        displayName: 'Advanced',
        name: 'ADVANCED',
        serviceName: 'KERBEROS_ADVANCED',
        configCategories: categoryForAdvancedConfigs,
        configs: advancedConfigs,
        configGroups: [],
        showConfig: true
      })
    ];
  },

  /**
   * creates categories for advanced secure configs
   * @returns {[App.ServiceConfigCategory]}
   */
  createCategoryForServices: function() {
    var services = [];
    if (this.get('wizardController.name') === 'addServiceController') {
      services = App.StackService.find().filter(function(item) {
        return item.get('isInstalled') || item.get('isSelected');
      });
    } else {
      services = App.Service.find();
    }
    return services.map(function(item) {
      return App.ServiceConfigCategory.create({ name: item.get('serviceName'), displayName: item.get('displayName'), collapsedByDefault: true});
    });
  },

  /**
   * Prepare step configs using stack descriptor properties.
   *
   * @param {App.ServiceConfigProperty[]} configs
   * @param {App.ServiceConfigProperty[]} stackConfigs converted kerberos descriptor
   */
  setStepConfigs: function(configs, stackConfigs) {
    var configProperties = this.prepareConfigProperties(configs),
      stackConfigProperties = stackConfigs ? this.prepareConfigProperties(stackConfigs) : [],
      alterProperties = ['value','initialValue', 'defaultValue'];
    if (this.get('wizardController.name') === 'addServiceController') {
      // config properties for installed services should be disabled on Add Service Wizard
      configProperties.forEach(function(item) {
        if (this.get('installedServiceNames').contains(item.get('serviceName')) || item.get('serviceName') == 'Cluster') {
          item.set('isEditable', false);
        } else if (stackConfigs) {
          var stackConfigProperty = stackConfigProperties.filterProperty('filename', item.get('filename')).findProperty('name', item.get('name'));
          if (stackConfigProperty) {
            alterProperties.forEach(function (alterProperty) {
              item.set(alterProperty, stackConfigProperty.get(alterProperty));
            });
          }
        }
      }, this);
      // Concat properties that are present in the stack's kerberos  descriptor but not in the cluster kerberos descriptor
      stackConfigProperties.forEach(function(_stackConfigProperty){
        var isPropertyInClusterDescriptor = configProperties.filterProperty('filename', _stackConfigProperty.get('filename')).someProperty('name', _stackConfigProperty.get('name'));
        if (!isPropertyInClusterDescriptor) {
          if (this.get('installedServiceNames').contains(_stackConfigProperty.get('serviceName')) || _stackConfigProperty.get('serviceName') === 'Cluster') {
            _stackConfigProperty.set('isEditable', false);
          }
          configProperties.pushObject(_stackConfigProperty);
        }
      }, this);
    }
    configProperties = App.config.sortConfigs(configProperties);
    var stepConfigs = this.createServiceConfig(configProperties);
    this.set('selectedService', stepConfigs[0]);
    this.get('stepConfigs').pushObjects(stepConfigs);
    return stepConfigs;
  },

  /**
   * Filter configs by installed services for Kerberos Wizard or by installed + selected services
   * for Add Service Wizard.
   * Set property value observer.
   * Set realm property with value from previous configuration step.
   * Set appropriate category for all configs.
   * Hide KDC related credentials properties if kerberos was manually enabled.
   *
   * @param {App.ServiceConfigProperty[]} configs
   * @returns {App.ServiceConfigProperty[]}
   */
  prepareConfigProperties: function(configs) {
    var self = this;
    // stored configs from previous steps (Configure Kerberos or Customize Services for ASW)
    var storedServiceConfigs = this.get('wizardController.content.serviceConfigProperties');
    var installedServiceNames = ['Cluster', 'AMBARI'].concat(App.Service.find().mapProperty('serviceName'));
    var configProperties = configs.slice(0);
    var siteProperties = App.configsCollection.getAll();
    var realmValue;
    // override stored values
    App.config.mergeStoredValue(configProperties, this.get('wizardController').loadCachedStepConfigValues(this));

    // show admin properties in add service wizard
    if (this.get('isWithinAddService')) {
      installedServiceNames = installedServiceNames.concat(this.get('selectedServiceNames'));
    }
    configProperties = configProperties.filter(function(item) {
      return installedServiceNames.contains(item.get('serviceName'));
    });
    if (this.get('wizardController.name') !== 'addServiceController') {
      realmValue = storedServiceConfigs.findProperty('name', 'realm').value;
      configProperties.findProperty('name', 'realm').set('value', realmValue);
      configProperties.findProperty('name', 'realm').set('savedValue', realmValue);
      configProperties.findProperty('name', 'realm').set('recommendedValue', realmValue);
    }

    configProperties.setEach('isSecureConfig', false);
    configProperties.forEach(function(property, item, allConfigs) {
      if (['spnego_keytab', 'spnego_principal'].contains(property.get('name'))) {
        property.addObserver('value', self, 'spnegoPropertiesObserver');
      }
      if (property.get('observesValueFrom') && allConfigs.someProperty('name', property.get('observesValueFrom'))) {
        var observedValue = Em.get(allConfigs.findProperty('name', property.get('observesValueFrom')), 'value');
        property.set('value', observedValue);
        property.set('recommendedValue', observedValue);
        property.set('isVisible', true);
      }
      if (property.get('serviceName') === 'Cluster') {
        property.set('category', 'Global');
      }
      else {
        property.set('category', property.get('serviceName'));
      }
      // All user identity except storm should be grouped under "Ambari Principals" category
      if (property.get('identityType') == 'user') property.set('category', 'Ambari Principals');
      var siteProperty = siteProperties.findProperty('name', property.get('name'));
      if (siteProperty) {
        if (siteProperty.category === property.get('category')) {
          property.set('displayName',siteProperty.displayName);
          if (siteProperty.index) {
            property.set('index', siteProperty.index);
          }
        }
        if (siteProperty.displayType) {
          property.set('displayType', siteProperty.displayType);
        }
      }
      this.tweakConfigProperty(property);
    },this);

    return configProperties;
  },

  /**
   * Function to override kerberos descriptor's property values
   */
  tweakConfigProperty: function(config) {
    var defaultHiveMsPort = "9083",
        hiveMSHosts,
        port,
        hiveMSHostNames,
        configValue;
    if (config.name === 'templeton.hive.properties') {
      hiveMSHosts = App.HostComponent.find().filterProperty('componentName', 'HIVE_METASTORE');
      if (hiveMSHosts.length > 1) {
        hiveMSHostNames = hiveMSHosts.mapProperty('hostName');
        port = config.value.match(/:[0-9]{2,4}/);
        port = port ? port[0].slice(1) : defaultHiveMsPort;
        for (var i = 0; i < hiveMSHostNames.length; i++) {
          hiveMSHostNames[i] = "thrift://" + hiveMSHostNames[i] + ":" + port;
        }
        configValue = config.value.replace(/thrift.+[0-9]{2,},/i, hiveMSHostNames.join('\\,') + ",");
        config.set('value', configValue);
        config.set('recommendedValue', configValue);
      }
    }
  },

  /**
   * Sync up values between inherited property and its reference.
   *
   * @param {App.ServiceConfigProperty} configProperty
   */
  spnegoPropertiesObserver: function(configProperty) {
    var stepConfig = this.get('stepConfigs').findProperty('name', 'ADVANCED');

    stepConfig.get('configs').forEach(function(config) {
      if (config.get('observesValueFrom') === configProperty.get('name')) {
        Em.run.once(this, function() {
          config.set('value', configProperty.get('value'));
          config.set('recommendedValue', configProperty.get('value'));
        });
      }
    }, this);
  },

  submit: function() {
    this.set('submitButtonClicked', true);
    this.saveConfigurations();
    App.router.send('next');
  },

  saveConfigurations: function() {
    var kerberosDescriptor = this.get('kerberosDescriptor');
    var configs = [];

    this.get('stepConfigs').forEach(function(_stepConfig){
      configs = configs.concat(_stepConfig.get('configs'));
    });
    this.updateKerberosDescriptor(kerberosDescriptor, configs);
    App.get('router.kerberosWizardController').saveKerberosDescriptorConfigs(kerberosDescriptor);
  },

  /**
   * Add/update property in `properties` object for each config type with
   * associated kerberos descriptor config value.
   *
   * @private
   * @param {object[]} configurations
   * @param {App.ServiceConfigProperty[]} kerberosDescriptor
   * @returns {object[]}
   */
  mergeDescriptorToConfigurations: function(configurations, kerberosDescriptor) {
    return configurations.map(function(configType) {
      var properties = $.extend({}, configType.properties);
      var filteredDescriptor = kerberosDescriptor.filterProperty('filename', configType.type);
      if (filteredDescriptor.length) {
        filteredDescriptor.forEach(function(descriptorConfig) {
          var configName = Em.get(descriptorConfig, 'name');
          properties[configName] = Em.get(descriptorConfig, 'value');
        });
      }
      return {
        type: configType.type,
        version: configType.version,
        tag: configType.tag,
        properties: properties
      };
    });
  },

  loadServerSideConfigsRecommendations: function(recommendations) {
    return App.ajax.send({
      'name': 'config.recommendations',
      'sender': this,
      'data': {
        stackVersionUrl: App.get('stackVersionURL'),
        dataToSend: {
          recommend: 'configurations',
          hosts: App.get('allHostNames'),
          services: this.get('serviceNames'),
          recommendations: recommendations
        }
      },
      'success': 'loadRecommendationsSuccess',
      'error': 'loadRecommendationsError'
    });
  },

  loadRecommendationsError: function(req, ajaxOpts, error, opt) {
    var resp;
    try {
      resp = $.parseJSON(req.responseText);
    } catch (e) { }
    return App.ModalPopup.show({
      header: Em.I18n.t('common.error'),
      secondary: false,
      bodyClass: App.AjaxDefaultErrorPopupBodyView.extend({
        type: opt.type || 'GET',
        url: opt.url,
        status: req.status,
        message: resp && resp.message || req.responseText
      })
    });
  },

  applyServiceConfigs: function(stepConfigs) {
    this.set('isRecommendedLoaded', true);
    this.set('selectedService', stepConfigs[0]);
  },

  /**
   * Callback executed when all configs specified by tags are loaded.
   * Here we handle configurations for instlled services and Kerberos.
   * Gather needed info for recommendation request such as configurations object.
   *
   * @override
   */
  getConfigTagsSuccess: function(data) {
    // here we get all installed services including KERBEROS
    var serviceNames = App.Service.find().mapProperty('serviceName').concat(['KERBEROS']).uniq();
    // collect all config types for selected services
    var installedServiceSites = Array.prototype.concat.apply([], App.config.get('preDefinedServiceConfigs').filter(function(serviceConfig) {
      return serviceNames.contains(Em.get(serviceConfig, 'serviceName'));
    }).map(function (service) {
      // when service have no configs return <code>null</code> instead return config types
      if (!service.get('configTypes')) return null;
      return Object.keys(service.get('configTypes'));
    }, this).compact()).uniq(); // cleanup <code>null</code>

    // take all configs for selected services by config types recieved from API response
    var serviceConfigTags = Em.keys(data.Clusters.desired_configs).reduce(function(tags, site) {
      if (data.Clusters.desired_configs.hasOwnProperty(site)) {
        // push cluster-env.xml also since it not associated with any service but need to further processing
        if (installedServiceSites.contains(site) || site === 'cluster-env') {
          tags.push({
            siteName: site,
            tagName: data.Clusters.desired_configs[site].tag,
            newTagName: null
          });
        }
      }
      return tags;
    }, []);
    // store configurations
    this.set('serviceConfigTags', serviceConfigTags);
    this.set('isAppliedConfigLoaded', true);
  },

  /**
   * Prepare all necessary data for recommendations payload.
   *
   * #mutates initialConfigValues
   * @returns {$.Deferred.promise()}
   */
  bootstrapRecommendationPayload: function(kerberosDescriptor) {
    var dfd = $.Deferred();
    var self = this;

    this.getServicesConfigurations().then(function(configurations) {
      var recommendations = self.getBlueprintPayloadObject(configurations, kerberosDescriptor);
      self.set('servicesConfigurations', configurations);
      self.set('initialConfigValues', recommendations.blueprint.configurations);
      dfd.resolve(recommendations);
    });
    return dfd.promise();
  },

  getServicesConfigurations: function() {
    var dfd = $.Deferred();
    var self = this;
    var configs, servicesConfigurations;
    if (this.get('isWithinAddService')) {
      configs = this.get('content.serviceConfigProperties');
      servicesConfigurations = configs.reduce(function(configTags, property) {
        var fileName = App.config.getConfigTagFromFileName(property.filename),
            configType;
        if (!configTags.someProperty('type', fileName)) {
          configTags.push({
            type: fileName,
            properties: {}
          });
        }
        configType = configTags.findProperty('type', fileName);
        configType.properties[property.name] = property.value;
        return configTags;
      }, []);
      dfd.resolve(servicesConfigurations);
    } else {
      this.getConfigTags().then(function() {
        App.router.get('configurationController').getConfigsByTags(self.get('serviceConfigTags')).done(function (configurations) {
          dfd.resolve(configurations);
        });
      });
    }

    return dfd.promise();
  },

  /**
   * Returns payload for recommendations request.
   * Takes services' configurations and merge them with kerberos descriptor properties.
   *
   * @param {object[]} configurations services' configurations fetched from API
   * @param {App.ServiceConfigProperty[]} kerberosDescriptor descriptor configs
   * @returns {object} payload for recommendations request
   */
  getBlueprintPayloadObject: function(configurations, kerberosDescriptor) {
    var recommendations = blueprintUtils.generateHostGroups(App.get('allHostNames'));
    var mergedConfigurations = this.mergeDescriptorToConfigurations(configurations, this.createServicesStackDescriptorConfigs(kerberosDescriptor));
    recommendations.blueprint.configurations = mergedConfigurations.reduce(function(p, c) {
      p[c.type] = {};
      p[c.type].properties = c.properties;
      return p;
    }, {});

    return recommendations;
  },

  /**
   * @override
   */
  _saveRecommendedValues: function(data) {
    var recommendedConfigurations = Em.getWithDefault(data, 'resources.0.recommendations.blueprint.configurations', {});
    var allConfigs = Array.prototype.concat.apply([], this.get('stepConfigs').mapProperty('configs'));
    var self = this;
    // iterate by each config file name e.g. hdfs-site
    var groupedProperties = this.groupRecommendationProperties(recommendedConfigurations, this.get('servicesConfigurations'), allConfigs);
    var newProperties = [];
    Em.keys(groupedProperties.add).forEach(function(fileName) {
      var serviceName = self.getServiceByFilename(fileName);
      Em.keys(groupedProperties.add[fileName]).forEach(function(propertyName) {
        var property = self._createNewProperty(propertyName, fileName, serviceName, groupedProperties.add[fileName][propertyName]);
        property.set('category', serviceName);
        property.set('isOverridable', false);
        property.set('supportsFinal', false);
        property.set('isUserProperty', false);
        property.set('filename', fileName);
        newProperties.push(property);
      });
    });
    Array.prototype.push.apply(self.getServicesConfigObject().get('configs'), newProperties);
    Em.keys(groupedProperties.update).forEach(function(fileName) {
      Em.keys(groupedProperties.update[fileName]).forEach(function(propertyName) {
        var configProperty = allConfigs.filterProperty('filename', fileName).findProperty('name', propertyName);
        if (configProperty) {
          self._updateConfigByRecommendation(configProperty, groupedProperties.update[fileName][propertyName], true, false);
        }
      });
    });
    Em.keys(groupedProperties.delete).forEach(function(fileName) {
      Em.keys(groupedProperties.delete[fileName]).forEach(function(propertyName) {
        var servicesConfigs = self.getServicesConfigObject().get('configs');
        servicesConfigs.removeObject(servicesConfigs.filterProperty('filename', fileName).findProperty('name', propertyName));
      });
    });
  },

  /**
   * Returns category where services' configuration located.
   *
   * @returns {App.ServiceConfig}
   */
  getServicesConfigObject: function() {
    return this.get('stepConfigs').findProperty('name', 'ADVANCED');
  },

  /**
   * Returns map with appropriate action and properties to process with.
   * Key is an action e.g. `add`, `update`, `delete` and value is  an object `fileName` -> `propertyName`: `propertyValue`.
   *
   * @param {object} recommendedConfigurations
   * @param {object[]} servicesConfigurations services' configurations fetched from API
   * @param {App.ServiceConfigProperty[]} allConfigs all current configurations stored in controller, basically kerberos descriptor
   * @returns {object}
   */
  groupRecommendationProperties: function(recommendedConfigurations, servicesConfigurations, allConfigs) {
    var resultMap = {
      update: {},
      add: {},
      delete: {}
    };

    /**
     * Adds property to associated group `add`,`delete`,`update`.
     *
     * @param {object} propertyMap <code>resultMap</code> object
     * @param {string} name property name
     * @param {string} propertyValue property value
     * @param {string} fileName property file name
     * @return {object} <code>resultMap</code>
     * @param {string} group, `add`,`delete`,`update`
     */
    var addProperty = function(propertyMap, name, propertyValue, fileName, group) {
      var ret = $.extend(true, {}, propertyMap);
      if (ret.hasOwnProperty(group)) {
        if (!ret[group].hasOwnProperty(fileName)) {
          ret[group][fileName] = {};
        }
        ret[group][fileName][name] = propertyValue;
      }
      return ret;
    };

    return Em.keys(recommendedConfigurations || {}).reduce(function(acc, fileName) {
      var propertyMap = acc;
      var recommendedProperties = Em.getWithDefault(recommendedConfigurations, fileName + '.properties', {});
      var recommendedAttributes = Em.getWithDefault(recommendedConfigurations, fileName + '.property_attributes', {});
      // check for properties that should be delted
      Em.keys(recommendedAttributes).forEach(function(propertyName) {
        var attribute = recommendedAttributes[propertyName];
        // delete properties which are present in kerberos descriptor
        if (attribute.hasOwnProperty('delete') && allConfigs.filterProperty('filename', fileName).someProperty('name', propertyName)) {
          propertyMap = addProperty(propertyMap, propertyName, '', fileName, 'delete');
        }
      });

      return Em.keys(recommendedProperties).reduce(function(a, propertyName) {
        var propertyValue = recommendedProperties[propertyName];
        // check if property exist in saved configurations on server
        var isExist = Em.getWithDefault(servicesConfigurations.findProperty('type', fileName) || {}, 'properties', {}).hasOwnProperty(propertyName);
        if (!isExist) {
          return addProperty(a, propertyName, propertyValue, fileName, 'add');
        }
        // when property exist check that it present in current step configs (kerberos descriptor)
        // and add it as property to `update`
        if (allConfigs.filterProperty('filename', fileName).someProperty('name', propertyName)) {
          return addProperty(a, propertyName, propertyValue, fileName, 'update');
        }
        return a;
      }, propertyMap);
    }, resultMap);
  },

  /**
   *
   * @method getServiceByFilename
   * @param {string}fileName
   * @returns {string}
   */
  getServiceByFilename: function(fileName) {
    // core-site properties goes to HDFS
    if (fileName === 'core-site' && App.Service.find().someProperty('serviceName', 'HDFS')) {
      return 'HDFS';
    }
    var associatedService = App.StackService.find().filter(function(service) {
      return Em.keys(service.get('configTypes')).contains(fileName);
    })[0];
    return associatedService ? associatedService.get('serviceName') : '';
  }
});
