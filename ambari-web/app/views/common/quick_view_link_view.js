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

  findComponentHost: function (componentName) {
    var components = this.get('content.hostComponents');
    return App.singleNodeInstall ? App.singleNodeAlias : components.findProperty('componentName', componentName).get('host.publicHostName')
  },

  setQuickLinks: function () {
    this.loadTags();
    var serviceName = this.get('content.serviceName');
    var components = this.get('content.hostComponents');
    var host;
    var self = this;
    var version = App.get('currentStackVersionNumber');
    var quickLinks = [];
    switch (serviceName) {
      case "HDFS":
        if (this.get('content.snameNode')) { // not HA
          host = this.findComponentHost('NAMENODE');
        } else {
          // HA
          if (this.get('content.activeNameNode')) {
            host = this.get('content.activeNameNode.publicHostName');
          } else {
            host = 'noActiveNN';
          }
        }
        break;
      case "MAPREDUCE":
      case "OOZIE":
      case "GANGLIA":
      case "NAGIOS":
      case "HUE":
        host = App.singleNodeInstall ? App.singleNodeAlias : components.findProperty('isMaster', true).get("host").get("publicHostName");
        break;
      case "HBASE":
        var component;
        if (App.supports.multipleHBaseMasters) {
          component = components.filterProperty('componentName', 'HBASE_MASTER').findProperty('haStatus', 'true');
        } else {
          component = components.findProperty('componentName', 'HBASE_MASTER');
        }
        if (component) {
          if (App.singleNodeInstall) {
            host = App.singleNodeAlias;
          } else {
            host = component.get('host.publicHostName');
          }
        } else {
          host = 'noActiveHbaseMaster';
        }
        break;
      case "YARN":
        host = this.findComponentHost('RESOURCEMANAGER');
        break;
      case "MAPREDUCE2":
        host = this.findComponentHost('HISTORYSERVER');
        break;
      case "FALCON":
        host = this.findComponentHost('FALCON_SERVER');
        break;
      case "STORM":
        host = this.findComponentHost('NIMBUS');
        break;
    }
    if (!host) {
      quickLinks = [
        {
          label: this.t('quick.links.error.label'),
          url: 'javascript:alert("' + this.t('contact.administrator') + '");return false;'
        }
      ];
    } else {
      quickLinks = this.get('content.quickLinks').map(function (item) {
        if (host == 'noActiveNN' || host == 'noActiveHbaseMaster') {
          item.set('disabled', true);
        } else {
          item.set('disabled', false);
          var protocol = self.setProtocol(item.get('service_id'));
          if (item.get('template')) {
            var port = item.get('http_config') && self.setPort(item, protocol, version);
            item.set('url', item.get('template').fmt(protocol, host, port));
          }
        }
        return item;
      });
    }
    this.set('quickLinks', quickLinks);
  }.observes('App.currentStackVersionNumber', 'App.singleNodeInstall'),

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
