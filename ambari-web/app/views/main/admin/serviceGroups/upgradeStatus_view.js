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
    canModifyPlan: function () {
      const step = this.get('parentView.upgrade.currentStep');
      
      if (step === 'prerequisites') {
        return true;
      }
      
      return false;
    }.property('parentView.upgrade.currentStep'),
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
    step: Em.computed.alias('parentView.upgrade.currentStep'),
    isPrerequisitesActive: function () {
      const step = this.get('step');
      
      if (step === 'prerequisites') {
        return true;
      }

      return false;
    }.property('step'),
    isPrerequisitesComplete: function () {
      const step = this.get('step');
      
      if (step === 'install' || step === 'upgrade') {
        return true;
      }

      return false;
    }.property('step'),
    isInstallActive: function () {
      const step = this.get('step');
      
      if (step === 'install') {
        return true;
      }

      return false;
    }.property('step'),
    isInstallComplete: function () {
      const step = this.get('step');
      
      if (step === 'upgrade') {
        return true;
      }

      return false;
    }.property('step'),
    isUpgradeActive: function () {
      const step = this.get('step');
      
      if (step === 'upgrade') {
        return true;
      }

      return false;
    }.property('step'),
    label: function () {
      const step = this.get('step');
      switch (step) {
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
    }.property('step'),
    action: Em.computed.alias('parentView.upgrade.currentAction')
  }),

  bodyClass: Em.View.extend({
    templateName: require('templates/main/admin/serviceGroups/upgradeStatusBody'),
    history: Em.computed.alias('parentView.upgrade.history'),
    mpacks: Em.computed.alias('parentView.upgrade.mpacks'),
  })
});

