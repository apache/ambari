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
    var hosts = App.Host.find().content.length;
    var allMasters = this.get('controller.content.hostComponents').filterProperty('isMaster').mapProperty('componentName').uniq();
    var disabled = this.get('controller.isStopDisabled');
    switch (service.get('serviceName')) {
      case 'GANGLIA':
      case 'NAGIOS':
        break;
      case 'YARN':
      case 'HDFS':
      case 'MAPREDUCE':
        if (App.supports.reassignMaster && hosts > 1) {
          allMasters.forEach(function (hostComponent) {
            if (App.reassignableComponents.contains(hostComponent)) {
              options.push({action: 'reassignMaster', context: hostComponent,
                'label': Em.I18n.t('services.service.actions.reassign.master').format(App.format.role(hostComponent)), disabled: false});
            }
          })
        }
      default:
        options.push({action: 'runSmokeTest', 'label': Em.I18n.t('services.service.actions.run.smoke'), disabled:disabled});
    }
    if (App.supports.hostOverrides) {
      options.push({action: 'manageConfigurationGroups', 'label': Em.I18n.t('services.service.actions.manage_configuration_groups'), disabled: false});
    }
    return options;
  }.property('controller.content', 'controller.isStopDisabled'),
  isMaintenanceActive: function() {
    return this.get('maintenance').length !== 0;
  }.property('maintenance'),
  hasConfigTab: function(){
    return this.get("controller.content.isConfigurable");
  }.property('controller.content.isConfigurable'),

  didInsertElement: function () {
    this.get('controller').setStartStopState();
  },
  service:function () {
    var svc = this.get('controller.content');
    var svcName = svc.get('serviceName');
    if (svcName) {
      switch (svcName.toLowerCase()) {
        case 'hdfs':
          svc = App.HDFSService.find().objectAt(0);
          break;
        case 'yarn':
          svc = App.YARNService.find().objectAt(0);
          break;
        case 'mapreduce':
          svc = App.MapReduceService.find().objectAt(0);
          break;
        case 'hbase':
          svc = App.HBaseService.find().objectAt(0);
          break;
        case 'flume':
          svc = App.FlumeService.find().objectAt(0);
          break;
        default:
          break;
      }
    }
    return svc;
  }.property('controller.content.serviceName').volatile()
});
