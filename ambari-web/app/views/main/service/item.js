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

App.MainServiceItemView = Em.View.extend({
  templateName: require('templates/main/service/item'),
  maintenance: function(){
    var options = [];
    var service = this.get('controller.content');
    switch(service.get('serviceName')) {
//      case 'HDFS':
//        options.push({action: 'runRebalancer', 'label': Em.I18n.t('services.service.actions.run.rebalancer')});
//        break;
//      case 'HBASE':
//        options.push({action: 'runCompaction', 'label': Em.I18n.t('services.service.actions.run.compaction')});
//        break;
      case 'GANGLIA':
      case 'NAGIOS':
        if (App.supports.reassignMaster) {
          this.get('controller.content.hostComponents').filterProperty('isMaster').forEach (function (hostComponent){
            options.push({action: 'reassignMaster', context: hostComponent, 'label': Em.I18n.t('services.service.actions.reassign.master').format(hostComponent.get('displayName'))});
          })
        }
        break;
      case 'HIVE':
        options.push({action: 'runSmokeTest', 'label': Em.I18n.t('services.service.actions.run.smoke')});
        if (App.supports.reassignMaster) {
          options.push({action: 'reassignMaster', context: App.HostComponent.find().findProperty('componentName', 'HIVE_METASTORE'), 'label': Em.I18n.t('services.service.actions.reassign.master').format(App.HostComponent.find().findProperty('componentName', 'HIVE_METASTORE').get('displayName'))});
          options.push({action: 'reassignMaster', context: App.HostComponent.find().findProperty('componentName', 'HIVE_SERVER'), 'label': Em.I18n.t('services.service.actions.reassign.master.hive')});
        }
        break;
      case 'WEBHCAT':
        options.push({action: 'runSmokeTest', 'label': Em.I18n.t('services.service.actions.run.smoke')});
        if (App.supports.reassignMaster) {
          options.push({action: 'reassignMaster', context: App.HostComponent.find().findProperty('componentName', 'HIVE_SERVER'), 'label': Em.I18n.t('services.service.actions.reassign.master.hive')});
        }
        break;
      case 'HUE':
        options.push({action: 'runSmokeTest', 'label': Em.I18n.t('services.service.actions.run.smoke')});
        break;
      default:
        var components = this.get('controller.content.hostComponents');
        options.push({action: 'runSmokeTest', 'label': Em.I18n.t('services.service.actions.run.smoke')});
        if (App.supports.reassignMaster) {
          components.filterProperty('isMaster').mapProperty('displayName').uniq().forEach (function (hostComponent){
            options.push({action: 'reassignMaster', context: components.findProperty('displayName', hostComponent), 'label': Em.I18n.t('services.service.actions.reassign.master').format(hostComponent)});
          })
        }
    }
    return options;
  }.property('controller.content'),
  isMaintenanceActive: function() {
    return this.get('maintenance').length !== 0;
  }.property('maintenance'),
  hasConfigTab: function(){
    return this.get("controller.content.isConfigurable");
  }.property('controller.content.isConfigurable')
});