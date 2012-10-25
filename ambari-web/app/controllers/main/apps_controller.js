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

App.MainAppsController = Em.ArrayController.extend({
  name:'mainAppsController',
  content:App.App.find(),
  routeHome:function () {
    App.router.transitionTo('main.dashboard');
    var view = Ember.View.views['main_menu'];
    $.each(view._childViews, function () {
      this.set('active', this.get('content.routing') == 'dashboard' ? "active" : "");
    });
  },
  arrayUnique:function (array) {
    var result = new Array();
    var i, j, unique;
    for (i = 0; i < array.length; i++) {
      j = 0;
      unique = true;
      do {
        if (array[i] === result[j]) unique = false;
        j++;
      } while (j < result.length);
      if (unique) result.push(array[i]);
    }
    return result;
  }
})
