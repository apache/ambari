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

App.WizardConfigureDownloadController = Em.Controller.extend({

  name: 'wizardConfigureDownloadController',

  loadStep: function () {
    let downloadConfig = this.get('content.downloadConfig');
    if (!downloadConfig) {
      this.set('content.downloadConfig', {
        useRedHatSatellite: false,
        useCustomRepo: false,
        useProxy: false,
        proxyUrl: null,
        proxyAuth: null,
        proxyTestPassed: false
      });
    }
  },

  usePublicRepo: function () {
    this.set('content.downloadConfig.useCustomRepo', false);
    this.set('content.downloadConfig.useRedHatSatellite', false);
  },

  useCustomRepo: function () {
    this.set('content.downloadConfig.useCustomRepo', true);
  },

  setProxyAuth: function (authType) {
    this.set('content.downloadConfig.proxyAuth', authType);
    this.proxySettingsChanged();
  },

  proxySettingsChanged: function () {
    this.set('content.downloadConfig.proxyTestPassed', false);
  },

  proxyTest: function () {
    //TODO: mpacks - implement test proxy connection
    this.set('content.downloadConfig.proxyTestPassed', true);
  },

  /**
   * Onclick handler for <code>Next</code> button.
   * Disable 'Next' button while it is already under process. (using Router's property 'nextBtnClickInProgress')
   * @method submit
   */
  submit: function () {
    if (App.get('router.nextBtnClickInProgress')) {
      return;
    }

    App.router.send('next');
  }
});
