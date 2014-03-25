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

App.WizardStep8View = Em.View.extend({

  templateName: require('templates/wizard/step8'),

  didInsertElement: function () {
    var controller = this.get('controller');
    controller.loadStep();
  },

  spinner : null,

  printReview: function() {
    var o = $("#step8-info");
    o.jqprint();
  },

  ajaxQueueLength: function() {
    return this.get('controller.ajaxQueueLength');
  }.property('controller.ajaxQueueLength'),

  ajaxQueueLeft: function() {
    return this.get('controller.ajaxQueueLeft');
  }.property('controller.ajaxQueueLeft'),

  // reference to modalPopup to make sure only one instance is created
  modalPopup: null,

  showLoadingIndicator: function() {
    if (!this.get('controller.isSubmitDisabled') || App.testMode) {
      if (this.get('modalPopup')) {
        this.get('modalPopup').hide();
        this.set('modalPopup', null);
      }
      return;
    }
    // don't create popup if it already exists
    if (this.get('modalPopup')) {
      return;
    }
    this.set('modalPopup', App.ModalPopup.show({
      header: '',

      showFooter: false,

      showCloseButton: false,

      bodyClass: Ember.View.extend({
        templateName: require('templates/wizard/step8_log_popup'),

        message: function() {
          return Em.I18n.t('installer.step8.deployPopup.message').format(this.get('ajaxQueueComplete'), this.get('ajaxQueueLength'));
        }.property('ajaxQueueComplete', 'ajaxQueueLength'),

        controllerBinding: 'App.router.wizardStep8Controller',

        ajaxQueueLength: function() {
          return this.get('controller.ajaxQueueLength');
        }.property(),

        ajaxQueueComplete: function() {
          return this.get('ajaxQueueLength') - this.get('controller.ajaxQueueLeft');
        }.property('controller.ajaxQueueLeft', 'ajaxQueueLength'),

        barWidth: function () {
          return 'width: ' + (this.get('ajaxQueueComplete') / this.get('ajaxQueueLength') * 100) + '%;';
        }.property('ajaxQueueComplete', 'ajaxQueueLength'),

        autoHide: function() {
          if (this.get('controller.servicesInstalled')) {
            this.get('parentView').hide();
          }
        }.observes('controller.servicesInstalled')
      })
    }));
  }.observes('controller.isSubmitDisabled')
});

