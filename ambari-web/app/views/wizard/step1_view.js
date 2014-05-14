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
   * List of available stacks
   * @type {Em.Object[]}
   */
  stacks: function () {
    return this.get('controller.content.stacks').map(function (stack) {
      return Em.Object.create({
        name: stack.get('name').replace('-', ' '),
        isSelected: stack.get('isSelected')
      });
    });
  }.property('controller.content.stacks.@each.isSelected'),

  /**
   * List of all repositories
   * @type {Array}
   */
  allRepositories: [],

  /**
   * List of all repo-groups
   * @type {Array}
   */
  allRepositoriesGroups: function () {
    var result = [];
    var stacks = this.get('controller.content.stacks');
    if (stacks && stacks.length) {
      var selectedStack = stacks.findProperty('isSelected', true);
      var allRepositories = this.get('allRepositories');
      var OSNames = allRepositories.mapProperty('osType').uniq();
      OSNames.forEach(function (os) {
        result.push(Ember.Object.create({
          checked: selectedStack.operatingSystems.findProperty('osType', os).selected,
          name: os,
          repositories: allRepositories.filterProperty('osType', os)
        }));
      });
    }
    return result;
  }.property('allRepositories.length', 'controller.content.stacks'),

  /**
   * Verify if some repo has empty base-url
   * @type {bool}
   */
  emptyRepoExist: function () {
    return this.get('allRepositories').someProperty('empty-error', true);
  }.property('allRepositories.@each.empty-error'),

  /**
   * Disable submit button flag
   * @type {bool}
   */
  isSubmitDisabled: function () {
    return this.get('emptyRepoExist') || this.get('allRepoUnchecked') || this.get('invalidUrlExist');
  }.property('emptyRepoExist', 'allRepoUnchecked', 'invalidUrlExist'),

  /**
   * Verify if some invalid repo-urls exist
   * @type {bool}
   */
  invalidUrlExist: function () {
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    var invalidExist = this.get('allRepositories').someProperty('validation', 'icon-exclamation-sign');
    return (selectedStack.get('invalidCnt') > 0) && invalidExist;
  }.property('controller.content.stacks.@each.invalidCnt', 'allRepositories.@each.validation'),

  /**
   * If all repo links are unchecked
   * @type {bool}
   */
  allRepoUnchecked: function () {
    return !this.get('allRepositoriesGroups').someProperty('checked', true);
  }.property('allRepositoriesGroups.@each.checked'),

  /**
   * Overall errors count
   * @type {number}
   */
  totalErrorCnt: function () {
    var emptyCnt = this.get('allRepositories').filterProperty('empty-error', true).length;
    var invalidCnt = this.get('allRepositories').filterProperty('validation', 'icon-exclamation-sign').length;
    if (this.get('allRepoUnchecked')) {
      return 1;
    } else if (emptyCnt || invalidCnt) {
      return emptyCnt + invalidCnt;
    } else {
      return 0;
    }
  }.property('allRepositories.@each.empty-error', 'allRepoUnchecked', 'allRepositories.@each.validation'),

  /**
   * Is Repositories Accordion collapsed
   * @type {bool}
   */
  isRLCollapsed: true,

  /**
   * Skip repo-validation
   * @type {bool}
   */
  skipValidationChecked: false,

  didInsertElement: function () {
    if (this.get('isRLCollapsed')) {
      this.$('.accordion-body').hide();
    }
    $("[rel=skip-validation-tooltip]").tooltip({ placement: 'right'});
  },

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
      this.get('controller.content.stacks').findProperty('name', this.get('content.name').replace(' ', '-')).set('isSelected', true);
    }
  }),

  /**
   * Popover for repo-url error indicator
   * @type {Em.View}
   */
  popoverView: Em.View.extend({
    tagName: 'i',
    classNameBindings: ['repoGroup.validation'],
    attributeBindings: ['repoGroup.errorTitle:title', 'repoGroup.errorContent:data-content'],
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
   * Format repo values and set it to <code>allRepositories</code>
   * @method loadRepositories
   */
  loadRepositories: function () {
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    var repos = [];
    if (selectedStack && selectedStack.operatingSystems) {
      selectedStack.operatingSystems.forEach(function (os) {
        repos.push(Ember.Object.create({
          'id': os.id,
          'repoId': os.repoId,
          'baseUrl': os.baseUrl,
          'osType': os.osType,
          'latestBaseUrl': os.latestBaseUrl,
          'defaultBaseUrl': os.defaultBaseUrl,
          'empty-error': !os.baseUrl,
          'invalid-error': os.validation == 'icon-exclamation-sign',
          'validation': os.validation,
          'undo': os.baseUrl != os.latestBaseUrl,
          'clearAll': os.baseUrl,
          'errorTitle': os.errorTitle,
          'errorContent': os.errorContent
        }));
      }, this);
    }
    this.set('allRepositories', repos);
  }.observes('controller.content.stacks.@each.isSelected', 'controller.content.stacks.@each.reload'),

  /**
   * Onclick handler for checkbox of each repo group
   * @method updateByCheckbox
   */
  updateByCheckbox: function () {
    //upload to content
    var repos = this.get('allRepositories');
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    var allRepositoriesGroups = this.get('allRepositoriesGroups');
    if (selectedStack && selectedStack.operatingSystems) {
      selectedStack.operatingSystems.forEach(function (os) {
        var targetRepo = repos.findProperty('id', os.id);
        var repoGroup = allRepositoriesGroups.findProperty('name', targetRepo.get('osType'));
        if (repoGroup && !repoGroup.get('checked')) {
          os.baseUrl = os.latestBaseUrl;
          os.validation = null;
          os.selected = false;
          targetRepo.set('baseUrl', os.latestBaseUrl);
          targetRepo.set('latestBaseUrl', os.latestBaseUrl);
          targetRepo.set('undo', targetRepo.get('baseUrl') != targetRepo.get('latestBaseUrl'));
          targetRepo.set('invalid-error', false);
          targetRepo.set('validation', null);
          targetRepo.set('clearAll', false);
          targetRepo.set('empty-error', !targetRepo.get('baseUrl'));
        } else {
          os.selected = true;
          os.skipValidation = this.get('skipValidationChecked');
          if (os.skipValidation) {
            targetRepo.set('validation', null);
            targetRepo.set('invalid-error', false);
          }
          targetRepo.set('clearAll', targetRepo.get('baseUrl'));
          targetRepo.set('empty-error', !targetRepo.get('baseUrl'));
        }
      }, this);
    }
  }.observes('allRepositoriesGroups.@each.checked', 'skipValidationChecked'),

  /**
   * Onclick handler for undo action of each repo group
   * @method undoGroupLocalRepository
   * @param {object} event
   */
  undoGroupLocalRepository: function (event) {
    this.doActionForGroupLocalRepository(event, 'latestBaseUrl');
  },

  /**
   * Handler for clear icon click
   * @method clearGroupLocalRepository
   * @param {object} event
   */
  clearGroupLocalRepository: function (event) {
    this.doActionForGroupLocalRepository(event, '');
  },

  /**
   * Common handler for repo groups actions
   * @method doActionForGroupLocalRepository
   * @param {object} event
   * @param {string} newBaseUrlField
   */
  doActionForGroupLocalRepository: function (event, newBaseUrlField) {
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    var cos = selectedStack.operatingSystems.findProperty('id', event.context.get('id'));
    cos.baseUrl = Em.isEmpty(newBaseUrlField) ? '' : Em.get(cos, newBaseUrlField);
    cos.validation = null;
    this.loadRepositories();
  },

  /**
   * Handler when editing any repo BaseUrl
   * @method editLocalRepository
   */
  editLocalRepository: function () {
    //upload to content
    var repos = this.get('allRepositories');
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    if (selectedStack && selectedStack.operatingSystems) {
      selectedStack.operatingSystems.forEach(function (os) {
        var targetRepo = repos.findProperty('id', os.id);
        if (os.baseUrl != targetRepo.get('baseUrl')) {
          os.baseUrl = targetRepo.get('baseUrl');
          os.validation = null;
          targetRepo.set('undo', targetRepo.get('baseUrl') != targetRepo.get('latestBaseUrl'));
          targetRepo.set('invalid-error', false);
          targetRepo.set('validation', null);
          targetRepo.set('clearAll', os.baseUrl);
          targetRepo.set('empty-error', !targetRepo.get('baseUrl'));
        }
      });
    }
  }.observes('allRepositories.@each.baseUrl')

});
