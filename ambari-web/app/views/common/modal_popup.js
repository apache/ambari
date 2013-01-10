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

  template: Ember.Handlebars.compile([
    '<div class="modal-backdrop"></div><div class="modal" id="modal" tabindex="-1" role="dialog" aria-labelledby="modal-label" aria-hidden="true">',
    '<div class="modal-header">',
    '<a class="close" {{action onClose target="view"}}>x</a>',
    '<h3 id="modal-label">',
    '{{#if headerClass}}{{view headerClass}}',
    '{{else}}{{header}}{{/if}}',
    '</h3>',
    '</div>',
    '<div class="modal-body">',
    '{{#if bodyClass}}{{view bodyClass}}',
    '{{else}}{{#if encodeBody}}{{body}}{{else}}{{{body}}}{{/if}}{{/if}}',
    '</div>',
    '{{#if showFooter}}',
    '<div class="modal-footer">',
    '{{#if view.secondary}}<a class="btn" {{action onSecondary target="view"}}>{{view.secondary}}</a>{{/if}}',
    '{{#if view.primary}}<a class="btn btn-success" {{action onPrimary target="view"}}>{{view.primary}}</a>{{/if}}',
    '</div>',
    '{{/if}}',
    '</div>'
  ].join('\n')),

  header: '&nbsp;',
  body: '&nbsp;',
  encodeBody: true,
  // define bodyClass which extends Ember.View to use an arbitrary Handlebars template as the body
  primary: 'OK',
  secondary: 'Cancel',
  autoHeight: true,

  onPrimary: function() {
  },

  onSecondary: function() {
    this.hide();
  },

  onClose: function() {
    this.hide();
  },

  hide: function() {
    this.destroy();
  },

  showFooter: true,

  didInsertElement: function(){
    if(this.autoHeight){
      this._super();
      var block = this.$().find('#modal > .modal-body').first();
      block.css('max-height', $(window).height()- block.offset().top - 100); // fix popup height
    }
  }
});

App.ModalPopup.reopenClass({

  show: function(options) {
    var popup = this.create(options);
    popup.appendTo('#wrapper');
  }

})

App.showReloadPopup = function(){
  return App.ModalPopup.show({
    primary: null,
    secondary: null,
    showFooter: false,
    header: this.t('app.reloadPopup.header'),
    body: "<div class='alert alert-info'><div class='spinner'>" + this.t('app.reloadPopup.text') + "</div></div><div><a href='#' onclick='location.reload();'>" + this.t('app.reloadPopup.link') + "</a></div>",
    encodeBody: false
  });
}