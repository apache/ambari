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
 * we use this parameter to defer saving configs before we make some decisions.
 * @param {$.Deferred} dfd
 * @return {App.ModalPopup}
 */
App.showDependentConfigsPopup = function (configs, dfd) {
  if (!configs || configs.length === 0) {
    dfd.resolve();
  }
  return App.ModalPopup.show({
    encodeBody: false,
    primary: Em.I18n.t('common.save'),
    secondary: Em.I18n.t('common.cancel'),
    third: Em.I18n.t('common.discard'),
    header: Em.I18n.t('popup.dependent.configs.header'),
    classNames: ['full-width-modal'],
    configs: configs,
    bodyClass: Em.View.extend({
      templateName: require('templates/common/modal_popups/dependent_configs_list')
    }),
    onPrimary: function () {
      this.hide();
      configs.filterProperty('allowSave', false).forEach(function(c) {
        c.set('value', c.get('defaultValue'));
      });
      dfd.resolve();
    },
    onSecondary: function () {
      App.get('router.mainServiceInfoConfigsController').set('isApplyingChanges', false);
      dfd.reject();
      this.hide();
    },
    onThird: function () {
      App.get('router.mainServiceInfoConfigsController').set('isApplyingChanges', false);
      App.get('router.mainServiceInfoConfigsController').set('preSelectedConfigVersion', null);
      App.get('router.mainServiceInfoConfigsController').onConfigGroupChange();
      dfd.reject();
      this.hide();
    },
    onClose:  function () {
      this.onSecondary();
    }
  });
};
