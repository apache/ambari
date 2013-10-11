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

  stackRadioButton: Ember.Checkbox.extend({
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

  allRepositories: [],
  repoErrorCnt: function () {
    return this.get('allRepositories').filterProperty('empty-error', true).length;
  }.property('allRepositories.@each.empty-error'),
  loadRepositories: function () {
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    var repos = [];
    if (selectedStack && selectedStack.operatingSystems) {
      selectedStack.operatingSystems.forEach(function (os) {
       var cur_repo = Em.Object.create({
         baseUrl: os.baseUrl,
         defaultBaseUrl: os.defaultBaseUrl,
         osType: os.osType,
         validation: os.validation
       });
        cur_repo.set('empty-error', !os.baseUrl);
        cur_repo.set('invalid-error', os.validation == 'icon-remove');
        cur_repo.set('undo', os.baseUrl != os.defaultBaseUrl);
        repos.pushObject(cur_repo);
      });
    }
    this.set('allRepositories', repos);
  }.observes('controller.content.stacks.@each.isSelected', 'controller.content.stacks.@each.reload'),

  undoLocalRepository: function (event) {
    var localRepo = event.context;
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    var cos = selectedStack.operatingSystems.findProperty('osType', localRepo.osType);
    cos.baseUrl = cos.defaultBaseUrl;
    cos.validation = null;
    this.loadRepositories();
  },
  editLocalRepository: function (event) {
    //upload to content
    var repos = this.get('allRepositories');
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    if (selectedStack && selectedStack.operatingSystems) {
      selectedStack.operatingSystems.forEach(function (os) {
        var target = repos.findProperty('osType', os.osType);
        if ( os.baseUrl != target.get('baseUrl')) {
          os.baseUrl = target.get('baseUrl');
          os.validation = null;
          target.set('undo', target.get('baseUrl') != target.get('defaultBaseUrl'));
          target.set('invalid-error', false);
          target.set('validation', null);
          target.set('empty-error',!target.get('baseUrl'));
        }
      });
    }
  }.observes('allRepositories.@each.baseUrl'),
  isSubmitDisabled: function() {
    return this.get('repoErrorCnt') != 0;
  }.property('repoErrorCnt'),
  invalidUrlExist: function () {
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    return (selectedStack.get('invalidCnt') > 0);
  }.property('controller.content.stacks.@each.invalidCnt'),

  /**
   * Onclick handler for Config Group Header. Used to show/hide block
   */
  onToggleBlock: function () {
    this.$('.accordion-body').toggle('blind', 500);
    this.set('isRLCollapsed', !this.get('isRLCollapsed'));
  },
  isRLCollapsed: true,
  didInsertElement: function () {
    if (this.get('isRLCollapsed')) {
      this.$('.accordion-body').hide();
    }
  }
});
