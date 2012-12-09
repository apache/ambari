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

App.MainServiceMenuView = Em.CollectionView.extend({
  content: function () {
    return App.router.get('mainServiceController.content');
  }.property('App.router.mainServiceController.content'),

  init: function () {
    this._super();
    this.renderOnRoute();
  },
  didInsertElement: function () {
    App.router.location.addObserver('lastSetURL', this, 'renderOnRoute');
    this.renderOnRoute();
  },

  /**
   *    Syncs navigation menu with requested URL
   */
  renderOnRoute: function () {
    var last_url = App.router.location.lastSetURL || location.href.replace(/^[^#]*#/, '');
    if (last_url.substr(1, 4) !== 'main' || !this._childViews) {
      return;
    }
    var reg = /^\/main\/services\/(\d+)/g;
    var sub_url = reg.exec(last_url);
    var service_id = (null != sub_url) ? sub_url[1] : 1;
    $.each(this._childViews, function () {
      this.set('active', this.get('content.id') == service_id ? "active" : "");
    });
  },

  tagName: 'ul',
  classNames: ["nav", "nav-list", "nav-services"],

  activateView: function () {
    var service = App.router.get('mainServiceItemController.content');
    $.each(this._childViews, function () {
      this.set('active', (this.get('content.serviceName') == service.get('serviceName') ? "active" : ""));
    });
  }.observes("App.router.mainServiceItemController.content"),

  itemViewClass: Em.View.extend({
    classNameBindings: ["active"],
    active: "",
    serviceOperationsCount: function () {
      var operations = App.router.get('backgroundOperationsController').getOperationsFor(this.get('content.serviceName'));
      return operations.length;
    }.property('App.router.backgroundOperationsController.serviceOperationsChangeTime'),

    templateName: require('templates/main/service/menu_item')
  })
});