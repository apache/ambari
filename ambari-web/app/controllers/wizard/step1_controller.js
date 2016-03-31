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

App.WizardStep1Controller = Em.Controller.extend({

  name: 'wizardStep1Controller',
  /**
   * Skip repo-validation
   * @type {bool}
   */
  skipValidationChecked: false,

  useRedhatSatellite: false,

  selectedStack: function() {
    return App.Stack.find().findProperty('isSelected');
  }.property('content.stacks.@each.isSelected'),

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
        url: 'http://',
        hasError: false,
        isSelected: false
      }
    }
  },

  /**
   * Used to set version definition file from FileUploader
   * @method setVDFFile
   * @param {string} vdf
   */
  setVDFFile: function (vdf) {
    this.set("optionsToSelect.useLocalRepo.uploadFile.file", vdf);
  },

  /**
   * Load selected file to current page content
   */
  readVersionInfo: function(){
    var data = {};
    var isXMLdata = false;
    var loadedVersionInfo = {};
    if (this.get("optionsToSelect.usePublicRepo.isSelected")) return;
    if (this.get("optionsToSelect.useLocalRepo.isSelected") && this.get("optionsToSelect.useLocalRepo.enterUrl.isSelected")) {
      var url = this.get("optionsToSelect.useLocalRepo.enterUrl.url");
      data = {
        "VersionDefinition": {
          "version_url": url
        }
      };
    } else if (this.get("optionsToSelect.useLocalRepo.uploadFile.isSelected")) {
      isXMLdata = true;
      // load from file browser
      data = this.get("optionsToSelect.useLocalRepo.uploadFile.file");
    }

    var installerController = App.router.get('installerController');
    var self = this;
    installerController.postVersionDefinitionFile(isXMLdata, data).done(function (versionInfo) {
      if (versionInfo.id && versionInfo.stackName && versionInfo.stackVersion) {
        installerController.getRepoById(versionInfo.id, versionInfo.stackName, versionInfo.stackVersion).done(function(response) {
          loadedVersionInfo.id = response.id;
          loadedVersionInfo.isPatch = response.type == 'PATCH';
          loadedVersionInfo.stackNameVersion = response.stackNameVersion;
          loadedVersionInfo.displayName = response.displayName;
          loadedVersionInfo.version = response.version || 'n/a';
          loadedVersionInfo.actualVersion = response.actualVersion || 'n/a';
          loadedVersionInfo.updateObj = response.updateObj;
          loadedVersionInfo.upgradeStack = {
            stack_name: response.stackName,
            stack_version: response.stackVersion,
            display_name: response.displayName
          };
          loadedVersionInfo.services = response.services || [];
          loadedVersionInfo.repoVersionFullName = response.repoVersionFullName;
          self.set('loadedVersionInfo', loadedVersionInfo);
          self.set('latestSelectedLocalRepoId', response.repoVersionFullName);

          Ember.run.next(function () {
            $("[rel=skip-validation-tooltip]").tooltip({ placement: 'right'});
            $("[rel=use-redhat-tooltip]").tooltip({ placement: 'right'});
          });
          // load successfully, so make this local stack repo as selectedStack
          self.get('content.stacks').setEach('isSelected', false);
          self.get('content.stacks').findProperty('id', response.repoVersionFullName).set('isSelected', true);
        })
      }
    });
  },

  /**
   * On click handler for removing OS
   */
  removeOS: function(event) {
    var osToRemove = event.context;
    Em.set(osToRemove, 'isSelected', false);
  },

  /**
   * On click handler for adding new OS
   */
  addOS: function(event) {
    var osToAdd = event.context;
    Em.set(osToAdd, 'isSelected', true);
  }
});
