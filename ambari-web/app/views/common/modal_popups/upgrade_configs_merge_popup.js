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
 * Popup with configs merge warnings from Rolling Upgrade prerequisite checks
 * @param conflicts
 * @param version
 * @param callback
 * @returns {App.ModalPopup}
 */
App.showUpgradeConfigsMergePopup = function (conflicts, version, callback) {
  var configs = conflicts.map(function (item) {
    var isDeprecated = Em.isNone(item.new_stack_value),
      willBeRemoved = Em.isNone(item.result_value);
    return {
      type: item.type,
      name: item.property,
      currentValue: item.current,
      recommendedValue: isDeprecated ? Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.deprecated') : item.new_stack_value,
      isDeprecated: isDeprecated,
      resultingValue: willBeRemoved ? Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.willBeRemoved') : item.result_value,
      willBeRemoved: willBeRemoved
    };
  });
  return App.ModalPopup.show({
    classNames: ['configs-merge-warnings'],
    primary: Em.I18n.t('common.proceedAnyway'),
    secondary: Em.I18n.t('form.cancel'),
    header: Em.I18n.t('popup.clusterCheck.Upgrade.header').format(version),
    bodyClass: Em.View.extend({
      templateName: require('templates/common/modal_popups/upgrade_configs_merge_popup'),
      configs: configs,
      didInsertElement: function () {
        App.tooltip($('.recommended-value'), {
          title: version
        });
      }
    }),
    onPrimary: function () {
      if (callback) {
        callback();
      }
      this._super();
    }
  });
};
