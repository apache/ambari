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
 * @param {Object} ajaxOpt - callbek funciton when clicking save
 * @param {Object} message - warning message
 * @return {*}
 */
App.showInvalidKDCPopup = function (ajaxOpt, message) {
  return App.ModalPopup.show({
    primary: Em.I18n.t('common.save'),
    header: Em.I18n.t('popup.invalid.KDC.header'),
    principal: "",
    password: "",
    bodyClass: Em.View.extend({
      warningMsg: message + Em.I18n.t('popup.invalid.KDC.msg'),
      templateName: require('templates/common/modal_popups/invalid_KDC_popup')
    }),
    onClose: function() {
      this.hide();
      if (ajaxOpt.kdcCancelHandler) {
        ajaxOpt.kdcCancelHandler();
      }
    },
    onSecondary: function() {
      this.hide();
      if (ajaxOpt.kdcCancelHandler) {
        ajaxOpt.kdcCancelHandler();
      }
    },
    onPrimary: function () {
      this.hide();
      App.get('router.clusterController').createKerberosAdminSession(this.get('principal'), this.get('password'), ajaxOpt);
    }
  });
};
