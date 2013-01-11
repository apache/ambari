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
  content: function(){
    return App.Run.find();
  }.property('App.router.clusterController.postLoadList.runs'),
  /**
   * Mark all Runs as not Filtered
   */
  clearFilteredRuns: function() {
    this.get('content').setEach('isFiltered', false);
    this.set('filteredRunsLength', 0);
  },
  /**
   * Mark Run as filtered
   * @param id runId
   */
  addFilteredRun: function(id) {
    this.get('content').findProperty('id', id).set('isFiltered', true);
    this.set('filteredRunsLength', this.get('content').filterProperty('isFiltered', true).length);
  },
  /**
   * Mark Runs as filtered
   * @param ids array of Run id
   */
  filterFilteredRuns: function(ids) {
    this.get('content').filter(function(item) {
      if ($.inArray(item.get('id'), ids) !== -1) {
        item.set('isFiltered', true);
      }
    });
    this.set('filteredRunsLength', this.get('content').filterProperty('isFiltered', true).length);
  },
  /**
   * Identifier of the last starred/unstarred run
   */
  lastStarClicked: null,
  /**
   * Starred Runs count
   */
  staredRunsLength: function() {
    return this.get('content').filterProperty('isStared', true).length;
  }.property('content'),
  /**
   * Click on star on table row
   * @return {Boolean} false for prevent default event handler
   */
  starClick: function(event) {
    event.target.classList.toggle('stared');
    var id = jQuery(event.target).parent().parent().parent().find('.appId').attr('title');
    var run = this.get('content').findProperty('id', id);
    if (run) {
      run.set('isStared', !run.get('isStared'));
    }
    this.set('staredRunsLength', this.get('content').filterProperty('isStared', true).length);
    this.set('lastStarClicked', id);
    return false;
  }
})
