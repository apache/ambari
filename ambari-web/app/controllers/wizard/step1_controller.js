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
        url: '',
        placeholder: 'Enter URL to Version Definition File',
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
    if (this.get("optionsToSelect.usePublicRepo.isSelected")) return;
    if (this.get("optionsToSelect.useLocalRepo.isSelected") && this.get("optionsToSelect.useLocalRepo.enterUrl.isSelected")) {
      var url = this.get("optionsToSelect.useLocalRepo.enterUrl.url");
      data = {
        "VersionDefinition": {
          "version_url": url
        }
      };
      App.db.setLocalRepoVDFData(url);
    } else if (this.get("optionsToSelect.useLocalRepo.uploadFile.isSelected")) {
      isXMLdata = true;
      // load from file browser
      data = this.get("optionsToSelect.useLocalRepo.uploadFile.file");
      App.db.setLocalRepoVDFData(data);
    }
    var installerController = App.router.get('installerController');
    var self = this;
    installerController.postVersionDefinitionFile(isXMLdata, data).done(function (response) {
      self.set('latestSelectedLocalRepoId', response.stackNameVersion + "-" + response.actualVersion);
      // load successfully, so make this local stack repo as selectedStack
      self.get('content.stacks').setEach('isSelected', false);
      self.get('content.stacks').findProperty('id', response.stackNameVersion + "-" + response.actualVersion).set('isSelected', true);
      Ember.run.next(function () {
        $("[rel=skip-validation-tooltip]").tooltip({ placement: 'right'});
        $("[rel=use-redhat-tooltip]").tooltip({ placement: 'right'});
      });
    });
  },

  /**
   * On click handler for removing OS
   */
  removeOS: function(event) {
    if (this.get('selectedStack.useRedhatSatellite')) {
      return;
    }
    var osToRemove = event.context;
    Em.set(osToRemove, 'isSelected', false);
  },

  /**
   * On click handler for adding new OS
   */
  addOS: function(event) {
    var osToAdd = event.context;
    Em.set(osToAdd, 'isSelected', true);
  },

  changeUseRedhatSatellite: function () {
    if (App.router.get('installerController.currentStep') !== "1") {
      return;
    }
    if (this.get('selectedStack.useRedhatSatellite')) {
      return App.ModalPopup.show({
        header: Em.I18n.t('common.important'),
        secondary: false,
        bodyClass: Ember.View.extend({
          template: Ember.Handlebars.compile(Em.I18n.t('installer.step1.advancedRepo.useRedhatSatellite.warning'))
        })
      });
    }
  }.observes('selectedStack.useRedhatSatellite')
});
