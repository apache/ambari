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

/**
 * @typedef {object} hostForQuickLink
 * @property {string} hostName
 * @property {string} publicHostName
 * @property {string} componentName
 * @property {?string} status
 */

App.QuickLinksView = Em.View.extend({

  /**
   * @type {boolean}
   */
  isLoaded: false,

  /**
   * @type {boolean}
   */
  showQuickLinks: false,

  /**
   * @type {boolean}
   */
  showNoLinks: false,

  /**
   * @type {string}
   */
  quickLinksErrorMessage: '',

  /**
   * Updated quick links. Here we put correct hostname to url
   *
   * @type {object[]}
   */
  quickLinks: [],

  /**
   * @type {string[]}
   */
  actualTags: [],

  /**
   * @type {object[]}
   */
  configProperties: [],

  /**
   * list of files that contains properties for enabling/disabling ssl
   *
   * @type {string[]}
   */
  requiredSiteNames: [],

  /**
   * services that supports security. this array is used to find out protocol.
   * besides YARN, MAPREDUCE2, ACCUMULO. These services use
   * their properties to know protocol
   */
  servicesSupportsHttps: ["HDFS", "HBASE"],

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
   * - App.router.clusterController.isHostComponentMetricsLoaded
   */
  setQuickLinks: function () {
    if (App.get('router.clusterController.isServiceMetricsLoaded')) {
      this.loadTags();
    }
  }.observes(
    'App.currentStackVersionNumber',
    'App.router.clusterController.isServiceMetricsLoaded',
    'App.router.clusterController.isHostComponentMetricsLoaded',
    'App.router.clusterController.quickLinksUpdateCounter'
  ),

  /**
   * call for configuration tags
   *
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

  /**
   * Success-callback for load-tags request
   *
   * @param {object} data
   * @method loadTagsSuccess
   */
  loadTagsSuccess: function (data) {
    this.get('actualTags').clear();
    var self = this;
    var tags = Object.keys(data.Clusters.desired_configs).map(function (prop) {
      return Em.Object.create({
        siteName: prop,
        tagName: data.Clusters.desired_configs[prop].tag
      });
    });
    this.get('actualTags').pushObjects(tags);
    this.setConfigProperties().done(function (configProperties) {
      self.get('configProperties').pushObjects(configProperties);
      self.getQuickLinksHosts();
    });
  },

  /**
   * Error-callback for load-tags request
   *
   * @method loadTagsError
   */
  loadTagsError: function () {
    this.getQuickLinksHosts();
  },

  /**
   * Request for quick-links config
   *
   * @returns {$.ajax}
   * @method loadQuickLinksConfigurations
   */
  loadQuickLinksConfigurations: function () {
    var serviceName = this.get('content.serviceName');
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

  /**
   * Sucess-callback for quick-links config request
   *
   * @param {object} data
   * @method loadQuickLinksConfigSuccessCallback
   */
  loadQuickLinksConfigSuccessCallback: function (data) {
    App.quicklinksMapper.map(data);
    var quickLinksConfig = this.getQuickLinksConfiguration();
    if (!Em.isNone(quickLinksConfig)) {
      var protocolConfig = Em.get(quickLinksConfig, 'protocol');
      var checks = Em.get(protocolConfig, 'checks');
      var sites = ['core-site', 'hdfs-site', 'admin-properties'];
      if (checks) {
        checks.forEach(function (check) {
          var protocolConfigSiteProp = Em.get(check, 'site');
          if (sites.indexOf(protocolConfigSiteProp) < 0) {
            sites.push(protocolConfigSiteProp);
          }
        }, this);
      }

      var links = Em.get(quickLinksConfig, 'links');
      if (!Em.isEmpty(links)) {
        links.forEach(function (link) {
          if (!link.remove) {
            var portConfig = Em.get(link, 'port');
            var portConfigSiteProp = Em.get(portConfig, 'site');
            if (!sites.contains(portConfigSiteProp)) {
              sites.push(portConfigSiteProp);
            }
          }
        }, this);
        this.set('requiredSiteNames', this.get('requiredSiteNames').pushObjects(sites).uniq());
        this.setQuickLinks();
      }
    } else {
      this.set('showNoLinks', true);

    }
  },

  /**
   * call for public host names
   *
   * @returns {$.ajax}
   * @method getQuickLinksHosts
   */
  getQuickLinksHosts: function () {
    var masterHosts = App.HostComponent.find().filterProperty('isMaster').mapProperty('hostName').uniq();

    if (masterHosts.length === 0) {
      return $.Deferred().reject().promise();
    }

    return App.ajax.send({
      name: 'hosts.for_quick_links',
      sender: this,
      data: {
        clusterName: App.get('clusterName'),
        masterHosts: masterHosts.join(','),
        urlParams: this.get('content.serviceName') === 'HBASE' ? ',host_components/metrics/hbase/master/IsActiveMaster' : ''
      },
      success: 'setQuickLinksSuccessCallback'
    });
  },

  /**
   * Success-callback for quick-links hosts request
   *
   * @param {object} response
   * @method setQuickLinksSuccessCallback
   */
  setQuickLinksSuccessCallback: function (response) {
    var serviceName = this.get('content.serviceName');
    var hosts = this.getHosts(response, serviceName);
    var hasQuickLinks = this.hasQuickLinksConfig(serviceName, hosts);
    var hasHosts = false;
    var componentNames = hosts.mapProperty('componentName');
    componentNames.forEach(function(_componentName){
      var masterComponent = App.MasterComponent.find().findProperty('componentName', _componentName);
      if (masterComponent) {
        hasHosts = hasHosts || !!masterComponent.get('totalCount');
      }
    });
    // no need to set quicklinks if
    // 1)current service does not have quick links configured
    // 2)No host component present for the configured quicklinks
    if(hasQuickLinks && hasHosts) {
      this.set('showQuickLinks', true);
    } else {
      this.set('showNoLinks', true);
    }

    var isMultipleComponentsInLinks = componentNames.uniq().length > 1;

    if (hosts.length === 0) {
      this.setEmptyLinks();
    } else if (hosts.length === 1 || isMultipleComponentsInLinks) {
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
   * @return {?string}
   **/
  getPublicHostName: function (hosts, hostName) {
    var host = hosts.findProperty('Hosts.host_name', hostName);
    return host ? Em.get(host, 'Hosts.public_host_name') : null;
  },

  /**
   * Get configs from `configurationController` for provided list of the tags
   *
   * @returns {$.Deferred}
   * @method setConfigProperties
   */
  setConfigProperties: function () {
    this.get('configProperties').clear();
    var requiredSiteNames = this.get('requiredSiteNames');
    var tags = this.get('actualTags').filter(function (tag) {
      return requiredSiteNames.contains(tag.siteName);
    });
    return App.router.get('configurationController').getConfigsByTags(tags);
  },

  /**
   * Get quick links config for <code>content.serviceName</code>
   *
   * @returns {?App.QuickLinksConfig}
   * @method getQuickLinksConfiguration
   */
  getQuickLinksConfiguration: function () {
    var serviceName = this.get('content.serviceName');
    var self = this;
    var quicklinks = {};
    if (self.hasQuickLinksConfig(serviceName)) {
      quicklinks = App.QuickLinksConfig.find().findProperty('id', serviceName);
      Em.set(quicklinks, 'links', Em.get(quicklinks, 'links').filterProperty('visible', true));
      return quicklinks;
    }
    return null;
  },

  /**
   * Check if <code>serviceName</code> has quick-links config
   *
   * @param {string} serviceName
   * @returns {boolean}
   * @method hasQuickLinksConfig
   */
  hasQuickLinksConfig: function (serviceName) {
    var result = App.QuickLinksConfig.find().findProperty('id', serviceName);
    if (!result) {
      return false;
    }
    var links = result.get('links');
    return Em.isEmpty(links) ? false : links.length !== links.filterProperty('remove').length;
  },

  /**
   *
   * @param {object} link
   * @param {string} host
   * @param {string} protocol
   * @param {object[]} configProperties
   * @param {object} response
   * @returns {?object}
   * @method getHostLink
   */
  getHostLink: function (link, host, protocol, configProperties, response) {
    var serviceName = this.get('content.serviceName');
    if (serviceName === 'MAPREDUCE2' && response) {
      var portConfig = Em.get(link, 'port');
      var siteName = Em.get(portConfig, 'site');
      var siteConfigs = this.get('configProperties').findProperty('type', siteName).properties;
      var hostPortConfigValue = siteConfigs[Em.get(portConfig, protocol + '_config')];
      if (!Em.isNone(hostPortConfigValue)) {
        var hostPortValue = hostPortConfigValue.match(new RegExp('([\\w\\d.-]*):(\\d+)'));
        var hostObj = response.items.findProperty('Hosts.host_name', hostPortValue[1]);
        if (!Em.isNone(hostObj)) {
          host = hostObj.Hosts.public_host_name;
        }
      }
    } else if (serviceName === 'RANGER') {
      var siteConfigs = this.get('configProperties').findProperty('type', 'admin-properties').properties;
      if (siteConfigs['policymgr_external_url']) {
        host = siteConfigs['policymgr_external_url'].split('://')[1].split(':')[0];
      }
    }

    var linkPort = this.setPort(Em.get(link, 'port'), protocol, configProperties);
    if (Em.get(link, 'url') && !Em.get(link, 'removed')) {
      var newItem = {};
      var requiresUserName = Em.get(link, 'requires_user_name');
      var template = Em.get(link, 'url');
      if ('true' === requiresUserName) {
        newItem.url = template.fmt(protocol, host, linkPort, App.router.get('loginName'));
      } else {
        newItem.url = template.fmt(protocol, host, linkPort);
      }
      newItem.label = link.label;
      return newItem;
    }
    return null;
  },

  /**
   * set empty links
   *
   * @method setEmptyLinks
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
   *
   * @param {hostForQuickLink[]} hosts
   * @param {object} response
   * @method setSingleHostLinks
   */
  setSingleHostLinks: function (hosts, response) {
    var quickLinksConfig = this.getQuickLinksConfiguration();
    if (!Em.isNone(quickLinksConfig)) {
      var quickLinks = [];
      var configProperties = this.get('configProperties');
      var protocol = this.setProtocol(configProperties, quickLinksConfig.get('protocol'));

      var links = Em.get(quickLinksConfig, 'links');
      links.forEach(function (link) {
        var componentName = link.component_name;
        var hostNameForComponent = hosts.findProperty('componentName',componentName);
        if (hostNameForComponent) {
          var publicHostName = hostNameForComponent.publicHostName;
          if (link.protocol) {
            protocol = this.setProtocol(configProperties, link.protocol);
          }
          var newItem = this.getHostLink(link, publicHostName, protocol, configProperties, response); //quicklink generated for the hbs template
          if (!Em.isNone(newItem)) {
            quickLinks.push(newItem);
          }
        }
      }, this);
      this.set('quickLinks', quickLinks);
      this.set('isLoaded', true);
    }
    else {
      this.set('quickLinks', []);
      this.set('isLoaded', false);
    }
  },

  /**
   * set links that contain multiple hosts
   *
   * @param {hostForQuickLink[]} hosts
   * @method setMultipleHostLinks
   */
  setMultipleHostLinks: function (hosts) {
    var quickLinksConfig = this.getQuickLinksConfiguration();
    if (Em.isNone(quickLinksConfig)) {
      this.set('quickLinksArray', []);
      this.set('isLoaded', false);
      return;
    }

    var quickLinksArray = [];
    hosts.forEach(function (host) {
      var publicHostName = host.publicHostName;
      var quickLinks = [];
      var configProperties = this.get('configProperties');

      var protocol = this.setProtocol(configProperties, quickLinksConfig.get('protocol'));
      var serviceName = Em.get(quickLinksConfig, 'serviceName');
      var links = Em.get(quickLinksConfig, 'links');
      links.forEach(function (link) {
        var linkRemoved = Em.get(link, 'removed');
        var url = Em.get(link, 'url');
        if (url && !linkRemoved) {
          var hostNameRegExp = new RegExp('([\\w\\W]*):\\d+');
          if (serviceName === 'HDFS') {
            var config;
            var configPropertiesObject = configProperties.findProperty('type', 'hdfs-site');
            if (configPropertiesObject && configPropertiesObject.properties) {
              var properties = configPropertiesObject.properties;
              var nameServiceId = properties['dfs.nameservices'];
              var nnProperties = ['dfs.namenode.{0}-address.{1}.nn1', 'dfs.namenode.{0}-address.{1}.nn2'].invoke('format', protocol, nameServiceId);
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
            Em.set(portConfig, protocol + '_property', config);
            Em.set(link, 'port', portConfig)
          }

          var newItem = this.getHostLink(link, publicHostName, protocol, configProperties); //quicklink generated for the hbs template
          if (!Em.isNone(newItem)) {
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
   *
   * @param {object[]} hosts
   * @returns {object[]}
   * @method processOozieHosts
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

    if (!oozieHostsArray.length) {
      this.set('quickLinksErrorMessage', Em.I18n.t('quick.links.error.oozie.label'));
    }
    return oozieHostsArray;
  },

  /**
   * set status to hosts with NAMENODE
   *
   * @param {hostForQuickLink[]} hosts
   * @returns {object[]}
   * @method hostForQuickLink
   */
  processHdfsHosts: function (hosts) {
    return hosts.map(function (host) {
      if (host.hostName === Em.get(this, 'content.activeNameNode.hostName')) {
        host.status = Em.I18n.t('quick.links.label.active');
      }
      else
        if (host.hostName === Em.get(this, 'content.standbyNameNode.hostName')) {
          host.status = Em.I18n.t('quick.links.label.standby');
        }
        else
          if (host.hostName === Em.get(this, 'content.standbyNameNode2.hostName')) {
            host.status = Em.I18n.t('quick.links.label.standby');
          }
      return host;
    }, this);
  },

  /**
   * set status to hosts with HBASE_MASTER
   *
   * @param {hostForQuickLink[]} hosts
   * @param {object} response
   * @returns {hostForQuickLink[]}
   * @method processHbaseHosts
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
      }
      else
        if (isActiveMaster === 'false') {
          host.status = Em.I18n.t('quick.links.label.standby');
        }
      return host;
    }, this);
  },

  /**
   * set status to hosts with RESOURCEMANAGER
   *
   * @param {hostForQuickLink[]} hosts
   * @returns {hostForQuickLink[]}
   * @method processYarnHosts
   */
  processYarnHosts: function (hosts) {
    return hosts.map(function (host) {
      var resourceManager = this.get('content.hostComponents')
        .filterProperty('componentName', 'RESOURCEMANAGER')
        .findProperty('hostName', host.hostName);
      var haStatus = resourceManager && resourceManager.get('haStatus');
      if (haStatus === 'ACTIVE') {
        host.status = Em.I18n.t('quick.links.label.active');
      }
      else
        if (haStatus === 'STANDBY') {
          host.status = Em.I18n.t('quick.links.label.standby');
        }
      return host;
    }, this);
  },

  /**
   * sets public host names for required masters of current service
   *
   * @param {object} response
   * @param {string} serviceName - selected serviceName
   * @returns {hostForQuickLink[]} containing hostName(s)
   * @method getHosts
   */
  getHosts: function (response, serviceName) {
    //The default error message when we cannot obtain the host information for the given service
    this.set('quickLinksErrorMessage', Em.I18n.t('quick.links.error.nohosts.label').format(serviceName));
    var hosts = [];
    var quickLinkConfigs = App.QuickLinksConfig.find().findProperty("id", serviceName);
    if (quickLinkConfigs) {
      var links = quickLinkConfigs.get('links');
      var componentNames = links.mapProperty('component_name').uniq();
      componentNames.forEach(function (_componentName) {
        var componentHosts = this.findHosts(_componentName, response);
        switch (serviceName) {
          case 'OOZIE':
            hosts = hosts.concat(this.processOozieHosts(componentHosts));
            break;
          case "HDFS":
            hosts = hosts.concat(this.processHdfsHosts(componentHosts));
            break;
          case "HBASE":
            hosts = hosts.concat(this.processHbaseHosts(componentHosts, response));
            break;
          case "YARN":
            hosts = hosts.concat(this.processYarnHosts(componentHosts));
            break;
          case "SMARTSENSE":
            if(!App.MasterComponent.find().findProperty('componentName', _componentName)) {
              hosts = [];
              break;
            }
          default:
            hosts = hosts.concat(componentHosts);
            break;
        }
      }, this);
    }
    return hosts;
  },

  /**
   * find host public names
   *
   * @param {string} componentName
   * @param {object} response
   * @returns {hostForQuickLink[]}
   */
  findHosts: function (componentName, response) {
    var hosts = [];
    var masterComponent = App.MasterComponent.find().findProperty('componentName', componentName);
    if (masterComponent) {
      var masterHostComponents = masterComponent.get('hostNames') || [];
      masterHostComponents.forEach(function (_hostName) {
        var host = this.getPublicHostName(response.items, _hostName);
        if (host) {
          hosts.push({
            hostName: _hostName,
            publicHostName: host,
            componentName: componentName
          });
        }
      }, this);
    }
    return hosts;
  },

  /**
   * 'http' for 'https'
   * 'https' for 'https'
   * empty string otherwise
   *
   * @param {string} type
   * @returns {string}
   * @method reverseType
   */
  reverseType: function (type) {
    if ('https' === type) {
      return 'http';
    }
    if ('http' === type) {
      return 'https';
    }
    return '';
  },

  /**
   *
   * @param {object[]} configProperties
   * @param {string} configType
   * @param {string} property
   * @param {string} desiredState
   * @returns {boolean}
   * @method meetDesired
   */
  meetDesired: function (configProperties, configType, property, desiredState) {
    var currentConfig = configProperties.findProperty('type', configType);
    if (!currentConfig) {
      return false;
    }
    var currentPropertyValue = currentConfig.properties[property];
    if ('NOT_EXIST' === desiredState) {
      return Em.isNone(currentPropertyValue);
    }
    if ('EXIST' === desiredState) {
      return !Em.isNone(currentPropertyValue);
    }
    return desiredState === currentPropertyValue;
  },

  /**
   * setProtocol - if cluster is secure for some services (GANGLIA, MAPREDUCE2, YARN and servicesSupportsHttps)
   * protocol becomes "https" otherwise "http" (by default)
   *
   * @param {Object} configProperties
   * @param {Object} protocolConfig
   * @returns {string} "https" or "http" only!
   * @method setProtocol
   */
  setProtocol: function (configProperties, protocolConfig) {
    var hadoopSslEnabled = false;

    if (!Em.isEmpty(configProperties)) {
      var hdfsSite = configProperties.findProperty('type', 'hdfs-site');
      hadoopSslEnabled = hdfsSite && Em.get(hdfsSite, 'properties') && hdfsSite.properties['dfs.http.policy'] === 'HTTPS_ONLY';
    }

    if (!protocolConfig) {
      return hadoopSslEnabled ? 'https' : 'http';
    }

    var protocolType = Em.get(protocolConfig, 'type');

    if ('HTTPS_ONLY' === protocolType) {
      return 'https';
    }
    if ('HTTP_ONLY' === protocolType) {
      return 'http';
    }

    protocolType = protocolType.toLowerCase();

    var count = 0;
    var checks = Em.get(protocolConfig, 'checks');
    if (!checks) {
      return hadoopSslEnabled ? 'https' : 'http';
    }
    checks.forEach(function (check) {
      var configType = Em.get(check, 'site');
      var property = Em.get(check, 'property');
      var desiredState = Em.get(check, 'desired');
      var checkMeet = this.meetDesired(configProperties, configType, property, desiredState);
      if (!checkMeet) {
        count++;
      }
    }, this);

    return count ? this.reverseType(protocolType) : protocolType;
  },

  /**
   * sets the port of quick link
   *
   * @param {object} portConfigs
   * @param {string} protocol
   * @param {object[]} configProperties
   * @returns {string}
   * @method setPort
   */
  setPort: function (portConfigs, protocol, configProperties) {

    var defaultPort = Em.get(portConfigs, protocol + '_default_port');
    var portProperty = Em.get(portConfigs, protocol + '_property');
    var site = configProperties.findProperty('type', Em.get(portConfigs, 'site'));
    var propertyValue = site && site.properties && site.properties[portProperty];

    if (!propertyValue) {
      return defaultPort;
    }

    var regexValue = Em.get(portConfigs, 'regex');
    if (protocol === 'https') {
      var httpsRegex = Em.get(portConfigs, 'https_regex');
      if (httpsRegex) {
        regexValue = httpsRegex;
      }
    }
    regexValue = regexValue.trim();
    if (regexValue) {
      var re = new RegExp(regexValue);
      var portValue = propertyValue.match(re);
      try {
        return portValue[1];
      } catch (err) {
        return defaultPort;
      }
    } else {
      return propertyValue;
    }
  }
});
