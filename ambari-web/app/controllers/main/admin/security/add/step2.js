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

  isSubmitDisabled: function () {
    return !this.stepConfigs.filterProperty('showConfig', true).everyProperty('errorCount', 0);
  }.property('stepConfigs.@each.errorCount'),

  clearStep: function () {
    this.get('stepConfigs').clear();
  },


  /**
   *  Function is called whenever the step is loaded
   */
  loadStep: function () {
    console.log("TRACE: Loading addSecurity step2: Configure Services");
    this.clearStep();
    this.addMasterHostToGlobals(this.get('content.services'));
    this.addSlaveHostToGlobals(this.get('content.services'));
    this.renderServiceConfigs(this.get('content.services'));
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
  setHostsToConfig: function(service, configName, componentName){
    if(service){
      var hosts = service.configs.findProperty('name', configName);
      if(hosts){
        hosts.defaultValue = App.Service.find(service.serviceName)
          .get('hostComponents')
          .filterProperty('componentName', componentName)
          .mapProperty('host.hostName');
      }
    }
  },

  addSlaveHostToGlobals: function(serviceConfigs){
    var hdfsService = serviceConfigs.findProperty('serviceName', 'HDFS');
    var mapReduceService = serviceConfigs.findProperty('serviceName', 'MAPREDUCE');
    var hbaseService = serviceConfigs.findProperty('serviceName', 'HBASE');
    this.setHostsToConfig(hdfsService, 'datanode_hosts', 'DATANODE');
    this.setHostsToConfig(mapReduceService, 'tasktracker_hosts', 'TASKTRACKER');
    this.setHostsToConfig(hbaseService, 'regionserver_hosts', 'HBASE_REGIONSERVER');
  },

  addMasterHostToGlobals: function (serviceConfigs) {
    var oozieService = serviceConfigs.findProperty('serviceName', 'OOZIE');
    var hiveService = serviceConfigs.findProperty('serviceName', 'HIVE');
    var webHcatService = App.Service.find().mapProperty('serviceName').contains('WEBHCAT');
    var nagiosService = serviceConfigs.findProperty('serviceName', 'NAGIOS');
    var generalService = serviceConfigs.findProperty('serviceName', 'GENERAL');
    var hbaseService = serviceConfigs.findProperty('serviceName', 'HBASE');
    var zooKeeperService = serviceConfigs.findProperty('serviceName', 'ZOOKEEPER');
    var hdfsService = serviceConfigs.findProperty('serviceName', 'HDFS');
    var mapReduceService = serviceConfigs.findProperty('serviceName', 'MAPREDUCE');
    if (oozieService) {
      var oozieServerHost = oozieService.configs.findProperty('name', 'oozie_servername');
      var oozieServerPrincipal = oozieService.configs.findProperty('name', 'oozie_principal_name');
      var oozieSpnegoPrincipal =  generalService.configs.findProperty('name', 'oozie_http_principal_name');
      if (oozieServerHost && oozieServerPrincipal && oozieSpnegoPrincipal) {
        oozieServerHost.defaultValue = App.Service.find('OOZIE').get('hostComponents').findProperty('componentName', 'OOZIE_SERVER').get('host.hostName');
        oozieServerPrincipal.defaultValue = 'oozie/' + oozieServerHost.defaultValue;
        oozieSpnegoPrincipal.defaultValue = 'HTTP/' + oozieServerHost.defaultValue;
        oozieSpnegoPrincipal.isVisible = true;
      }
    }
    if (hiveService) {
      var hiveServerHost = hiveService.configs.findProperty('name', 'hive_metastore');
      if (hiveServerHost) {
        hiveServerHost.defaultValue = App.Service.find('HIVE').get('hostComponents').findProperty('componentName', 'HIVE_SERVER').get('host.hostName');
      }
    }

    if(webHcatService) {
      var webHcatHost =  App.Service.find('WEBHCAT').get('hostComponents').findProperty('componentName', 'WEBHCAT_SERVER').get('host.hostName');
      var webHcatSpnegoPrincipal =  generalService.configs.findProperty('name', 'webHCat_http_principal_name');
      if(webHcatHost && webHcatSpnegoPrincipal) {
        webHcatSpnegoPrincipal.defaultValue = 'HTTP/' + webHcatHost;
        webHcatSpnegoPrincipal.isVisible = true;
      }
    }

    if(nagiosService) {
      var nagiosServerHost = nagiosService.configs.findProperty('name', 'nagios_server');
      var nagiosServerPrincipal = nagiosService.configs.findProperty('name', 'nagios_principal_name');
      if (nagiosServerHost && nagiosServerPrincipal) {
        nagiosServerHost.defaultValue = App.Service.find('NAGIOS').get('hostComponents').findProperty('componentName', 'NAGIOS_SERVER').get('host.hostName');
        nagiosServerPrincipal.defaultValue = 'nagios/' + nagiosServerHost.defaultValue;
      }
    }
    if(hdfsService){
      var namenodeHost = hdfsService.configs.findProperty('name', 'namenode_host');
      var sNamenodeHost = hdfsService.configs.findProperty('name', 'snamenode_host');
      if (namenodeHost && sNamenodeHost) {
        namenodeHost.defaultValue = App.Service.find('HDFS').get('hostComponents').findProperty('componentName', 'NAMENODE').get('host.hostName');
        sNamenodeHost.defaultValue = App.Service.find('HDFS').get('hostComponents').findProperty('componentName', 'SECONDARY_NAMENODE').get('host.hostName');
      }
    }
    if(mapReduceService){
      var jobTrackerHost = mapReduceService.configs.findProperty('name', 'jobtracker_host');
      if (jobTrackerHost) {
        jobTrackerHost.defaultValue = App.Service.find('MAPREDUCE').get('hostComponents').findProperty('componentName', 'JOBTRACKER').get('host.hostName');
      }
    }
    this.setHostsToConfig(hbaseService, 'hbasemaster_host', 'HBASE_MASTER');
    this.setHostsToConfig(zooKeeperService, 'zookeeperserver_hosts', 'ZOOKEEPER_SERVER');
  },

  showHostPrincipalKeytabList: function(){
    App.ModalPopup.show({
      self: this,
      header: Em.I18n.t('admin.security.step2.popup.header'),
      primary: Em.I18n.t('common.proceed'),
      downloadCsv: Em.I18n.t('admin.security.step2.popup.downloadCSV'),
      classNames: ['sixty-percent-width-modal'],
      csvContent: [],
      onDownloadCsv: function(){
        var blob = new Blob([this.get('csvContent')], {type: "text/csv;charset=utf-8"});
        saveAs(blob, "host-principal-keytab-list.csv");
      },
      onPrimary: function(){
        this.hide();
        App.router.send('next');
      },
      buildCsvContent: function(data){
        this.set('csvContent', stringUtils.arrayToCSV(data));
      },
      bodyClass: Em.View.extend({
        componentsToDisplay: ['NAMENODE', 'SECONDARY_NAMENODE', 'DATANODE', 'JOBTRACKER', 'ZOOKEEPER_SERVER', 'HIVE_SERVER', 'TASKTRACKER',
        'OOZIE_SERVER', 'NAGIOS_SERVER', 'HBASE_MASTER', 'HBASE_REGIONSERVER'],
        hostComponents: function(){
          var componentsToDisplay = this.get('componentsToDisplay');
          var configs = this.get('parentView.self.stepConfigs');
          var hosts = App.Host.find();
          var result = [];
          hosts.forEach(function(host){
            host.get('hostComponents').forEach(function(hostComponent){
              if(componentsToDisplay.contains(hostComponent.get('componentName'))){
                var serviceConfigs = configs.findProperty('serviceName', hostComponent.get('service.serviceName')).get('configs');
                var principal, keytab;
                serviceConfigs.forEach(function(config){
                  if (config.get('component') && config.get('component') === hostComponent.get('componentName')) {
                    if (config.get('name').substr(-15, 15) === '_principal_name') {
                      principal = config.get('value').replace('_HOST', host.get('hostName')) + config.get('unit');
                    } else if (config.get('name').substr(-7, 7) === '_keytab' || config.get('name').substr(-12, 12) === '_keytab_path') {
                      keytab = config.get('value');
                    }
                  } else if (config.get('components') && config.get('components').contains(hostComponent.get('componentName'))) {
                    if (config.get('name').substr(-15, 15) === '_principal_name') {
                      principal = config.get('value').replace('_HOST', host.get('hostName')) + config.get('unit');
                    } else if (config.get('name').substr(-7, 7) === '_keytab' || config.get('name').substr(-12, 12) === '_keytab_path') {
                      keytab = config.get('value');
                    }
                  }
                });

                result.push({
                  host: host.get('hostName'),
                  component: hostComponent.get('displayName'),
                  principal: principal,
                  keytab: keytab
                });
              }
            });
          });
          this.get('parentView').buildCsvContent(result);
          return result;
        }.property(),
        template: Em.Handlebars.compile([
          '<div class="alert alert-info">{{t admin.security.step2.popup.notice}}</div>',
          '<div class="long-popup-list">',
            '<table class="table table-bordered table-striped">',
            '<thead>',
              '<tr>',
                '<th>{{t common.host}}</th>',
                '<th>{{t common.component}}</th>',
                '<th>{{t admin.security.step2.popup.table.principal}}</th>',
                '<th>{{t admin.security.step2.popup.table.keytab}}</th>',
              '</tr>',
            '</thead>',
            '<tbody>',
            '{{#each hostComponent in view.hostComponents}}',
              '<tr>',
                '<td>{{hostComponent.host}}</td>',
                '<td>{{hostComponent.component}}</td>',
                '<td>{{hostComponent.principal}}</td>',
                '<td>{{hostComponent.keytab}}</td>',
              '</tr>',
            '{{/each}}',
            '</tbody>',
            '</table>',
          '</div>'
        ].join(''))
      }),
      footerClass: Em.View.extend({
        classNames: ['modal-footer'],
        template: Em.Handlebars.compile([
          '{{#if view.parentView.downloadCsv}}<a class="btn btn-info" {{action onDownloadCsv target="view.parentView"}}>{{view.parentView.downloadCsv}}</a>&nbsp;{{/if}}',
          '{{#if view.parentView.secondary}}<a class="btn" {{action onSecondary target="view.parentView"}}>{{view.parentView.secondary}}</a>&nbsp;{{/if}}',
          '{{#if view.parentView.primary}}<a class="btn btn-success" {{action onPrimary target="view.parentView"}}>{{view.parentView.primary}}</a>{{/if}}'
        ].join(''))
      })
    });
  },

  /**
   *  submit and move to step3
   */

  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      if (App.supports.secureClusterProceedPopup) {
        this.showHostPrincipalKeytabList();
      } else {
        App.router.send('next');
      }
    }
  },

  doDownloadCsv: function(){
      var blob = new Blob([this.buildCvsContent()], {type: "text/csv;charset=utf-8"});
      saveAs(blob, "host-principal-keytab-list.csv");
    },
    
    buildCvsContent: function() {
      var configs = this.get('stepConfigs');
      var hosts = App.Host.find();
      var result = [];
      var componentsToDisplay = ['NAMENODE', 'SECONDARY_NAMENODE', 'DATANODE', 'JOBTRACKER', 'ZOOKEEPER_SERVER', 'HIVE_SERVER', 'TASKTRACKER',
                                 'OOZIE_SERVER', 'NAGIOS_SERVER', 'HBASE_MASTER', 'HBASE_REGIONSERVER'];
      var securityUsers = App.router.get('mainAdminSecurityController').get('serviceUsers');
      if (!securityUsers || securityUsers.length < 1) { // Page could be refreshed in middle
        if (App.testMode) {
          securityUsers.pushObject({id: 'puppet var', name: 'hdfs_user', value: 'hdfs'});
          securityUsers.pushObject({id: 'puppet var', name: 'mapred_user', value: 'mapred'});
          securityUsers.pushObject({id: 'puppet var', name: 'hbase_user', value: 'hbase'});
          securityUsers.pushObject({id: 'puppet var', name: 'hive_user', value: 'hive'});
          securityUsers.pushObject({id: 'puppet var', name: 'smokeuser', value: 'ambari-qa'});
        } else {
          App.router.get('mainAdminSecurityController').setSecurityStatus();
          securityUsers = App.router.get('mainAdminSecurityController').get('serviceUsers');
        }
      }
      var isHbaseInstalled = App.Service.find().findProperty('serviceName', 'HBASE');
      var generalConfigs = configs.findProperty('serviceName', 'GENERAL').configs;
      var realm = generalConfigs.findProperty('name', 'kerberos_domain').get('value');
      var smokeUserId = securityUsers.findProperty('name', 'smokeuser').value;
      var hdfsUserId = securityUsers.findProperty('name', 'hdfs_user').value;
      var hbaseUserId = securityUsers.findProperty('name', 'hbase_user').value;
      var mapredUserId = securityUsers.findProperty('name', 'mapred_user').value;
      var hiveUserId = securityUsers.findProperty('name', 'hive_user').value;
      var zkUserId = securityUsers.findProperty('name', 'zk_user').value;
      var oozieUserId = securityUsers.findProperty('name', 'oozie_user').value;
      var nagiosUserId = securityUsers.findProperty('name', 'nagios_user').value;
      var hadoopGroupId = securityUsers.findProperty('name', 'user_group').value;
      
      var smokeUser = smokeUserId + '@' + realm;
      var hdfsUser = hdfsUserId + '@' + realm;
      var hbaseUser = hbaseUserId + '@' + realm;
      var smokeUserKeytabPath = generalConfigs.findProperty('name', 'smokeuser_keytab').get('value');
      var hdfsUserKeytabPath = generalConfigs.findProperty('name', 'keytab_path').get('value') + "/hdfs.headless.keytab";
      var hbaseUserKeytabPath = generalConfigs.findProperty('name', 'keytab_path').get('value') + "/hbase.headless.keytab";
      var httpPrincipal = generalConfigs.findProperty('name', 'hadoop_http_principal_name');
      var httpKeytabPath = generalConfigs.findProperty('name', 'hadoop_http_keytab').get('value');
      var componentToOwnerMap = {
          'NAMENODE': hdfsUserId,
          'SECONDARY_NAMENODE': hdfsUserId,
          'DATANODE': hdfsUserId,
          'TASKTRACKER': mapredUserId,
          'JOBTRACKER': mapredUserId,
          'ZOOKEEPER_SERVER': zkUserId,
          'HIVE_SERVER': hiveUserId,
          'OOZIE_SERVER': oozieUserId,
          'NAGIOS_SERVER': nagiosUserId,
          'HBASE_MASTER': hbaseUserId,
          'HBASE_REGIONSERVER': hbaseUserId
      };
      
      var addedPrincipalsHost = {}; //Keys = host_principal, Value = 'true'
      
      hosts.forEach(function(host){
        result.push({
          host: host.get('hostName'),
          component: Em.I18n.t('admin.addSecurity.user.smokeUser'),
          principal: smokeUser,
          keytab: smokeUserKeytabPath,
          owner: smokeUserId,
          group: hadoopGroupId,
          acl: '440'
        });
        result.push({
          host: host.get('hostName'),
          component: Em.I18n.t('admin.addSecurity.user.hdfsUser'),
          principal: hdfsUser,
          keytab: hdfsUserKeytabPath,
          owner: hdfsUserId,
          group: hadoopGroupId,
          acl: '440'
        });
        if (isHbaseInstalled) {
          result.push({
            host: host.get('hostName'),
            component: Em.I18n.t('admin.addSecurity.user.hbaseUser'),
            principal: hbaseUser,
            keytab: hbaseUserKeytabPath,
            owner: hbaseUserId,
            group: hadoopGroupId,
            acl: '440'
          });
        }
        if(host.get('hostComponents').someProperty('componentName', 'NAMENODE') || 
          host.get('hostComponents').someProperty('componentName', 'SECONDARY_NAMENODE') ||
          host.get('hostComponents').someProperty('componentName', 'WEBHCAT_SERVER') ||
          host.get('hostComponents').someProperty('componentName', 'OOZIE_SERVER')){
          result.push({
            host: host.get('hostName'),
            component: Em.I18n.t('admin.addSecurity.user.httpUser'),
            principal: httpPrincipal.get('value').replace('_HOST', host.get('hostName')) + httpPrincipal.get('unit'),
            keytab: httpKeytabPath,
            owner: 'root',
            group: hadoopGroupId,
            acl: '440'
          });
        }
        host.get('hostComponents').forEach(function(hostComponent){
          if(componentsToDisplay.contains(hostComponent.get('componentName'))){
            var serviceConfigs = configs.findProperty('serviceName', hostComponent.get('service.serviceName')).get('configs');
            var principal, keytab;
            serviceConfigs.forEach(function(config){
              if (config.get('component') && config.get('component') === hostComponent.get('componentName')) {
                if (config.get('name').endsWith('_principal_name')) {
                  principal = config.get('value').replace('_HOST', host.get('hostName')) + config.get('unit');
                } else if (config.get('name').endsWith('_keytab') || config.get('name').endsWith('_keytab_path')) {
                  keytab = config.get('value');
                }
              } else if (config.get('components') && config.get('components').contains(hostComponent.get('componentName'))) {
                if (config.get('name').endsWith('_principal_name')) {
                  principal = config.get('value').replace('_HOST', host.get('hostName')) + config.get('unit');
                } else if (config.get('name').endsWith('_keytab') || config.get('name').endsWith('_keytab_path')) {
                  keytab = config.get('value');
                }
              }
            });
            
   
            var key = host.get('hostName') + "--" + principal;
            if (!addedPrincipalsHost[key]) {
              var owner = componentToOwnerMap[hostComponent.get('componentName')];
              if(!owner){
                owner = '';
              }
              result.push({
                host: host.get('hostName'),
                component: hostComponent.get('displayName'),
                principal: principal,
                keytab: keytab,
                owner: owner,
                group: hadoopGroupId,
                acl: '400'
              });
              addedPrincipalsHost[key] = true;
            }
          }
        });
      });
      return stringUtils.arrayToCSV(result);
    }
});