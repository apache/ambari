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

  isAddOSDisabled: true,
  localRepositories: [],
  defaultRepositories: [],
  refreshRepositoryInfo: function () {
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    var localRepos = [];
    var defaultRepos = [];
    if (selectedStack && selectedStack.operatingSystems) {
      selectedStack.operatingSystems.forEach(function (os) {
        if (os.baseUrl !== os.defaultBaseUrl) {
          localRepos.push($.extend({}, os));
        } else {
          defaultRepos.push($.extend({}, os));
        }
      });
    }
    this.set('localRepositories', localRepos);
    this.set('defaultRepositories', defaultRepos);
    this.set('isAddOSDisabled', defaultRepos.get('length') < 1);
  }.observes('controller.content.stacks.@each.isSelected', 'controller.content.stacks.@each.operatingSystems.@each.baseUrl'),

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

  removeLocalRepository: function (event) {
    var localRepo = event.context;

    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);
    var cos = selectedStack.operatingSystems.findProperty('osType', localRepo.osType);
    cos.baseUrl = cos.defaultBaseUrl;

    this.refreshRepositoryInfo();
  },

  addLocalRepository: function () {
    var self = this;
    var defaultRepos = self.get('defaultRepositories');
    var selectedStack = this.get('controller.content.stacks').findProperty('isSelected', true);

    App.ModalPopup.show({
      // classNames: ['big-modal'],
      classNames: [ 'sixty-percent-width-modal' ],
      header: "Add Local Repository",
      primary: 'Add',
      secondary: 'Cancel',
      onPrimary: function () {
        var error = null;
        var childViews = this.get('childViews');
        if (childViews && childViews.get('length') > 0) {
          var childView = childViews.objectAt(0);
          if (childView) {
            if (childView.get('enteredUrl')) {
              if (childView.get('selectedOS').baseUrl !== childView.get('enteredUrl') && childView.get('selectedOS').defaultBaseUrl !== childView.get('enteredUrl')) {
                var selectedStack = self.get('controller.content.stacks').findProperty('isSelected', true);
                var cos = selectedStack.operatingSystems.findProperty('osType', childView.get('selectedOS').osType);
                cos.baseUrl = childView.get('enteredUrl');
                self.refreshRepositoryInfo();
                this.hide();
              } else {
                error = Em.I18n.t('installer.step1.advancedRepo.localRepo.error.modifyUrl');
              }
            } else {
              error = Em.I18n.t('installer.step1.advancedRepo.localRepo.error.noUrl')
            }
            if (childView.get('isVisible'))
              childView.set('errorMessage', error);
          }
        }
      },
      onSecondary: function () {
        this.hide();
      },
      bodyClass: Ember.View.extend({
        templateName: require('templates/wizard/step1_addLocalRepository'),
        controller: self.get('controller'),
        stackName: selectedStack.get('name'),
        selectedOS: defaultRepos.objectAt(0),
        enteredUrl: defaultRepos.objectAt(0).baseUrl,
        oses: defaultRepos,
        errorMessage: null,
        selectOS: function (event) {
          var os = event.context;
          this.set('selectedOS', os);
          this.set('enteredUrl', os.baseUrl);
        }
      })
    });
  }
});
