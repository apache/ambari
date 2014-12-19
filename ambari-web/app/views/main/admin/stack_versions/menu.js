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

App.StackVersionMenuView = Em.CollectionView.extend({
  tagName: 'ul',
  classNames: ["nav", "nav-tabs"],
  content:function(){
    var menuItems = [
      { label: Em.I18n.t('common.installed'), routing:'versions', url:"versions", active:"active"},
      { label: Em.I18n.t('admin.stackVersions.updateTab.title.not.available'), routing:'updates', url:"versions/updates"}
    ];
    return menuItems;
  }.property(),

  init: function(){ this._super(); this.activateView(); },

  activateView:function () {
    var self = this;
    self.changeNewRepoCount();
    $.each(this._childViews, function () {
      this.set('active', self.getActive(this.get('content.routing')));
      this.set('label', self.updateLabel(this.get('content.routing'), this.get('content.label')));
    });
  }.observes('App.router.location.lastSetURL', 'controller.dataIsLoaded'),

  deactivateChildViews: function() {
    $.each(this._childViews, function(){
      this.set('active', "");
    });
  },

  /**
   * disable update available tab if there is no any updates
   * otherwise set active selected tab
   * @param routing
   * @returns {string}
   * @method getActive
   */
  getActive: function(routing) {
    if (routing == 'updates' && this.get('newRepoCount') == 0) {
      return 'not-active-link';
    }
    return document.URL.endsWith(routing) ? "active" : "";
  },

  /**
   * update label on updates tab if there is any new repo vreison
   * otherwise returns same label as is
   * @param {String} routing
   * @param {String} defauldLabel
   * @returns {string}
   * @method getActive
   */
  updateLabel: function(routing, defauldLabel) {
    if (routing == 'updates' && this.get('newRepoCount') > 0) {
      return Em.I18n.t('admin.stackVersions.updateTab.title.available').format(this.get('newRepoCount'));
    }
    return defauldLabel;
  },

  changeNewRepoCount: function() {
    this.set('newRepoCount', App.RepositoryVersion.find().filterProperty('stackVersion', null).get('length'));
  },

  newRepoCount: 0,

  itemViewClass: Em.View.extend({
    classNameBindings: ["active"],
    active: "",
    label: "",
    template: Ember.Handlebars.compile('<a href="#/main/admin/{{unbound view.content.url}}"> {{view.label}}</a>')
  })
});