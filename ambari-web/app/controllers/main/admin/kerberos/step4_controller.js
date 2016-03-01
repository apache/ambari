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
require('controllers/wizard/step7_controller');

App.KerberosWizardStep4Controller = App.WizardStep7Controller.extend(App.AddSecurityConfigs, App.ToggleIsRequiredMixin, App.KDCCredentialsControllerMixin, {
  name: 'kerberosWizardStep4Controller',
  isWithinAddService: function () {
    return this.get('wizardController.name') == 'addServiceController';
  }.property('wizardController.name'),

  clearStep: function() {
    this.set('isRecommendedLoaded', false);
    this.set('selectedService', null);
    this.set('stepConfigs', []);
  },

  loadStep: function() {
    if (this.get('wizardController.skipConfigureIdentitiesStep')) {
      App.router.send('next');
      return;
    }
    var self = this;
    this.clearStep();
    this.getDescriptorConfigs().then(function (properties) {
      self.setStepConfigs(properties);
    }).always(function() {
      self.set('isRecommendedLoaded', true);
    });
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
      showConfig: true
    }),
      App.ServiceConfig.create({
        displayName: 'Advanced',
        name: 'ADVANCED',
        serviceName: 'KERBEROS_ADVANCED',
        configCategories: categoryForAdvancedConfigs,
        configs: advancedConfigs,
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
    if (this.get('wizardController.name') == 'addServiceController') {
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
   * @param {App.ServiceConfigProperty[]} stackConfigs
   */
  setStepConfigs: function(configs, stackConfigs) {
    var configProperties = this.prepareConfigProperties(configs),
      stackConfigProperties = stackConfigs ? this.prepareConfigProperties(stackConfigs) : [],
      alterProperties = ['value','initialValue', 'defaultValue'];
    if (this.get('wizardController.name') == 'addServiceController') {
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
          if (this.get('installedServiceNames').contains(_stackConfigProperty.get('serviceName')) || _stackConfigProperty.get('serviceName') == 'Cluster') {
            _stackConfigProperty.set('isEditable', false);
          }
          configProperties.pushObject(_stackConfigProperty);
        }
      }, this);
    }
    configProperties = App.config.sortConfigs(configProperties);
    this.get('stepConfigs').pushObjects(this.createServiceConfig(configProperties));
    this.set('selectedService', this.get('stepConfigs')[0]);
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
    console.log("call prepare");
    var self = this;
    // stored configs from previous steps (Configure Kerberos or Customize Services for ASW)
    var storedServiceConfigs = this.get('wizardController').getDBProperty('serviceConfigProperties');
    var installedServiceNames = ['Cluster'].concat(App.Service.find().mapProperty('serviceName'));
    var configProperties = configs.slice(0);
    var siteProperties = App.config.get('preDefinedSiteProperties');
    // override stored values
    App.config.mergeStoredValue(configProperties, this.get('wizardController').loadCachedStepConfigValues(this));

    // show admin properties in add service wizard
    if (this.get('isWithinAddService')) {
      installedServiceNames = installedServiceNames.concat(this.get('selectedServiceNames'));
    }
    configProperties = configProperties.filter(function(item) {
      return installedServiceNames.contains(item.get('serviceName'));
    });
    if (this.get('wizardController.name') != 'addServiceController') {
      var realmValue = storedServiceConfigs.findProperty('name', 'realm').value;
      configProperties.findProperty('name', 'realm').set('value', realmValue);
      configProperties.findProperty('name', 'realm').set('savedValue', realmValue);
      configProperties.findProperty('name', 'realm').set('recommendedValue', realmValue);
    }

    configProperties.setEach('isSecureConfig', false);
    configProperties.forEach(function(property, item, allConfigs) {
      if (['spnego_keytab', 'spnego_principal'].contains(property.get('name'))) {
        property.addObserver('value', self, 'spnegoPropertiesObserver');
      }
      if (property.get('observesValueFrom')) {
        var observedValue = allConfigs.findProperty('name', property.get('observesValueFrom')).get('value');
        property.set('value', observedValue);
        property.set('recommendedValue', observedValue);
      }
      if (property.get('serviceName') == 'Cluster') {
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
    if (config.name === 'templeton.hive.properties') {
      var defaultHiveMsPort = "9083";
      var hiveMSHosts = App.HostComponent.find().filterProperty('componentName', 'HIVE_METASTORE');
      if (hiveMSHosts.length > 1) {
        var hiveMSHostNames = hiveMSHosts.mapProperty('hostName');
        var port = config.value.match(/:[0-9]{2,4}/);
        port = port ? port[0].slice(1) : defaultHiveMsPort;
        for (var i = 0; i < hiveMSHostNames.length; i++) {
          hiveMSHostNames[i] = "thrift://" + hiveMSHostNames[i] + ":" + port;
        }
        var configValue =  config.value.replace(/thrift.+[0-9]{2,},/i, hiveMSHostNames.join('\\,') + ",");
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
    var self = this;
    var stepConfig =  this.get('stepConfigs').findProperty('name', 'ADVANCED');
    stepConfig.get('configs').forEach(function(config) {
      if (config.get('observesValueFrom') == configProperty.get('name')) {
        Em.run.once(self, function() {
          config.set('value', configProperty.get('value'));
          config.set('recommendedValue', configProperty.get('value'));
        });
      }
    });
  },

  submit: function() {
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
  }
});
