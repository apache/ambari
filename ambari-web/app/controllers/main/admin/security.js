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
  isRecommendedLoaded: true,
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
  content: Em.Object.create({
    isATSInstalled: function() {
      // Because the ATS component can be installed/removed at will, the check has to happen every time that security is added.
      var yarnService = App.Service.find().findProperty('serviceName','YARN');
      return !!yarnService && yarnService.get('hostComponents').someProperty('componentName', 'APP_TIMELINE_SERVER');
    }.property('App.router.clusterController.isLoaded')
  }),
  notifySecurityOff: false,
  notifySecurityAdd: false,

  stepConfigs: [],
  desiredConfigs: [],
  securityUsers: [],
  serviceConfigTags: [],
  selectedService: null,
  isNotEditable: true,
  /** need to define <code>filter, filterColumns</code> properties
   * for preventing errors in <code>App.ServiceConfigsByCategoryView</code>
   */
  filter: '',
  filterColumns: function () {
    return [];
  }.property(''),
  services: function () {
    var secureServices = $.extend(true, [], require('data/HDP2/secure_configs'));
    var services = [];

    // Typically, ATS will support Kerberos in HDP 2.2 and higher
    if (this.get('content.isATSInstalled') && App.get('doesATSSupportKerberos')) {
      var yarnConfigCategories = secureServices.findProperty('serviceName', 'YARN').configCategories;
      yarnConfigCategories.push(App.ServiceConfigCategory.create({ name: 'AppTimelineServer', displayName : 'Application Timeline Service'}));
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
  }.property('App.router.clusterController.isLoaded'),

  /**
   * default values of configs, which contains user names
   */
  userNameMap: {
    'hdfs_user': {defaultValue: 'hdfs', siteName: 'hadoop-env', serviceName: 'HDFS'},
    'yarn_user': {defaultValue: 'yarn', siteName: 'yarn-env', serviceName: 'YARN'},
    'mapred_user': {defaultValue: 'mapred', siteName: 'mapred-env', serviceName: 'MAPREDUCE2'},
    'hbase_user': {defaultValue: 'hbase', siteName: 'hbase-env', serviceName: 'HBASE'},
    'hive_user': {defaultValue: 'hive', siteName: 'hive-env', serviceName: 'HIVE'},
    'proxyuser_group': {defaultValue: 'users', siteName: 'hadoop-env', serviceName: 'HDFS'},
    'smokeuser': {defaultValue: 'ambari-qa', siteName: 'cluster-env', serviceName: 'CLUSTER'},
    'zk_user': {defaultValue: 'zookeeper', siteName: 'zookeeper-env', serviceName: 'ZOOKEEPER'},
    'oozie_user': {defaultValue: 'oozie', siteName: 'oozie-env', serviceName: 'OOZIE'},
    'user_group': {defaultValue: 'hadoop', siteName: 'hadoop-env', serviceName: 'HDFS'},
    'storm_user': {defaultValue: 'storm', siteName: 'storm-env', serviceName: 'STORM'},
    'falcon_user': {defaultValue: 'falcon', siteName: 'falcon-env', serviceName: 'FALCON'},
    'knox_user': {defaultValue: 'knox', siteName: 'knox-env', serviceName: 'KNOX'}
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
      if (App.get('testMode')) {
        securityUsers.pushObject({ name: 'hdfs_user', value: 'hdfs'});
        securityUsers.pushObject({ name: 'mapred_user', value: 'mapred'});
        securityUsers.pushObject({ name: 'hbase_user', value: 'hbase'});
        securityUsers.pushObject({ name: 'hive_user', value: 'hive'});
        securityUsers.pushObject({ name: 'smokeuser', value: 'ambari-qa'});
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
    if (App.get('testMode')) {
      this.set('securityEnabled', !App.get('testEnableSecurity'));
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
    var serviceNames = this.get('services').mapProperty('serviceName');
    var configTags = [];
    this.set('desiredConfigs', configs);
    for (var key in this.userNameMap) {
      if (serviceNames.contains(this.userNameMap[key]['serviceName']) || this.userNameMap[key]['serviceName'] === 'CLUSTER')
        configTags.push(this.userNameMap[key]['siteName']);
    }
    configTags = configTags.uniq();

    var errorFlag = false;
    configTags.forEach(function (_tag) {
      if (!configs[_tag]) {
        errorFlag = true;
      }
    }, this);

    if (errorFlag) {
      this.showSecurityErrorPopup();
    }  else {
      var tags = configTags.map(function (_tag) {
        this.set('tag.' + _tag, configs[_tag].tag);
        return {
          siteName: _tag,
          tagName: configs[_tag].tag
        }
      }, this);

      if ('hdfs-site' in configs) {
        this.set('tag.hdfs-site', configs['hdfs-site'].tag);
        tags.pushObject({
          siteName: "hdfs-site",
          tagName: this.get('tag.hdfs-site')
        });
      }
      this.getServiceConfigsFromServer(tags);
    }
  },

  getServiceConfigsFromServer: function (tags) {
    var self = this;

    App.router.get('configurationController').getConfigsByTags(tags).done(function (data) {
      var configs = data.findProperty('tag', self.get('tag.cluster-env')).properties;
      if (configs && (configs['security_enabled'] === 'true' || configs['security_enabled'] === true)) {
        self.set('securityEnabled', true);
      }
      else {
        self.set('securityEnabled', false);
        if (!!self.get('tag.hdfs-site')) {
          var hdfsConfigs = data.findProperty('tag', self.get('tag.hdfs-site')).properties;
          self.setNnHaStatus(hdfsConfigs);
        }
      }
      var userConfigs = {};
      data.forEach(function(_config){
        $.extend(userConfigs, _config.properties);
      });
      self.loadUsers(userConfigs);
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
    var defaultUserNameMap = this.get('userNameMap');
    this.set('serviceUsers',[]);

    for (var configName in defaultUserNameMap) {
      this.get('serviceUsers').push({
        name: configName,
        value: configs[configName] || defaultUserNameMap[configName]['defaultValue']
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


