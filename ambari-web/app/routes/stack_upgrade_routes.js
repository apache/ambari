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

module.exports = App.WizardRoute.extend({
  route: 'stack/upgrade',

  enter: function (router) {
    console.log('in /admin/stack/upgrade:enter');
    Ember.run.next(function () {
      var upgradeVersion = 'HDP-2.2.1';
      App.router.get('updateController').set('isWorking', false);

      return App.ModalPopup.show({
        header: Em.I18n.t('admin.stackUpgrade.dialog.header').format(upgradeVersion),
        bodyClass: Em.View.extend({
          controllerBinding: 'App.router.mainAdminStackAndUpgradeController',
          templateName: require('templates/main/admin/stack_upgrade/stack_upgrade_dialog'),
          willInsertElement: function () {
            this.startPolling();
          },
          /**
           * start polling upgrade tasks data
           */
          startPolling: function () {
            this.get('controller').loadUpgradeData();
          },
          groups: function () {
            return this.get('controller.upgradeGroups');
          }.property('controller.upgradeGroups')
        }),
        primary: null,
        secondary: null,
        onClose: function() {
          App.router.get('updateController').set('isWorking', true);
          App.router.transitionTo('main.admin.stackAndUpgrade');
          this._super();
          App.ModalPopup.show({
            header: Em.I18n.t('admin.stackUpgrade.state.inProgress'),
            body: Em.I18n.t('admin.stackUpgrade.dialog.close'),
            primary: Em.I18n.t('admin.stackUpgrade.dialog.keepRunning'),
            secondary: Em.I18n.t('admin.stackUpgrade.dialog.stop'),
            secondaryClass: 'btn-danger',
            showCloseButton: false,
            onSecondary: function() {
              App.router.get('mainAdminStackAndUpgradeController').stopUpgrade();
              this._super();
            }
          })
        }
      });
    });
  }
});
