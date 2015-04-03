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
 * @param {function} [callback=null]
 * we use this parameter to defer saving configs before we make some decisions.
 * @return {App.ModalPopup}
 */
App.showDependentConfigsPopup = function (configs, callback) {
  return App.ModalPopup.show({
    encodeBody: false,
    header: Em.I18n.t('popup.dependent.configs.header'),
    classNames: ['sixty-percent-width-modal','modal-full-width'],
    configs: configs,
    bodyClass: Em.View.extend({
      templateName: require('templates/common/modal_popups/dependent_configs_list')
    }),
    stepConfigs: function() {
      return App.get('router.mainServiceInfoConfigsController.stepConfigs').objectAt(0).get('configs');
    }.property('controller.stepConfigs.@each'),
    onPrimary: function () {
      this._super();
      configs.forEach(function(c) {
        Em.set(c, 'saveRecommendedDefault', Em.get(c, 'saveRecommended'));
      });
      if (callback) {
        callback();
      }
    },
    onSecondary: function() {
      this._super();
      configs.forEach(function(c) {
        Em.set(c, 'saveRecommended', Em.get(c, 'saveRecommendedDefault'));
      });
    }
  });
};
