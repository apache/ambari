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
 * By Step 7, we have the following information stored in App.db and set on this
 * controller by the router.
 *
 *   selectedServices: App.db.selectedServices (the services that the user selected in Step 4)
 *   masterComponentHosts: App.db.masterComponentHosts (master-components-to-hosts mapping the user selected in Step 5)
 *   slaveComponentHosts: App.db.slaveComponentHosts (slave-components-to-hosts mapping the user selected in Step 6)
 *
 */

App.WizardStep7Controller = Em.Controller.extend({

  name: 'wizardStep7Controller',

  stepConfigs: [], //contains all field properties that are viewed in this step

  selectedService: null,

  slaveHostToGroup: null,

  isSubmitDisabled: function () {
    return !this.stepConfigs.filterProperty('showConfig', true).everyProperty('errorCount', 0);
  }.property('stepConfigs.@each.errorCount'),

  selectedServiceNames: function () {
    return this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false).mapProperty('serviceName');
  }.property('content.services').cacheable(),

  allInstalledServiceNames: function () {
    return this.get('content.services').filterProperty('isSelected', true).mapProperty('serviceName');
  }.property('content.services').cacheable(),

  masterComponentHosts: function () {
    return this.get('content.masterComponentHosts');
  }.property('content.masterComponentHosts'),

  slaveComponentHosts: function () {
    return this.get('content.slaveGroupProperties');
  }.property('content.slaveGroupProperties', 'content.slaveComponentHosts'),

  customData: [],

  clearStep: function () {
    this.get('stepConfigs').clear();
  },

  /**
   * On load function
   */
  loadStep: function () {
    console.log("TRACE: Loading step7: Configure Services");
    this.clearStep();
    //STEP 1: Load advanced configs
    var advancedConfigs = this.get('content.advancedServiceConfig');
    //STEP 2: Load on-site configs by service from local DB
    var storedConfigs = this.get('content.serviceConfigProperties');
    //STEP 3: Merge pre-defined configs with loaded on-site configs
    var configs = App.config.mergePreDefinedWithStored(storedConfigs, advancedConfigs);
    //STEP 4: Add advanced configs
    App.config.addAdvancedConfigs(configs, advancedConfigs);
    //STEP 5: Add custom configs
    App.config.addCustomConfigs(configs);
    //STEP 6: Distribute configs by service and wrap each one in App.ServiceConfigProperty (configs -> serviceConfigs)
    var serviceConfigs = App.config.renderConfigs(configs, this.get('allInstalledServiceNames'), this.get('selectedServiceNames'));
    this.set('stepConfigs', serviceConfigs);
    this.activateSpecialConfigs();
    this.set('selectedService', this.get('stepConfigs').filterProperty('showConfig', true).objectAt(0));
  },
   /**
   * make some configs visible depending on active services
   */
  activateSpecialConfigs: function () {
    var miscConfigs = this.get('stepConfigs').findProperty('serviceName', 'MISC').configs;
    var showProxyGroup = this.get('selectedServiceNames').contains('HIVE') ||
      this.get('selectedServiceNames').contains('HCATALOG') ||
      this.get('selectedServiceNames').contains('OOZIE');
    miscConfigs.findProperty('name', 'proxyuser_group').set('isVisible', showProxyGroup);
    miscConfigs.findProperty('name', 'hbase_user').set('isVisible', this.get('selectedServiceNames').contains('HBASE'));
    miscConfigs.findProperty('name', 'mapred_user').set('isVisible', this.get('selectedServiceNames').contains('MAPREDUCE'));
    miscConfigs.findProperty('name', 'hive_user').set('isVisible', this.get('selectedServiceNames').contains('HIVE'));
    miscConfigs.findProperty('name', 'hcat_user').set('isVisible', this.get('selectedServiceNames').contains('HCATALOG'));
    miscConfigs.findProperty('name', 'webhcat_user').set('isVisible', this.get('selectedServiceNames').contains('WEBHCAT'));
    miscConfigs.findProperty('name', 'oozie_user').set('isVisible', this.get('selectedServiceNames').contains('OOZIE'));
    miscConfigs.findProperty('name', 'zk_user').set('isVisible', this.get('selectedServiceNames').contains('ZOOKEEPER'));
    miscConfigs.findProperty('name', 'gmetad_user').set('isVisible', this.get('selectedServiceNames').contains('GANGLIA'));
    miscConfigs.findProperty('name', 'rrdcached_base_dir').set('isVisible', this.get('selectedServiceNames').contains('GANGLIA'));
    miscConfigs.findProperty('name', 'nagios_user').set('isVisible', this.get('selectedServiceNames').contains('NAGIOS'));
  },

  /**
   * @param: An array of display names
   */
  setDisplayMessage: function (siteProperty, displayNames) {
    var displayMsg = null;
    if (displayNames && displayNames.length) {
      if (displayNames.length === 1) {
        displayMsg = siteProperty + ' ' + Em.I18n.t('as') + ' ' + displayNames[0];
      } else {
        var name = null;
        displayNames.forEach(function (_name, index) {
          if (index === 0) {
            name = _name;
          } else if (index === displayNames.length - 1) {
            name = name + ' ' + Em.I18n.t('and') + ' ' + _name;
          } else {
            name = name + ', ' + _name;
          }
        }, this);
        displayMsg = siteProperty + ' ' + Em.I18n.t('as') + ' ' + name;
      }
    } else {
      displayMsg = siteProperty;
    }
    return displayMsg;
  },

  /**
   * Set display names of the property tfrom he puppet/global names
   * @param displayNames: a field to be set with displayNames
   * @param names: array of property puppet/global names
   * @param configProperties: array of config properties of the respective service to the name param
   */
  setPropertyDisplayNames: function (displayNames, names, configProperties) {
    names.forEach(function (_name, index) {
      if (configProperties.someProperty('name', _name)) {
        displayNames.push(configProperties.findProperty('name', _name).displayName);
      }
    }, this);
  },

  /**
   * Display Error Message with service name, its custom configuration name and displaynames on the page
   * @param customConfig: array with custom configuration, serviceName and displayNames relative to custom configuration
   */
  showCustomConfigErrMsg: function (customConfig) {

    App.ModalPopup.show({
      header: Em.I18n.t('installer.step7.ConfigErrMsg.header'),
      primary: Em.I18n.t('ok'),
      secondary: null,
      onPrimary: function () {
        this.hide();
      },
      bodyClass: Ember.View.extend({
        message: Em.I18n.t('installer.step7.ConfigErrMsg.message'),
        siteProperties: customConfig,
        getDisplayMessage: function () {

        }.property('customConfig.@each.siteProperties.@each.siteProperty'),
        customConfig: customConfig,
        template: Ember.Handlebars.compile([
          '<h5>{{view.message}}</h5>',
          '<br/>',
          '<div class="pre-scrollable" style="max-height: 250px;">',
          '<ul>',
          '{{#each val in view.customConfig}}',
          '{{#if val.siteProperties}}',
          '<li>',
          '{{val.serviceName}}',
          '<ul>',
          '{{#each item in  val.siteProperties}}',
          '<li>',
          '{{item.displayMsg}}',
          '</li>',
          '{{/each}}',
          '</ul>',
          '</li>',
          '{{/if}}',
          '{{/each}}',
          '</ul>',
          '</div>'
        ].join('\n'))
      })
    });
  },

  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      App.router.send('next');
    }
  }, 
  
  /**
   * Provides service component name and display-name information for 
   * the current selected service. 
   */
  getCurrentServiceComponents: function () {
    var selectedServiceName = this.get('selectedService.serviceName');
    var masterComponents = this.get('content.masterComponentHosts');
    var slaveComponents = this.get('content.slaveComponentHosts');
    var scMaps = require('data/service_components');
    
    var validComponents = Ember.A([]);
    var seenComponents = {};
    masterComponents.forEach(function(component){
      var cn = component.component
      var cdn = component.display_name;
      if(component.serviceId===selectedServiceName && !seenComponents[cn]){
        validComponents.pushObject(Ember.Object.create({
          componentName: cn,
          displayName: cdn,
          selected: false
        }));
        seenComponents[cn] = cn;
      }
    });
    slaveComponents.forEach(function(component){
      var cn = component.componentName
      var cdn = component.displayName;
      var componentDef = scMaps.findProperty('component_name', cn);
      if(componentDef!=null && selectedServiceName===componentDef.service_name && !seenComponents[cn]){
        validComponents.pushObject(Ember.Object.create({
          componentName: cn,
          displayName: cdn,
          selected: false
        }));
        seenComponents[cn] = cn;
      }
    });
    return validComponents;
  }.property('content'),
  

  getAllHosts: function () {
    // Load hosts
    var allHosts = Ember.A([]);
    var hostNameToHostMap = {};
    var hosts = this.get('content.hosts');
    for ( var hostName in hosts) {
      var host = hosts[hostName];
      hostNameToHostMap[hostName] = App.Host.createRecord({
        id: host.name,
        hostName: host.name,
        publicHostName: host.name,
        cpu: host.cpu,
        memory: host.memory
      });
      allHosts.pushObject(hostNameToHostMap[hostName]);
    }

    // Load host-components
    var masterComponents = this.get('content.masterComponentHosts');
    var slaveComponents = this.get('content.slaveComponentHosts');
    masterComponents.forEach(function (component) {
      var host = hostNameToHostMap[component.hostName];
      var hc = App.HostComponent.createRecord({
        componentName: component.component,
        host: host
      });
      if (host != null) {
        host.get('hostComponents').pushObject(hc);
      }
    });
    slaveComponents.forEach(function (component) {
      component.hosts.forEach(function (host) {
        var h = hostNameToHostMap[host.hostName];
        var hc = App.HostComponent.createRecord({
          componentName: component.componentName,
          host: h
        });
        if (h != null) {
          h.get('hostComponents').pushObject(hc);
        }
      });
    });
    return allHosts;
  }.property('content')

});
