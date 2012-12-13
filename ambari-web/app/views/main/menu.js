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
      { label:'Dashboard', routing:'dashboard', active:'active'},
      { label:'Heatmaps', routing:'charts'},
      { label:'Services', routing:'services'},
      { label:'Hosts', routing:'hosts'},
      { label:'Jobs', routing:'apps'}

    ];
      if(App.db.getUser().admin) result.push({ label:'Admin', routing:'admin'});
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
      if (this.get('content').routing == 'dashboard') {
        return App.router.get('mainDashboardController.alertsCount');
      }
    }.property(),

//    hostDetailsOperationsCount:function () {
//      if (this.get('content').routing == 'hosts') {
//        if (App.router.currentState.parentState.name == 'hostDetails') {
//          return App.router.get('mainHostDetailsController.hostOperationsCount');
//        }
//      }
//    }.property('App.router.currentState.parentState.name', 'App.router.mainHostDetailsController.hostOperationsCount'),

    templateName: require('templates/main/menu_item')
  })
});