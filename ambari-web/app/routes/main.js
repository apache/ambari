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



module.exports = Em.Route.extend({

  route: '/main',

  enter: function (router) {

    console.log('in /main:enter');

    if (router.getAuthenticated()) {
      // TODO: redirect to last known state
      /*
       Ember.run.next(function () {
       router.transitionTo('step' + router.getInstallerCurrentStep());
       });
       */
    } else {
      Ember.run.next(function () {
        router.transitionTo('login');
      });
    }
  },

  connectOutlets: function (router, context) {
    router.get('applicationController').connectOutlet('main');
  },

  charts:Em.Route.extend({
    route:'/charts',
    connectOutlets:function (router, context) {
      router.get('mainController').connectOutlet('mainCharts');
    }
  }),

  hosts:Em.Route.extend({
    route:'/hosts',
    connectOutlets:function (router, context) {
      router.get('mainController').connectOutlet('mainHosts');
    }
  }),

  admin:Em.Route.extend({
    route:'/admin',
    connectOutlets:function (router, context) {
      router.get('mainController').connectOutlet('mainAdmin');
    }
  }),

  dashboard:Em.Route.extend({
    route:'/dashboard',
    connectOutlets:function (router, context) {
      router.get('mainController').connectOutlet('mainDashboard');
    }
  }),

  service:Em.Route.extend({
    route:'/services',
    enter:function (router) {
      Ember.run.next(function () {
        var service = router.get('mainServiceItemController.content');
        if (!service) {
          service = App.Service.find(1); // getting the first service to display
        }
        router.transitionTo('advanced', service);
      });
    },

    connectOutlets:function (router, context) {
      router.get('mainController').connectOutlet('mainService');
    },

    advanced:Em.Route.extend({
      route:'/:name',
      connectOutlets:function (router, service) {
        router.get('mainServiceController').connectOutlet('mainServiceItem', service);
      }
    }),

    showService: Em.Router.transitionTo('advanced')
  }),

  navigate:function (router, event) {
    var parent = event.view._parentView;
    parent.deactivateChildViews();
    event.view.set('active', "active");
    router.transitionTo(event.context);
  }

  // TODO: create new routes here
  // dashboard
  // charts
  // hosts
  // hosts/:hostname
  // admin
  // etc...


});