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
    '<h3 id="modal-label">{{view.header}}</h3>',
    '</div>',
    '<div class="modal-body">',
    '{{#if bodyClass}}{{view bodyClass}}',
    '{{else}}{{body}}{{/if}}',
    '</div>',
    '<div class="modal-footer">',
    '{{#if view.secondary}}<a class="btn" {{action onSecondary target="view"}}>{{view.secondary}}</a>{{/if}}',
    '{{#if view.primary}}<a class="btn btn-success" {{action onPrimary target="view"}}>{{view.primary}}</a>{{/if}}',
    '</div>',
    '</div>'
  ].join('\n')),

  header: '&nbsp;',
  body: '&nbsp;',
  // define bodyClass which extends Ember.View to use an arbitrary Handlebars template as the body
  primary: 'OK',
  secondary: 'Cancel',

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
  }

});

App.ModalPopup.reopenClass({

  show: function(options) {
    var popup = this.create(options);
    popup.appendTo('#wrapper');
  }

})

