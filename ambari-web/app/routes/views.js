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

module.exports = Em.Route.extend({
  route: '/views',
  enter: function (router) {
    router.get('mainViewsController').loadAmbariViews();
  },
  index: Em.Route.extend({
    route: '/',
    connectOutlets: function (router) {
      router.get('mainViewsController').dataLoading().done(function() {
        router.get('mainController').connectOutlet('mainViews');
      });
    }
  }),
  viewDetails: Em.Route.extend({
    route: '/:viewName/:version/:instanceName',
    connectOutlets: function (router, params) {
      // find and set content for `mainViewsDetails` and associated controller
      router.get('mainViewsController').dataLoading().done(function() {
        router.get('mainController').connectOutlet('mainViewsDetails', App.router.get('mainViewsController.ambariViews')
          .findProperty('href', ['/views', params.viewName, params.version, params.instanceName].join('/')));
      });
    }
  })
});
