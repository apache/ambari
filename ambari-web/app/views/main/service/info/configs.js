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

App.MainServiceInfoConfigsView = Em.View.extend({
  templateName: require('templates/main/service/info/configs'),
  didInsertElement: function () {
    var controller = this.get('controller');
    controller.loadStep();
  },

  componentsCount: null,
  hostsCount: null,
  isStopCommand:true,

  updateComponentInformation: function() {
    var hc = this.get('controller.content.restartRequiredHostsAndComponents');
    var hostsCount = 0;
    var componentsCount = 0;
    for (var host in hc) {
      hostsCount++;
      componentsCount += hc[host].length;
    }
    this.set('componentsCount', componentsCount);
    this.set('hostsCount', hostsCount);
  }.observes('controller.content.restartRequiredHostsAndComponents'),

  rollingRestartSlaveComponentName : function() {
    return batchUtils.getRollingRestartComponentName(this.get('controller.content.serviceName'));
  }.property('controller.content.serviceName'),

  rollingRestartActionName : function() {
    var label = null;
    var componentName = this.get('rollingRestartSlaveComponentName');
    if (componentName) {
      label = Em.I18n.t('rollingrestart.dialog.title').format(App.format.role(componentName));
    }
    return label;
  }.property('rollingRestartSlaveComponentName')
});
