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


  loadTags: function() {
    App.ajax.send({
      name: 'config.tags.sync',
      sender: this,
      success: 'loadTagsSuccess',
      error: 'loadTagsError'
    });
  },

  loadTagsSuccess: function(data) {
    var tags = [];
    for( var prop in data.Clusters.desired_configs){
      tags.push(Em.Object.create({
        siteName: prop,
        tagName: data.Clusters.desired_configs[prop]['tag']
      }));
    }
    this.set('actualTags', tags);
    this.getSecurityProperties();
  },

  actualTags: [],

  securityProperties: [],

  /**
   * list of files that contains properties for enabling/disabling ssl
   */
  requiredSiteNames: ['core-site'],

  getSecurityProperties: function () {
    this.set('securityProperties', []);
    var requiredSiteNames = this.get('requiredSiteNames');
    var tags = this.get('actualTags').filter(function(tag){
      return requiredSiteNames.contains(tag.siteName);
    });
    var data = App.router.get('configurationController').getConfigsByTags(tags);
    var properties = this.get('securityProperties');
    var coreSiteProperties = data.findProperty('type', 'core-site');
    if(coreSiteProperties) {
      properties.pushObject(coreSiteProperties);
      this.set('securityProperties', properties);
    }
  },

  ambariProperties: function() {
    return App.router.get('clusterController.ambariProperties');
  },
  /**
   * Updated quick links. Here we put correct hostname to url
   */
  quickLinks: [],

  didInsertElement: function() {
    this.setQuickLinks();
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
        if ( this.get('content.snameNode')) { // not HA
          host = App.singleNodeInstall ? App.singleNodeAlias : components.findProperty('componentName', 'NAMENODE').get('host.publicHostName');
        } else {
          // HA
          if (this.get('content.activeNameNode')) {
            host = this.get('content.activeNameNode.publicHostName');
          }else {
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
        }
        break;
      case "YARN":
        host = App.singleNodeInstall ? App.singleNodeAlias : components.findProperty('componentName', 'RESOURCEMANAGER').get('host.publicHostName');
        break;
      case "MAPREDUCE2":
        host = App.singleNodeInstall ? App.singleNodeAlias : components.findProperty('componentName', 'HISTORYSERVER').get('host.publicHostName');
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
      if (host == 'noActiveNN') {
        item.set('disabled', true);
      } else {
        item.set('disabled', false);
        var protocol = self.setProtocol(item.get('service_id'));
        if (item.get('template')) {
          if(item.get('service_id') === 'YARN'){
            var port = self.setPort(item.get('service_id'),protocol, version);
            item.set('url', item.get('template').fmt(protocol,host,port));
          } else {
            item.set('url', item.get('template').fmt(protocol,host));
          }
        }
      }
      return item;
    });
    }
    this.set('quickLinks',quickLinks);
  }.observes('content.quickLinks.@each.label'),

  setProtocol: function(service_id){
    var properties  = this.ambariProperties();
    var securityProperties = this.get('securityProperties');
    var hadoopSslEnabled = false;
    if(securityProperties) {
      securityProperties.forEach(function(property){
        property['hadoop.ssl.enabled'] && property['hadoop.ssl.enabled'] === 'true' ?  hadoopSslEnabled = true : null;
      });
    }
    switch(service_id){
      case "GANGLIA":
        return (properties && properties.hasOwnProperty('ganglia.https') && properties['ganglia.https']) ? "https" : "http";
        break;
      case "NAGIOS":
        return (properties && properties.hasOwnProperty('nagios.https') && properties['nagios.https']) ? "https" : "http";
        break;
      case "HDFS":
      case "YARN":
      case "MAPREDUCE":
      case "MAPREDUCE2":
      case "HBASE":
        return hadoopSslEnabled ? "https" : "http";
        break;
      default:
        return "http";
    }
  },

  setPort: function(service_id, protocol, version) {
    var port = '';
    if (service_id === 'YARN') {
      port = (protocol === 'https' && stringUtils.compareVersions(version,'2.0.5') === 1) ? '8090' : '8088'
    }
    return port;
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
        return "_blank";
        break;
      default:
        return "";
        break;
    }
  }.property('service')

});
