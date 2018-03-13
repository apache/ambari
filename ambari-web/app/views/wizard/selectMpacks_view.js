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

App.WizardSelectMpacksView = Em.View.extend({
  templateName: require('templates/wizard/selectMpacks'),

  didInsertElement: function () {
    this.get('controller').loadStep();

    //enable initial tooltips
    $('[data-toggle="tooltip"]').tooltip();
    //enables tooltips added later
    const target = document.querySelector('#select-mpacks');
    const observer = new MutationObserver(() => {
      $('[data-toggle="tooltip"]').tooltip();
    });
    observer.observe(target, { childList: true, subtree: true });
  },

  toggleMode: function () {
    const isAdvancedMode = this.get('controller.content.advancedMode');
    const controller = this.get('controller');
    const toggleMode = controller.toggleMode.bind(controller);

    if (isAdvancedMode) { //toggling to Basic (Use Cases) Mode
      const selectedServices = controller.get('selectedServices');
      if (selectedServices.length > 0) {
        this.showToggleToBasicBox(toggleMode);
      } else {
        toggleMode();
      }
    } else { //toggling to Advanced (Mpacks/Services) Mode
      const selectedUseCases = controller.get('selectedUseCases');
      if (selectedUseCases.length > 0) {
        this.showToggleToAdvancedBox(toggleMode);
      } else {
        toggleMode();
      }  
    }
  },

  showToggleToAdvancedBox: function (callback) {
    App.ModalPopup.show({
      primary: Em.I18n.t('common.cancel'),
      secondary: Em.I18n.t('ok'),
      header: Em.I18n.t('installer.selectMpacks.changeMode'),
      body: Em.I18n.t('installer.selectMpacks.basicModeMessage'),
      showCloseButton: false,
      onPrimary: function () {
        this._super();
      },
      onSecondary: function () {
        this._super();
        callback();
      }
    });
  },

  showToggleToBasicBox: function (callback) {
    App.ModalPopup.show({
      primary: Em.I18n.t('common.cancel'),
      secondary: Em.I18n.t('ok'),
      header: Em.I18n.t('installer.selectMpacks.changeMode'),
      body: Em.I18n.t('installer.selectMpacks.advancedModeMessage'),
      showCloseButton: false,
      onPrimary: function () {
        this._super();
      },
      onSecondary: function () {
        this._super();
        callback();
      }
    });
  }
})
  
/**
 * View for each use case in the registry
 */
App.WizardUsecaseView = Em.View.extend({
  templateName: require('templates/wizard/selectMpacks/usecase'),

  /**
   * Handle add/remove button clicked
   */
  toggle: function () {
    this.get('controller').toggleUsecaseHandler(this.get('usecase.id'));
  }
});

/**
 * View for each mpack in the registry
 */
App.WizardMpackView = Em.View.extend({
  templateName: require('templates/wizard/selectMpacks/mpack'),

  services: function () {
    return this.get('mpack.versions').filterProperty('displayed')[0].services;
  }.property('mpack.versions.@each.displayed'),

  /**
   * Handle mpack version changed
   * 
   * @param {any} event 
   */
  changeVersion: function (event) {
    const versionId = event.target.value;
    this.get('controller').displayMpackVersion(versionId);
  },

  /**
   * Handle add service button clicked
   *
   * @param  {type} event
   */
  addService: function (event) {
    const serviceId = event.context;
    this.get('controller').addServiceHandler(serviceId);
  },

  addMpack: function (event) {
    const version = this.get('mpack.versions').filterProperty('displayed')[0];
    this.get('controller').addMpackHandler(version.get('id'));
  }
});

/**
 * View for each service in the registry
 */
App.WizardServiceView = Em.View.extend({
  templateName: require('templates/wizard/selectMpacks/service'),

  /**
   * Handle add button clicked
   */
  add: function () {
    const service = this.get('service.versions').filterProperty('displayed')[0];
    this.get('controller').addServiceHandler(service.get('id'));
  },

  /**
   * Handle service version changed
   * 
   * @param {any} event 
   */
  changeVersion: function (event) {
    const versionId = event.target.value;
    this.get('controller').displayServiceVersion(versionId);
  },
});

/**
 * View for each selected mpack
 */
App.WizardSelectedMpackVersionView = Em.View.extend({
  templateName: require('templates/wizard/selectMpacks/selectedMpackVersion'),

  /**
   * Handle remove service button clicked.
   *
   * @param  {type} event
   */
  removeService: function (event) {
    const serviceId = event.context;
    this.get('controller').removeServiceHandler(serviceId);
  },

  /**
   * Handle remove mpack button clicked.
   * 
   * @param {any} event 
   */
  removeMpack: function (event) {
    const mpackId = event.context;
    this.get('controller').removeMpackHandler(mpackId);
  }
});
