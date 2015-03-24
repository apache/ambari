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
 * @param {function} [secondaryCallback=null]
 * we use this parameter to defer saving configs before we make some decisions.
 * @return {App.ModalPopup}
 */
App.showDependentConfigsPopup = function (configs, callback, secondaryCallback) {
  return App.ModalPopup.show({
    encodeBody: false,
    primary: Em.I18n.t('common.save'),
    secondary: Em.I18n.t('common.cancel'),
    header: Em.I18n.t('popup.dependent.configs.header'),
    classNames: ['full-width-modal'],
    configs: configs,
    bodyClass: Em.View.extend({
      templateName: require('templates/common/modal_popups/dependent_configs_list')
    }),
    stepConfigs: function() {
      return App.get('router.mainServiceInfoConfigsController.stepConfigs').objectAt(0).get('configs');
    }.property('controller.stepConfigs.@each'),
    onPrimary: function () {
      this.hide();
      configs.filterProperty('saveRecommended', true).forEach(function(c) {
        c.set('value', c.get('recommendedValue'));
        var stepConfig = this.get('stepConfigs').find(function(stepConf) {
          return stepConf.get('name') === c.get('name') && stepConf.get('filename') === c.get('fileName');
        });
        if (stepConfig) {
          stepConfig.set('value', c.get('recommendedValue'));
        }
      }, this);
      if (callback) {
        callback();
      }
    },
    onSecondary: function() {
      this.hide();
      if(secondaryCallback) {
        secondaryCallback();
      }
    }
  });
};
