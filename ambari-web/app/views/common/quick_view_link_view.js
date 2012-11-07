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
  quickLinks : function(){
    var serviceName = this.get('content.serviceName');
    var components = this.get('content.components');
    var host;

    if(serviceName === 'hdfs'){
      host = components.filterProperty('componentName', 'NameNode').objectAt(0).get('host.hostName');
    } else if(serviceName === 'mapreduce'){
      host = components.filterProperty('componentName', 'JobTracker').objectAt(0).get('host.hostName');
    } else if(serviceName === 'hbase'){
      host = components.filterProperty('componentName', 'HBase Master').objectAt(0).get('host.hostName');
    }
    if(!host){
      return [];
    }
    return this.get('content.quickLinks').map(function(item){
      if(item.get('url')){
        item.set('url', item.get('url').fmt(host));
      }
      return item;
    });
  }.property('content.quickLinks.@each.label')
});
