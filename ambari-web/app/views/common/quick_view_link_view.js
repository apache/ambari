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

  /**
   * service which has blank target of link
   * @type {Array}
   */
  servicesHasBlankTarget: [
    'HDFS',
    'YARN',
    'MAPREDUCE2',
    'HBASE',
    'OOZIE',
    'STORM',
    'SPARK',
    'FALCON',
    'ACCUMULO',
    'ATLAS',
    'RANGER'
  ],

  /**
   * Updated quick links. Here we put correct hostname to url
   * @type {Array}
   */
  quickLinks: [],

  actualTags: [],

  configProperties: [],

  /**
   * list of files that contains properties for enabling/disabling ssl
   */
  requiredSiteNames: ['hadoop-env', 'yarn-env', 'hbase-env', 'oozie-env', 'mapred-env', 'storm-env', 'falcon-env', 'core-site', 'hdfs-site', 'hbase-site', 'oozie-site', 'yarn-site', 'mapred-site', 'storm-site', 'spark-defaults', 'accumulo-site', 'application-properties', 'ranger-admin-site', 'ranger-site', 'admin-properties'],

  /**
   * @type {string}
   */
  linkTarget: function () {
    if (this.get('servicesHasBlankTarget').contains(this.get('content.serviceName'))) {
      return "_blank";
    }
    return "";
  }.property('content.serviceName'),

  /**
   * @type {object}
   */
  ambariProperties: function () {
    return App.router.get('clusterController.ambariProperties');
  }.property().volatile(),

  didInsertElement: function () {
    this.setQuickLinks();
  },

  willDestroyElement: function () {
    this.get('configProperties').clear();
    this.get('actualTags').clear();
    this.get('quickLinks').clear();
  },

  setQuickLinks: function () {
    if (App.get('router.clusterController.isServiceMetricsLoaded')) {
      this.loadTags();
    }
  }.observes('App.currentStackVersionNumber', 'App.singleNodeInstall', 'App.router.clusterController.isServiceMetricsLoaded'),

  /**
   * call for configuration tags
   * @returns {$.ajax}
   */
  loadTags: function () {
    return App.ajax.send({
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

  loadTagsError: function () {
    this.getQuickLinksHosts();
  },

  /**
   * call for public host names
   * @returns {$.ajax}
   */
  getQuickLinksHosts: function () {
    var masterHosts = App.HostComponent.find().filterProperty('isMaster').mapProperty('hostName').uniq();

    return App.ajax.send({
      name: 'hosts.for_quick_links',
      sender: this,
      data: {
        clusterName: App.get('clusterName'),
        masterHosts: masterHosts.join(','),
        urlParams: (this.get('content.serviceName') === 'HBASE') ? ',host_components/metrics/hbase/master/IsActiveMaster' : ''
      },
      success: 'setQuickLinksSuccessCallback'
    });
  },

  setQuickLinksSuccessCallback: function (response) {
    var hosts = this.getHosts(response, this.get('content.serviceName'));
    if (hosts.length === 0 || Em.isNone(this.get('content.quickLinks'))) {
      this.setEmptyLinks();
    } else if (hosts.length === 1) {
      this.setSingleHostLinks(hosts);
    } else {
      this.setMultipleHostLinks(hosts);
    }
  },

  /**
   * Get public host name by its host name.
   *
   * @method getPublicHostName
   * @param {Object[]} hosts - list of hosts from response
   * @param {string} hostName
   * @return {string|null}
   **/
  getPublicHostName: function (hosts, hostName) {
    var host = hosts.findProperty('Hosts.host_name', hostName);
    if (host) {
      return Em.get(host, 'Hosts.public_host_name');
    }
    return null;
  },

  setConfigProperties: function () {
    this.get('configProperties').clear();
    var requiredSiteNames = this.get('requiredSiteNames');
    var tags = this.get('actualTags').filter(function (tag) {
      return requiredSiteNames.contains(tag.siteName);
    });
    return App.router.get('configurationController').getConfigsByTags(tags);
  },

  /**
   * set empty links
   */
  setEmptyLinks: function () {
    var quickLinks = [{
      label: this.t('quick.links.error.label'),
      url: 'javascript:alert("' + this.t('contact.administrator') + '");return false;'
    }];
    this.set('quickLinks', quickLinks);
    this.set('isLoaded', true);
  },

  /**
   * set links that contain only one host
   * @param {Array} hosts
   */
  setSingleHostLinks: function (hosts) {
    var quickLinks = this.get('content.quickLinks').map(function (item) {
      var protocol = this.setProtocol(item.get('serviceName'), this.get('configProperties'), this.get('ambariProperties'), item);
      var publicHostName = hosts[0].publicHostName;
      var port = item.get('http_config') && this.setPort(item, protocol);
      var siteConfigs = {};

      if (item.get('template')) {
        if (item.get('serviceName') === 'MAPREDUCE2') {
          siteConfigs = this.get('configProperties').findProperty('type', item.get('site')).properties;
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
        } else if (item.get('serviceName') === 'RANGER') {
          siteConfigs = this.get('configProperties').findProperty('type', 'admin-properties').properties;
          if (siteConfigs['policymgr_external_url']) {
            // external_url example: "http://c6404.ambari.apache.org:6080"
            var hostAndPort = siteConfigs['policymgr_external_url'].split('://')[1];
            item.set('url', protocol + '://' + hostAndPort);
          } else {
            item.set('url', item.get('template').fmt(protocol, publicHostName, port));
          }
        } else {
          item.set('url', item.get('template').fmt(protocol, publicHostName, port, App.router.get('loginName')));
        }
      }
      return item;
    }, this);
    this.set('quickLinks', quickLinks);
    this.set('isLoaded', true);
  },

  /**
   * set links that contain multiple hosts
   * @param {Array} hosts
   */
  setMultipleHostLinks: function (hosts) {
    var quickLinksArray = [];
    hosts.forEach(function (host) {
      var quickLinks = [];
      this.get('content.quickLinks').forEach(function (item) {
        var newItem = {};
        var protocol = this.setProtocol(item.get('serviceName'), this.get('configProperties'), this.get('ambariProperties'), item);
        if (item.get('template')) {
          var port;
          var hostNameRegExp = new RegExp('([\\w\\W]*):\\d+');
          if (item.get('serviceName') === 'HDFS') {
            var config;
            var configPropertiesObject = this.get('configProperties').findProperty('type', item.get('site'));
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
            port = this.setPort(item, protocol, config);
          } else {
            port = item.get('http_config') && this.setPort(item, protocol);
          }
          if (item.get('serviceName') === 'OOZIE') {
            newItem.url = item.get('template').fmt(protocol, host.publicHostName, port, App.router.get('loginName'));
          } else {
            newItem.url = item.get('template').fmt(protocol, host.publicHostName, port);
          }
          newItem.label = item.get('label');
        }
        quickLinks.push(newItem);
      }, this);
      if (host.status) {
        quickLinks.set('publicHostNameLabel', Em.I18n.t('quick.links.publicHostName').format(host.publicHostName, host.status));
      } else {
        quickLinks.set('publicHostNameLabel', host.publicHostName);
      }
      quickLinksArray.push(quickLinks);
    }, this);
    this.set('quickLinksArray', quickLinksArray);
    this.set('isLoaded', true);
  },

  /**
   * set status to hosts with OOZIE_SERVER
   * @param {Array} hosts
   * @returns {Array}
   */
  processOozieHosts: function (hosts) {
    var activeOozieServers = this.get('content.hostComponents')
      .filterProperty('componentName', 'OOZIE_SERVER')
      .filterProperty('workStatus', 'STARTED')
      .mapProperty('hostName');

    return hosts.filter(function (host) {
      host.status = Em.I18n.t('quick.links.label.active');
      return activeOozieServers.contains(host.hostName);
    }, this);
  },

  /**
   * set status to hosts with NAMENODE
   * @param {Array} hosts
   * @returns {Array}
   */
  processHdfsHosts: function (hosts) {
    return hosts.map(function (host) {
      if (host.hostName === Em.get(this, 'content.activeNameNode.hostName')) {
        host.status = Em.I18n.t('quick.links.label.active');
      } else if (host.hostName === Em.get(this, 'content.standbyNameNode.hostName')) {
        host.status = Em.I18n.t('quick.links.label.standby');
      } else if (host.hostName === Em.get(this, 'content.standbyNameNode2.hostName')) {
        host.status = Em.I18n.t('quick.links.label.standby');
      }
      return host;
    }, this);
  },

  /**
   * set status to hosts with HBASE_MASTER
   * @param {Array} hosts
   * @param {object} response
   * @returns {Array}
   */
  processHbaseHosts: function (hosts, response) {
    return hosts.map(function (host) {
      var isActiveMaster;
      response.items.filterProperty('Hosts.host_name', host.hostName).filter(function (item) {
        var hbaseMaster = item.host_components.findProperty('HostRoles.component_name', 'HBASE_MASTER');
        isActiveMaster = hbaseMaster && Em.get(hbaseMaster, 'metrics.hbase.master.IsActiveMaster');
      });
      if (isActiveMaster === 'true') {
        host.status = Em.I18n.t('quick.links.label.active');
      } else if (isActiveMaster === 'false') {
        host.status = Em.I18n.t('quick.links.label.standby');
      }
      return host;
    }, this);
  },

  /**
   * set status to hosts with RESOURCEMANAGER
   * @param {Array} hosts
   * @returns {Array}
   */
  processYarnHosts: function (hosts) {
    return hosts.map(function (host) {
      var resourceManager = this.get('content.hostComponents')
        .filterProperty('componentName', 'RESOURCEMANAGER')
        .findProperty('hostName', host.hostName);
      var haStatus = resourceManager && resourceManager.get('haStatus');
      if (haStatus === 'ACTIVE') {
        host.status = Em.I18n.t('quick.links.label.active');
      } else if (haStatus === 'STANDBY') {
        host.status = Em.I18n.t('quick.links.label.standby');
      }
      return host;
    }, this);
  },

  /**
   * sets public host names for required masters of current service
   * @param {string} serviceName - selected serviceName
   * @param {JSON} response
   * @returns {Array} containing hostName(s)
   * @method getHosts
   */
  getHosts: function (response, serviceName) {
    if (App.get('singleNodeInstall')) {
      return [{
        hostName: App.get('singleNodeAlias'),
        publicHostName: App.get('singleNodeAlias')
      }];
    }
    var hosts = [];
    switch (serviceName) {
      case 'OOZIE':
        hosts = this.processOozieHosts(this.findHosts('OOZIE_SERVER', response));
        break;
      case "HDFS":
        hosts = this.processHdfsHosts(this.findHosts('NAMENODE', response));
        break;
      case "HBASE":
        hosts = this.processHbaseHosts(this.findHosts('HBASE_MASTER', response), response);
        break;
      case "YARN":
        hosts = this.processYarnHosts(this.findHosts('RESOURCEMANAGER', response));
        break;
      case "STORM":
        hosts = this.findHosts('STORM_UI_SERVER', response);
        break;
      case "ACCUMULO":
        hosts = this.findHosts('ACCUMULO_MONITOR', response);
        break;
      case "ATLAS":
        hosts = this.findHosts('ATLAS_SERVER', response);
        break;
      default:
        if (this.getWithDefault('content.hostComponents', []).someProperty('isMaster')) {
          hosts = this.findHosts(this.get('content.hostComponents').findProperty('isMaster').get('componentName'), response);
        }
        break;
    }
    return hosts;
  },

  /**
   * find host public names
   * @param {string} componentName
   * @param {object} response
   * @returns {Array}
   */
  findHosts: function (componentName, response) {
    var hosts = [];
    this.get('content.hostComponents')
      .filterProperty('componentName', componentName)
      .forEach(function (component) {
        var host = this.getPublicHostName(response.items, component.get('hostName'));
        if (host) {
          hosts.push({
            hostName: component.get('hostName'),
            publicHostName: host
          });
        }
      }, this);
    return hosts;
  },

  /**
   * services that supports security. this array is used to find out protocol.
   * besides GANGLIA, YARN, MAPREDUCE2, ACCUMULO. These services use
   * their properties to know protocol
   */
  servicesSupportsHttps: ["HDFS", "HBASE"],

  /**
   * setProtocol - if cluster is secure for some services (GANGLIA, MAPREDUCE2, YARN and servicesSupportsHttps)
   * protocol becomes "https" otherwise "http" (by default)
   * @param {String} serviceName - service name
   * @param {Object} configProperties
   * @param {Object} ambariProperties
   * @returns {string} "https" or "http" only!
   * @method setProtocol
   * @param item
   */
  setProtocol: function (serviceName, configProperties, ambariProperties, item) {
    var hadoopSslEnabled = false;
    if (configProperties && configProperties.length > 0) {
      var hdfsSite = configProperties.findProperty('type', 'hdfs-site');
      hadoopSslEnabled = (hdfsSite && Em.get(hdfsSite, 'properties') && hdfsSite.properties['dfs.http.policy'] === 'HTTPS_ONLY');
    }
    switch (serviceName) {
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
        break;
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
        break;
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
        break;
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
        break;
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
        break;
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
        break;
      default:
        return this.get('servicesSupportsHttps').contains(serviceName) && hadoopSslEnabled ? "https" : "http";
    }
  },

  /**
   * sets the port of quick link
   * @param item
   * @param protocol
   * @param config
   * @returns {string}
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
      if (item.get('serviceName') == 'RANGER') {
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
  }
});
