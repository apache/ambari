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
    this.get('controller').loadStep();
  },

  /**
   * Print review-report
   * @method printReview
   */
  printReview: function () {
    var o = $("#step8-info");
    o.jqprint();
  },

  /**
   * Reference to modalPopup to make sure only one instance is created
   * @type {App.ModalPopup|null}
   */
  modalPopup: null,

  /**
   * Should ajax-queue progress bar be displayed
   * @method showLoadingIndicator
   */
  showLoadingIndicator: function () {
    if (!this.get('controller.isSubmitDisabled') || App.get('testMode')) {
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

      bodyClass: Em.View.extend({

        templateName: require('templates/wizard/step8/step8_log_popup'),

        controllerBinding: 'App.router.wizardStep8Controller',

        /**
         * Css-property for progress-bar
         * @type {string}
         */
        barWidth: '',

        /**
         * Popup-message
         * @type {string}
         */
        message: '',

        /**
         * Set progress bar width and popup message when ajax-queue requests are proccessed
         * @method ajaxQueueChangeObs
         */
        ajaxQueueChangeObs: function () {
          var length = this.get('controller.ajaxQueueLength');
          var left = this.get('controller.ajaxRequestsQueue.queue.length');
          this.set('barWidth', 'width: ' + ((length - left) / length * 100) + '%;');
          this.set('message', Em.I18n.t('installer.step8.deployPopup.message').format((length - left), length));
        }.observes('controller.ajaxQueueLength', 'controller.ajaxRequestsQueue.queue.length'),

        /**
         * Hide popup when ajax-queue is finished
         * @method autoHide
         */
        autoHide: function () {
          if (this.get('controller.servicesInstalled')) {
            this.get('parentView').hide();
          }
        }.observes('controller.servicesInstalled')
      })

    }));
  }.observes('controller.isSubmitDisabled')
});

