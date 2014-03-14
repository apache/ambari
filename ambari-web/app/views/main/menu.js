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

/**
 * this menu extended by other with modifying content and itemViewClass.template
 * @type {*}
 */
App.MainMenuView = Em.CollectionView.extend({
  tagName:'ul',
  classNames:['nav'],
  content:function(){
    var result = [
      { label:Em.I18n.t('menu.item.dashboard'), routing:'dashboard', active:'active'},
      { label:Em.I18n.t('menu.item.heatmaps'), routing:'charts'},
      { label:Em.I18n.t('menu.item.services'), routing:'services'},
      { label:Em.I18n.t('menu.item.hosts'), routing:'hosts'}
    ];

    if (App.supports.mirroring && App.Service.find().findProperty('serviceName', 'FALCON')) {
      result.push({ label:Em.I18n.t('menu.item.mirroring'), routing:'mirroring'});
    }

    if (!App.get('isHadoop2Stack')) {
      result.push({ label:Em.I18n.t('menu.item.jobs'), routing:'apps'});
    } else if( App.router.get('mainAdminController.isAccessAvailable') && App.supports.jobs
      && (App.router.get('mainAdminAccessController.showJobs') || App.get('isAdmin'))) {
      result.push({ label:Em.I18n.t('menu.item.jobs'), routing:'jobs'});
    }

    if (App.get('isAdmin')) {
      result.push({ label:Em.I18n.t('menu.item.admin'), routing:'admin'});
    }
    return result;
  }.property(),
    /**
     *    Adds observer on lastSetURL and calls navigation sync procedure
     */
  didInsertElement:function () {
    App.router.location.addObserver('lastSetURL', this, 'renderOnRoute');
    this.renderOnRoute();
  },

  /**
   *    Syncs navigation menu with requested URL
   */
  renderOnRoute:function () {
    var last_url = App.router.location.lastSetURL || location.href.replace(/^[^#]*#/, '');
    if (last_url.substr(1, 4) !== 'main' || !this._childViews) {
      return;
    }
    var reg = /^\/main\/([a-z]+)/g;
    var sub_url = reg.exec(last_url);
    var chunk = (null != sub_url) ? sub_url[1] : 'dashboard';
    $.each(this._childViews, function () {
      this.set('active', this.get('content.routing') == chunk ? "active" : "");
    });
  },

  itemViewClass:Em.View.extend({

    classNameBindings:['active', ':span2'],
    active:'',

    alertsCount:function () {
      if (this.get('content').routing == 'hosts') {
        return App.router.get('mainHostController.content').mapProperty('criticalAlertsCount')
          .reduce(function(pv, cv) { return pv + parseInt(cv); }, 0);
      }
    }.property('App.router.mainHostController.content.@each.criticalAlertsCount'),

    templateName: require('templates/main/menu_item')
  })
});
