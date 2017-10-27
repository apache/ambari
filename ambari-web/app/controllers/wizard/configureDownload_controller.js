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

  optionsToSelect: {
    'usePublicRepo': {
      index: 0,
      isSelected: true
    },
    'useLocalRepo': {
      index: 1,
      isSelected: false,
      'uploadFile': {
        index: 0,
        name: 'uploadFile',
        file: '',
        hasError: false,
        isSelected: true
      },
      'enterUrl': {
        index: 1,
        name: 'enterUrl',
        url: '',
        placeholder: Em.I18n.t('installer.step1.useLocalRepo.enterUrl.placeholder'),
        hasError: false,
        isSelected: false
      }
    }
  },

  loadStep: function () {
    if (!this.get('content.downloadConfig')) {
      let downloadConfig = this.get('wizardController').getDBProperty('downloadConfig');

      if (!downloadConfig) {
        downloadConfig = {
          useRedhatSatellite: false,
          usePublicRepo: true
        };
      }

      this.set('content.downloadConfig', downloadConfig);
    }
  },

  /**
   * Restore base urls for selected stack when user select to use public repository
   */
  usePublicRepo: function () {
    this.set('content.downloadConfig', {
      useRedhatSatellite: false,
      usePublicRepo: true
    });
  },

  useLocalRepo: function () {
    this.set('content.downloadConfig', {
      useRedhatSatellite: false,
      usePublicRepo: false
    });
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

    this.get('wizardController').setDBProperty('downloadConfig', this.get('content.downloadConfig'));

    App.router.send('next');
  }
});
