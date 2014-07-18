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
App.MainAdminSecurityController = Em.Controller.extend({
  name: 'mainAdminSecurityController',
  isSubmitDisabled: false,
  securityEnabled: false,
  dataIsLoaded: false,
  serviceUsers: [],
  tag: {},
  getAddSecurityWizardStatus: function () {
    return App.db.getSecurityWizardStatus();
  },
  setAddSecurityWizardStatus: function (status) {
    App.db.setSecurityWizardStatus(status);
  },

  setDisableSecurityStatus: function (status) {
    App.db.setDisableSecurityStatus(status);
  },
  getDisableSecurityStatus: function (status) {
    return App.db.getDisableSecurityStatus();
  },

  notifySecurityOff: false,
  notifySecurityAdd: false,

  stepConfigs: [],
  desiredConfigs: [],
  securityUsers: [],
  serviceConfigTags: [],
  selectedService: null,
  isNotEditable: true,
  services: function () {
    var secureServices;
    var services = [];
    if (App.get('isHadoop2Stack')) {
      secureServices = $.extend(true, [], require('data/HDP2/secure_configs'));
    } else {
      secureServices = $.extend(true, [], require('data/secure_configs'));
    }

    var installedServices = App.Service.find().mapProperty('serviceName');
    //General (only non service tab) tab is always displayed
    services.push(secureServices.findProperty('serviceName', 'GENERAL'));
    installedServices.forEach(function (_service) {
      var secureService = secureServices.findProperty('serviceName', _service);
      if (secureService) {
        services.push(secureService);
      }
    }, this);
    return services;
  }.property(),

  /**
   * default values of configs, which contains user names
   */
  defaultUserNameMap: {
    'hdfs_user': 'hdfs',
    'yarn_user': 'yarn',
    'mapred_user': 'mapred',
    'hbase_user': 'hbase',
    'hive_user': 'hive',
    'proxyuser_group': 'users',
    'smokeuser': 'ambari-qa',
    'zk_user': 'zookeeper',
    'oozie_user': 'oozie',
    'nagios_user': 'nagios',
    'user_group': 'hadoop',
    'storm_user': 'storm',
    'falcon_user': 'falcon'
  },

  loadStep: function () {
    var step2Controller = App.router.get('mainAdminSecurityAddStep2Controller');
    var services = this.get('services');
    var self = this;
    step2Controller.set('content', Em.Object.create({services: []}));
    step2Controller.set('content.services', services);
    this.get('stepConfigs').clear();
    this.get('securityUsers').clear();
    this.get('serviceConfigTags').clear();
    this.loadSecurityUsers();
    //loadSecurityUsers - desired configs fetched from server
    step2Controller.addUserPrincipals(services, this.get('securityUsers'));
    step2Controller.addMasterHostToConfigs();
    step2Controller.addSlaveHostToConfigs();
    this.renderServiceConfigs(services);
    step2Controller.changeCategoryOnHa(services, this.get('stepConfigs'));

    services.forEach(function (_secureService) {
      this.setServiceTagNames(_secureService, this.get('desiredConfigs'));
    }, this);
    App.router.get('configurationController').getConfigsByTags(this.get('serviceConfigTags')).done(function (serverConfigs) {
      self.setConfigValuesFromServer(self.get('stepConfigs'), serverConfigs);
      self.set('installedServices', App.Service.find().mapProperty('serviceName'));
    });
  },

  /**
   * get actual values of configurations from server
   * @param stepConfigs
   * @param serverConfigs
   */
  setConfigValuesFromServer: function (stepConfigs, serverConfigs) {
    var allConfigs = {};
    serverConfigs.mapProperty('properties').forEach(function (_properties) {
      allConfigs = $.extend(allConfigs, _properties);
    }, this);
    // for all services`
    stepConfigs.forEach(function (_content) {
      //for all components
      _content.get('configs').forEach(function (_config) {

        var componentVal = allConfigs[_config.get('name')];
        //if we have config for specified component
        if (componentVal) {
          //set it
          _config.set('value', componentVal);
        }
      }, this);
    }, this);
  },

  /**
   * set tag names according to installed services and desired configs
   * @param secureService
   * @param configs
   * @return {Object}
   */
  setServiceTagNames: function (secureService, configs) {
    //var serviceConfigTags = this.get('serviceConfigTags');
    for (var index in configs) {
      if (secureService.sites && secureService.sites.contains(index)) {
        var serviceConfigObj = {
          siteName: index,
          tagName: configs[index].tag,
          newTagName: null,
          configs: {}
        };
        this.get('serviceConfigTags').pushObject(serviceConfigObj);
      }
    }
    return serviceConfigObj;
  },

  loadSecurityUsers: function () {
    var securityUsers = this.get('serviceUsers');
    if (!securityUsers || securityUsers.length < 1) { // Page could be refreshed in middle
      if (App.testMode) {
        securityUsers.pushObject({id: 'puppet var', name: 'hdfs_user', value: 'hdfs'});
        securityUsers.pushObject({id: 'puppet var', name: 'mapred_user', value: 'mapred'});
        securityUsers.pushObject({id: 'puppet var', name: 'hbase_user', value: 'hbase'});
        securityUsers.pushObject({id: 'puppet var', name: 'hive_user', value: 'hive'});
        securityUsers.pushObject({id: 'puppet var', name: 'smokeuser', value: 'ambari-qa'});
      } else {
        this.setSecurityStatus();
        securityUsers = this.get('serviceUsers');
      }
    }
    this.set('securityUsers', securityUsers);
  },
  /**
   * Load child components to service config object
   * @param _componentConfig
   * @param componentConfig
   */
  loadComponentConfigs: function (_componentConfig, componentConfig) {
    _componentConfig.configs.forEach(function (_serviceConfigProperty) {
      var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);
      componentConfig.configs.pushObject(serviceConfigProperty);
      serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isReconfigurable'));
      serviceConfigProperty.validate();
    }, this);
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
        showConfig: true,
        configs: []
      });

      this.loadComponentConfigs(_serviceConfig, serviceConfig);

      console.log('pushing ' + serviceConfig.serviceName, serviceConfig);

      this.get('stepConfigs').pushObject(serviceConfig);
    }, this);
    this.set('selectedService', this.get('stepConfigs').filterProperty('showConfig', true).objectAt(0));
  },

  notifySecurityOffPopup: function () {
    var self = this;
    if (!this.get('isSubmitDisabled')) {
      App.ModalPopup.show({
        header: Em.I18n.t('popup.confirmation.commonHeader'),
        primary: Em.I18n.t('ok'),
        onPrimary: function () {
          App.db.setSecurityDeployCommands(undefined);
          self.setDisableSecurityStatus("RUNNING");
          App.router.transitionTo('disableSecurity');
          this.hide();
        },
        bodyClass: Ember.View.extend({
          isMapReduceInstalled: App.Service.find().mapProperty('serviceName').contains('MAPREDUCE'),
          templateName: require('templates/main/admin/security/notify_security_off_popup')
        })
      })
    }
  },

  getUpdatedSecurityStatus: function () {
    this.setSecurityStatus();
    return this.get('securityEnabled');
  },

  setSecurityStatus: function () {
    if (App.testMode) {
      this.set('securityEnabled', !App.testEnableSecurity);
      this.set('dataIsLoaded', true);
    } else {
      //get Security Status From Server
      App.ajax.send({
        name: 'admin.security_status',
        sender: this,
        success: 'getSecurityStatusFromServerSuccessCallback',
        error: 'errorCallback'
      });
    }
  },

  errorCallback: function (jqXHR) {
    this.set('dataIsLoaded', true);
    // Show the error popup if the API call received a response from the server.
    // jqXHR.status will be empty when browser cancels the request. Refer to AMBARI-5921 for more info
    if (!!jqXHR.status) {
      this.showSecurityErrorPopup();
    }
  },

  getSecurityStatusFromServerSuccessCallback: function (data) {
    var configs = data.Clusters.desired_configs;
    this.set('desiredConfigs', configs);
    if ('hadoop-env' in configs && 'hdfs-site' in configs) {
      this.set('tag.hadoop-env', configs['hadoop-env'].tag);
      this.set('tag.hdfs-site', configs['hdfs-site'].tag);
      this.getServiceConfigsFromServer();
    }
    else {
      this.showSecurityErrorPopup();
    }
  },

  getServiceConfigsFromServer: function () {
    var tags = [
      {
        siteName: "hadoop-env",
        tagName: this.get('tag.hadoop-env')
      },
      {
        siteName: "hdfs-site",
        tagName: this.get('tag.hdfs-site')
      }
    ];
    var self = this;

    App.router.get('configurationController').getConfigsByTags(tags).done(function(data) {
      var configs = data.findProperty('tag', self.get('tag.hadoop-env')).properties;
      if (configs && (configs['security_enabled'] === 'true' || configs['security_enabled'] === true)) {
        self.set('securityEnabled', true);
      }
      else {
        self.set('securityEnabled', false);
        var hdfsConfigs = data.findProperty('tag', self.get('tag.hdfs-site')).properties;
        self.setNnHaStatus(hdfsConfigs);
      }
      self.loadUsers(configs);
      self.set('dataIsLoaded', true);
    });
  },

  setNnHaStatus: function (hdfsConfigs) {
    var nnHaStatus = hdfsConfigs && hdfsConfigs['dfs.nameservices'];
    var namenodesKey;
    if (nnHaStatus) {
      namenodesKey = 'dfs.ha.namenodes.' + hdfsConfigs['dfs.nameservices'];
    }
    if (nnHaStatus && hdfsConfigs[namenodesKey]) {
      App.db.setIsNameNodeHa('true');
    } else {
      App.db.setIsNameNodeHa('false');
    }
  },

  /**
   * load users names,
   * substitute missing values with default
   * @param configs {Object}
   */
  loadUsers: function (configs) {
    var defaultUserNameMap = this.get('defaultUserNameMap');
    var serviceUsers = this.get('serviceUsers');

    for (var configName in defaultUserNameMap) {
      serviceUsers.push({
        id: 'puppet var',
        name: configName,
        value: configs[configName] || defaultUserNameMap[configName]
      });
    }
    App.db.setSecureUserInfo(this.get('serviceUsers'));
  },

  showSecurityErrorPopup: function () {
    App.ModalPopup.show({
      header: Em.I18n.t('common.error'),
      secondary: false,
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile('<p>{{t admin.security.status.error}}</p>')
      })
    });
  }
});


