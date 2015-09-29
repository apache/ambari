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

App.MainServiceInfoMenuView = Em.CollectionView.extend({
  tagName: 'ul',
  classNames: ["nav", "nav-tabs", "background-text"],
  content:function(){
    var menuItems = [
      { label: Em.I18n.t('services.service.info.menu.summary'), id: 'summary-service-tab',routing:'summary', active:"active"}
      //{ label:'Audit', routing:'audit'}
    ];

    if(this.get('heatmapTab')) menuItems.push({ label: Em.I18n.t('services.service.info.menu.heatmaps'), id: 'heatmap-service-tab', routing:'heatmaps'});
    if(this.get('configTab')) menuItems.push({ label: Em.I18n.t('services.service.info.menu.configs'), id: 'configs-service-tab', routing:'configs'});
    return menuItems;
  }.property(),

  init: function(){ this._super(); this.activateView(); },

  activateView:function () {
    $.each(this._childViews, function () {
      this.set('active', (document.URL.endsWith(this.get('content.routing')) ? "active" : ""));
    });
  }.observes('App.router.location.lastSetURL'),

  deactivateChildViews: function() {
    $.each(this._childViews, function(){
      this.set('active', "");
    });
  },

  itemViewClass: Em.View.extend({
    classNameBindings: ["active"],
    active: "",
    template: Ember.Handlebars.compile('<a {{action showInfo view.content.routing}} {{bindAttr id="view.content.id"}} href="#"> {{unbound view.content.label}}</a>')
  })
});
