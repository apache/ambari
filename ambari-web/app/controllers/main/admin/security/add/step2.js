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

App.MainAdminSecurityAddStep2Controller = Em.Controller.extend({

  name: 'mainAdminSecurityAddStep2Controller',
  stepConfigs: [],
  installedServices: [],
  selectedService: null,
  securityUsers: [],

  isSubmitDisabled: function () {
    return !this.stepConfigs.filterProperty('showConfig', true).everyProperty('errorCount', 0);
  }.property('stepConfigs.@each.errorCount'),

  clearStep: function () {
    this.get('stepConfigs').clear();
    this.get('securityUsers').clear();
  },


  /**
   *  Function is called whenever the step is loaded
   */
  loadStep: function () {
    console.log("TRACE: Loading addSecurity step2: Configure Services");
    this.clearStep();
    this.loadUsers();
    this.addUserPrincipals(this.get('content.services'), this.get('securityUsers'));
    this.addMasterHostToGlobals(this.get('content.services'));
    this.addSlaveHostToGlobals(this.get('content.services'));
    this.renderServiceConfigs(this.get('content.services'));
    this.changeCategoryOnHa(this.get('content.services'), this.get('stepConfigs'));
    var storedServices = this.get('content.serviceConfigProperties');
    if (storedServices) {
      var configs = new Ember.Set();

      // for all services`
      this.get('stepConfigs').forEach(function (_content) {
        //for all components
        _content.get('configs').forEach(function (_config) {

          var componentVal = storedServices.findProperty('name', _config.get('name'));
          //if we have config for specified component
          if (componentVal) {
            //set it
            _config.set('value', componentVal.value);
          }

        }, this);
      }, this);

    }
    //
    this.set('installedServices', App.Service.find().mapProperty('serviceName'));
    console.log("The services are: " + this.get('installedServices'));
    //
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

      this.get('stepConfigs').pushObject(serviceConfig);
    }, this);
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
      componentConfig.configs.pushObject(serviceConfigProperty);
      serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isReconfigurable'));
      serviceConfigProperty.validate();
    }, this);
  },

  /**
   * fill config with hosts of component
   * @param service
   * @param configName
   * @param componentName
   */
  setHostsToConfig: function (service, configName, componentName) {
    if (service) {
      var hosts = service.configs.findProperty('name', configName);
      if (hosts) {
        hosts.defaultValue = App.Service.find(service.serviceName)
          .get('hostComponents')
          .filterProperty('componentName', componentName)
          .mapProperty('host.hostName');
      }
    }
  },

  /**
   * fill principal _HOST part with actual hostname of component
   * @param service
   * @param hostConfigName
   * @param principalConfigName
   * @param defaultPrimaryName
   */
  setHostToPrincipal: function (service, hostConfigName, principalConfigName, defaultPrimaryName) {
    if (service) {
      var host = service.configs.findProperty('name', hostConfigName);
      var principal = service.configs.findProperty('name', principalConfigName);
      if (host && principal) {
        if (host.defaultValue instanceof Array) {
          host.defaultValue = host.defaultValue[0];
        }
        principal.defaultValue = defaultPrimaryName + host.defaultValue.toLowerCase();
      }
    }
  },


  loadUsers: function () {
    var securityUsers = App.router.get('mainAdminSecurityController').get('serviceUsers');
    if (!securityUsers || securityUsers.length < 1) { // Page could be refreshed in middle
      if (App.testMode) {
        securityUsers.pushObject({id: 'puppet var', name: 'hdfs_user', value: 'hdfs'});
        securityUsers.pushObject({id: 'puppet var', name: 'mapred_user', value: 'mapred'});
        securityUsers.pushObject({id: 'puppet var', name: 'hbase_user', value: 'hbase'});
        securityUsers.pushObject({id: 'puppet var', name: 'hive_user', value: 'hive'});
        securityUsers.pushObject({id: 'puppet var', name: 'smokeuser', value: 'ambari-qa'});
      } else {
        securityUsers = App.db.getSecureUserInfo();
      }
    }
    this.set('securityUsers', securityUsers);
  },

  addUserPrincipals: function (serviceConfigs, securityUsers) {
    var smokeUser = securityUsers.findProperty('name', 'smokeuser');
    var hdfsUser = securityUsers.findProperty('name', 'hdfs_user');
    var hbaseUser = securityUsers.findProperty('name', 'hbase_user');
    var generalService = serviceConfigs.findProperty('serviceName', 'GENERAL');
    var smokeUserPrincipal = generalService.configs.findProperty('name', 'smokeuser_principal_name');
    var hdfsUserPrincipal = generalService.configs.findProperty('name', 'hdfs_principal_name');
    var hbaseUserPrincipal = generalService.configs.findProperty('name', 'hbase_principal_name');
    var hbaseUserKeytab = generalService.configs.findProperty('name', 'hbase_user_keytab');
    var hbaseService = serviceConfigs.findProperty('serviceName', 'HBASE');
    if (smokeUser && smokeUserPrincipal) {
      smokeUserPrincipal.defaultValue = smokeUser.value;
    }
    if (hdfsUser && hdfsUserPrincipal) {
      hdfsUserPrincipal.defaultValue = hdfsUser.value;
    }
    if (hbaseService && hbaseUser && hbaseUserPrincipal) {
      hbaseUserPrincipal.defaultValue = hbaseUser.value;
      hbaseUserPrincipal.isVisible = true;
      hbaseUserKeytab.isVisible = true;
    }
  },

  addSlaveHostToGlobals: function (serviceConfigs) {
    var serviceComponentMap = [
      {
        serviceName: 'HDFS',
        configName: 'datanode_hosts',
        component: 'DATANODE'
      },
      {
        serviceName: 'MAPREDUCE',
        configName: 'tasktracker_hosts',
        component: 'TASKTRACKER'
      },
      {
        serviceName: 'YARN',
        configName: 'nodemanager_host',
        component: 'NODEMANAGER'
      },
      {
        serviceName: 'HBASE',
        configName: 'regionserver_hosts',
        component: 'HBASE_REGIONSERVER'
      },
      {
        serviceName: 'STORM',
        configName: 'supervisor_hosts',
        component: 'SUPERVISOR'
      }
    ];
    serviceComponentMap.forEach(function(service) {
      this.setHostsToConfig(serviceConfigs.findProperty('serviceName', service.serviceName), service.configName, service.component);
    }, this);
  },

  addMasterHostToGlobals: function (serviceConfigs) {
    var oozieService = serviceConfigs.findProperty('serviceName', 'OOZIE');
    var hiveService = serviceConfigs.findProperty('serviceName', 'HIVE');
    var webHcatService = serviceConfigs.findProperty('serviceName', 'WEBHCAT');
    var nagiosService = serviceConfigs.findProperty('serviceName', 'NAGIOS');
    var hbaseService = serviceConfigs.findProperty('serviceName', 'HBASE');
    var zooKeeperService = serviceConfigs.findProperty('serviceName', 'ZOOKEEPER');
    var hdfsService = serviceConfigs.findProperty('serviceName', 'HDFS');
    var mapReduceService = serviceConfigs.findProperty('serviceName', 'MAPREDUCE');
    var mapReduce2Service = serviceConfigs.findProperty('serviceName', 'MAPREDUCE2');
    var yarnService = serviceConfigs.findProperty('serviceName', 'YARN');
    var stormService = serviceConfigs.findProperty('serviceName', 'STORM');
    var falconService = serviceConfigs.findProperty('serviceName', 'FALCON');


    this.setHostsToConfig(oozieService, 'oozie_servername', 'OOZIE_SERVER');
    this.setHostsToConfig(hiveService, 'hive_metastore', 'HIVE_SERVER');
    this.setHostsToConfig(webHcatService, 'webhcatserver_host', 'WEBHCAT_SERVER');
    this.setHostsToConfig(nagiosService, 'nagios_server', 'NAGIOS_SERVER');
    this.setHostsToConfig(hdfsService, 'namenode_host', 'NAMENODE');
    this.setHostsToConfig(hdfsService, 'snamenode_host', 'SECONDARY_NAMENODE');
    this.setHostsToConfig(hdfsService, 'journalnode_hosts', 'JOURNALNODE');
    this.setHostsToConfig(mapReduceService, 'jobtracker_host', 'JOBTRACKER');
    this.setHostsToConfig(mapReduceService, 'jobhistoryserver_host', 'HISTORYSERVER');
    this.setHostsToConfig(mapReduce2Service, 'jobhistoryserver_host', 'HISTORYSERVER');
    this.setHostsToConfig(yarnService, 'resourcemanager_host', 'RESOURCEMANAGER');
    this.setHostsToConfig(hbaseService, 'hbasemaster_host', 'HBASE_MASTER');
    this.setHostsToConfig(zooKeeperService, 'zookeeperserver_hosts', 'ZOOKEEPER_SERVER');
    this.setHostsToConfig(falconService, 'falcon_server_host', 'FALCON_SERVER');
    if (stormService) {
      var stormComponents = ['STORM_UI_SERVER','NIMBUS','SUPERVISOR'];
      var stormHosts = [];
      stormComponents.forEach(function(componentName) {
        stormHosts.pushObjects(App.Service.find(stormService.serviceName)
          .get('hostComponents')
          .filterProperty('componentName', componentName)
          .mapProperty('host.hostName'));
      }, this);
      var hosts = stormService.configs.findProperty('name', 'storm_host');
      hosts.defaultValue  = stormHosts.uniq();
    }

    // Oozie, Falcon, WebHcat and Nagios does not support _HOST in the principal name. Actual hostname should be set instead of _HOST

    this.setHostToPrincipal(oozieService, 'oozie_servername','oozie_principal_name','oozie/');
    this.setHostToPrincipal(oozieService, 'oozie_servername','oozie_http_principal_name','HTTP/');
    this.setHostToPrincipal(falconService, 'falcon_server_host','falcon_principal_name','falcon/');
    this.setHostToPrincipal(falconService, 'falcon_server_host','falcon_http_principal_name','HTTP/');
    this.setHostToPrincipal(webHcatService, 'webhcatserver_host','webHCat_http_principal_name','HTTP/');
    this.setHostToPrincipal(nagiosService, 'nagios_server','nagios_principal_name','nagios/');
  },

  changeCategoryOnHa: function (serviceConfigs, stepConfigs) {
    var hdfsService = serviceConfigs.findProperty('serviceName', 'HDFS');
    if (hdfsService) {
      var hdfsProperties = stepConfigs.findProperty('serviceName', 'HDFS').get('configs');
      var configCategories = hdfsService.configCategories;
      if ((App.testMode && App.testNameNodeHA) || (this.get('content.isNnHa') === 'true')) {
        hdfsProperties.filterProperty('category', 'SNameNode').forEach(function (_snConfig) {
          _snConfig.set('isVisible', false);
        }, this);
        var snCategory = configCategories.findProperty('name', 'SNameNode');
        if (snCategory) {
          configCategories.removeObject(snCategory);
        }
      } else {
        hdfsProperties.filterProperty('category', 'JournalNode').forEach(function (_jnConfig) {
          _jnConfig.set('isVisible', false);
        }, this);
        var jnCategory = configCategories.findProperty('name', 'JournalNode');
        if (jnCategory) {
          configCategories.removeObject(jnCategory);
        }
      }
    }
  },

  /**
   *  submit and move to step3
   */

  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      App.router.send('next');
    }
  }

});
