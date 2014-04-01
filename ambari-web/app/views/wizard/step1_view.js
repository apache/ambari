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
    var stacks = [];
    this.get('controller.content.stacks').forEach(function (stack) {
      stacks.pushObject(Em.Object.create({
        name: stack.get('name').replace('-', ' '),
        isSelected: stack.get('isSelected')
      }));
    });
    return stacks;
  }.property('controller.content.stacks.@each.isSelected'),

  /**
   * Checkbox for each stack
   * @type {Em.Checkbox}
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
   * List of all repo-groups
   * @type {Object[][]}
   */
  allRepositoriesGroup: [[],[],[]],

  /**
   * Verify if some repo has empty base-url
   * @type {bool}
   */
  emptyRepoExist: function () {
    return (this.get('allRepositoriesGroup').filterProperty('empty-error', true).length != 0);
  }.property('allRepositoriesGroup.@each.empty-error'),

  /**
   * Disable submit button flag
   * @type {bool}
   */
  isSubmitDisabled: function() {
    return this.get('emptyRepoExist') || this.get('allRepoUnchecked') || this.get('invalidUrlExist') ;
  }.property('emptyRepoExist', 'allRepoUnchecked', 'invalidUrlExist'),

  /**
   * Verify if some invalid repo-urls exist
   * @type {bool}
   */
  invalidUrlExist: function () {
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    var invalidExist = this.get('allRepositoriesGroup').filterProperty('validation', 'icon-exclamation-sign').length != 0;
    return (selectedStack.get('invalidCnt') > 0) && invalidExist;
  }.property('controller.content.stacks.@each.invalidCnt', 'allRepositoriesGroup.@each.validation'),

  /**
   * If all repo links are unchecked
   * @type {bool}
   */
  allRepoUnchecked: function () {
    return (!this.get('allRepositoriesGroup').filterProperty('checked', true).length);
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
    } else if ( emptyCnt || invalidCnt) {
      return emptyCnt + invalidCnt;
    } else {
      return 0;
    }
  }.property('allRepositoriesGroup.@each.empty-error', 'allRepoUnchecked', 'allRepositoriesGroup.@each.validation'),

  /**
   * Onclick handler for Config Group Header. Used to show/hide block
   */
  onToggleBlock: function () {
    this.$('.accordion-body').toggle('blind', 500);
    this.set('isRLCollapsed', !this.get('isRLCollapsed'));
  },

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
   * Popover for repo-url error indicator
   * @type {Em.View}
   */
  popoverView: Em.View.extend({
    tagName: 'i',
    classNameBindings: ['repoGroup.validation'],
    attributeBindings: ['repoGroup.errorTitle:title', 'repoGroup.errorContent:data-content'],
    didInsertElement: function() {
      App.popover($(this.get('element')), {'trigger': 'hover'});
    }
  }),

  /**
   * Format repo-group values and set it to <code>allRepositoriesGroup</code>
   */
  loadRepositories: function () {
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    var reposGroup = [[],[],[]];
    if (App.supports.ubuntu) reposGroup.push([]); // @todo: remove after Ubuntu support confirmation
    var self = this;
    if (selectedStack && selectedStack.operatingSystems) {
      selectedStack.operatingSystems.forEach(function (os) {
        var cur_repo = Em.Object.create({
          baseUrl: os.baseUrl
        });
        switch(os.osType) {
          case 'redhat5':
            cur_repo.set('osType', 'Red Hat 5');
            reposGroup[0][0] = cur_repo;
            // set group 0 properties by redhat5 (any of the three is ok)
            self.setGroupByOs(reposGroup[0], os, 0);
            break;
          case 'centos5':
            cur_repo.set('osType', 'CentOS 5');
            reposGroup[0][1] = cur_repo;
            break;
          case 'oraclelinux5':
            cur_repo.set('osType', 'Oracle Linux 5');
            reposGroup[0][2] = cur_repo;
            break;
          case 'redhat6':
            cur_repo.set('osType', 'Red Hat 6');
            reposGroup[1][0] = cur_repo;
            // set group 1 properties by redhat6 (any of the three is ok)
            self.setGroupByOs(reposGroup[1], os, 1);
            break;
          case 'centos6':
            cur_repo.set('osType', 'CentOS 6');
            reposGroup[1][1] = cur_repo;
            break;
          case 'oraclelinux6':
            cur_repo.set('osType', 'Oracle Linux 6');
            reposGroup[1][2] = cur_repo;
            break;
          case 'sles11':
            cur_repo.set('osType', 'SLES 11');
            reposGroup[2][0] = cur_repo;
            // set group 2 properties by sles11 (any of the twe is ok)
            self.setGroupByOs(reposGroup[2], os, 2);
            break;
          case 'suse11':
            cur_repo.set('osType', 'SUSE 11');
            reposGroup[2][1] = cur_repo;
            break;
          case 'ubuntu12':
            cur_repo.set('osType','Ubuntu 12');
            reposGroup[3][0] = cur_repo;
            self.setGroupByOs(reposGroup[3], os, 3);
            break;
        }
      });
    }
    this.set('allRepositoriesGroup', reposGroup);
  }.observes('controller.content.stacks.@each.isSelected', 'controller.content.stacks.@each.reload'),

  /**
   * Set group parameters according to operation system
   * @param {Em.Object} group
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
    group.set('undo', os.baseUrl != os.defaultBaseUrl);
    group.set('clearAll', os.baseUrl);
    group.set('errorTitle', os.errorTitle);
    group.set('errorContent', os.errorContent);
    group.set('group-number', groupNumber);
  },

  /**
   * Onclick handler for checkbox of each repo group
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
          targetGroup.set('undo', targetGroup.get('baseUrl') != targetGroup.get('defaultBaseUrl'));
          targetGroup.set('invalid-error', false);
          targetGroup.set('validation', null);
          targetGroup.set('clearAll', false);
          targetGroup.set('empty-error',!targetGroup.get('baseUrl'));
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
          targetGroup.set('empty-error',!targetGroup.get('baseUrl'));
          self.get('allGroupsCheckbox')[groupNumber] = true;
        }
      });
    }
  }.observes('allRepositoriesGroup.@each.checked', 'skipValidationChecked'),

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

  /**
   * Onclick handler for undo action of each repo group
   */
  undoGroupLocalRepository: function (event) {
    var group = event.context;
    var osTypes = this.groupToOsType(group.get('group-number'));
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    osTypes.forEach( function (os) {
      var cos = selectedStack.operatingSystems.findProperty('osType', os );
      cos.baseUrl = cos.latestBaseUrl;
      cos.validation = null;
    });
    this.loadRepositories();
  },

  /**
   * Handler for clear icon click
   * @param {object} event
   */
  clearGroupLocalRepository: function (event) {
    var group = event.context;
    var osTypes = this.groupToOsType(group.get('group-number'));
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    osTypes.forEach( function (os) {
      var cos = selectedStack.operatingSystems.findProperty('osType', os );
      cos.baseUrl = '';
      cos.validation = null;
    });
    this.loadRepositories();
  },

  /**
   * Handler when editing any repo group BaseUrl
   * @param {object} event
   */
  editGroupLocalRepository: function (event) {
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
          targetGroup.set('undo', targetGroup.get('baseUrl') != targetGroup.get('defaultBaseUrl'));
          targetGroup.set('invalid-error', false);
          targetGroup.set('validation', null);
          targetGroup.set('clearAll', os.baseUrl);
          targetGroup.set('empty-error',!targetGroup.get('baseUrl'));
        }
      });
    }
  }.observes('allRepositoriesGroup.@each.baseUrl'),

  /**
   * Get list of OS for provided group number
   * @param {number} groupNumber
   * @returns {Array}
   */
  groupToOsType: function (groupNumber) {
    switch (groupNumber) {
      case 0:
        return ['redhat5', 'centos5', 'oraclelinux5'];
      case 1:
        return ['redhat6', 'centos6', 'oraclelinux6'];
      case 2:
        return ['sles11', 'suse11'];
      case 3:
        return ['ubuntu12'];
    }
    return [];
  },

  /**
   * Get group number for provided OS
   * @param  {string} osType
   * @returns {number}
   */
  osTypeToGroup: function (osType) {
    switch(osType) {
      case 'redhat5':
      case 'centos5':
      case 'oraclelinux5':
        return 0;
      case 'redhat6':
      case 'centos6':
      case 'oraclelinux6':
        return 1;
      case 'sles11':
      case 'suse11':
        return 2;
      case 'ubuntu12':
        return 3;
    }
    return -1;
  }
});
