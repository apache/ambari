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

  hasQuickLinksConfiged: false,

  quickLinksErrorMessage: '',

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
    'RANGER',
    'AMBARI_METRICS'
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
  requiredSiteNames: [],

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
    this.loadQuickLinksConfigurations();
  },

  willDestroyElement: function () {
    this.get('configProperties').clear();
    this.get('actualTags').clear();
    this.get('quickLinks').clear();
    this.get('requiredSiteNames').clear();
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

  loadQuickLinksConfigurations: function(){
    var serviceName = this.get('content.serviceName');
    console.info("Loading quicklinks configurations for " + serviceName);
    return App.ajax.send({
      name: 'configs.quicklinksconfig',
      sender: this,
      data: {
        serviceName: serviceName,
        stackVersionUrl: App.get('stackVersionURL')
      },
      success: 'loadQuickLinksConfigSuccessCallback'
    });
  },

  loadQuickLinksConfigSuccessCallback: function(data){
    App.quicklinksMapper.map(data);
    var quickLinksConfig = this.getQuickLinksConfiguration();
    if(quickLinksConfig != null){
      var protocolConfig = Em.get(quickLinksConfig, 'protocol');
      var checks = Em.get(protocolConfig, 'checks');
      var sites = ['core-site', 'hdfs-site'];
      if(checks){
        checks.forEach(function(check){
          var protocolConfigSiteProp = Em.get(check, 'site');
          if (sites.indexOf(protocolConfigSiteProp) < 0){
            sites.push(protocolConfigSiteProp);
          }
        }, this);
      }

      var links = Em.get(quickLinksConfig, 'links');
      if(links && links.length > 0){
        links.forEach(function(link){
          if(!link.remove){
            var portConfig = Em.get(link, 'port');
            var portConfigSiteProp = Em.get(portConfig, 'site');
            if(sites.indexOf(portConfigSiteProp) < 0){
              sites.push(portConfigSiteProp);
            }
          }
        }, this);
        this.set('requiredSiteNames', this.get('requiredSiteNames').pushObjects(sites).uniq());
        this.setQuickLinks();
      }
    }
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
    var serviceName = this.get('content.serviceName');
    var hosts = this.getHosts(response, serviceName);
    var hasQuickLinks = this.hasQuickLinksConfig(serviceName);
    this.set('hasQuickLinksConfiged', hasQuickLinks); // no need to set quicklinks if current service does not have quick links configured...

    if (hosts.length === 0){
      this.setEmptyLinks();
    } else if (hosts.length === 1) {
      this.setSingleHostLinks(hosts, response);
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

  getQuickLinksConfiguration: function(){
    var serviceName =  this.get('content.serviceName');
    var self = this;
    if(self.hasQuickLinksConfig(serviceName)){
      var quickLinksConfiguration = App.QuickLinksConfig.find().findProperty("id", serviceName);
      return quickLinksConfiguration;
    }
    return null;
  },

  hasQuickLinksConfig: function(serviceName) {
    var result = App.QuickLinksConfig.find().findProperty('id', serviceName);
    if(!result)
      return false;

    var links = result.get("links");
    if(links && links.length > 0){
      var toBeRemoved = 0;
      links.forEach(function(link){
        if(link.remove)
          toBeRemoved++;
      });
      return !(links.length  === toBeRemoved);
    } else {
      return false;
    }
  },

  toAddLink: function(link){
    var linkRemoved = Em.get(link, 'removed');
    var url = Em.get(link, 'url');
    return (url && !linkRemoved);
  },

  getHostLink: function(link, host, protocol, configProperties, response){
    var serviceName = this.get('content.serviceName');
    if (serviceName === 'MAPREDUCE2' && response) {
      var portConfig = Em.get(link, 'port');
      var siteName = Em.get(portConfig, 'site');
      var siteConfigs = this.get('configProperties').findProperty('type', siteName).properties;
      var hostPortConfigValue = siteConfigs[Em.get(portConfig, protocol + '_config')];
      if (hostPortConfigValue != null) {
        var hostPortValue = hostPortConfigValue.match(new RegExp("([\\w\\d.-]*):(\\d+)"));
        var hostObj = response.items.findProperty('Hosts.host_name', hostPortValue[1]);
        if (hostObj != null) {
          host = hostObj.Hosts.public_host_name;
        }
      }
    }

    var linkPort = this.setPort(Em.get(link, 'port'), protocol, configProperties);
    if (this.toAddLink(link)) {
      var newItem = {};
      var requiresUserName = Em.get(link, 'requires_user_name');
      var template = Em.get(link, 'url');
        if('true' === requiresUserName){
          newItem.url = template.fmt(protocol, host, linkPort, App.router.get('loginName'));
        } else {
          newItem.url = template.fmt(protocol, host, linkPort);
        }
        newItem.label = link.label;
        return newItem;
    } else {
      return null;
    }
  },

  /**
   * set empty links
   */
  setEmptyLinks: function () {
    //display an error message
    var quickLinks = [{
      label: this.get('quickLinksErrorMessage')
    }];
    this.set('quickLinks', quickLinks);
    this.set('isLoaded', true);
  },

  /**
   * set links that contain only one host
   * @param {Array} hosts
   */
  setSingleHostLinks: function (hosts, response) {
    var quickLinksConfig = this.getQuickLinksConfiguration();
    if(quickLinksConfig != null){
      var quickLinks = [];
      var configProperties = this.get('configProperties');
      var protocol = this.setProtocol(configProperties, quickLinksConfig);
      var publicHostName = hosts[0].publicHostName;

      var links = Em.get(quickLinksConfig, 'links');
      links.forEach(function(link){
        var newItem = this.getHostLink(link, publicHostName, protocol, configProperties, response); //quicklink generated for the hbs template
        if(newItem != null){
          quickLinks.push(newItem);
        }
      }, this);
      this.set('quickLinks', quickLinks);
      this.set('isLoaded', true);
    } else {
      this.set('quickLinks', []);
      this.set('isLoaded', false);
    }
  },

  /**
   * set links that contain multiple hosts
   * @param {Array} hosts
   */
  setMultipleHostLinks: function (hosts) {
    var quickLinksConfig = this.getQuickLinksConfiguration();
    if(quickLinksConfig == null){
      this.set('quickLinksArray', []);
      this.set('isLoaded', false);
      return;
    }

    var quickLinksArray = [];
    hosts.forEach(function (host) {
      var publicHostName = host.publicHostName;
      var quickLinks = [];
      var configProperties = this.get('configProperties');

      var protocol = this.setProtocol(configProperties, quickLinksConfig);
      var serviceName = Em.get(quickLinksConfig, 'serviceName');
      var links = Em.get(quickLinksConfig, 'links');
      links.forEach(function(link){
        var linkRemoved = Em.get(link, 'removed');
        var url = Em.get(link, 'url');
        if (url && !linkRemoved) {
          var port;
          var hostNameRegExp = new RegExp('([\\w\\W]*):\\d+');
          if (serviceName === 'HDFS') {
            var config;
            var configPropertiesObject = configProperties.findProperty('type', 'hdfs-site');
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
            var portConfig = Em.get(link, 'port');
            Em.set(portConfig, protocol +'_property', config);
            Em.set(link, 'port', portConfig)
          }

          var newItem = this.getHostLink(link, publicHostName, protocol, configProperties); //quicklink generated for the hbs template
          if(newItem != null){
            quickLinks.push(newItem);
          }
        }
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

    var oozieHostsArray = hosts.filter(function (host) {
      host.status = Em.I18n.t('quick.links.label.active');
      return activeOozieServers.contains(host.hostName);
    }, this);

    if (oozieHostsArray.length == 0)
      this.set('quickLinksErrorMessage', Em.I18n.t('quick.links.error.oozie.label'));
    return oozieHostsArray;
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
    //The default error message when we cannot obtain the host information for the given service
    this.set('quickLinksErrorMessage', Em.I18n.t('quick.links.error.nohosts.label').format(serviceName));
    if (App.get('singleNodeInstall')) {
      return [{
        hostName: App.get('singleNodeAlias'),
        publicHostName: App.get('singleNodeAlias')
      }];
    }
    if (Em.isNone(this.get('content.hostComponents'))) {
      return [];
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
      case "MAPREDUCE2":
        hosts = this.findHosts('HISTORYSERVER', response);
        break;
      case "AMBARI_METRICS":
        hosts = this.findHosts('METRICS_GRAFANA', response);
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

  reverseType: function(type){
    if("https" === type)
      return "http";
    else if("http" === type)
      return "https"
  },

  meetDesired: function(configProperties, configType, property, desiredState){
    var currentConfig = configProperties.findProperty('type', configType);
    var currentPropertyValue = currentConfig.properties[property];
    if("NOT_EXIST" === desiredState){
      if(currentPropertyValue == null)
        return true;
      else
        return false
    } else if("EXIST" === desiredState){
      if(currentPropertyValue == null)
        return false;
      else
        return true;
    } else {
      return (desiredState === currentPropertyValue)
    }
  },

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
  setProtocol: function (configProperties, item) {
    var hadoopSslEnabled = false;

    if (configProperties && configProperties.length > 0) {
      var hdfsSite = configProperties.findProperty('type', 'hdfs-site');
      hadoopSslEnabled = (hdfsSite && Em.get(hdfsSite, 'properties') && hdfsSite.properties['dfs.http.policy'] === 'HTTPS_ONLY');
    }

    var protocolConfig = Em.get(item, 'protocol');
    if(!protocolConfig){
      if(hadoopSslEnabled)
        return "https";
      else
        return "http";
    }

    var protocolType = Em.get(protocolConfig, 'type');

    if ("HTTPS_ONLY" === protocolType)
      return "https";
    else if ("HTTP_ONLY" === protocolType)
      return "http";
    else {
      var count = 0;
      var checks = Em.get(protocolConfig, 'checks');
      if(!checks){
        if(hadoopSslEnabled)
          return 'https';
        else
          return 'http';
      }
      checks.forEach(function(check){
        var configType = Em.get(check, 'site');
        var property = Em.get(check, 'property');
        var desiredState = Em.get(check, 'desired');
        var checkMeet = this.meetDesired(configProperties, configType, property, desiredState)
        if(!checkMeet){
          count++;
        }
      }, this);

      if(count > 0)
        return this.reverseType(protocolType);
      else
        return protocolType;
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
  setPort: function (portConfigs, protocol, configProperties, configPropertyKey) {

    var defaultPort = Em.get(portConfigs, protocol+'_default_port');
    var portProperty = Em.get(portConfigs,  protocol+'_property');
    var site = configProperties.findProperty('type', Em.get(portConfigs, 'site'));
    var propertyValue = site && site.properties && site.properties[portProperty];

    if (!propertyValue)
      return defaultPort;

    var regexValue = Em.get(portConfigs, 'regex');
    regexValue = regexValue.trim();
    if(regexValue){
      var re = new RegExp(regexValue);
      var portValue = propertyValue.match(re);
      try {
        return portValue[1];
      }catch(err) {
        return defaultPort;
      }
    } else {
      return propertyValue;
    }
  }
});
