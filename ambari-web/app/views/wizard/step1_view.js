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
   * List of all repo-groups
   * @type {Object[][]}
   */
  allRepositoriesGroup: [
    [],
    [],
    []
  ],

  /**
   * Verify if some repo has empty base-url
   * @type {bool}
   */
  emptyRepoExist: function () {
    return this.get('allRepositoriesGroup').someProperty('empty-error', true);
  }.property('allRepositoriesGroup.@each.empty-error'),

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
    var invalidExist = this.get('allRepositoriesGroup').someProperty('validation', 'icon-exclamation-sign');
    return (selectedStack.get('invalidCnt') > 0) && invalidExist;
  }.property('controller.content.stacks.@each.invalidCnt', 'allRepositoriesGroup.@each.validation'),

  /**
   * If all repo links are unchecked
   * @type {bool}
   */
  allRepoUnchecked: function () {
    return !this.get('allRepositoriesGroup').someProperty('checked', true);
  }.property('allRepositoriesGroup.@each.checked'),

  /**
   * Overall errors count
   * @type {number}
   */
  totalErrorCnt: function () {
    var emptyCnt = this.get('allRepositoriesGroup').filterProperty('empty-error', true).length;
    var invalidCnt = this.get('allRepositoriesGroup').filterProperty('validation', 'icon-exclamation-sign').length;
    if (this.get('allRepoUnchecked')) {
      return 1;
    } else if (emptyCnt || invalidCnt) {
      return emptyCnt + invalidCnt;
    } else {
      return 0;
    }
  }.property('allRepositoriesGroup.@each.empty-error', 'allRepoUnchecked', 'allRepositoriesGroup.@each.validation'),

  /**
   * Is Repositories Accordion collapsed
   * @type {bool}
   */
  isRLCollapsed: true,

  /**
   * Checked flags for each repo-checkbox
   * @type {bool[]}
   */
  allGroupsCheckbox: [true, true, true],

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
   * Format repo-group values and set it to <code>allRepositoriesGroup</code>
   * @method loadRepositories
   */
  loadRepositories: function () {
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    var reposGroup = [
      [],
      [],
      []
    ];
    if (App.get('supports.ubuntu')) reposGroup.push([]); // @todo: remove after Ubuntu support confirmation
    var self = this;
    if (selectedStack && selectedStack.operatingSystems) {
      selectedStack.operatingSystems.forEach(function (os) {
        var cur_repo = Em.Object.create({
          baseUrl: os.baseUrl
        });
        switch (os.osType) {
          case 'redhat5':
            cur_repo.set('osType', 'Red Hat 5');
            reposGroup[0][0] = cur_repo;
            // set group 0 properties by redhat5 (any of the three is ok)
            self.setGroupByOs(reposGroup[0], os, 0);
            break;
          case 'redhat6':
            cur_repo.set('osType', 'Red Hat 6');
            reposGroup[1][0] = cur_repo;
            // set group 1 properties by redhat6 (any of the three is ok)
            self.setGroupByOs(reposGroup[1], os, 1);
            break;
          case 'suse11':
            cur_repo.set('osType', 'SLES 11');
            reposGroup[2][0] = cur_repo;
            // set group 2 properties by suse11 (any of the twe is ok)
            self.setGroupByOs(reposGroup[2], os, 2);
            break;
          case 'debian12':
            if (App.get('supports.ubuntu')) {
              cur_repo.set('osType', 'Ubuntu 12');
              reposGroup[3][0] = cur_repo;
              self.setGroupByOs(reposGroup[3], os, 3);
            }
            break;
        }
      });
    }
    this.set('allRepositoriesGroup', reposGroup);
  }.observes('controller.content.stacks.@each.isSelected', 'controller.content.stacks.@each.reload'),

  /**
   * Set group parameters according to operation system
   * @method setGroupByOs
   * @param {Ember.Object} group
   * @param {Object} os
   * @param {number} groupNumber
   */
  setGroupByOs: function (group, os, groupNumber) {
    var isChecked = this.get('allGroupsCheckbox')[groupNumber];
    group.set('checked', isChecked);
    group.set('baseUrl', os.baseUrl);
    group.set('latestBaseUrl', os.latestBaseUrl);
    group.set('defaultBaseUrl', os.defaultBaseUrl);
    group.set('empty-error', !os.baseUrl);
    group.set('invalid-error', os.validation == 'icon-exclamation-sign');
    group.set('validation', os.validation);
    group.set('undo', os.baseUrl != os.latestBaseUrl);
    group.set('clearAll', os.baseUrl);
    group.set('errorTitle', os.errorTitle);
    group.set('errorContent', os.errorContent);
    group.set('group-number', groupNumber);
  },

  /**
   * Onclick handler for checkbox of each repo group
   * @method updateByCheckbox
   */
  updateByCheckbox: function () {
    //upload to content
    var groups = this.get('allRepositoriesGroup');
    var self = this;
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    if (selectedStack && selectedStack.operatingSystems) {
      selectedStack.operatingSystems.forEach(function (os) {
        var groupNumber = self.osTypeToGroup(os.osType);
        var targetGroup = groups.findProperty('group-number', groupNumber);
        if (!targetGroup.get('checked')) {
          os.baseUrl = os.latestBaseUrl;
          os.validation = null;
          os.selected = false;
          targetGroup.set('baseUrl', os.latestBaseUrl);
          targetGroup.set('latestBaseUrl', os.latestBaseUrl);
          targetGroup.set('undo', targetGroup.get('baseUrl') != targetGroup.get('latestBaseUrl'));
          targetGroup.set('invalid-error', false);
          targetGroup.set('validation', null);
          targetGroup.set('clearAll', false);
          targetGroup.set('empty-error', !targetGroup.get('baseUrl'));
          self.get('allGroupsCheckbox')[groupNumber] = false;
          self.set('allGroupsCheckbox', self.get('allGroupsCheckbox'));
        } else {
          os.selected = true;
          os.skipValidation = self.get('skipValidationChecked');
          if (os.skipValidation) {
            targetGroup.set('validation', null);
            targetGroup.set('invalid-error', false);
          }
          targetGroup.set('clearAll', targetGroup.get('baseUrl'));
          targetGroup.set('empty-error', !targetGroup.get('baseUrl'));
          self.get('allGroupsCheckbox')[groupNumber] = true;
        }
      });
    }
  }.observes('allRepositoriesGroup.@each.checked', 'skipValidationChecked'),

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
    var osTypes = this.groupToOsType(event.context.get('group-number'));
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    osTypes.forEach(function (os) {
      var cos = selectedStack.operatingSystems.findProperty('osType', os);
      cos.baseUrl = Em.isEmpty(newBaseUrlField) ? '' : Em.get(cos, newBaseUrlField);
      cos.validation = null;
    });
    this.loadRepositories();
  },

  /**
   * Handler when editing any repo group BaseUrl
   * @method editGroupLocalRepository
   */
  editGroupLocalRepository: function () {
    //upload to content
    var groups = this.get('allRepositoriesGroup');
    var self = this;
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    if (selectedStack && selectedStack.operatingSystems) {
      selectedStack.operatingSystems.forEach(function (os) {
        var targetGroup = groups.findProperty('group-number', self.osTypeToGroup(os.osType));
        if (os.baseUrl != targetGroup.get('baseUrl')) {
          os.baseUrl = targetGroup.get('baseUrl');
          os.validation = null;
          targetGroup.set('undo', targetGroup.get('baseUrl') != targetGroup.get('latestBaseUrl'));
          targetGroup.set('invalid-error', false);
          targetGroup.set('validation', null);
          targetGroup.set('clearAll', os.baseUrl);
          targetGroup.set('empty-error', !targetGroup.get('baseUrl'));
        }
      });
    }
  }.observes('allRepositoriesGroup.@each.baseUrl'),

  /**
   * Get list of OS for provided group number
   * @method groupToOsType
   * @param {number} groupNumber
   * @returns {Array}
   */
  groupToOsType: function (groupNumber) {
    return Em.getWithDefault({
      '0': ['redhat5'],
      '1': ['redhat6'],
      '2': ['suse11'],
      '3': ['debian12']
    }, groupNumber.toString(), []);
  },

  /**
   * Get group number for provided OS
   * @method osTypeToGroup
   * @param {string} osType
   * @returns {number}
   */
  osTypeToGroup: function (osType) {
    return Em.getWithDefault({
      'redhat5': 0,
      'redhat6': 1,
      'suse11': 2,
      'debian12': 3
    }, osType, -1);
  }

});
