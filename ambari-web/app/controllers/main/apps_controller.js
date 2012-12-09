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
  filteredRuns: [],
  routeHome:function () {
    App.router.transitionTo('main.dashboard');
    var view = Ember.View.views['main_menu'];
    $.each(view._childViews, function () {
      this.set('active', this.get('content.routing') == 'dashboard' ? "active" : "");
    });
  },
  clearFilteredRuns: function() {
    this.set('filteredRuns', []);
    this.set('filteredRunsLength', 0);
  },
  addFilteredRun: function(id) {
    this.get('filteredRuns').push(this.getRunById(id));
    this.set('filteredRunsLength', this.get('filteredRuns').length);
  },
  /**
   * Get run by id
   * @param id run identifier (NOT runId)
   * @return {*} run if exists, undefined - not exists
   */
  getRunById: function(id) {
    var r;
    this.get('content').forEach(function(item){
      if (item.get('id') == id) {
        r = item;
      }
    });
    return r;
  },
  /**
   * Check if run with such id exists
   * @param id run identifier (NOT runId)
   * @return {Boolean} true - record with this id exists, false - not exists
   */
  issetStaredRun: function(id) {
    var r = false;
    this.get('staredRuns').forEach(function(item){
      if (item.get('id') == id) {
        r = true;
      }
    });
    return r;
  },
  /**
   * Identifier of the last starred/unstarred run
   */
  lastStarClicked: -1,
  /**
   * Click on star on table row
   * @return {Boolean} false for prevent default event handler
   */
  starClick: function(event) {
    $(event.target).closest('table').find('.containerRow').remove(); // hack for valid "turning-off" star in table, where graphs row where enabled. We remove it
    event.target.classList.toggle('stared');
    var cell = event.target.parentNode.parentNode;
    var row = cell.parentNode;
    var id = parseInt(jQuery(event.target).parent().children(1).text());
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
    this.set('lastStarClicked', id);
    return false;
  }
})
