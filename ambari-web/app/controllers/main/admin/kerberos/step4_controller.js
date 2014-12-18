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

App.KerberosWizardStep4Controller = App.WizardStep7Controller.extend(App.AddSecurityConfigs, {
  name: 'kerberosWizardStep4Controller',
  
  clearStep: function() {
    this.set('isRecommendedLoaded', false);
    this.set('selectedService', null);
    this.set('stepConfigs', []);
  },
  
  loadStep: function() {
    var self = this;
    this.clearStep();
    this.getStackDescriptorConfigs().then(function(properties) {
      self.setStepConfigs(properties);
      self.set('isRecommendedLoaded', true);
    });
  },

  /**
   * Create service config object for Kerberos service.
   *
   * @param {Em.Object[]} configCategories
   * @param {App.ServiceConfigProperty[]} configs
   * @returns {Em.Object} 
   */
  createServiceConfig: function(configCategories, configs) {
    return App.ServiceConfig.create({
      displayName: 'Kerberos Descriptor',
      name: 'KERBEROS',
      serviceName: 'KERBEROS',
      configCategories: configCategories,
      configs: configs,
      showConfig: true,
      selected: true
    });
  },

  /**
   * Prepare step configs using stack descriptor properties.
   * 
   * @param {App.ServiceConfigProperty[]} configs
   */
  setStepConfigs: function(configs) {
    var selectedService = App.StackService.find().findProperty('serviceName', 'KERBEROS');
    var configCategories = selectedService.get('configCategories');
    this.get('stepConfigs').pushObject(this.createServiceConfig(configCategories, this.prepareConfigProperties(configs)));
    this.set('selectedService', this.get('stepConfigs')[0]);
  },

  /**
   * Filter configs by installed services. Set property value observer.
   * Set realm property with value from previous configuration step.
   * Set appropriate category for all configs.
   *
   * @param {App.ServiceCofigProperty[]} configs
   * @returns {App.ServiceConfigProperty[]}
   */
  prepareConfigProperties: function(configs) {
    var self = this;
    var realmValue = this.get('wizardController').getDBProperty('serviceConfigProperties').findProperty('name', 'realm').value;
    var installedServiceNames = ['Cluster'].concat(App.Service.find().mapProperty('serviceName'));
    configs = configs.filter(function(item) {
      return installedServiceNames.contains(item.get('serviceName'));
    });
    configs.findProperty('name', 'realm').set('value', realmValue);
    configs.findProperty('name', 'realm').set('defaultValue', realmValue);
    
    configs.setEach('isSecureConfig', false);
    configs.forEach(function(property, item, allConfigs) {
      if (['spnego_keytab', 'spnego_principal'].contains(property.get('name'))) {
        property.addObserver('value', self, 'spnegoPropertiesObserver');
      }
      if (property.get('observesValueFrom')) {
        var observedValue = allConfigs.findProperty('name', property.get('observesValueFrom')).get('value');
        property.set('value', observedValue);
        property.set('defaultValue', observedValue);
      }
      if (property.get('serviceName') == 'Cluster') property.set('category', 'General');
      else property.set('category', 'Advanced');
    });

    return configs;
  },

  /**
   * Sync up values between inherited property and its reference.
   * 
   * @param {App.ServiceConfigProperty} configProperty
   */
  spnegoPropertiesObserver: function(configProperty) {
    var self = this;
    this.get('stepConfigs')[0].get('configs').forEach(function(config) {
      if (config.get('observesValueFrom') == configProperty.get('name')) {
        Em.run.once(self, function() {
          config.set('value', configProperty.get('value'));
          config.set('defaultValue', configProperty.get('value'));
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
    var configs = this.get('stepConfigs')[0].get('configs');
    this.updateKerberosDescriptor(kerberosDescriptor, configs);
    this.get('wizardController').setDBProperty('kerberosDescriptorConfigs', kerberosDescriptor);
    this.set('wizardController.content.kerberosDescriptorConfigs', kerberosDescriptor);
  }
});
