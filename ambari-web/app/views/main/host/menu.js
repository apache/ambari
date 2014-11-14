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

App.MainHostMenuView = Em.CollectionView.extend({
  tagName: 'ul',
  classNames: ["nav", "nav-tabs"],
  content: function () {
    var array = [
      {
        label: Em.I18n.t('common.summary'),
        routing: 'summary'
      },
      {
        label: Em.I18n.t('common.configs'),
        routing: 'configs'
      }
      /* { label:'Audit', routing:'audit'} */
    ];
    if (App.get('supports.stackUpgrade')) {
      array.push({
        label: Em.I18n.t('hosts.host.menu.stackVersions'),
        routing: 'stackVersions'
      });
    }
    if (App.get('supports.alerts')) {
      array.push({
        label: 'Alerts',
        routing: 'alerts'
      });
    }
    return array;
  }.property(''),

  init: function(){ this._super(); this.activateView(); },

  activateView: function () {
    var defaultRoute = App.router.get('currentState.name') || "summary";
    $.each(this._childViews, function () {
      this.set('active', (this.get('content.routing') == defaultRoute ? "active" : ""));
    });
  },

  deactivateChildViews: function() {
    $.each(this._childViews, function(){
      this.set('active', "");
    });
  },

  itemViewClass: Em.View.extend({
    classNameBindings: ["active"],
    active: "",
    template: Ember.Handlebars.compile('<a {{action hostNavigate view.content.routing }} href="#"> {{unbound view.content.label}}</a>')
  })
});