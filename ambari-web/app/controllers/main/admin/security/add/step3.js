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

App.MainAdminSecurityAddStep3Controller = Em.Controller.extend({
  name: 'mainAdminSecurityAddStep3Controller',
  hostComponents: [],
  doDownloadCsv: function () {
    if ($.browser.msie && $.browser.version < 10) {
      this.openInfoInNewTab();
    } else {
      try {
        var blob = new Blob([stringUtils.arrayToCSV(this.get('hostComponents'))], {type: "text/csv;charset=utf-8;"});
        saveAs(blob, "host-principal-keytab-list.csv");
      } catch(e) {
         this.openInfoInNewTab();
      }
    }
  },
  openInfoInNewTab: function () {
    var newWindow = window.open('');
    var newDocument = newWindow.document;
    newDocument.write(stringUtils.arrayToCSV(this.get('hostComponents')));
    newWindow.focus();
  },
  loadStep: function(){
    var configs = this.get('content.serviceConfigProperties');
    var hosts = App.Host.find();
    var result = [];
    var componentsToDisplay = ['NAMENODE', 'SECONDARY_NAMENODE', 'DATANODE', 'JOBTRACKER', 'ZOOKEEPER_SERVER', 'HIVE_SERVER', 'TASKTRACKER',
      'OOZIE_SERVER', 'NAGIOS_SERVER', 'HBASE_MASTER', 'HBASE_REGIONSERVER','HISTORYSERVER','RESOURCEMANAGER','NODEMANAGER','JOURNALNODE',
      'SUPERVISOR', 'NIMBUS', 'STORM_UI_SERVER','FALCON_SERVER'];
    var securityUsers = [];
    if (!securityUsers || securityUsers.length < 1) { // Page could be refreshed in middle
      securityUsers = this.getSecurityUsers();
    }
    var isHbaseInstalled = App.Service.find().findProperty('serviceName', 'HBASE');
    var isStormInstalled = App.Service.find().findProperty('serviceName', 'STORM');
    var generalConfigs = configs.filterProperty('serviceName', 'GENERAL');
    var hdfsConfigs = configs.filterProperty('serviceName', 'HDFS');
    var realm = generalConfigs.findProperty('name', 'kerberos_domain').value;
    var smokeUserId = securityUsers.findProperty('name', 'smokeuser').value;
    var hdfsUserId = securityUsers.findProperty('name', 'hdfs_user').value;
    var hbaseUserId = securityUsers.findProperty('name', 'hbase_user').value;
    var mapredUserId = securityUsers.findProperty('name', 'mapred_user').value;
    var yarnUserId =  securityUsers.findProperty('name', 'yarn_user').value;
    var hiveUserId = securityUsers.findProperty('name', 'hive_user').value;
    var zkUserId = securityUsers.findProperty('name', 'zk_user').value;
    var oozieUserId = securityUsers.findProperty('name', 'oozie_user').value;
    var nagiosUserId = securityUsers.findProperty('name', 'nagios_user').value;
    var hadoopGroupId = securityUsers.findProperty('name', 'user_group').value;
    var stormUserId = securityUsers.findProperty('name', 'storm_user').value;
    var falconUserId =  securityUsers.findProperty('name', 'falcon_user').value;

    var smokeUser = smokeUserId + '@' + realm;
    var hdfsUser = hdfsUserId + '@' + realm;
    var hbaseUser = hbaseUserId + '@' + realm;
    var stormUser = stormUserId + '@' + realm;

    var smokeUserKeytabPath = generalConfigs.findProperty('name', 'smokeuser_keytab').value;
    var hdfsUserKeytabPath = generalConfigs.findProperty('name', 'hdfs_user_keytab').value;
    var hbaseUserKeytabPath = generalConfigs.findProperty('name', 'hbase_user_keytab').value;

    var hadoopHttpPrincipal = hdfsConfigs.findProperty('name', 'hadoop_http_principal_name');
    var hadoopHttpKeytabPath = hdfsConfigs.findProperty('name', 'hadoop_http_keytab').value;
    var componentToOwnerMap = {
      'NAMENODE': hdfsUserId,
      'SECONDARY_NAMENODE': hdfsUserId,
      'DATANODE': hdfsUserId,
      'JOURNALNODE': hdfsUserId,
      'TASKTRACKER': mapredUserId,
      'JOBTRACKER': mapredUserId,
      'HISTORYSERVER': mapredUserId,
      'RESOURCEMANAGER':yarnUserId,
      'NODEMANAGER':yarnUserId,
      'ZOOKEEPER_SERVER': zkUserId,
      'HIVE_SERVER': hiveUserId,
      'OOZIE_SERVER': oozieUserId,
      'NAGIOS_SERVER': nagiosUserId,
      'HBASE_MASTER': hbaseUserId,
      'HBASE_REGIONSERVER': hbaseUserId,
      'SUPERVISOR': stormUserId,
      'NIMBUS': stormUserId,
      'STORM_UI_SERVER': stormUserId,
      'FALCON_SERVER': falconUserId
    };

    var addedPrincipalsHost = {}; //Keys = host_principal, Value = 'true'

    hosts.forEach(function (host) {
      result.push({
        host: host.get('hostName'),
        component: Em.I18n.t('admin.addSecurity.user.smokeUser'),
        principal: smokeUser,
        keytabFile: stringUtils.getFileFromPath(smokeUserKeytabPath),
        keytab: stringUtils.getPath(smokeUserKeytabPath),
        owner: smokeUserId,
        group: hadoopGroupId,
        acl: '440'
      });
      result.push({
        host: host.get('hostName'),
        component: Em.I18n.t('admin.addSecurity.user.hdfsUser'),
        principal: hdfsUser,
        keytabFile: stringUtils.getFileFromPath(hdfsUserKeytabPath),
        keytab: stringUtils.getPath(hdfsUserKeytabPath),
        owner: hdfsUserId,
        group: hadoopGroupId,
        acl: '440'
      });
      if (isHbaseInstalled) {
        result.push({
          host: host.get('hostName'),
          component: Em.I18n.t('admin.addSecurity.user.hbaseUser'),
          principal: hbaseUser,
          keytabFile: stringUtils.getFileFromPath(hbaseUserKeytabPath),
          keytab: stringUtils.getPath(hbaseUserKeytabPath),
          owner: hbaseUserId,
          group: hadoopGroupId,
          acl: '440'
        });
      }

      this.setComponentConfig(result,host,'NAMENODE','HDFS','hadoop_http_principal_name','hadoop_http_keytab',Em.I18n.t('admin.addSecurity.hdfs.user.httpUser'),hadoopGroupId);
      this.setComponentConfig(result,host,'SECONDARY_NAMENODE','HDFS','hadoop_http_principal_name','hadoop_http_keytab',Em.I18n.t('admin.addSecurity.hdfs.user.httpUser'),hadoopGroupId);
      this.setComponentConfig(result,host,'JOURNALNODE','HDFS','hadoop_http_principal_name','hadoop_http_keytab',Em.I18n.t('admin.addSecurity.hdfs.user.httpUser'),hadoopGroupId);
      this.setComponentConfig(result,host,'WEBHCAT_SERVER','WEBHCAT','webHCat_http_principal_name','webhcat_http_keytab',Em.I18n.t('admin.addSecurity.webhcat.user.httpUser'),hadoopGroupId);
      this.setComponentConfig(result,host,'OOZIE_SERVER','OOZIE','oozie_http_principal_name','oozie_http_keytab',Em.I18n.t('admin.addSecurity.oozie.user.httpUser'),hadoopGroupId);
      this.setComponentConfig(result,host,'FALCON_SERVER','FALCON','falcon_http_principal_name','falcon_http_keytab',Em.I18n.t('admin.addSecurity.falcon.user.httpUser'),hadoopGroupId);
      //Derive Principal name and Keytabs only if its HDP-2 stack
      if (App.get('isHadoop2Stack')) {
        this.setComponentConfig(result,host,'HISTORYSERVER','MAPREDUCE2','jobhistory_http_principal_name','jobhistory_http_keytab',Em.I18n.t('admin.addSecurity.historyServer.user.httpUser'),hadoopGroupId);
        this.setComponentConfig(result,host,'RESOURCEMANAGER','YARN','resourcemanager_http_principal_name','resourcemanager_http_keytab',Em.I18n.t('admin.addSecurity.rm.user.httpUser'),hadoopGroupId);
        this.setComponentConfig(result,host,'NODEMANAGER','YARN','nodemanager_http_principal_name','nodemanager_http_keytab',Em.I18n.t('admin.addSecurity.nm.user.httpUser'),hadoopGroupId);
      }

      host.get('hostComponents').forEach(function(hostComponent){
        if(componentsToDisplay.contains(hostComponent.get('componentName'))){
          var serviceConfigs = configs.filterProperty('serviceName', hostComponent.get('service.serviceName'));
          var principal, keytab;
          serviceConfigs.forEach(function (config) {
            if (config.component && config.component === hostComponent.get('componentName')) {
              if (config.name.endsWith('_principal_name')) {
                principal = config.value.replace('_HOST', host.get('hostName').toLowerCase()) + config.unit;
              } else if (config.name.endsWith('_keytab') || config.name.endsWith('_keytab_path')) {
                keytab = config.value;
              }
            } else if (config.components && config.components.contains(hostComponent.get('componentName'))) {
              if (config.name.endsWith('_principal_name')) {
                principal = config.value.replace('_HOST', host.get('hostName').toLowerCase()) + config.unit;
              } else if (config.name.endsWith('_keytab') || config.name.endsWith('_keytab_path')) {
                keytab = config.value;
              }
            }
          });
          var displayName = this.changeDisplayName(hostComponent.get('displayName'));
          var key = host.get('hostName') + "--" + principal;
          if (!addedPrincipalsHost[key]) {
            var owner = componentToOwnerMap[hostComponent.get('componentName')];
            if(!owner){
              owner = '';
            }
            result.push({
              host: host.get('hostName'),
              component: displayName,
              principal: principal,
              keytabFile: stringUtils.getFileFromPath(keytab),
              keytab: stringUtils.getPath(keytab),
              owner: owner,
              group: hadoopGroupId,
              acl: '400'
            });
            addedPrincipalsHost[key] = true;
          }
        }
      },this);
    },this);
    this.set('hostComponents', result);
  },

  getSecurityUsers: function() {
    var securityUsers = [];
    if (App.testMode) {
      securityUsers.pushObject({id: 'puppet var', name: 'hdfs_user', value: 'hdfs'});
      securityUsers.pushObject({id: 'puppet var', name: 'mapred_user', value: 'mapred'});
      securityUsers.pushObject({id: 'puppet var', name: 'yarn_user', value: 'yarn'});
      securityUsers.pushObject({id: 'puppet var', name: 'hbase_user', value: 'hbase'});
      securityUsers.pushObject({id: 'puppet var', name: 'hive_user', value: 'hive'});
      securityUsers.pushObject({id: 'puppet var', name: 'falcon_user', value: 'falcon'});
      securityUsers.pushObject({id: 'puppet var', name: 'smokeuser', value: 'ambari-qa'});
      securityUsers.pushObject({id: 'puppet var', name: 'zk_user', value: 'zookeeper'});
      securityUsers.pushObject({id: 'puppet var', name: 'oozie_user', value: 'oozie'});
      securityUsers.pushObject({id: 'puppet var', name: 'nagios_user', value: 'nagios'});
      securityUsers.pushObject({id: 'puppet var', name: 'user_group', value: 'hadoop'});
    } else {
      securityUsers = App.db.getSecureUserInfo();
    }
    return securityUsers;
  },

  setComponentConfig: function(hostComponents,host,componentName,serviceName,principal,keytab,displayName,groupId) {
    if (host.get('hostComponents').someProperty('componentName', componentName)) {
      var result = {};
      var configs = this.get('content.serviceConfigProperties');
      var serviceConfigs = configs.filterProperty('serviceName', serviceName);
      var servicePrincipal = serviceConfigs.findProperty('name', principal);
      var serviceKeytabPath = serviceConfigs.findProperty('name', keytab).value;
      result.host = host.get('hostName');
      result.component = displayName;
      result.principal = servicePrincipal.value.replace('_HOST', host.get('hostName').toLowerCase()) + servicePrincipal.unit;
      result.keytabfile = stringUtils.getFileFromPath(serviceKeytabPath);
      result.keytab = stringUtils.getPath(serviceKeytabPath);
      result.owner = 'root';
      result.group = groupId;
      result.acl = '440';
      hostComponents.push(result);
    }
  },

  changeDisplayName: function (name) {
    if (name === 'HiveServer2') {
      return 'Hive Metastore and HiveServer2';
    } else {
      return name;
    }
  }
});
