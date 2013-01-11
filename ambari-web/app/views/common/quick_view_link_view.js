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

  /**
   * Updated quick links. Here we put correct hostname to url
   */
  quickLinks:function () {
    var serviceName = this.get('content.serviceName');
    var components = this.get('content.components');
    var host;

    if (serviceName === 'HDFS') {
      host = components.filterProperty('id', 'NAMENODE').objectAt(0).get('host.publicHostName');
    } else if (serviceName === 'MAPREDUCE') {
      host = components.filterProperty('id', 'JOBTRACKER').objectAt(0).get('host.publicHostName');
    } else if (serviceName === 'HBASE') {
      host = components.filterProperty('id', 'HBASE_MASTER').objectAt(0).get('host.publicHostName');
    }
    if (!host) {
      return [];
    }
    return this.get('content.quickLinks').map(function (item) {
      if (item.get('url')) {
        item.set('url', item.get('url').fmt(host));
      }
      return item;
    });
  }.property('content.quickLinks.@each.label'),

  linkTarget:function () {
    switch (this.get('content.serviceName').toLowerCase()) {
      case "hdfs":
      case "mapreduce":
      case "hbase":
        return "_blank";
        break;
      default:
        return "";
        break;
    }
  }.property('service')

});
