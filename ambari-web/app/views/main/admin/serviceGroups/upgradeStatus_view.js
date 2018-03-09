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

App.UpgradeStatusView = App.DashRow.extend({
  //bound from template
  upgrade: null,

  collapsed: false,

  collapseId: Em.computed.alias('upgrade.id'),

  menuClass: Em.View.extend({
    templateName: require('templates/main/admin/serviceGroups/upgradeStatusMenu'),
    canModifyPlan: Em.computed.equal('parentView.upgrade.currentStep', 'prerequisites'),
    handleEditPlan: function () {
      if (this.get('canModifyPlan')) {
        this.get('parentView.controller').editPlan();
      };
    },
    handleDiscardPlan: function () {
      if (this.get('canModifyPlan')) {
        this.get('parentView.controller').discardPlan();
      };
    }
  }),

  headerClass: Em.View.extend({
    templateName: require('templates/main/admin/serviceGroups/upgradeStatusHeader'),
    isUpgradeActive: Em.computed.equal('parentView.upgrade.currentStep', 'upgrade'),
    isInstallComplete: Em.computed.alias('isUpgradeActive'),
    isInstallActive: Em.computed.equal('parentView.upgrade.currentStep', 'install'),
    isPrerequisitesComplete: Em.computed.or('isInstallActive', 'isUpgradeActive'),
    isPrerequisitesActive: Em.computed.equal('parentView.upgrade.currentStep', 'prerequisites'),
    label: function () {
      switch (this.get('parentView.upgrade.currentStep')) {
        case 'prerequisites':
          return Em.I18n.t('admin.serviceGroups.upgradeStatus.button.prerequisites');
          break;
        case 'install':
          return Em.I18n.t('admin.serviceGroups.upgradeStatus.button.install');
          break;
        case 'upgrade':
          return Em.I18n.t('admin.serviceGroups.upgradeStatus.button.upgrade');
          break;
      }
    }.property('parentView.upgrade.currentStep'),
    action: Em.computed.alias('parentView.upgrade.currentAction')
  }),

  bodyClass: Em.View.extend({
    templateName: require('templates/main/admin/serviceGroups/upgradeStatusBody'),
    history: Em.computed.alias('parentView.upgrade.history'),
    mpacks: Em.computed.alias('parentView.upgrade.mpacks'),
  })
});

