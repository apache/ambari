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
 * popup to display requirements that are not met
 * for current action
 * @param data
 * @param header
 * @param failTitle
 * @param failAlert
 * @param warningTitle
 * @param warningAlert
 * @param callback
 * @param configs
 * @param upgradeVersion
 * @returns {*|void}
 */
App.showClusterCheckPopup = function (data, header, failTitle, failAlert, warningTitle, warningAlert, callback, configs, upgradeVersion) {
  var fails = data.items.filterProperty('UpgradeChecks.status', 'FAIL'),
    warnings = data.items.filterProperty('UpgradeChecks.status', 'WARNING'),
    hasConfigsMergeConflicts = !!(configs && configs.length),
    popupBody = {
      failTitle: failTitle,
      failAlert: failAlert,
      warningTitle: warningTitle,
      warningAlert: warningAlert,
      templateName: require('templates/common/modal_popups/cluster_check_dialog'),
      fails: fails,
      warnings: warnings,
      hasConfigsMergeConflicts: hasConfigsMergeConflicts
    };
  if (hasConfigsMergeConflicts) {
    popupBody.configsMergeTable = Em.View.extend({
      templateName: require('templates/main/admin/stack_upgrade/upgrade_configs_merge_table'),
      configs: configs,
      didInsertElement: function () {
        App.tooltip($('.recommended-value'), {
          title: upgradeVersion
        });
      }
    });
  }
  return App.ModalPopup.show({
    primary: fails.length ? Em.I18n.t('common.dismiss') : Em.I18n.t('common.proceedAnyway'),
    secondary: fails.length ? false : Em.I18n.t('common.cancel'),
    header: header,
    classNames: ['cluster-check-popup'],
    bodyClass: Em.View.extend(popupBody),
    onPrimary: function () {
      if (!fails.length && callback) {
        callback();
      }
      this._super();
    }
  });
};
