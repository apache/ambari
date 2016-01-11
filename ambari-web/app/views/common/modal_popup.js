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
  marginBottom: 300,
  disablePrimary: false,
  disableSecondary: false,
  disableThird: false,
  primaryClass: 'btn-success',
  secondaryClass: '',
  thirdClass: '',
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
    if (this.autoHeight && !$.mocho) {
      var block = this.$().find('#modal > .modal-body').first();
      if(block.offset()) {
        block.css('max-height', $(window).height() - block.offset().top - this.marginBottom + $(window).scrollTop()); // fix popup height
      }
    }
    this.fitZIndex();
    var firstInputElement = this.$('#modal').find(':input').not(':disabled, .no-autofocus').first();
    this.focusElement(firstInputElement);
  },

  /**
   * If popup is opened from another popup it should be displayed above
   * @method fitZIndex
   */
  fitZIndex: function () {
    var existedPopups = $('.modal-backdrop');
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

  focusElement: function(elem) {
    elem.focus();
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
