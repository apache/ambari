/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

require('models/repository');

App.WizardStep1View = Em.View.extend({

  templateName: require('templates/wizard/step1'),

  didInsertElement: function () {
    $("[rel=skip-validation-tooltip]").tooltip({ placement: 'right'});
    $("[rel=use-redhat-tooltip]").tooltip({ placement: 'right'});
    if (this.get('controller.selectedStack') && this.get('controller.selectedStack.showAvailable')) {
      // first time load
      this.set('controller.latestSelectedPublicRepoId',this.get('controller.selectedStack.id'));
    } else {
      var selected = this.get('controller.content.stacks') && this.get('controller.content.stacks').findProperty('showAvailable');
      if (selected) {
        // get back from other steps, set default public repo as selected public
        this.set('controller.latestSelectedPublicRepoId', selected.get('id'));
      } else {
        // network disconnection
        this.set('controller.latestSelectedPublicRepoId', null);
        this.set('controller.optionsToSelect.useLocalRepo.isSelected', true);
        this.set('controller.optionsToSelect.usePublicRepo.isSelected', false);
      }
    }
  },

  /**
   * =========================== Option "Use Public Repository" starts from here ==================
   */

  /**
   * The public reo version shown on the dropdown button
   */
  selectedPublicRepoVersion: function () {
    var selectedId = this.get('controller.latestSelectedPublicRepoId');
    return selectedId ? this.get('controller.content.stacks').findProperty('id', selectedId): null;
  }.property('controller.latestSelectedPublicRepoId'),

  /**
   * List of other available stack repos within the same stack name
   * @type {Em.Object[]}
   */
  availableStackRepoList: function () {
    var selectedStack = this.get('controller.selectedStack');
    var availableStackRepos = this.get('controller.content.stacks').filter(function (item) {
      return item.get('showAvailable') && item.get('id') != selectedStack.get('id');
    });
    return availableStackRepos.toArray().map(function (stack) {
      return Em.Object.create({
        id: stack.get('id'),
        repositoryVersion: stack.get('repositoryVersion'),
        displayName: stack.get('stackName') + "-" + stack.get('repositoryVersion'),
        isSelected: false
      });
    });
  }.property('controller.selectedStack'),

  selectRepoInList: function (event) {
    if (this.get('controller.optionsToSelect.useLocalRepo.isSelected')) return;
    this.get('controller.content.stacks').setEach('isSelected', false);
    this.get('controller.content.stacks').findProperty('id', event.context.id).set('isSelected', true);
    this.set('controller.latestSelectedPublicRepoId',event.context.id);
  },

  selectedServices: function () {
    var selectedStack = this.get('controller.selectedStack');
    return Em.isNone(selectedStack) ? [] : selectedStack.get('stackServices').toArray().filter(function (service) {
      return !service.get('isHidden');
    }).map(function (service) {
      return Em.Object.create({
        displayName: service.get('displayName'),
        version: service.get('latestVersion')
      });
    });
  }.property('controller.selectedStack'),

  openPublicOptionDisabledWindow: function () {
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step1.selectUseRepoOptions.public.networkLost.popup.title'),
      message: Em.I18n.t('installer.step1.selectUseRepoOptions.public.networkLost.popup.msg'),
      option1: Em.I18n.t('installer.step1.selectUseRepoOptions.public.networkLost.popup.msg1'),
      option2: Em.I18n.t('installer.step1.selectUseRepoOptions.public.networkLost.popup.msg2'),
      option3: Em.I18n.t('installer.step1.selectUseRepoOptions.public.networkLost.popup.msg3'),
      bodyClass: Ember.View.extend({
        template: Em.Handlebars.compile('<div class="public-disabled-message">{{message}}</div>' +
          '<li class="public-disabled-option">{{option1}}</li>' +
          '<li class="public-disabled-option">{{option2}}</li>' +
          '<li class="public-disabled-option">{{option3}}</li>')
      }),
      secondary: false
    });
  },

  /**
   * Disable submit button flag
   * @type {bool}
   */
  isSubmitDisabled: Em.computed.or('controller.content.isCheckInProgress'),

  /**
   * Onclick handler for recheck repos urls. Used in Advanced Repository Options.
   */
  retryRepoUrls: function () {
    App.router.get('installerController').checkRepoURL(this.get('controller'));
  },


  /**
   * =========================== Option "Use Local Repository" starts from here ==================
   */

  /**
   * Checkbox for use Public repo
   * @type {Ember.Checkbox}
   */
  usePublicRepoRadioButton: Em.Checkbox.extend({
    tagName: 'input',
    attributeBindings: [ 'type', 'checked' ],
    classNames: [''],
    checked: Em.computed.alias('controller.optionsToSelect.usePublicRepo.isSelected'),
    type: 'radio',
    disabled: function() {
      return !this.get('controller.latestSelectedPublicRepoId');
    }.property('controller.latestSelectedPublicRepoId'),

    click: function () {
      this.set('controller.optionsToSelect.usePublicRepo.isSelected', true);
      this.set('controller.optionsToSelect.useLocalRepo.isSelected', false);
      var latestSelectedPublicRepoId = this.get('controller.latestSelectedPublicRepoId');
      if (latestSelectedPublicRepoId) {
        this.get('controller.content.stacks').setEach('isSelected', false);
        this.get('controller.content.stacks').findProperty('id', latestSelectedPublicRepoId).set('isSelected', true);
      } else {
        // make the 1st public repo as selected
        this.get('controller.content.stacks').findProperty('id').set('isSelected', true);
      }
    }
  }),

  /**
   * Checkbox for use Public repo
   * @type {Ember.Checkbox}
   */
  useLocalRepoRadioButton: Em.Checkbox.extend({
    tagName: 'input',
    attributeBindings: [ 'type', 'checked' ],
    classNames: [''],
    checked: Em.computed.alias('controller.optionsToSelect.useLocalRepo.isSelected'),
    type: 'radio',

    click: function () {
      this.set('controller.optionsToSelect.useLocalRepo.isSelected', true);
      this.set('controller.optionsToSelect.usePublicRepo.isSelected', false);
      var latestSelectedLocalRepoId = this.get('controller.latestSelectedLocalRepoId');
      if (latestSelectedLocalRepoId) {
        this.get('controller.content.stacks').setEach('isSelected', false);
        this.get('controller.content.stacks').findProperty('id', latestSelectedLocalRepoId).set('isSelected', true);
      }
    }
  }),

  /**
   * Checkbox for Use local Repo > Upload VDF file
   * @type {Ember.Checkbox}
   */
  uploadFileRadioButton: Em.Checkbox.extend({
    tagName: 'input',
    attributeBindings: [ 'type', 'checked' ],
    classNames: [''],
    checked: Em.computed.alias('controller.optionsToSelect.useLocalRepo.uploadFile.isSelected'),
    type: 'radio',
    disabled: function () {
      return this.get("controller.optionsToSelect.usePublicRepo.isSelected");
    }.property("controller.optionsToSelect.usePublicRepo.isSelected"),

    click: function () {
      this.set('controller.optionsToSelect.useLocalRepo.uploadFile.isSelected', true);
      this.set('controller.optionsToSelect.useLocalRepo.enterUrl.isSelected', false);
      this.set('controller.optionsToSelect.useLocalRepo.enterUrl.hasError', false);
      this.set('controller.optionsToSelect.useLocalRepo.uploadFile.hasError', false);
    }
  }),

  /**
   * Checkbox for Use local Repo > Enter Url of VDF file
   * @type {Ember.Checkbox}
   */
  enterUrlRadioButton: Em.Checkbox.extend({
    tagName: 'input',
    attributeBindings: [ 'type', 'checked' ],
    classNames: [''],
    checked: Em.computed.alias('controller.optionsToSelect.useLocalRepo.enterUrl.isSelected'),
    type: 'radio',
    disabled: function () {
      return this.get("controller.optionsToSelect.usePublicRepo.isSelected");
    }.property("controller.optionsToSelect.usePublicRepo.isSelected"),

    click: function () {
      this.set('controller.optionsToSelect.useLocalRepo.enterUrl.isSelected', true);
      this.set('controller.optionsToSelect.useLocalRepo.uploadFile.isSelected', false);
      this.set('controller.optionsToSelect.useLocalRepo.enterUrl.hasError', false);
      this.set('controller.optionsToSelect.useLocalRepo.uploadFile.hasError', false);
    }
  }),

 /*
  * Is File API available
  * @type {bool}
  */
  isFileApi: function () {
    return window.File && window.FileReader && window.FileList;
  }.property(),

  fileBrowserDisabled: function () {
    return this.get("controller.optionsToSelect.usePublicRepo.isSelected") || this.get("controller.optionsToSelect.useLocalRepo.enterUrl.isSelected");
  }.property("controller.optionsToSelect.usePublicRepo.isSelected", "controller.optionsToSelect.useLocalRepo.enterUrl.isSelected"),
  enterUrlFieldDisabled: function () {
    return this.get("controller.optionsToSelect.usePublicRepo.isSelected") || this.get("controller.optionsToSelect.useLocalRepo.uploadFile.isSelected");
  }.property("controller.optionsToSelect.usePublicRepo.isSelected", "controller.optionsToSelect.useLocalRepo.uploadFile.isSelected"),
  readInfoButtonDisabled: function () {
    if (this.get('controller.optionsToSelect.useLocalRepo.isSelected')) {
      if(this.get('controller.optionsToSelect.useLocalRepo.uploadFile.isSelected')) {
        return !this.get('controller.optionsToSelect.useLocalRepo.uploadFile.file');
      } else if (this.get('controller.optionsToSelect.useLocalRepo.enterUrl.isSelected')) {
        return !this.get('controller.optionsToSelect.useLocalRepo.enterUrl.url');
      }
    } else {
      return true;
    }
  }.property('controller.optionsToSelect.useLocalRepo.isSelected', 'controller.optionsToSelect.useLocalRepo.uploadFile.isSelected',
    'controller.optionsToSelect.useLocalRepo.uploadFile.file', 'controller.optionsToSelect.useLocalRepo.enterUrl.url'),

  operatingSystems: function () {
    var selectedStack = this.get('controller.selectedStack');
    return Em.isNone(selectedStack) ? [] : selectedStack.get('operatingSystems');
  }.property('controller.selectedStack'),

  isAddOsButtonDisabled: function () {
    return this.get('operatingSystems').get('length') == this.get('operatingSystems').filterProperty('isSelected').get('length') || this.get('controller.selectedStack.useRedhatSatellite') === true;
  }.property('operatingSystems', 'operatingSystems.@each.isSelected', 'controller.selectedStack.useRedhatSatellite'),

  /**
   * List of all repositories under selected stack operatingSystems
   */
  allRepositories: function () {
    var selectedStack = this.get('controller.selectedStack');
    return Em.isNone(selectedStack) ? [] : selectedStack.get('repositories');
  }.property('controller.selectedStack'),

  /**
   * Verify if some repo has invalid base-url
   * @type {bool}
   */
  invalidFormatUrlExist: Em.computed.someBy('allRepositories', 'invalidFormatError', true),
  /**
   * Verify if some invalid repo-urls exist
   * @type {bool}
   */
  invalidUrlExist: Em.computed.someBy('allRepositories', 'validation', App.Repository.validation['INVALID']),
  /**
   * If all repo links are unchecked
   * @type {bool}
   */
  isNoOsChecked: Em.computed.everyBy('operatingSystems', 'isSelected', false),

  popoverView: Em.View.extend({
    tagName: 'i',
    classNameBindings: ['repository.validation'],
    attributeBindings: ['repository.errorTitle:title', 'repository.errorContent:data-content'],
    didInsertElement: function () {
      App.popover($(this.get('element')), {'trigger': 'hover'});
    }
  }),

  /**
   * Handler when editing any repo BaseUrl
   * @method editLocalRepository
   */
  editLocalRepository: function () {
    //upload to content
    var repositories = this.get('allRepositories');
    repositories.forEach(function (repository) {
      if (repository.get('lastBaseUrl') != repository.get('baseUrl')) {
        repository.setProperties({
          lastBaseUrl: repository.get('baseUrl'),
          validation: App.Repository.validation['PENDING']
        });
      }
    }, this);
  }.observes('allRepositories.@each.baseUrl')

});


App.VersionDefinitionFileUploader = Em.View.extend({
  template: Em.Handlebars.compile('<input type="file" {{bindAttr disabled="view.disabled"}} />'),

  classNames: ['vdf-input-indentation'],

  change: function (e) {
    var self = this;
    if (e.target.files && e.target.files.length == 1) {
      var file = e.target.files[0];
      var reader = new FileReader();

      reader.onload = (function () {
        return function (e) {
          self.get("controller").setVDFFile(e.target.result);
        };
      })(file);
      reader.readAsText(file);
    }
  }

});