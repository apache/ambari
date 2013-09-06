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

App.QuickViewLinks = Em.View.extend({

  ambariProperties: function() {
    return App.router.get('clusterController.ambariProperties');
  },
  /**
   * Updated quick links. Here we put correct hostname to url
   */
  quickLinks: function () {
    var serviceName = this.get('content.serviceName');
    var components = this.get('content.hostComponents');
    var host;
    var self = this;

    switch (serviceName) {
      case "HDFS":
        host = App.singleNodeInstall ? App.singleNodeAlias : components.findProperty('componentName', 'NAMENODE').get('host.publicHostName');
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
          component = components.filterProperty('componentName', 'HBASE_MASTER').findProperty('haStatus', 'active');
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
      return [
        {
          label: this.t('quick.links.error.label'),
          url: 'javascript:alert("' + this.t('contact.administrator') + '");return false;'
        }
      ];
    }
    return this.get('content.quickLinks').map(function (item) {
      var protocol = self.setProtocol(item.get('service_id'));
      if (item.get('url')) {
        item.set('url', item.get('url').fmt(protocol,host));
      }
      return item;
    });
  }.property('content.quickLinks.@each.label'),

  setProtocol: function(service_id){
    var properties  = this.ambariProperties();
    switch(service_id){
      case "GANGLIA":
        return (properties && properties.hasOwnProperty('ganglia.https') && properties['ganglia.https']) ? "https" : "http";
        break;
      case "NAGIOS":
        return (properties && properties.hasOwnProperty('nagios.https') && properties['nagios.https']) ? "https" : "http";
        break;
      default:
        return "http";
    }
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
