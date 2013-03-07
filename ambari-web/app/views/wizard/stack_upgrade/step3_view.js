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

App.StackUpgradeStep3View = Em.View.extend({
  templateName: require('templates/wizard/stack_upgrade/step3'),
  didInsertElement: function(){
    this.get('controller').navigateStep();
    this.onStatus();
  },
  statusMessage: null,
  statusClass: 'alert-info',
  barColor: '',
  servicesStopWidth: function(){
    return 'width: ' + this.get('controller.servicesStopProgress') + '%;';
  }.property('controller.servicesStopProgress'),
  /**
   * change message of Upgrade status depending on the Upgrade status
   */
  onStatus: function(){
    switch (this.get('controller.status')){
      case 'SUCCESS':
        this.set('statusMessage', Em.I18n.t('installer.stackUpgrade.step3.status.success').format(this.get('controller.content.upgradeVersion')));
        this.set('statusClass', 'alert-success');
        break;
      case 'FAILED':
        this.set('statusMessage', Em.I18n.t('installer.stackUpgrade.step3.status.failed'));
        this.set('statusClass', 'alert-error');
        break;
      case 'WARNING':
        this.set('statusMessage', Em.I18n.t('installer.stackUpgrade.step3.status.warning').format(this.get('controller.content.upgradeVersion')));
        this.set('statusClass', 'alert-block');
        break;
      case 'IN_PROGRESS':
      default:
        this.set('statusMessage', Em.I18n.t('installer.stackUpgrade.step3.status.info'));
        this.set('statusClass', 'alert-info');
    }
  }.observes('controller.status'),

  serviceView: Em.View.extend({
    barColor: '',
    icon:'',
    iconColor:'',
    didInsertElement: function(){
      this.onStatus();
    },
    isServiceCompleted: function(){
      return this.get('content.status') === 'SUCCESS';
    }.property('content.status'),
    barWidth: function(){
      return 'width: ' + this.get('content.progress') + '%;';
    }.property('content.progress'),
    /**
     * change service appearance(icon, progress-bar color,) depending on the service status
     */
    onStatus:function () {
      if (this.get('content.status') === 'IN_PROGRESS') {
        this.set('barColor', 'progress-info');
        this.set('icon', 'icon-cog');
        this.set('iconColor', 'text-info');
      } else if (this.get('content.status') === 'WARNING') {
        this.set('barColor', 'progress-warning');
        this.set('icon', 'icon-warning-sign');
        this.set('iconColor', 'text-warning');
      } else if (this.get('content.status') === 'FAILED') {
        this.set('barColor', 'progress-danger');
        this.set('icon', 'icon-exclamation-sign');
        this.set('iconColor', 'text-error');
      } else if (this.get('content.status') === 'SUCCESS') {
        this.set('barColor', 'progress-success');
        this.set('icon', 'icon-ok');
        this.set('iconColor', 'text-success');
      } else {
        this.set('barColor', 'progress-info');
        this.set('icon', 'icon-cog');
        this.set('iconColor', '');
      }
    }.observes('content.status'),
    inProgress: function(){
      return this.get('content.status') === "IN_PROGRESS";
    }.property('content.status'),
    /**
     * open popup with list of hosts, that associated to service
     * @param event
     */
    hostsLogPopup: function(event){
      //TODO show popup with hosts
      var serviceName = event.contexts[0];
    }
  })
});