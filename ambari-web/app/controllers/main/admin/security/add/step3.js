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
  hosts: [],
  isLoaded: false,

  componentToUserMap: function() {
    var map = {
      'NAMENODE': 'hdfs_user',
      'SECONDARY_NAMENODE': 'hdfs_user',
      'DATANODE': 'hdfs_user',
      'JOURNALNODE': 'hdfs_user',
      'TASKTRACKER': 'mapred_user',
      'JOBTRACKER': 'mapred_user',
      'HISTORYSERVER': 'mapred_user',
      'RESOURCEMANAGER': 'yarn_user',
      'NODEMANAGER': 'yarn_user',
      'ZOOKEEPER_SERVER': 'zk_user',
      'HIVE_SERVER': 'hive_user',
      'HIVE_METASTORE': 'hive_user',
      'OOZIE_SERVER': 'oozie_user',
      'HBASE_MASTER': 'hbase_user',
      'HBASE_REGIONSERVER': 'hbase_user',
      'SUPERVISOR': 'storm_user',
      'NIMBUS': 'storm_user',
      'STORM_UI_SERVER': 'storm_user',
      'FALCON_SERVER': 'falcon_user',
      'KNOX_GATEWAY': 'knox_user',
      'APP_TIMELINE_SERVER': 'yarn_user'
    };
    if (App.get('isHadoop22Stack')) {
      map['DRPC_SERVER'] = 'storm_user'
    }
    return map;
  }.property('App.isHadoop22Stack'),
  // The componentName, principal, and keytab have to coincide with the values in secure_properties.js
  componentToConfigMap: [
      {
        componentName: 'NAMENODE',
        principal: 'hadoop_http_principal_name',
        keytab: 'hadoop_http_keytab',
        displayName: Em.I18n.t('admin.addSecurity.hdfs.user.httpUser')
      },
      {
        componentName: 'SECONDARY_NAMENODE',
        principal: 'hadoop_http_principal_name',
        keytab: 'hadoop_http_keytab',
        displayName: Em.I18n.t('admin.addSecurity.hdfs.user.httpUser')
      },
      {
        componentName: 'JOURNALNODE',
        principal: 'hadoop_http_principal_name',
        keytab: 'hadoop_http_keytab',
        displayName: Em.I18n.t('admin.addSecurity.hdfs.user.httpUser')
      },
      {
        componentName: 'WEBHCAT_SERVER',
        principal: 'webHCat_http_principal_name',
        keytab: 'webhcat_http_keytab',
        displayName: Em.I18n.t('admin.addSecurity.webhcat.user.httpUser')
      },
      {
        componentName: 'HIVE_SERVER',
        principal: 'hive_metastore_http_principal_name',
        keytab: 'hive_metastore_http_keytab',
        displayName: Em.I18n.t('admin.addSecurity.hive.user.httpUser')
      },
      {
        componentName: 'OOZIE_SERVER',
        principal: 'oozie_http_principal_name',
        keytab: 'oozie_http_keytab',
        displayName: Em.I18n.t('admin.addSecurity.oozie.user.httpUser')
      },
      {
        componentName: 'FALCON_SERVER',
        principal: 'falcon_http_principal_name',
        keytab: 'falcon_http_keytab',
        displayName: Em.I18n.t('admin.addSecurity.falcon.user.httpUser')
      },
      {
        componentName: 'HISTORYSERVER',
        principal: 'jobhistory_http_principal_name',
        keytab: 'jobhistory_http_keytab',
        displayName: Em.I18n.t('admin.addSecurity.historyServer.user.httpUser')
      },
      {
        componentName: 'RESOURCEMANAGER',
        principal: 'resourcemanager_http_principal_name',
        keytab: 'resourcemanager_http_keytab',
        displayName: Em.I18n.t('admin.addSecurity.rm.user.httpUser')
      },
      {
        componentName: 'NODEMANAGER',
        principal: 'nodemanager_http_principal_name',
        keytab: 'nodemanager_http_keytab',
        displayName: Em.I18n.t('admin.addSecurity.nm.user.httpUser')
      },
      {
        componentName: 'APP_TIMELINE_SERVER',
        principal: 'apptimelineserver_http_principal_name',
        keytab: 'apptimelineserver_http_keytab',
        displayName: Em.I18n.t('admin.addSecurity.user.yarn.atsHTTPUser')
      },
      {
        componentName: 'STORM_UI_SERVER',
        principal: 'storm_ui_principal_name',
        keytab: 'storm_ui_keytab',
        displayName: Em.I18n.t('admin.addSecurity.storm.user.httpUser'),
        isHadoop22Stack: true
      }
  ],

  mandatoryConfigs: [
    {
      userConfig: 'smokeuser',
      keytab: 'smokeuser_keytab',
      displayName: Em.I18n.t('admin.addSecurity.user.smokeUser')
    },
    {
      userConfig: 'hdfs_user',
      keytab: 'hdfs_user_keytab',
      displayName: Em.I18n.t('admin.addSecurity.user.hdfsUser'),
      checkService: 'HDFS'
    },
    {
      userConfig: 'hbase_user',
      keytab: 'hbase_user_keytab',
      displayName: Em.I18n.t('admin.addSecurity.user.hbaseUser'),
      checkService: 'HBASE'
    }
  ],

  /**
   * download CSV file
   */
  doDownloadCsv: function () {
    if ($.browser.msie && $.browser.version < 10) {
      this.openInfoInNewTab();
    } else {
      try {
        var blob = new Blob([stringUtils.arrayToCSV(this.get('hostComponents'))], {type: "text/csv;charset=utf-8;"});
        saveAs(blob, "host-principal-keytab-list.csv");
      } catch (e) {
        this.openInfoInNewTab();
      }
    }
  },

  /**
   * open content of CSV file in new window
   */
  openInfoInNewTab: function () {
    var newWindow = window.open('');
    var newDocument = newWindow.document;
    newDocument.write(stringUtils.arrayToCSV(this.get('hostComponents')));
    newWindow.focus();
  },

  /**
   * load hosts from server
   */
  loadHosts: function () {
    App.ajax.send({
      name: 'hosts.security.wizard',
      sender: this,
      data: {},
      error: 'loadHostsErrorCallback',
      success: 'loadHostsSuccessCallback'
    })
  },

  loadHostsSuccessCallback: function (data, opt, params) {
    var hosts = [];

    data.items.forEach(function (item) {
      var hostComponents = [];

      item.host_components.forEach(function (hostComponent) {
        hostComponents.push(Em.Object.create({
          componentName: hostComponent.HostRoles.component_name,
          service: Em.Object.create({
            serviceName: hostComponent.HostRoles.service_name
          }),
          displayName: App.format.role(hostComponent.HostRoles.component_name, false)
        }));
      });
      hosts.push(Em.Object.create({
        hostName: item.Hosts.host_name,
        hostComponents: hostComponents
      }));
    });
    this.set('isLoaded', true);
    this.set('hosts', hosts);
    this.loadStep();
  },
  loadHostsErrorCallback: function () {
    this.set('isLoaded', true);
    this.set('hosts', []);
    this.loadStep();
  },

  /**
   * load step info
   */
  loadStep: function () {
    var hosts = this.get('hosts');
    var result = [];
    var securityUsers = this.getSecurityUsers();
    var hadoopGroupId = securityUsers.findProperty('name', 'user_group').value;
    var addedPrincipalsHost = {}; //Keys = host_principal, Value = 'true'

    hosts.forEach(function (host) {
      this.setMandatoryConfigs(result, securityUsers, host.get('hostName'), hadoopGroupId);
      this.setComponentsConfig(result, host, hadoopGroupId);
      this.setHostComponentsSecureValue(result, host, addedPrincipalsHost, securityUsers, hadoopGroupId);
    }, this);
    this.set('hostComponents', result);
  },

  /**
   * Returns host name for Nimbus component
   */
  getNimbusHostName: function () {
    var host = this.get('hosts').find(function (host) {
      return !!host.get('hostComponents').findProperty('componentName', 'NIMBUS');
    });
    if (host) {
      return host.get('hostName');
    }
  },

  /**
   * build map of connections between component and user
   * @param securityUsers
   */
  buildComponentToOwnerMap: function (securityUsers) {
    var componentToUserMap = this.get('componentToUserMap');
    var componentToOwnerMap = {};
    for (var component in componentToUserMap) {
      var user = componentToUserMap[component];
      var securityUser = securityUsers.findProperty('name', user);
      componentToOwnerMap[component] = securityUser.value;
    }
    return componentToOwnerMap;
  },

  /**
   * set security settings(principal and keytab) to component depending on whether host has such component
   * @param result
   * @param host
   * @param hadoopGroupId
   */
  setComponentsConfig: function (result, host, hadoopGroupId) {
    var hostComponents = host.get('hostComponents');

    var isATSInstalled = this.get('content.isATSInstalled');
    var doesATSSupportKerberos = App.get("doesATSSupportKerberos");

    this.get('componentToConfigMap').forEach(function (component) {
      //add specific components that supported only in Hadoop2 stack
      if (component.isHadoop22Stack && !App.get('isHadoop22Stack')) return;

      if (hostComponents.someProperty('componentName', component.componentName)) {

        if (component.componentName === "APP_TIMELINE_SERVER" && (!isATSInstalled || !doesATSSupportKerberos)) {
          return;
        }

        var configs = this.get('content.serviceConfigProperties');
        var serviceName = App.StackServiceComponent.find(component.componentName).get('serviceName');
        var serviceConfigs = configs.filterProperty('serviceName', serviceName);
        var servicePrincipal = serviceConfigs.findProperty('name', component.principal);
        var serviceKeytabPath = serviceConfigs.findProperty('name', component.keytab).value;
        result.push({
          host: host.get('hostName'),
          component: component.displayName,
          principal: this.getPrincipal(servicePrincipal, host.get('hostName')),
          keytabfile: stringUtils.getFileFromPath(serviceKeytabPath),
          keytab: stringUtils.getPath(serviceKeytabPath),
          owner: 'root',
          group: hadoopGroupId,
          acl: '440'
        });
      }
    }, this);
  },

  /**
   * set security settings(principal and keytab) to component
   * if checkService is passed then verify that service to his existence in order to set configs to such service
   * @param result
   * @param securityUsers
   * @param hostName
   * @param hadoopGroupId
   */
  setMandatoryConfigs: function (result, securityUsers, hostName, hadoopGroupId) {
    var generalConfigs = this.get('content.serviceConfigProperties').filterProperty('serviceName', 'GENERAL');
    var realm = generalConfigs.findProperty('name', 'kerberos_domain').value;
    var installedServices = App.Service.find().mapProperty('serviceName');

    this.get('mandatoryConfigs').forEach(function (config) {
      if (config.checkService && !installedServices.contains(config.checkService)) return;

      var userId = securityUsers.findProperty('name', config.userConfig).value;
      var userKeytabPath = generalConfigs.findProperty('name', config.keytab).value;
      result.push({
        host: hostName,
        component: config.displayName,
        principal: userId + '@' + realm,
        keytabFile: stringUtils.getFileFromPath(userKeytabPath),
        keytab: stringUtils.getPath(userKeytabPath),
        owner: userId,
        group: hadoopGroupId,
        acl: '440'
      });
    }, this);
  },

  /**
   * set secure properties(keytab and principal) for components, which should be displayed
   * @param result
   * @param host
   * @param addedPrincipalsHost
   * @param securityUsers
   * @param hadoopGroupId
   */
  setHostComponentsSecureValue: function (result, host, addedPrincipalsHost, securityUsers, hadoopGroupId) {
    var componentsToDisplay = ['NAMENODE', 'SECONDARY_NAMENODE', 'DATANODE', 'JOBTRACKER', 'ZOOKEEPER_SERVER', 'HIVE_SERVER', 'HIVE_METASTORE',
      'TASKTRACKER', 'OOZIE_SERVER', 'HBASE_MASTER', 'HBASE_REGIONSERVER', 'HISTORYSERVER', 'RESOURCEMANAGER', 'NODEMANAGER',
      'JOURNALNODE', 'SUPERVISOR', 'NIMBUS', 'STORM_UI_SERVER','FALCON_SERVER', 'KNOX_GATEWAY', 'APP_TIMELINE_SERVER'];
    if (App.get('isHadoop22Stack')) {
      componentsToDisplay.push('DRPC_SERVER');
    }
    var configs = this.get('content.serviceConfigProperties');
    var componentToOwnerMap = this.buildComponentToOwnerMap(securityUsers);
    var hostName = host.get('hostName');

    var isATSInstalled = this.get('content.isATSInstalled');
    var doesATSSupportKerberos = App.get("doesATSSupportKerberos");

    host.get('hostComponents').forEach(function (hostComponent) {
      if (componentsToDisplay.contains(hostComponent.get('componentName'))) {
        var serviceConfigs = configs.filterProperty('serviceName', hostComponent.get('service.serviceName'));
        var targetHost = hostName;
        if (App.get('isHadoop22Stack') && hostComponent.get('componentName') === 'DRPC_SERVER') {
          targetHost = this.getNimbusHostName()
        }
        var secureProperties = this.getSecureProperties(serviceConfigs, hostComponent.get('componentName'), targetHost);
        var displayName = this.changeDisplayName(hostComponent.get('displayName'));
        var key = hostName + "--" + secureProperties.principal;

        if (hostComponent.get('componentName') === "APP_TIMELINE_SERVER" && (!isATSInstalled || !doesATSSupportKerberos)) {
          return;
        }

        if (Em.isNone(addedPrincipalsHost[key])) {
          var owner = componentToOwnerMap[hostComponent.get('componentName')] || '';

          result.push({
            host: hostName,
            component: displayName,
            principal: secureProperties.principal,
            keytabFile: stringUtils.getFileFromPath(secureProperties.keytab),
            keytab: stringUtils.getPath(secureProperties.keytab),
            owner: owner,
            group: hadoopGroupId,
            acl: '400'
          });
          addedPrincipalsHost[key] = true;
        }
      }
    }, this);
  },

  /**
   * get properties (keytab and principle) of secure config that match component
   * @param serviceConfigs
   * @param componentName
   * @param hostName
   * @return {Object}
   */
  getSecureProperties: function (serviceConfigs, componentName, hostName) {
    var secureProperties = {};
    serviceConfigs.forEach(function (config) {
      if ((config.component && config.component === componentName) ||
        (config.components && config.components.contains(componentName))) {
        if (config.name.endsWith('_principal_name')) {
          secureProperties.principal = this.getPrincipal(config, hostName);
        } else if (config.name.endsWith('_keytab') || config.name.endsWith('_keytab_path')) {
          secureProperties.keytab = config.value;
        }
      }
    }, this);
    return secureProperties;
  },

  /**
   * get formatted principal value
   * @param config
   * @param hostName
   * @return {String}
   */
  getPrincipal: function (config, hostName) {
    return config.value.replace('_HOST', hostName.toLowerCase()) + config.unit;
  },

  /**
   * get users from security configs
   * @return {Array}
   */
  getSecurityUsers: function () {
    return App.db.getSecureUserInfo();
  },

  /**
   * format  display names of specific components
   * @param name
   * @return {*}
   */
  changeDisplayName: function (name) {
    if (name === 'HiveServer2' || name === 'Hive Metastore') {
      return 'Hive Metastore and HiveServer2';
    } else {
      return name;
    }
  }
});
