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

  serviceConfigs: require('data/service_configs'),
  configMapping: require('data/config_mapping'),
  customConfigs: require('data/custom_configs'),
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
    var serviceConfigs = this.get('serviceConfigs');
    var advancedConfig = this.get('content.advancedServiceConfig') || [];
    this.loadAdvancedConfig(serviceConfigs, advancedConfig);
    this.loadCustomConfig();
    this.renderServiceConfigs(serviceConfigs);
    var storedServices = this.get('content.serviceConfigProperties');
    if (storedServices) {
      var configs = new Ember.Set();

      // for all services`
      this.get('stepConfigs').forEach(function (_content) {
        //for all components
        
        // Update existing values
        var seenStoredConfigs = {};
        _content.get('configs').forEach(function (_config) {
          var componentVal = storedServices.findProperty('name', _config.get('name'));
          //if we have config for specified component
          if (componentVal) {
            //set it
            seenStoredConfigs[componentVal.name] = 'true';
            _config.set('value', componentVal.value)
            this.updateHostOverrides(_config, componentVal);
          }
        }, this);
        
        // Create new values
        var currentServiceStoredConfigs = storedServices.filterProperty('service', _content.serviceName);
        currentServiceStoredConfigs.forEach(function (storedConfig) {
          if(!(storedConfig.name in seenStoredConfigs)){
            console.log("loadStep7(): New property from local storage: ", storedConfig);
            // Determine category
            var configCategory = 'Advanced';
            var serviceConfigMetaData = serviceConfigs.findProperty('serviceName', _content.serviceName);
            var categoryMetaData = serviceConfigMetaData == null ? null : serviceConfigMetaData.configCategories.findProperty('siteFileName', storedConfig.filename);
            if (categoryMetaData != null) {
              configCategory = categoryMetaData.get('name');
            }
            // Configuration data
            var configData = {
                id: storedConfig.id,
                name: storedConfig.name,
                displayName: storedConfig.name,
                serviceName: _content.serviceName,
                value: storedConfig.value,
                defaultValue: storedConfig.defaultValue,
                displayType: "advanced",
                filename: storedConfig.filename,
                category: configCategory,
                isUserProperty: true
            }
            var serviceConfigProperty = App.ServiceConfigProperty.create(configData);
            serviceConfigProperty.serviceConfig = _content;
            serviceConfigProperty.initialValue();
            this.updateHostOverrides(serviceConfigProperty, storedConfig);
            _content.configs.pushObject(serviceConfigProperty);
            serviceConfigProperty.validate();
          }
        }, this);
      }, this);
    }
  },
  
  updateHostOverrides: function (configProperty, storedConfigProperty) {
    if(storedConfigProperty.overrides!=null && storedConfigProperty.overrides.length>0){
      var overrides = configProperty.get('overrides');
      if (!overrides) {
        overrides = []; 
        configProperty.set('overrides', overrides);
      }
      storedConfigProperty.overrides.forEach(function(overrideEntry){
        // create new override with new value
        var newSCP = App.ServiceConfigProperty.create(configProperty);
        newSCP.set('value', overrideEntry.value);
        newSCP.set('isOriginalSCP', false); // indicated this is overridden value,
        newSCP.set('parentSCP', configProperty);
        var hostsArray = Ember.A([]);
        overrideEntry.hosts.forEach(function(host){
          hostsArray.push(host);
        });
        newSCP.set('selectedHostOptions', hostsArray);
        overrides.pushObject(newSCP);
      });
    }
  },

  /*
   Loads the advanced configs fetched from the server metadata libarary
   */

  loadAdvancedConfig: function (serviceConfigs, advancedConfig) {
    advancedConfig.forEach(function (_config) {
      if (_config) {
        var service = serviceConfigs.findProperty('serviceName', _config.serviceName);
        if (service) {
          if (this.get('configMapping').someProperty('name', _config.name)) {
          } else if (!(service.configs.someProperty('name', _config.name))) {
            _config.id = "site property";
            _config.category = 'Advanced';
            var serviceConfigMetaData = this.get('serviceConfigs').findProperty('serviceName', this.get('content.serviceName'));
            var categoryMetaData = serviceConfigMetaData == null ? null : serviceConfigMetaData.configCategories.findProperty('siteFileName', serviceConfigObj.filename);
            if (categoryMetaData != null) {
              _config.category = categoryMetaData.get('name');
            }
            _config.displayName = _config.name;
            _config.defaultValue = _config.value;
            // make all advanced configs optional and populated by default
            /*
             * if (/\${.*}/.test(_config.value) || (service.serviceName !==
             * 'OOZIE' && service.serviceName !== 'HBASE')) { _config.isRequired =
             * false; _config.value = ''; } else if
             * (/^\s+$/.test(_config.value)) { _config.isRequired = false; }
             */
            _config.isRequired = false;
            _config.isVisible = true;
            _config.displayType = 'advanced';
            service.configs.pushObject(_config);
          }
        }
      }
    }, this);
  },


  /**
   * Render a custom conf-site box for entering properties that will be written in *-site.xml files of the services
   */
  loadCustomConfig: function () {
    var serviceConfigs = this.get('serviceConfigs');
    this.get('customConfigs').forEach(function (_config) {
      var service = serviceConfigs.findProperty('serviceName', _config.serviceName);
      if (service) {
        if (!(service.configs.someProperty('name', _config.name))) {
          if( Object.prototype.toString.call( _config.defaultValue ) === '[object Array]' ) {
            this.loadDefaultCustomConfig(_config);
          }
          service.configs.pushObject(_config);
        }
      }
    }, this);
  },

  loadDefaultCustomConfig: function (customConfig) {
    var customValue = '';
    var length = customConfig.defaultValue.length;
    customConfig.defaultValue.forEach(function (_config, index) {
      customValue += _config.name + '=' + _config.value;
      if (index !== length - 1) {
        customValue += '\n';
      }
    }, this);
    customConfig.value = customValue;
  },

  /**
   * Render configs for active services
   * @param serviceConfigs
   */
  renderServiceConfigs: function (serviceConfigs) {
    serviceConfigs.forEach(function (_serviceConfig) {

      var serviceConfig = App.ServiceConfig.create({
        filename: _serviceConfig.filename,
        serviceName: _serviceConfig.serviceName,
        displayName: _serviceConfig.displayName,
        configCategories: _serviceConfig.configCategories,
        showConfig: false,
        configs: []
      });

      if (this.get('allInstalledServiceNames').contains(serviceConfig.serviceName) || serviceConfig.serviceName === 'MISC') {

        this.loadComponentConfigs(_serviceConfig, serviceConfig);

        console.log('pushing ' + serviceConfig.serviceName, serviceConfig);

        if (this.get('selectedServiceNames').contains(serviceConfig.serviceName) || serviceConfig.serviceName === 'MISC') {
          serviceConfig.showConfig = true;
        }

        this.get('stepConfigs').pushObject(serviceConfig);

      } else {
        console.log('skipping ' + serviceConfig.serviceName);
      }
    }, this);

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
    miscConfigs.findProperty('name', 'pig_user').set('isVisible', this.get('selectedServiceNames').contains('PIG'));
    miscConfigs.findProperty('name', 'sqoop_user').set('isVisible', this.get('selectedServiceNames').contains('SQOOP'));
    miscConfigs.findProperty('name', 'zk_user').set('isVisible', this.get('selectedServiceNames').contains('ZOOKEEPER'));
    miscConfigs.findProperty('name', 'rrdcached_base_dir').set('isVisible', this.get('selectedServiceNames').contains('GANGLIA'));

    this.set('selectedService', this.get('stepConfigs').filterProperty('showConfig', true).objectAt(0));
  },

  /**
   * Load child components to service config object
   * @param _componentConfig
   * @param componentConfig
   */
  loadComponentConfigs: function (_componentConfig, componentConfig) {
    _componentConfig.configs.forEach(function (_serviceConfigProperty) {
      var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);
      serviceConfigProperty.serviceConfig = componentConfig;
      serviceConfigProperty.initialValue();
      componentConfig.configs.pushObject(serviceConfigProperty);
      serviceConfigProperty.validate();
    }, this);
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
