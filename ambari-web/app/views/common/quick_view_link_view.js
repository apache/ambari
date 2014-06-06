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
      name: 'config.tags.sync',
      sender: this,
      success: 'loadTagsSuccess'
    });
  },

  loadTagsSuccess: function (data) {
    var tags = [];
    for (var prop in data.Clusters.desired_configs) {
      tags.push(Em.Object.create({
        siteName: prop,
        tagName: data.Clusters.desired_configs[prop]['tag']
      }));
    }
    this.set('actualTags', tags);
    this.setConfigProperties();
  },

  actualTags: [],

  configProperties: [],

  /**
   * list of files that contains properties for enabling/disabling ssl
   */
  requiredSiteNames: ['global','core-site', 'hdfs-site', 'hbase-site', 'oozie-site', 'yarn-site', 'mapred-site'],

  setConfigProperties: function () {
    this.set('configProperties', []);
    var requiredSiteNames = this.get('requiredSiteNames');
    var tags = this.get('actualTags').filter(function (tag) {
      return requiredSiteNames.contains(tag.siteName);
    });
    var data = App.router.get('configurationController').getConfigsByTags(tags);
    this.set('configProperties', data);

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
    return App.singleNodeInstall ? App.singleNodeAlias : components.find(function (item) {
      return item.host_components.mapProperty('HostRoles.component_name').contains(componentName);
    }).Hosts.public_host_name
  },

  setQuickLinks: function () {
    App.ajax.send({
      name: 'hosts.for_quick_links',
      sender: this,
      data: {
        clusterName: App.get('clusterName'),
        masterComponents: App.StackServiceComponent.find().filterProperty('isMaster', true).mapProperty('componentName').join(','),
        urlParams: ',host_components/component/metrics/hbase/master/IsActiveMaster'
      },
      success: 'setQuickLinksSuccessCallback'
    });
  }.observes('App.currentStackVersionNumber', 'App.singleNodeInstall'),

  setQuickLinksSuccessCallback: function (response) {
    this.loadTags();
    var serviceName = this.get('content.serviceName');
    var hosts = [];
    var self = this;
    var version = App.get('currentStackVersionNumber');
    var quickLinks = [];
    switch (serviceName) {
      case "HDFS":
        var otherHost;
        if (this.get('content.snameNode')) {
          // not HA
          hosts[0] = this.findComponentHost(response.items, 'NAMENODE');
        } else {
          // HA enabled, need both two namenodes hosts
          var nameNodes = response.items.filter(function (item) {
            return item.host_components.mapProperty('HostRoles.component_name').contains('NAMENODE');
          });
          nameNodes.forEach(function(item) {
            hosts.push({'publicHostName': item.Hosts.public_host_name});
          });
          // assign each namenode status label
          if (this.get('content.activeNameNode')) {
            hosts.findProperty('publicHostName', this.get('content.activeNameNode.publicHostName')).status = Em.I18n.t('quick.links.label.active');
          }
          if (this.get('content.standbyNameNode')) {
            hosts.findProperty('publicHostName', this.get('content.standbyNameNode.publicHostName')).status = Em.I18n.t('quick.links.label.standby');
          }
          if (this.get('content.standbyNameNode2')) {
            hosts.findProperty('publicHostName', this.get('content.standbyNameNode2.publicHostName')).status = Em.I18n.t('quick.links.label.standby');
          }
        }
        break;
      case "MAPREDUCE":
      case "OOZIE":
      case "GANGLIA":
      case "NAGIOS":
      case "HUE":
        hosts[0] = App.singleNodeInstall ? App.singleNodeAlias : response.items[0].Hosts.public_host_name;
        break;
      case "HBASE":
        var masterComponents = response.items.filter(function (item) {
            return item.host_components.mapProperty('HostRoles.component_name').contains('HBASE_MASTER');
        });
        var activeMaster, standbyMasters, otherMasters;
        if (App.supports.multipleHBaseMasters) {
          activeMaster = masterComponents.filter(function (item) {
            return item.host_components.mapProperty('component')[0].mapProperty('metrics.hbase.master.IsActiveMaster').contains('true');
          });
          standbyMasters = masterComponents.filter(function (item) {
            return item.host_components.mapProperty('component')[0].mapProperty('metrics.hbase.master.IsActiveMaster').contains('false');
          });
          otherMasters = masterComponents.filter(function (item) {
            return item.host_components.mapProperty('component')[0].mapProperty('metrics.hbase.master.IsActiveMaster').contains(undefined);
          });
        }
        if (masterComponents) {
          if (App.singleNodeInstall) {
            hosts[0] = App.singleNodeAlias;
          } else if (masterComponents.length > 1) {
            // need all hbase_masters hosts in quick links
            if (activeMaster) {
              activeMaster.forEach(function(item) {
                hosts.push({'publicHostName': item.Hosts.public_host_name, 'status': Em.I18n.t('quick.links.label.active')});
              });
            }
            if (standbyMasters) {
              standbyMasters.forEach(function(item) {
                hosts.push({'publicHostName': item.Hosts.public_host_name, 'status': Em.I18n.t('quick.links.label.standby')});
              });
            }
            if (otherMasters) {
              otherMasters.forEach(function(item) {
                hosts.push({'publicHostName': item.Hosts.public_host_name});
              });
            }
          } else {
            hosts[0] = masterComponents[0].Hosts.public_host_name;
          }
        }
        break;
      case "YARN":
        hosts[0] = this.findComponentHost(response.items, 'RESOURCEMANAGER');
        break;
      case "MAPREDUCE2":
        hosts[0] = this.findComponentHost(response.items, 'HISTORYSERVER');
        break;
      case "FALCON":
        hosts[0] = this.findComponentHost(response.items, 'FALCON_SERVER');
        break;
      case "STORM":
        hosts[0] = this.findComponentHost(response.items, 'STORM_UI_SERVER');
        break;
    }
    if (!hosts) {
      quickLinks = [
        {
          label: this.t('quick.links.error.label'),
          url: 'javascript:alert("' + this.t('contact.administrator') + '");return false;'
        }
      ];
      this.set('quickLinks', quickLinks);
      this.set('isLoaded', true);
    } else if (hosts.length == 1) {

      quickLinks = this.get('content.quickLinks').map(function (item) {
        var protocol = self.setProtocol(item.get('service_id'));
        if (item.get('template')) {
          var port = item.get('http_config') && self.setPort(item, protocol, version);
          item.set('url', item.get('template').fmt(protocol, hosts[0], port));
        }
        return item;
      });
      this.set('quickLinks', quickLinks);
      this.set('isLoaded', true);
    } else {
      // multiple hbase masters or HDFS HA enabled
      var quickLinksArray = [];
      hosts.forEach(function(host){
        var quickLinks = [];
        self.get('content.quickLinks').forEach(function (item) {
          var newItem = {};
          var protocol = self.setProtocol(item.get('service_id'));
          if (item.get('template')) {
            var port = item.get('http_config') && self.setPort(item, protocol, version);
            newItem.url = item.get('template').fmt(protocol, host.publicHostName, port);
            newItem.label =  item.get('label');
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
    }

  },

  setProtocol: function (service_id) {
    var properties = this.ambariProperties();
    var configProperties = this.get('configProperties');
    var hadoopSslEnabled = false;
    if (configProperties) {
      var site = configProperties.findProperty('type', 'core-site');
      site.properties['hadoop.ssl.enabled'] && site.properties['hadoop.ssl.enabled'] === 'true' ? hadoopSslEnabled = true : null;
    }
    switch (service_id) {
      case "GANGLIA":
        return (properties && properties.hasOwnProperty('ganglia.https') && properties['ganglia.https']) ? "https" : "http";
        break;
      case "NAGIOS":
        return (properties && properties.hasOwnProperty('nagios.https') && properties['nagios.https']) ? "https" : "http";
        break;
      case "HDFS":
      case "HBASE":
      case "MAPREDUCE":
        return hadoopSslEnabled ? "https" : "http";
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
        return "http";
    }
  },

  setPort: function (item, protocol, version) {
    var service_id = item.get('service_id');
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
