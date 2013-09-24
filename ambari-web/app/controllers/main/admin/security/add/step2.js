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

      console.log('pushing ' + serviceConfig.serviceName, serviceConfig);

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
    var hdfsService = serviceConfigs.findProperty('serviceName', 'HDFS');
    var mapReduceService = serviceConfigs.findProperty('serviceName', 'MAPREDUCE');
    var yarnService = serviceConfigs.findProperty('serviceName', 'YARN');
    var hbaseService = serviceConfigs.findProperty('serviceName', 'HBASE');
    this.setHostsToConfig(hdfsService, 'datanode_hosts', 'DATANODE');
    this.setHostsToConfig(mapReduceService, 'tasktracker_hosts', 'TASKTRACKER');
    this.setHostsToConfig(yarnService, 'nodemanager_host', 'NODEMANAGER');
    this.setHostsToConfig(hbaseService, 'regionserver_hosts', 'HBASE_REGIONSERVER');
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
    if (oozieService) {
      var oozieServerHost = oozieService.configs.findProperty('name', 'oozie_servername');
      var oozieServerPrincipal = oozieService.configs.findProperty('name', 'oozie_principal_name');
      var oozieSpnegoPrincipal = oozieService.configs.findProperty('name', 'oozie_http_principal_name');
      if (oozieServerHost && oozieServerPrincipal && oozieSpnegoPrincipal) {
        oozieServerHost.defaultValue = App.Service.find('OOZIE').get('hostComponents').findProperty('componentName', 'OOZIE_SERVER').get('host.hostName');
        oozieServerPrincipal.defaultValue = 'oozie/' + oozieServerHost.defaultValue.toLowerCase();
        oozieSpnegoPrincipal.defaultValue = 'HTTP/' + oozieServerHost.defaultValue.toLowerCase();
      }
    }
    if (hiveService) {
      var hiveServerHost = hiveService.configs.findProperty('name', 'hive_metastore');
      if (hiveServerHost) {
        hiveServerHost.defaultValue = App.Service.find('HIVE').get('hostComponents').findProperty('componentName', 'HIVE_SERVER').get('host.hostName');
      }
    }
    if (webHcatService) {
      var webHcatHost = webHcatService.configs.findProperty('name', 'webhcatserver_host');
      var webHcatSpnegoPrincipal = webHcatService.configs.findProperty('name', 'webHCat_http_principal_name');
      if (webHcatHost && webHcatSpnegoPrincipal) {
        webHcatHost.defaultValue = App.Service.find('WEBHCAT').get('hostComponents').findProperty('componentName', 'WEBHCAT_SERVER').get('host.hostName');
        webHcatSpnegoPrincipal.defaultValue = 'HTTP/' + webHcatHost.defaultValue.toLowerCase();
      }
    }

    if (nagiosService) {
      var nagiosServerHost = nagiosService.configs.findProperty('name', 'nagios_server');
      var nagiosServerPrincipal = nagiosService.configs.findProperty('name', 'nagios_principal_name');
      if (nagiosServerHost && nagiosServerPrincipal) {
        nagiosServerHost.defaultValue = App.Service.find('NAGIOS').get('hostComponents').findProperty('componentName', 'NAGIOS_SERVER').get('host.hostName');
        nagiosServerPrincipal.defaultValue = 'nagios/' + nagiosServerHost.defaultValue.toLowerCase();
      }
    }
    if (hdfsService) {
      var namenodeHost = hdfsService.configs.findProperty('name', 'namenode_host');
      var sNamenodeHost = hdfsService.configs.findProperty('name', 'snamenode_host');
      var jnHosts = hdfsService.configs.findProperty('name', 'journalnode_hosts');
      var snComponent = App.Service.find('HDFS').get('hostComponents').findProperty('componentName', 'SECONDARY_NAMENODE');
      var jnComponent = App.Service.find('HDFS').get('hostComponents').findProperty('componentName', 'JOURNALNODE');
      if (namenodeHost) {
        namenodeHost.defaultValue = App.Service.find('HDFS').get('hostComponents').filterProperty('componentName', 'NAMENODE').mapProperty('host.hostName');
      }
      if(sNamenodeHost && snComponent) {
        sNamenodeHost.defaultValue = snComponent.get('host.hostName');
      }
      if(jnHosts && jnComponent) {
        this.setHostsToConfig(hdfsService, 'journalnode_hosts', 'JOURNALNODE');
      }
    }
    if (mapReduceService) {
      var jobTrackerHost = mapReduceService.configs.findProperty('name', 'jobtracker_host');
      if (jobTrackerHost) {
        jobTrackerHost.defaultValue = App.Service.find('MAPREDUCE').get('hostComponents').findProperty('componentName', 'JOBTRACKER').get('host.hostName');
      }
    }
    if (mapReduce2Service) {
      var jobHistoryServerHost = mapReduce2Service.configs.findProperty('name', 'jobhistoryserver_host');
      if (jobHistoryServerHost) {
        jobHistoryServerHost.defaultValue = App.Service.find('MAPREDUCE2').get('hostComponents').findProperty('componentName', 'HISTORYSERVER').get('host.hostName');
      }
    }
    if (yarnService) {
      var resourceManagerHost = yarnService.configs.findProperty('name', 'resourcemanager_host');
      if (resourceManagerHost) {
        resourceManagerHost.defaultValue = App.Service.find('YARN').get('hostComponents').findProperty('componentName', 'RESOURCEMANAGER').get('host.hostName');
      }
    }
    this.setHostsToConfig(hbaseService, 'hbasemaster_host', 'HBASE_MASTER');
    this.setHostsToConfig(zooKeeperService, 'zookeeperserver_hosts', 'ZOOKEEPER_SERVER');
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
