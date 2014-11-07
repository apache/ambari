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
      success: 'setQuickLinksSuccessCallback'
    });
  },

  actualTags: [],

  configProperties: [],

  /**
   * list of files that contains properties for enabling/disabling ssl
   */
  requiredSiteNames: ['hadoop-env','yarn-env','hbase-env','oozie-env','mapred-env','core-site', 'hdfs-site', 'hbase-site', 'oozie-site', 'yarn-site', 'mapred-site'],
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

  findComponentHost: function (components, componentName) {
    var component = components.find(function (item) {
      return item.host_components.someProperty('HostRoles.component_name', componentName);
    });
    return component && component.Hosts.public_host_name;
  },

  setQuickLinks: function () {
    this.loadTags();
  }.observes('App.currentStackVersionNumber', 'App.singleNodeInstall'),

  setQuickLinksSuccessCallback: function (response) {
    var self = this;
    var quickLinks = [];
    var hosts = this.setHost(response, this.get('content.serviceName'));
    if (!hosts || !this.get('content.quickLinks')) {
      quickLinks = [{
          label: this.t('quick.links.error.label'),
          url: 'javascript:alert("' + this.t('contact.administrator') + '");return false;'
      }];
      this.set('quickLinks', quickLinks);
      this.set('isLoaded', true);
      /**
       * MAPREDUCE is only service that use 2 different masters in quick links
       * so we must work with this service as with one-master-service but set up
       * two hosts for two components. (JOBTRACKER and HISTORYSERVER)
       */
    } else if (hosts.length == 1 || this.get('content.serviceName') == "MAPREDUCE") {

      quickLinks = this.get('content.quickLinks').map(function (item) {
        var protocol = self.setProtocol(item.get('service_id'), self.get('configProperties'), self.ambariProperties());
        if (item.get('template')) {
          var port = item.get('http_config') && self.setPort(item, protocol);
          /**
           * setting other host for mapreduce (only for MAPREDUCE and JobHistory Server)!!!
           */
          if (self.get('content.serviceName') == "MAPREDUCE" && item.get('label') == "JobHistory Server") {
            item.set('url', item.get('template').fmt(protocol, hosts[1], port));
          } else {
            item.set('url', item.get('template').fmt(protocol, hosts[0], port));
          }
        }
        return item;
      });
      this.set('quickLinks', quickLinks);
      this.set('isLoaded', true);
    } else {
      // multiple hbase masters or HDFS HA enabled
      var quickLinksArray = [];
      hosts.forEach(function(host) {
        var quickLinks = [];
        self.get('content.quickLinks').forEach(function (item) {
          var newItem = {};
          var protocol = self.setProtocol(item.get('service_id'), self.get('configProperties'), self.ambariProperties());
          if (item.get('template')) {
            var port = item.get('http_config') && self.setPort(item, protocol);
            newItem.url = item.get('template').fmt(protocol, host.publicHostName, port);
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
      this.set('quickLinksArray', quickLinksArray);
      this.set('isLoaded', true);
    }
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
    switch (serviceName) {
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
        if (App.get('supports.multipleHBaseMasters')) {
          activeMaster = masterComponents.filter(function (item) {
            return item.host_components.someProperty('metrics.hbase.master.IsActiveMaster', 'true');
          });
          standbyMasters = masterComponents.filter(function (item) {
            return item.host_components.someProperty('metrics.hbase.master.IsActiveMaster', 'false');
          });
          otherMasters = masterComponents.filter(function (item) {
            return !(item.host_components.someProperty('metrics.hbase.master.IsActiveMaster', 'true') || item.host_components.someProperty('metrics.hbase.master.IsActiveMaster', 'false'));
          });
        }
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
      case "MAPREDUCE":
        hosts[0] = this.findComponentHost(response.items, "JOBTRACKER");
        hosts[1] = this.findComponentHost(response.items, "HISTORYSERVER");
        break;
      case "STORM":
        hosts[0] = this.findComponentHost(response.items, "STORM_UI_SERVER");
        break;
      default:
        if (App.StackService.find().findProperty('serviceName', serviceName).get('hasMaster')) {
          hosts[0] = this.findComponentHost(response.items, this.get('content.hostComponents') && this.get('content.hostComponents').findProperty('isMaster', true).get('componentName'));
        }
        break;
    }
    return hosts;
  },

  /**
   * services that supports security. this array is used to find out protocol.
   * becides GANGLIA, NAGIOS, YARN, MAPREDUCE2. These properties use
   * their properties to know protocol
   */
  servicesSupportsHttps: ["HDFS", "HBASE", "MAPREDUCE"],

  /**
   * setProtocol - if cluster is secure for some services (GANGLIA, NAGIOS, MAPREDUCE2, YARN and servicesSupportsHttps)
   * protocol becomes "https" otherwise "http" (by default)
   * @param {String} service_id - service name
   * @param {Object} configProperties
   * @param {Object} ambariProperties
   * @returns {string} "https" or "http" only!
   * @method setProtocol
   */
  setProtocol: function (service_id, configProperties, ambariProperties) {
    var hadoopSslEnabled = false;
    if (configProperties && configProperties.length > 0) {
      var coreSite = configProperties.findProperty('type', 'core-site');
      var hdfsSite = configProperties.findProperty('type', 'hdfs-site');
      if (App.get('isHadoop2Stack')) {
        hadoopSslEnabled = (Em.get(hdfsSite, 'properties') && hdfsSite.properties['dfs.http.policy'] === 'HTTPS_ONLY');
      } else {
        hadoopSslEnabled = (Em.get(coreSite, 'properties') && coreSite.properties['hadoop.ssl.enabled'] == true);
      }
    }
    switch (service_id) {
      case "GANGLIA":
        return (ambariProperties && ambariProperties['ganglia.https'] == true) ? "https" : "http";
        break;
      case "NAGIOS":
        return (ambariProperties && ambariProperties['nagios.https'] == true) ? "https" : "http";
        break;
      case "YARN":
        var yarnProperties = configProperties.findProperty('type', 'yarn-site');
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
        var mapred2Properties = configProperties.findProperty('type', 'mapred-site');
        if (mapred2Properties && mapred2Properties.properties) {
          if (mapred2Properties.properties['mapreduce.jobhistory.http.policy'] === 'HTTPS_ONLY') {
            return "https";
          } else if (mapred2Properties.properties['mapreduce.jobhistory.http.policy'] === 'HTTP_ONLY') {
            return "http";
          }
        }
        return hadoopSslEnabled ? "https" : "http";
        break;
      default:
        return this.get('servicesSupportsHttps').contains(service_id) && hadoopSslEnabled ? "https" : "http";
    }
  },

  /**
   * sets the port of quick link
   * @param item
   * @param protocol
   * @returns {*}
   * @method setPort
   */
  setPort: function (item, protocol) {
    var configProperties = this.get('configProperties');
    var config = item.get('http_config');
    var defaultPort = item.get('default_http_port');
    if (protocol === 'https' && item.get('https_config')) {
      config = item.get('https_config');
      if (item.get('default_https_port')) {
        defaultPort = item.get('default_https_port');
      }
    }
    var site = configProperties.findProperty('type', item.get('site'));
    var propertyValue = site && site.properties[config];
    if (!propertyValue) {
      return defaultPort;
    }

    var re = new RegExp(item.get('regex'));

    var portValue = propertyValue.match(re);
    return  portValue[1];
  },

  linkTarget: function () {
    switch (this.get('content.serviceName').toLowerCase()) {
      case "hdfs":
      case "yarn":
      case "mapreduce2":
      case "mapreduce":
      case "hbase":
      case "oozie":
      case "ganglia":
      case "nagios":
      case "hue":
      case "storm":
      case "falcon":
        return "_blank";
        break;
      default:
        return "";
        break;
    }
  }.property('service')

});
