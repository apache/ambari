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

App.SupportClientConfigsDownload = Em.Mixin.create({

  /**
   *
   * @param {{hostName: string, componentName: string, displayName: string, serviceName: string}} data
   */
  downloadClientConfigsCall: function (data) {
    var url = this._getUrl(data.hostName, data.serviceName, data.componentName);
    try {
      var self = this;
      $.fileDownload(url).fail(function (error) {
        var errorMessage = '';
        var isNoConfigs = false;
        if (error && $(error).text()) {
          var errorObj = JSON.parse($(error).text());
          if (errorObj && errorObj.message && errorObj.status) {
            isNoConfigs = errorObj.message.indexOf(Em.I18n.t('services.service.actions.downloadClientConfigs.fail.noConfigFile')) !== -1;
            errorMessage += isNoConfigs ? Em.I18n.t('services.service.actions.downloadClientConfigs.fail.noConfigFile') :
              Em.I18n.t('services.service.actions.downloadClientConfigs.fail.popup.body.errorMessage').format(data.displayName, errorObj.status, errorObj.message);
          }
          else {
            errorMessage += Em.I18n.t('services.service.actions.downloadClientConfigs.fail.popup.body.noErrorMessage').format(data.displayName);
          }
          errorMessage += isNoConfigs ? '' : Em.I18n.t('services.service.actions.downloadClientConfigs.fail.popup.body.question');
        }
        else {
          errorMessage += Em.I18n.t('services.service.actions.downloadClientConfigs.fail.popup.body.noErrorMessage').format(data.displayName) +
            Em.I18n.t('services.service.actions.downloadClientConfigs.fail.popup.body.question');
        }
        return App.ModalPopup.show({
          header: Em.I18n.t('services.service.actions.downloadClientConfigs.fail.popup.header').format(data.displayName),
          bodyClass: Em.View.extend({
            template: Em.Handlebars.compile(errorMessage)
          }),
          secondary: isNoConfigs ? false : Em.I18n.t('common.cancel'),
          onPrimary: function () {
            this.hide();
            if (!isNoConfigs) {
              self.downloadClientConfigs({
                context: Em.Object.create(data)
              })
            }
          }
        });
      });
    } catch (err) {
      var newWindow = window.open(url);
      newWindow.focus();
    }
  },

  /**
   *
   * @param {string|null} hostName
   * @param {string} serviceName
   * @param {string} componentName
   * @returns {string}
   * @private
   */
  _getUrl: function (hostName, serviceName, componentName) {
    var isForHost = !Em.isNone(hostName);
    return App.get('apiPrefix') + '/clusters/' + App.router.getClusterName() + '/' +
      (isForHost ? 'hosts/' + hostName + '/host_components/' : 'services/' + serviceName + '/components/') +
      componentName + '?format=client_config_tar';
  }

});