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

App.QuickViewLinks = Em.View.extend({

  isLoaded: false,

  areQuickLinksUndefined: false,

  loadTags: function () {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'loadTagsSuccess',
      error: 'loadTagsError'
    });
  },

  loadTagsSuccess: function (data) {
    this.get('actualTags').clear();
    var tags = [];
    var self = this;
    for (var prop in data.Clusters.desired_configs) {
      tags.push(Em.Object.create({
        siteName: prop,
        tagName: data.Clusters.desired_configs[prop]['tag']
      }));
    }
    this.get('actualTags').pushObjects(tags);
    this.setConfigProperties().done(function (data) {
      self.get('configProperties').pushObjects(data);
      self.getQuickLinksHosts();
    });
  },

  loadTagsError: function() {
    this.getQuickLinksHosts();
  },

  getQuickLinksHosts: function () {
    var masterHosts = App.HostComponent.find().filterProperty('isMaster').mapProperty('hostName').uniq();

    App.ajax.send({
      name: 'hosts.for_quick_links',
      sender: this,
      data: {
        clusterName: App.get('clusterName'),
        masterHosts: masterHosts.join(','),
        urlParams: ',host_components/metrics/hbase/master/IsActiveMaster'
      },
      success: 'setQuickLinksSuccessCallback',
      error: 'setQuickLinksErrorCallback'
    });
  },

  actualTags: [],

  configProperties: [],

  /**
   * list of files that contains properties for enabling/disabling ssl
   */
  requiredSiteNames: [
    'ams-grafana-ini',
    'hadoop-env',
    'yarn-env',
    'hbase-env',
    'oozie-env',
    'mapred-env',
    'storm-env',
    'falcon-env',
    'core-site',
    'hdfs-site',
    'hbase-site',
    'oozie-site',
    'yarn-site',
    'mapred-site',
    'storm-site',
    'spark-defaults',
    'accumulo-site',
    'application-properties',
    'ranger-admin-site',
    'ranger-site',
    'admin-properties'
  ],
  /**
   * Get public host name by its host name.
   *
   * @method getPublicHostName
   * @param {Object[]} hosts - list of hosts from response
   * @param {String} hostName
   * @return {String}
   **/
  getPublicHostName: function(hosts, hostName) {
    return Em.get(hosts.findProperty('Hosts.host_name', hostName), 'Hosts.public_host_name');
  },

  setConfigProperties: function () {
    this.get('configProperties').clear();
    var requiredSiteNames = this.get('requiredSiteNames');
    var tags = this.get('actualTags').filter(function (tag) {
      return requiredSiteNames.contains(tag.siteName);
    });
    return App.router.get('configurationController').getConfigsByTags(tags);
  },

  ambariProperties: function () {
    return App.router.get('clusterController.ambariProperties');
  },
  /**
   * Updated quick links. Here we put correct hostname to url
   */
  quickLinks: [],

  didInsertElement: function () {
    this.setQuickLinks();
  },

  willDestroyElement: function() {
    this.get('configProperties').clear();
    this.get('actualTags').clear();
    this.get('quickLinks').clear();
  },

  findComponentHost: function (components, componentName) {
    var component = components.find(function (item) {
      return item.host_components.someProperty('HostRoles.component_name', componentName);
    });
    return component && component.Hosts.public_host_name;
  },

  /**
   * The flags responsible for data to build quick links:
   * - App.router.clusterController.isServiceMetricsLoaded
   *
   * The flags responsible for correct, up-to-date state of quick links:
   * - App.currentStackVersionNumber
   * - App.singleNodeInstall
   * - App.router.clusterController.isHostComponentMetricsLoaded
   */
  setQuickLinks: function () {
    if (App.get('router.clusterController.isServiceMetricsLoaded')) {
      this.loadTags();
    }
  }.observes(
    'App.currentStackVersionNumber',
    'App.singleNodeInstall',
    'App.router.clusterController.isServiceMetricsLoaded',
    'App.router.clusterController.isHostComponentMetricsLoaded'
  ),

  setQuickLinksSuccessCallback: function (response) {
    var self = this;
    var quickLinks = [];
    var hosts = this.setHost(response, this.get('content.serviceName'));
    if (hosts.length === 0 || !this.get('content.quickLinks')) {
      this.setQuickLinksErrorCallback();
    } else if (hosts.length == 1) {

      quickLinks = this.get('content.quickLinks').map(function (item) {
        var protocol = self.setProtocol(item.get('service_id'), self.get('configProperties'), self.ambariProperties(), item);
        var siteConfigs = {};

        if (item.get('template')) {
          var port = item.get('http_config') && self.setPort(item, protocol);
          if (['FALCON', 'OOZIE', 'ATLAS'].contains(item.get('service_id'))) {
            item.set('url', item.get('template').fmt(protocol, hosts[0], port, App.router.get('loginName')));
          } else if (item.get('service_id') === 'MAPREDUCE2') {
            siteConfigs = self.get('configProperties').findProperty('type', item.get('site')).properties;
            var hostPortConfigValue = siteConfigs[item.get(protocol + '_config')];
            if (hostPortConfigValue != null) {
              var hostPortValue = hostPortConfigValue.match(new RegExp("([\\w\\d.-]*):(\\d+)"));
              var hostObj = response.items.findProperty('Hosts.host_name', hostPortValue[1]);
              if (hostObj != null) {
                var publicHostValue = hostObj.Hosts.public_host_name;
                hostPortConfigValue = "%@:%@".fmt(publicHostValue, hostPortValue[2])
              }
            }
            item.set('url', item.get('template').fmt(protocol, hostPortConfigValue));
          } else if (item.get('service_id') === 'RANGER') {
            siteConfigs = self.get('configProperties').findProperty('type', 'admin-properties').properties;
            if (siteConfigs['policymgr_external_url']) {
              // external_url example: "http://c6404.ambari.apache.org:6080"
              var hostAndPort = siteConfigs['policymgr_external_url'] && siteConfigs['policymgr_external_url'].split('://')[1];
              item.set('url', protocol + '://' + hostAndPort);
            } else {
              item.set('url', item.get('template').fmt(protocol, hosts[0], port));
            }
          } else {
            item.set('url', item.get('template').fmt(protocol, hosts[0], port));
          }
        }
        return item;
      });
      this.setProperties({
        quickLinks: quickLinks,
        isLoaded: true,
        areQuickLinksUndefined: false
      });
    } else {
      // multiple hbase masters or HDFS HA enabled
      var quickLinksArray = [];
      hosts.forEach(function(host) {
        var quickLinks = [];
        self.get('content.quickLinks').forEach(function (item) {
          var newItem = {};
          var protocol = self.setProtocol(item.get('service_id'), self.get('configProperties'), self.ambariProperties(), item);
          if (item.get('template')) {
            var port;
            var hostNameRegExp = new RegExp('([\\w\\W]*):\\d+');
            if (item.get('service_id') === 'HDFS') {
              var config;
              var configPropertiesObject = self.get('configProperties').findProperty('type', item.get('site'));
              if (configPropertiesObject && configPropertiesObject.properties) {
                var properties = configPropertiesObject.properties;
                var nameServiceId = properties['dfs.nameservices'];
                var nnProperties = ['dfs.namenode.{0}-address.{1}.nn1', 'dfs.namenode.{0}-address.{1}.nn2'].map(function (c) {
                  return c.format(protocol, nameServiceId);
                });
                var nnPropertiesLength = nnProperties.length;
                for (var i = nnPropertiesLength; i--;) {
                  var propertyName = nnProperties[i];
                  var hostNameMatch = properties[propertyName] && properties[propertyName].match(hostNameRegExp);
                  if (hostNameMatch && hostNameMatch[1] === host.publicHostName) {
                    config = propertyName;
                    break;
                  }
                }
              }
              port = self.setPort(item, protocol, config);
            } else {
              port = item.get('http_config') && self.setPort(item, protocol);
            }
            if (item.get('service_id')==='OOZIE') {
              newItem.url = item.get('template').fmt(protocol, host.publicHostName, port, App.router.get('loginName'));
            } else if (item.get('service_id')==='MAPREDUCE2') {
              var hostPortConfigValue = "%@:%@".fmt(host.publicHostName, port);
              newItem.url = item.get('template').fmt(protocol, hostPortConfigValue);
            } else {
              newItem.url = item.get('template').fmt(protocol, host.publicHostName, port);
            }
            newItem.label = item.get('label');
          }
          quickLinks.push(newItem);
        });
        if (host.status) {
          quickLinks.set('publicHostNameLabel', Em.I18n.t('quick.links.publicHostName').format(host.publicHostName, host.status));
        } else {
          quickLinks.set('publicHostNameLabel', host.publicHostName);
        }
        quickLinksArray.push(quickLinks);
      }, this);
      this.setProperties({
        quickLinksArray: quickLinksArray,
        isLoaded: true,
        areQuickLinksUndefined: false
      });
    }
  },

  setQuickLinksErrorCallback: function () {
    this.setProperties({
      quickLinks: [{
        label: this.t('quick.links.error.label'),
        url: 'javascript:alert("' + this.t('contact.administrator') + '");'
      }],
      isLoaded: true,
      areQuickLinksUndefined: true
    });
  },

  /**
   * sets public host names for required masters of current service
   * @param {String} serviceName - selected serviceName
   * @param {JSON} response
   * @returns {Array} containing hostName(s)
   * @method setHost
   */
  setHost: function(response, serviceName) {
    if (App.get('singleNodeInstall')) {
      return [App.get('singleNodeAlias')];
    }
    var hosts = [];
    var components;
    switch (serviceName) {
      case 'OOZIE':
        // active OOZIE components
        components = this.get('content.hostComponents').filterProperty('componentName','OOZIE_SERVER').filterProperty('workStatus', 'STARTED');
        if (components && components.length > 1) {
          components.forEach(function (component) {
            hosts.push({
              'publicHostName': response.items.findProperty('Hosts.host_name', component.get('hostName')).Hosts.public_host_name,
              'status': Em.I18n.t('quick.links.label.active')
            });
          });
        } else if (components && components.length === 1) {
          hosts[0] = this.findComponentHost(response.items, 'OOZIE_SERVER');
        }
        break;
      case "HDFS":
        if (this.get('content.snameNode')) {
          // not HA
          hosts[0] = this.findComponentHost(response.items, 'NAMENODE');
        } else {
          // HA enabled, need both two namenodes hosts
          this.get('content.hostComponents').filterProperty('componentName', 'NAMENODE').forEach(function (component) {
            hosts.push({'publicHostName': response.items.findProperty('Hosts.host_name', component.get('hostName')).Hosts.public_host_name});
          });
          // assign each namenode status label
          if (this.get('content.activeNameNode')) {
            hosts.findProperty('publicHostName', this.getPublicHostName(response.items, this.get('content.activeNameNode.hostName'))).status = Em.I18n.t('quick.links.label.active');
          }
          if (this.get('content.standbyNameNode')) {
            hosts.findProperty('publicHostName', this.getPublicHostName(response.items, this.get('content.standbyNameNode.hostName'))).status = Em.I18n.t('quick.links.label.standby');
          }
          if (this.get('content.standbyNameNode2')) {
            hosts.findProperty('publicHostName', this.getPublicHostName(response.items, this.get('content.standbyNameNode2.hostName'))).status = Em.I18n.t('quick.links.label.standby');
          }
        }
        break;
      case "HBASE":
        var masterComponents = response.items.filter(function (item) {
          return item.host_components.someProperty('HostRoles.component_name', 'HBASE_MASTER');
        });
        var activeMaster, standbyMasters, otherMasters;
        activeMaster = masterComponents.filter(function (item) {
          return item.host_components.someProperty('metrics.hbase.master.IsActiveMaster', 'true');
        });
        standbyMasters = masterComponents.filter(function (item) {
          return item.host_components.someProperty('metrics.hbase.master.IsActiveMaster', 'false');
        });
        otherMasters = masterComponents.filter(function (item) {
          return !(item.host_components.someProperty('metrics.hbase.master.IsActiveMaster', 'true') || item.host_components.someProperty('metrics.hbase.master.IsActiveMaster', 'false'));
        });
        if (masterComponents.length > 1) {
          // need all hbase_masters hosts in quick links
          if (activeMaster) {
            activeMaster.forEach(function (item) {
              hosts.push({'publicHostName': item.Hosts.public_host_name, 'status': Em.I18n.t('quick.links.label.active')});
            });
          }
          if (standbyMasters) {
            standbyMasters.forEach(function (item) {
              hosts.push({'publicHostName': item.Hosts.public_host_name, 'status': Em.I18n.t('quick.links.label.standby')});
            });
          }
          if (otherMasters) {
            otherMasters.forEach(function (item) {
              hosts.push({'publicHostName': item.Hosts.public_host_name});
            });
          }
        } else {
          hosts[0] = masterComponents[0].Hosts.public_host_name;
        }
        break;
      case "YARN":
        if (App.get('isRMHaEnabled')) {
          this.get('content.hostComponents').filterProperty('componentName', 'RESOURCEMANAGER').forEach(function (component) {
            var newHost = {'publicHostName': response.items.findProperty('Hosts.host_name', component.get('hostName')).Hosts.public_host_name};
            var status = '';
            switch (component.get('haStatus')) {
              case 'ACTIVE':
                status = Em.I18n.t('quick.links.label.active');
                break;
              case 'STANDBY':
                status = Em.I18n.t('quick.links.label.standby');
                break;
            }
            if (status) {
              newHost.status = status;
            }
            hosts.push(newHost);
          }, this);
        } else {
          hosts[0] = this.findComponentHost(response.items, 'RESOURCEMANAGER');
        }
        break;
      case "STORM":
        hosts[0] = this.findComponentHost(response.items, "STORM_UI_SERVER");
        break;
      case "ACCUMULO":
        hosts[0] = this.findComponentHost(response.items, "ACCUMULO_MONITOR");
        break;
      case "ATLAS":
        hosts[0] = this.findComponentHost(response.items, "ATLAS_SERVER");
        break;
      case "AMBARI_METRICS":
        hosts[0] = this.findComponentHost(response.items, "METRICS_GRAFANA");
        break;
      case "MAPREDUCE2":
        components = this.get('content.hostComponents').filterProperty('componentName', 'HISTORYSERVER');
        if (components && components.length > 1) {
          components.forEach(function (component) {
            hosts.push({
              'publicHostName': response.items.findProperty('Hosts.host_name', component.get('hostName')).Hosts.public_host_name
            });
          });
        } else if (components && components.length === 1) {
          hosts[0] = this.findComponentHost(response.items, 'HISTORYSERVER');
        }
        break;
      default:
        var service = App.StackService.find().findProperty('serviceName', serviceName);
        if (service && service.get('hasMaster')) {
          hosts[0] = this.findComponentHost(response.items, this.get('content.hostComponents') && this.get('content.hostComponents').findProperty('isMaster', true).get('componentName'));
        }
        break;
    }
    return hosts;
  },

  /**
   * services that supports security. this array is used to find out protocol.
   * becides GANGLIA, YARN, MAPREDUCE2, ACCUMULO. These services use
   * their properties to know protocol
   */
  servicesSupportsHttps: ["HDFS", "HBASE"],

  /**
   * setProtocol - if cluster is secure for some services (GANGLIA, MAPREDUCE2, YARN and servicesSupportsHttps)
   * protocol becomes "https" otherwise "http" (by default)
   * @param {String} service_id - service name
   * @param {Object} configProperties
   * @param {Object} ambariProperties
   * @returns {string} "https" or "http" only!
   * @method setProtocol
   */
  setProtocol: function (service_id, configProperties, ambariProperties, item) {
    var hadoopSslEnabled = false;
    if (configProperties && configProperties.length > 0) {
      var hdfsSite = configProperties.findProperty('type', 'hdfs-site');
      hadoopSslEnabled = (hdfsSite && Em.get(hdfsSite, 'properties') && hdfsSite.properties['dfs.http.policy'] === 'HTTPS_ONLY');
    }
    switch (service_id) {
      case "GANGLIA":
        return (ambariProperties && ambariProperties['ganglia.https'] == "true") ? "https" : "http";
      case "AMBARI_METRICS":
        var grafanaProperties = configProperties && configProperties.findProperty('type', 'ams-grafana-ini');
        return grafanaProperties.properties['protocol'] ? grafanaProperties.properties['protocol'] : 'http';
      case "YARN":
        var yarnProperties = configProperties && configProperties.findProperty('type', 'yarn-site');
        if (yarnProperties && yarnProperties.properties) {
          if (yarnProperties.properties['yarn.http.policy'] === 'HTTPS_ONLY') {
            return "https";
          } else if (yarnProperties.properties['yarn.http.policy'] === 'HTTP_ONLY') {
            return "http";
          }
        }
        return hadoopSslEnabled ? "https" : "http";
      case "MAPREDUCE2":
        var mapred2Properties = configProperties && configProperties.findProperty('type', 'mapred-site');
        if (mapred2Properties && mapred2Properties.properties) {
          if (mapred2Properties.properties['mapreduce.jobhistory.http.policy'] === 'HTTPS_ONLY') {
            return "https";
          } else if (mapred2Properties.properties['mapreduce.jobhistory.http.policy'] === 'HTTP_ONLY') {
            return "http";
          }
        }
        return hadoopSslEnabled ? "https" : "http";
      case "ACCUMULO":
        var accumuloProperties = configProperties && configProperties.findProperty('type', 'accumulo-site');
        if (accumuloProperties && accumuloProperties.properties) {
          if (accumuloProperties.properties['monitor.ssl.keyStore'] && accumuloProperties.properties['monitor.ssl.trustStore']) {
            return "https";
          } else {
            return "http";
          }
        }
        return "http";
      case "ATLAS":
        var atlasProperties = configProperties && configProperties.findProperty('type', 'application-properties');
        if (atlasProperties && atlasProperties.properties) {
          if (atlasProperties.properties['metadata.enableTLS'] == "true") {
            return "https";
          } else {
            return "http";
          }
        }
        return "http";
      case "OOZIE":
        var site = configProperties.findProperty('type', 'oozie-site');
        var properties = site && site.properties;
        var url = properties && properties['oozie.base.url'];
        var re = new RegExp(item.get('regex'));
        var portValue = url && url.match(re);
        var port = portValue && portValue.length && portValue[1];
        var protocol = 'http';
        var isHttpsPropertiesEnabled = properties && (properties['oozie.https.port'] ||
                                                      properties['oozie.https.keystore.file'] ||
                                                      properties['oozie.https.keystore.pass']);
        if (port === '11443' || isHttpsPropertiesEnabled) {
          protocol = 'https';
        }
        return protocol;
      case "RANGER":
        var rangerProperties = configProperties && configProperties.findProperty('type', 'ranger-admin-site');
        var rangerSiteProperties = configProperties && configProperties.findProperty('type', 'ranger-site');
        if (rangerProperties && rangerProperties.properties &&
          rangerProperties.properties['ranger.service.https.attrib.ssl.enabled'] == "true" &&
          rangerProperties.properties['ranger.service.http.enabled'] == "false") {
          //HDP2.3
          return "https";
        } else if (rangerProperties && rangerProperties.properties &&
          rangerProperties.properties['ranger.service.https.attrib.ssl.enabled'] == "false" &&
          rangerProperties.properties['ranger.service.http.enabled'] == "true") {
          //HDP2.3
          return "http";
        } else if (rangerSiteProperties && rangerSiteProperties.properties && rangerSiteProperties.properties['http.enabled'] == "false") {
          //HDP2.2
          return "https";
        } else {
          return "http";
        }
      default:
        return this.get('servicesSupportsHttps').contains(service_id) && hadoopSslEnabled ? "https" : "http";
    }
  },

  /**
   * sets the port of quick link
   * @param item
   * @param protocol
   * @param config
   * @returns {*}
   * @method setPort
   */
  setPort: function (item, protocol, config) {
    var configProperties = this.get('configProperties');
    var configProp = config || item.get('http_config');
    var defaultPort = item.get('default_http_port');
    if (protocol === 'https' && (config || item.get('https_config'))) {
      configProp = config || item.get('https_config');
      if (item.get('default_https_port')) {
        defaultPort = item.get('default_https_port');
      }
    }
    var site = configProperties.findProperty('type', item.get('site'));
    var propertyValue = site && site.properties && site.properties[configProp];
    if (!propertyValue) {
      if (item.get('service_id') == 'RANGER') {
        // HDP 2.3
        var adminSite = configProperties.findProperty('type', 'ranger-admin-site');
        if (protocol === 'https') {
          propertyValue = adminSite && adminSite.properties && adminSite.properties['ranger.service.https.port'];
        } else {
          propertyValue = adminSite && adminSite.properties && adminSite.properties['ranger.service.http.port'];
        }
      }
    }

    if (!propertyValue) {
      return defaultPort;
    }

    var re = new RegExp(item.get('regex'));
    var portValue = propertyValue.match(re);

    return portValue[1];
  },

  linkTarget: function () {
    if (this.get('isLoaded') && this.get('areQuickLinksUndefined')) {
      return '';
    }
    switch (this.get('content.serviceName').toLowerCase()) {
      case "hdfs":
      case "yarn":
      case "mapreduce2":
      case "hbase":
      case "oozie":
      case "ganglia":
      case "storm":
      case "spark":
      case "falcon":
      case "accumulo":
      case "atlas":
      case "ranger":
      case "ambari_metrics":
        return "_blank";
        break;
      default:
        return "";
        break;
    }
  }.property('service', 'isLoaded')

});
