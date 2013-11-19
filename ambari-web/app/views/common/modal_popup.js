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
  autoHeight: true,
  enablePrimary: true,
  onPrimary: function () {
    this.hide();
  },

  onSecondary: function () {
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
    body: "<div class='alert alert-info'><div class='spinner'><span>" + this.t('app.reloadPopup.text') + "</span></div></div><div><a href='#' onclick='location.reload();'>" + this.t('app.reloadPopup.link') + "</a></div>",
    encodeBody: false
  });
};

/**
 * Show confirmation popup
 *
 * @param {Function} primary - "OK" button click handler
 * @param {String} body - additional text constant. Will be placed in the popup-body
 * @return {*}
 */
App.showConfirmationPopup = function (primary, body, secondary) {
  if (!primary) {
    return false;
  }
  return App.ModalPopup.show({
    primary: Em.I18n.t('ok'),
    encodeBody: false,
    secondary: Em.I18n.t('common.cancel'),
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