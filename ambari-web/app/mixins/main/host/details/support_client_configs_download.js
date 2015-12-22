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
    var newWindow = window.open(url);
    newWindow.focus();
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
