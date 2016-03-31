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
    this.set('controller.latestSelectedPublicRepoId',this.get('controller.selectedStack.id'));

  },

  /**
   * =========================== Option "Use Public Repository" starts from here ==================
   */

  /**
   * List of available stack names HDP 2.2, HDP 2.3 .etc
   * @type {Em.Object[]}
   */
  stackNames: function () {
    var stackNamesMap = {};
    var selectedStack = this.get('controller.selectedStack');
    return this.get('controller.content.stacks').toArray().map(function (stack) {
      if (!stackNamesMap[stack.get('stackNameVersion')] && stack.get('showAvailable')) {
        stackNamesMap[stack.get('stackNameVersion')] = true;
        return Em.Object.create({
          id: stack.get('id'),
          name: stack.get('stackNameVersion').replace('-', ' '),
          isActive: stack.get('stackNameVersion') == selectedStack.get('stackNameVersion')
        });
      } else {
        return {};
      }
    });
  }.property('controller.selectedStack'),

  selectStackNameOnTab: function (event) {
    this.get('controller.content.stacks').setEach('isSelected', false);
    this.get('controller.content.stacks').findProperty('id', event.context.id).set('isSelected', true);
    this.set('controller.latestSelectedPublicRepoId',event.context.id);
  },

  /**
   * List of other available stack repos within the same stack name
   * @type {Em.Object[]}
   */
  availableStackRepoList: function () {
    var selectedStack = this.get('controller.selectedStack');
    var availableStackRepos = this.get('controller.content.stacks').filter(function (item) {
      return item.get('showAvailable') && item.get('stackNameVersion') == selectedStack.get('stackNameVersion') && item.get('id') != selectedStack.get('id');
    });
    return availableStackRepos.toArray().map(function (stack) {
      return Em.Object.create({
        id: stack.get('id'),
        repositoryVersion: stack.get('repositoryVersion'),
        isSelected: false
      });
    });
  }.property('controller.selectedStack'),

  selectRepoInList: function (event) {
    this.get('controller.content.stacks').setEach('isSelected', false);
    this.get('controller.content.stacks').findProperty('id', event.context.id).set('isSelected', true);
    this.set('controller.latestSelectedPublicRepoId',event.context.id);
  },

  selectedServices: function () {
    var selectedStack = this.get('controller.selectedStack');
    return Em.isNone(selectedStack) ? [] : selectedStack.get('stackServices').toArray().map(function (service) {
      return Em.Object.create({
        displayName: service.get('displayName'),
        version: service.get('latestVersion')
      });
    });
  }.property('controller.selectedStack'),


  /**
   * Disable submit button flag
   * @type {bool}
   */
  isSubmitDisabled: Em.computed.or('controller.content.isCheckInProgress'),

  /**
   * Enable error count badge
   * @type {bool}
   */
  showErrorsWarningCount: false,
  totalErrorCnt: 0,

  viewRepositories: function () {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step1.usePublicRepo.viewRepos'),
      primary: Em.I18n.t('common.save'),
      onPrimary: function () {
        self.retryRepoUrls();
        var modal = this;
        setTimeout(function(){
          modal.hide();
        }, 1500);
      },
      classNames: ['view-repositories-popup', 'sixty-percent-width-modal'],
      bodyClass: Em.View.extend({
        templateName: require('templates/wizard/step1_viewRepositories'),
        controllerBinding: 'App.router.wizardStep1Controller',

        operatingSystems: function () {
          var selectedStack = this.get('controller.selectedStack');
          return Em.isNone(selectedStack) ? [] : selectedStack.get('operatingSystems');
        }.property('controller.selectedStack'),

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

        /**
         * Overall errors count
         * @type {number}
         */
        updateTotalErrorCnt: function () {
          var invalidFormatCnt = this.get('allRepositories').filterProperty('invalidFormatError').length;
          var invalidCnt = this.get('allRepositories').filterProperty('validation', App.Repository.validation['INVALID']).length;
          var cnt = 0;
          if (this.get('isNoOsChecked')) {
            cnt = 1;
          } else if (invalidFormatCnt || invalidCnt) {
            cnt = invalidFormatCnt + invalidCnt;
          }
          self.set('totalErrorCnt', cnt);
          self.set('showErrorsWarningCount', cnt > 0);
        }.observes('allRepositories', 'allRepositories.@each.invalidFormatError', 'isNoOsChecked', 'allRepositories.@each.validation'),

        popoverView: Em.View.extend({
          tagName: 'i',
          classNameBindings: ['repository.validation'],
          attributeBindings: ['repository.errorTitle:title', 'repository.errorContent:data-content'],
          didInsertElement: function () {
            App.popover($(this.get('element')), {'trigger': 'hover'});
          }
        }),

        /**
         * Onclick handler for recheck repos urls. Used in Advanced Repository Options.
         */
        retryRepoUrls: function () {
          App.router.get('installerController').checkRepoURL(this.get('controller'));
        },

        /**
         * Onclick handler for checkbox of each repo group
         * @method updateByCheckbox
         */
        updateByCheckbox: function () {
          //upload to content
          var operatingSystems = this.get('operatingSystems');
          if (operatingSystems) {
            operatingSystems.forEach(function (os) {
              if (!os.get('isSelected')) {
                os.get('repositories').forEach(function (repository) {
                  repository.setProperties({
                    baseUrl: repository.get('latestBaseUrl'),
                    validation: App.Repository.validation['PENDING']
                  });
                });
              } else {
                os.get('repositories').forEach(function (repository) {
                  if (this.get('controller.skipValidationChecked')) {
                    repository.set('validation', App.Repository.validation['PENDING']);
                  }
                }, this);
              }
            }, this);
          }
        }.observes('operatingSystems.@each.isSelected', 'controller.skipValidationChecked'),

        /**
         * Onclick handler for undo action of each repo group
         * @method undoGroupLocalRepository
         * @param {object} event
         */
        undoGroupLocalRepository: function (event) {
          event.context.setProperties({
            baseUrl: event.context.get('latestBaseUrl'),
            validation: App.Repository.validation['PENDING']
          });
        },

        /**
         * Handler for clear icon click
         * @method clearGroupLocalRepository
         * @param {object} event
         */
        clearGroupLocalRepository: function (event) {
          if (!event.context.get('isSelected')) {
            return;
          }
          event.context.setProperties({
            baseUrl: '',
            validation: App.Repository.validation['PENDING']
          });
        },

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
      })
    });
  },

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

    click: function () {
      this.set('controller.optionsToSelect.usePublicRepo.isSelected', true);
      this.set('controller.optionsToSelect.useLocalRepo.isSelected', false);
      var latestSelectedPublicRepoId = this.get('controller.latestSelectedPublicRepoId');
      if (latestSelectedPublicRepoId) {
        this.get('controller.content.stacks').setEach('isSelected', false);
        this.get('controller.content.stacks').findProperty('id', latestSelectedPublicRepoId).set('isSelected', true);
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
          ////$('#sshKey').html(e.target.result);
          self.get("controller").setVDFFile(e.target.result);
        };
      })(file);
      reader.readAsText(file);
    }
  }

});