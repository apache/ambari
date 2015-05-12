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

App.WizardStep1View = Em.View.extend({

  templateName: require('templates/wizard/step1'),

  /**
   * Is Repositories Accordion collapsed
   * @type {bool}
   */
  isRLCollapsed: true,

  didInsertElement: function () {
    if (this.get('isRLCollapsed')) {
      this.$('.accordion-body').hide();
    }
    $("[rel=skip-validation-tooltip]").tooltip({ placement: 'right'});
  },

  /**
   * List of available stacks
   * @type {Em.Object[]}
   */
  stacks: function () {
    return this.get('controller.content.stacks').toArray().map(function (stack) {
      return Em.Object.create({
        name: stack.get('id').replace('-', ' '),
        isSelected: stack.get('isSelected')
      });
    });
  }.property('controller.selectedStack'),

  operatingSystems: function () {
    var selectedStack = this.get('controller.selectedStack');
    return Em.isNone(selectedStack) ? [] : selectedStack.get('operatingSystems');
  }.property('controller.selectedStack'),

  /**
   * List of all repositories under selected stack operatingSystems
   * API and ember data model structure:
   * stack = [{OS-1},{OS-2}]
   * OS-1 = [{repository-1},{repository-2}]
   * OS-2 = [{repository-3},{repository-4}]
   * @return: [{repository-1},{repository-2},{repository-3},{repository-4}]
   */
  allRepositories: function () {
    var selectedStack = this.get('controller.selectedStack');
    return Em.isNone(selectedStack) ? [] : selectedStack.get('repositories');
  }.property('controller.selectedStack'),

  /**
   * Verify if some repo has empty base-url
   * @type {bool}
   */
  invalidFormatUrlExist: function () {
    return this.get('allRepositories').someProperty('invalidFormatError', true);
  }.property('allRepositories.@each.invalidFormatError'),

  /**
   * Disable submit button flag
   * @type {bool}
   */
  isSubmitDisabled: function () {
    return this.get('invalidFormatUrlExist') || this.get('isNoOsChecked') || this.get('invalidUrlExist') || this.get('controller.content.isCheckInProgress');
  }.property('invalidFormatUrlExist', 'isNoOsChecked', 'invalidUrlExist', 'controller.content.isCheckInProgress'),

  /**
   * Enable error count badge
   * @type {bool}
   */
  showErrorsWarningCount: function () {
    return this.get('isSubmitDisabled') && !!this.get('totalErrorCnt');
  }.property('isSubmitDisabled', 'totalErrorCnt'),

  /**
   * Verify if some invalid repo-urls exist
   * @type {bool}
   */
  invalidUrlExist: function () {
    return this.get('allRepositories').someProperty('validation', App.Repository.validation['INVALID']);
  }.property('allRepositories.@each.validation'),

  /**
   * If all repo links are unchecked
   * @type {bool}
   */
  isNoOsChecked: function () {
    return this.get('operatingSystems').everyProperty('isSelected', false);
  }.property('operatingSystems.@each.isSelected'),

  /**
   * Overall errors count
   * @type {number}
   */
  totalErrorCnt: function () {
    var invalidFormatCnt = this.get('allRepositories').filterProperty('invalidFormatError').length;
    var invalidCnt = this.get('allRepositories').filterProperty('validation', App.Repository.validation['INVALID']).length;
    if (this.get('isNoOsChecked')) {
      return 1;
    } else if (invalidFormatCnt || invalidCnt) {
      return invalidFormatCnt + invalidCnt;
    } else {
      return 0;
    }
  }.property('allRepositories.@each.invalidFormatError', 'isNoOsChecked', 'allRepositories.@each.validation'),

  /**
   * Checkbox for each stack
   * @type {Ember.Checkbox}
   */
  stackRadioButton: Em.Checkbox.extend({
    tagName: 'input',
    attributeBindings: [ 'type', 'checked' ],
    checked: function () {
      return this.get('content.isSelected');
    }.property('content.isSelected'),
    type: 'radio',

    click: function () {
      this.get('controller.content.stacks').setEach('isSelected', false);
      this.get('controller.content.stacks').findProperty('id', this.get('content.name').replace(' ', '-')).set('isSelected', true);
    }
  }),

  /**
   * Popover for repo-url error indicator
   * @type {Em.View}
   */
  popoverView: Em.View.extend({
    tagName: 'i',
    classNameBindings: ['repository.validation'],
    attributeBindings: ['repository.errorTitle:title', 'repository.errorContent:data-content'],
    didInsertElement: function () {
      App.popover($(this.get('element')), {'trigger': 'hover'});
    }
  }),

  /**
   * Onclick handler for Config Group Header. Used to show/hide block
   * @method onToggleBlock
   */
  onToggleBlock: function () {
    this.$('.accordion-body').toggle('blind', 500);
    this.set('isRLCollapsed', !this.get('isRLCollapsed'));
  },

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

});
