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
require('utils/jquery.unique');

App.MainAppsController = Em.ArrayController.extend({
  name:'mainAppsController',
  content:App.Run.find(),
  apps:App.App.find(),
  staredRuns: [],
  routeHome:function () {
    App.router.transitionTo('main.dashboard');
    var view = Ember.View.views['main_menu'];
    $.each(view._childViews, function () {
      this.set('active', this.get('content.routing') == 'dashboard' ? "active" : "");
    });
  },
  getRunById: function(id) {
    var r;
    this.get('content').forEach(function(item){
      if (item.get('id') == id) {
        r = item;
      }
    });
    return r;
  },
  issetStaredRun: function(id) {
    r = false;
    this.get('staredRuns').forEach(function(item){
      if (item.get('id') == id) {
        r = true;
      }
    });
    return r;
  },
  starClick: function() {
    console.log(event);
    event.srcElement.classList.toggle('stared');
    var id = parseInt(event.srcElement.parentNode.childNodes[1].innerText);
    if (!this.issetStaredRun(id)) {
      this.get('staredRuns').push(this.getRunById(id));
    }
    else {
      var key = this.get('staredRuns').indexOf(this.getRunById(id));
      if (key != -1) {
        this.get('staredRuns').splice(key, 1);
      }
    }
    this.set('staredRunsLength', this.get('staredRuns').length);
    return false;
  },
  clearStars: function() {
    this.set('staredRuns', []);
    this.set('staredRunsLength', 0);
    $('.icon-star').removeClass('stared');
  },
  /**
   * Row, which is expanded at the moment, will update this property.
   * Used to collapse rows, which are not used at the moment
   */
  expandedRowId : null
})
