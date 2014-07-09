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
var batchUtils = require('utils/batch_scheduled_requests');

App.MainServiceItemView = Em.View.extend({
  templateName: require('templates/main/service/item'),
  maintenance: function(){
    var options = [];
    var service = this.get('controller.content');
    var hosts = App.router.get('mainHostController.hostsCountMap')['TOTAL'];
    var allMasters = this.get('controller.content.hostComponents').filterProperty('isMaster').mapProperty('componentName').uniq();
    var disabled = this.get('controller.isStopDisabled');
    var serviceName = service.get('serviceName');
    var displayName = service.get('displayName');
    var disableRefreshConfgis = !service.get('isRestartRequired');

    if (this.get('controller.isClientsOnlyService')) {
      if (serviceName != 'TEZ') {
        options.push({action: 'runSmokeTest', cssClass: 'icon-thumbs-up-alt', 'label': Em.I18n.t('services.service.actions.run.smoke')});
      }
      options.push({action: 'refreshConfigs', cssClass: 'icon-refresh', 'label': Em.I18n.t('hosts.host.details.refreshConfigs'), disabled: disableRefreshConfgis});
    } else {
      // Restart All action
      options.push({action:'restartAllHostComponents', cssClass: 'icon-repeat', context: serviceName, 'label': Em.I18n.t('restart.service.all'), disabled: false});
      // Rolling Restart action
      var rrComponentName = batchUtils.getRollingRestartComponentName(serviceName);
      if (rrComponentName) {
        var label = Em.I18n.t('rollingrestart.dialog.title').format(App.format.role(rrComponentName));
        options.push({action:'rollingRestart', cssClass: 'icon-time', context: rrComponentName, 'label': label, disabled: false});
      }
      if (serviceName == 'FLUME') {
        options.push({action: 'refreshConfigs', cssClass: 'icon-refresh', 'label': Em.I18n.t('hosts.host.details.refreshConfigs'), disabled: disableRefreshConfgis});
      }
      if (serviceName == 'HDFS') {
        if (App.isHaEnabled) {
          if (App.supports.autoRollbackHA) {
            options.push({action: 'disableHighAvailability', cssClass: 'icon-arrow-down', 'label': Em.I18n.t('admin.highAvailability.button.disable')});
          }
        } else {
          options.push({action: 'enableHighAvailability', cssClass: 'icon-arrow-up', 'label': Em.I18n.t('admin.highAvailability.button.enable')});
        }
      }
      // Service Check and Reassign Master actions
      switch (serviceName) {
        case 'GANGLIA':
        case 'NAGIOS':
          break;
        case 'YARN':
        case 'HDFS':
        case 'MAPREDUCE':
          if (App.supports.reassignMaster && hosts > 1) {
            allMasters.forEach(function (hostComponent) {
              if (App.get('components.reassignable').contains(hostComponent)) {
                options.push({action: 'reassignMaster', context: hostComponent, cssClass: 'icon-share-alt',
                  'label': Em.I18n.t('services.service.actions.reassign.master').format(App.format.role(hostComponent)), disabled: false});
              }
            })
          }
        default:
          options.push({action: 'runSmokeTest', cssClass: 'icon-thumbs-up-alt', 'label': Em.I18n.t('services.service.actions.run.smoke'), disabled:disabled});
      }
      var requestLabel = service.get('passiveState') === "OFF" ?
          Em.I18n.t('passiveState.turnOnFor').format(displayName) :
          Em.I18n.t('passiveState.turnOffFor').format(displayName);
      var passiveLabel = service.get('passiveState') === "OFF" ?
          Em.I18n.t('passiveState.turnOn') :
          Em.I18n.t('passiveState.turnOff');
      options.push({action:'turnOnOffPassive', cssClass: 'icon-medkit', context:requestLabel, 'label':passiveLabel , disabled: false});
    }
    return options;
  }.property('controller.content', 'controller.isStopDisabled','controller.isClientsOnlyService'),
  isMaintenanceActive: function() {
    return this.get('maintenance').length !== 0;
  }.property('maintenance'),
  hasConfigTab: function() {
    return !App.get('services.noConfigTypes').concat('HCATALOG').contains('controller.content.serviceName');
  }.property('controller.content.serviceName','App.services.noConfigTypes'),

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
