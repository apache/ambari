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

App.ModalPopup = Ember.View.extend({

  viewName: 'modalPopup',
  templateName: require('templates/common/modal_popup'),
  header: '&nbsp;',
  body: '&nbsp;',
  encodeBody: true,
  // define bodyClass which extends Ember.View to use an arbitrary Handlebars template as the body
  primary: Em.I18n.t('ok'),
  secondary: Em.I18n.t('common.cancel'),
  third: null,
  autoHeight: true,
  disablePrimary: false,
  disableSecondary: false,
  disableThird: false,
  primaryClass: 'btn-success',
  onPrimary: function () {
    this.hide();
  },

  onSecondary: function () {
    this.hide();
  },

  onThird: function () {
    this.hide();
  },

  onClose: function () {
    this.hide();
  },

  hide: function () {
    this.destroy();
  },

  showFooter: true,

  /**
   * Hide or show 'X' button for closing popup
   */
  showCloseButton: true,

  didInsertElement: function () {
    if (this.autoHeight) {
      var block = this.$().find('#modal > .modal-body').first();
      block.css('max-height', $(window).height() - block.offset().top - 300 + $(window).scrollTop()); // fix popup height
    }
    // If popup is opened from another popup it should be displayed above
    var existedPopups = $(document).find('.modal-backdrop');
    if (existedPopups) {
      var maxZindex = 1;
      existedPopups.each(function(index, popup) {
        if ($(popup).css('z-index') > maxZindex) {
          maxZindex = $(popup).css('z-index');
        }
      });
      this.$().find('.modal-backdrop').css('z-index', maxZindex * 2);
      this.$().find('.modal').css('z-index', maxZindex * 2 + 1);
    }
  },

  fitHeight: function () {
    var popup = this.$().find('#modal');
    var block = this.$().find('#modal > .modal-body');
    var wh = $(window).height();

    var top = wh * .05;
    popup.css({
      'top': top + 'px',
      'marginTop': 0
    });

    block.css('max-height', $(window).height() - top * 2 - (popup.height() - block.height()));
  }
});

App.ModalPopup.reopenClass({

  show: function (options) {
    var popup = this.create(options);
    popup.appendTo('#wrapper');
    return popup;
  }

});

App.showReloadPopup = function () {
  return App.ModalPopup.show({
    primary: null,
    secondary: null,
    showFooter: false,
    header: this.t('app.reloadPopup.header'),
    body: "<div id='reload_popup' class='alert alert-info'><div class='spinner'><span>" + this.t('app.reloadPopup.text') + "</span></div></div><div><a href='#' onclick='location.reload();'>" + this.t('app.reloadPopup.link') + "</a></div>",
    encodeBody: false
  });
};

/**
 * Show confirmation popup
 *
 * @param {Function} primary - "OK" button click handler
 * @param {String} body - additional text constant. Will be placed in the popup-body
 * @param {Function} secondary
 * @return {*}
 */
App.showConfirmationPopup = function (primary, body, secondary) {
  if (!primary) {
    return false;
  }
  return App.ModalPopup.show({
    encodeBody: false,
    header: Em.I18n.t('popup.confirmation.commonHeader'),
    body: body || Em.I18n.t('question.sure'),
    onPrimary: function () {
      this.hide();
      primary();
    },
    onSecondary: function () {
      this.hide();
      if (secondary) {
        secondary();
      }
    },
    onClose:  function () {
      this.hide();
      if (secondary) {
        secondary();
      }
    }
  });
};

/**
 * Show confirmation popup
 * After sending command watch status of query,
 * and in case of failure provide ability to retry to launch an operation.
 *
 * @param {Function} primary - "OK" button click handler
 * @param {Object} bodyMessage - confirmMsg:{String},
                                 confirmButton:{String},
                                 additionalWarningMsg:{String},
 * @param {Function} secondary - "Cancel" button click handler
 * @return {*}
 */
App.showConfirmationFeedBackPopup = function (primary, bodyMessage, secondary) {
  if (!primary) {
    return false;
  }
  return App.ModalPopup.show({
    header: Em.I18n.t('popup.confirmation.commonHeader'),
    bodyClass: Em.View.extend({
      templateName: require('templates/common/confirmation_feedback')
    }),
    query: Em.Object.create({status: "INIT"}),
    primary: function () {
      return bodyMessage? bodyMessage.confirmButton : Em.I18n.t('ok');
    }.property('bodyMessage'),
    onPrimary: function () {
      this.set('query.status', "INIT");
      this.set('disablePrimary', true);
      this.set('disableSecondary', true);
      this.set('statusMessage', Em.I18n.t('popup.confirmationFeedBack.sending'));
      this.hide();
      primary(this.get('query'), this.get('runMmOperation'));
    },
    statusMessage: function () {
      return bodyMessage? bodyMessage.confirmMsg : Em.I18n.t('question.sure');
    }.property('bodyMessage'),
    additionalWarningMsg: function () {
      return bodyMessage? bodyMessage.additionalWarningMsg : null;
    }.property('bodyMessage'),
    putInMaintenance: function () {
      return bodyMessage ? bodyMessage.putInMaintenance : null;
    }.property('bodyMessage'),
    runMmOperation: false,
    turnOnMmMsg: function () {
      return bodyMessage ? bodyMessage.turnOnMmMsg : null;
    }.property('bodyMessage'),
    watchStatus: function() {
      if (this.get('query.status') === "SUCCESS") {
        this.hide();
      } else if(this.get('query.status') === "FAIL") {
        this.set('primaryClass', 'btn-primary');
        this.set('primary', Em.I18n.t('common.retry'));
        this.set('disablePrimary', false);
        this.set('disableSecondary', false);
        this.set('statusMessage', Em.I18n.t('popup.confirmationFeedBack.query.fail'));
      }
    }.observes('query.status'),
    onSecondary: function () {
      this.hide();
      if (secondary) {
        secondary();
      }
    }
  });
};

/**
 * Show alert popup
 *
 * @param {String} header - header of the popup
 * @param {String} body - body of the popup
 * @param {Function} primary - function to call upon clicking the OK button
 * @return {*}
 */
App.showAlertPopup = function (header, body, primary) {
  return App.ModalPopup.show({
    primary: Em.I18n.t('ok'),
    secondary: null,
    header: header,
    body: body,
    onPrimary: function () {
      this.hide();
      if (primary) {
        primary();
      }
    }
  });
};

/**
 * Show prompt popup
 *
 * @param {String} text - additional text constant. Will be placed on the top of the input field
 * @param {Function} primary - "OK" button click handler
 * @param {String} defaultValue - additional text constant. Will be default value for input field
 * @param {Function} secondary
 * @return {*}
 */
App.showPromptPopup = function (text, primary, defaultValue, secondary) {
  if (!primary) {
    return false;
  }
  return App.ModalPopup.show({
    header: Em.I18n.t('popup.prompt.commonHeader'),
    bodyClass: Em.View.extend({
      templateName: require('templates/common/prompt_popup'),
      text: text
    }),
    inputValue: defaultValue || '',
    isInvalid: false,
    errorMessage: '',
    onPrimary: function () {
      this.hide();
      primary(this.get('inputValue'));
    },
    onSecondary: function () {
      this.hide();
      if (secondary) {
        secondary();
      }
    }
  });
};
