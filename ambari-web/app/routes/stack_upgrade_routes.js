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
      //if upgrade id is absent then upgrade is completed
      if (Em.isNone(App.db.get('MainAdminStackAndUpgrade', 'upgradeId'))) {
        router.transitionTo('main.admin.stackAndUpgrade.versions');
        return null;
      }

      App.router.get('updateController').set('isWorking', false);

      return App.ModalPopup.show({
        classNames: ['full-width-modal'],
        header: function () {
          var controller = App.router.get('mainAdminStackAndUpgradeController');
          if (controller.get('isDowngrade')) {
            return Em.I18n.t('admin.stackUpgrade.dialog.downgrade.header').format(controller.get('upgradeVersion'));
          } else {
            return Em.I18n.t('admin.stackUpgrade.dialog.header').format(controller.get('upgradeTypeDisplayName'), controller.get('upgradeVersion'));
          }
        }.property('App.router.mainAdminStackAndUpgradeController.upgradeVersion', 'App.router.mainAdminStackAndUpgradeController.isDowngrade'),
        bodyClass: App.upgradeWizardView,
        primary: Em.I18n.t('common.dismiss'),
        secondary: null,
        didInsertElement: function () {
          this.fitHeight();
          this.fitInnerHeight();
        },

        /**
         * fir height of scrollable block inside of modal body
         */
        fitInnerHeight: function () {
          var block = this.$().find('#modal > .modal-body');
          var scrollable = this.$().find('#modal .scrollable-block');

          scrollable.css('max-height', Number(block.css('max-height').slice(0, -2)) - block.height());
          block.css('max-height', 'none');
        },
        onPrimary: function () {
          this.closeWizard();
        },
        onClose: function () {
          this.closeWizard();
        },
        closeWizard: function () {
          App.router.get('updateController').set('isWorking', true);
          App.router.transitionTo('main.admin.stackAndUpgrade.versions');
          this.hide();
          location.reload();
        }
      });
    });
  }
});
