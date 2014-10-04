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

App.CreateAppWizardView = Ember.View.extend({

  classNames: ['create-app-wizard-wrapper'],

  didInsertElement: function(){
    this.setHeight();
    $(window).resize(this.setHeight);
    this.get('controller').loadStep();
  },

  isStep1: function () {
    return this.get('controller.currentStep') == 1;
  }.property('controller.currentStep'),

  isStep2: function () {
    return this.get('controller.currentStep') == 2;
  }.property('controller.currentStep'),

  isStep3: function () {
    return this.get('controller.currentStep') == 3;
  }.property('controller.currentStep'),

  isStep4: function () {
    return this.get('controller.currentStep') == 4;
  }.property('controller.currentStep'),

  isStep1Disabled: function () {
    return this.get('controller.currentStep') < 1;
  }.property('controller.currentStep'),

  isStep2Disabled: function () {
    return this.get('controller.currentStep') < 2;
  }.property('controller.currentStep'),

  isStep3Disabled: function () {
    return this.get('controller.currentStep') < 3;
  }.property('controller.currentStep'),

  isStep4Disabled: function () {
    return this.get('controller.currentStep') < 4;
  }.property('controller.currentStep'),

  actions: {
    hide: function () {
      this.hidePopup();
    },
    finish: function () {
      this.hidePopup();
    }
  },

  hidePopup: function () {
    $(this.get('element')).find('.modal').hide();
    this.get('controller').transitionToRoute('slider_apps');
  },

  setHeight: function () {
    var height = $(window).height() * 0.8;
    $('.slider-modal-body').css('max-height', height + 'px');
    $('#createAppWizard').css('margin-top', -(height / 2) + 'px');
  }
});
