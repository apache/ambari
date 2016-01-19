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

/**
 * Show confirmation popup
 * @param {[Object]} configs
 * @param {function} [primary=null]
 * @param {function} [secondary=null]
 * we use this parameter to defer saving configs before we make some decisions.
 * @return {App.ModalPopup}
 */
App.showDependentConfigsPopup = function (configs, primary, secondary) {
  return App.ModalPopup.show({
    encodeBody: false,
    header: Em.I18n.t('popup.dependent.configs.header'),
    classNames: ['sixty-percent-width-modal','modal-full-width'],
    configs: configs,
    secondaryClass: 'cancel-button',
    bodyClass: Em.View.extend({
      templateName: require('templates/common/modal_popups/dependent_configs_list'),

      toggleAll: Em.Checkbox.extend({
        didInsertElement: function () {
          this.updateCheckbox();
        },
        click: function () {
          Em.run.next(this, 'updateSaveRecommended');
        },
        updateCheckboxObserver: function () {
          Em.run.once(this, 'updateCheckbox');
        }.observes('parentView.parentView.configs.@each.saveRecommended'),

        updateCheckbox: function() {
          this.set('checked', !(this.get('parentView.parentView.configs')|| []).someProperty('saveRecommended', false));
        },
        updateSaveRecommended: function() {
          this.get('parentView.parentView.configs').setEach('saveRecommended', this.get('checked'));
        }
      })
    }),
    onPrimary: function () {
      this._super();
      var changedConfigs = this.get('configs').filter(function(c) {
        return Em.get(c, 'saveRecommendedDefault') != Em.get(c, 'saveRecommended');
      });
      changedConfigs.forEach(function (c) {
        Em.set(c, 'saveRecommendedDefault', Em.get(c, 'saveRecommended'));
        Em.set(c,'forceUpdate', true);
      });
      if (primary) {
        primary();
      }
    },
    onSecondary: function() {
      this._super();
      var changedConfigs = this.get('configs').filter(function(c) {
        return Em.get(c, 'saveRecommendedDefault') != Em.get(c, 'saveRecommended');
      });
      changedConfigs.forEach(function(c) {
        Em.set(c, 'saveRecommended', Em.get(c, 'saveRecommendedDefault'));
        Em.set(c, 'forceUpdate', true);
      });
      if (secondary) {
        secondary();
      }
    },
    onClose: function () {
      this._super();
      if (secondary) {
        secondary();
      }
    }
  });
};
