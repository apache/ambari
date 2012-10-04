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
App.Router = Em.Router.extend({

  enableLogging: true,
  isFwdNavigation: true,

  setNavigationFlow: function (step) {
    var newStep = parseInt(step.slice(-1));
    var previousStep = parseInt(this.getInstallerCurrentStep());
    if (newStep >= previousStep) {
      this.set('isFwdNavigation', true);
    } else {
      this.set('isFwdNavigation', false);
    }
  },

  setInstallerCurrentStep: function (currentStep, completed) {
    var loginName = this.getLoginName();
    App.db.setInstallerCurrentStep(currentStep, completed);
    this.set('installerController.currentStep', currentStep);
  },

  getInstallerCurrentStep: function () {
    var loginName = this.getLoginName();
    var currentStep = App.db.getInstallerCurrentStep();
    console.log('getInstallerCurrentStep: loginName=' + loginName + ", currentStep=" + currentStep);
    if (!currentStep) {
      currentStep = '1';
    }
    console.log('returning currentStep=' + currentStep);
    return currentStep;
  },

  loggedIn: false,

  getAuthenticated: function () {
    // TODO: this needs to be hooked up with server authentication
    var auth = App.db.getAuthenticated();
    var authResp = (auth && auth === true);
    this.set('loggedIn', authResp);
    return authResp;
  },

  setAuthenticated: function (authenticated) {
    // TODO: this needs to be hooked up with server authentication
    console.log("TRACE: Entering router:setAuthenticated function");
    App.db.setAuthenticated(authenticated);
    this.set('loggedIn', authenticated);
  },

  getLoginName: function () {
    // TODO: this needs to be hooked up with server authentication
    return App.db.getLoginName();
    //return localStorage.getItem('Ambari' + 'loginName');
  },

  setLoginName: function (loginName) {
    // TODO: this needs to be hooked up with server authentication
    App.db.setLoginName(loginName);
    //localStorage.setItem('Ambari' + 'loginName', loginName);
  },

  // that works incorrectly
  setUser: function (user) {
    App.db.setUser(user);
  },
  // that works incorrectly
  getUser: function () {
    return App.db.getUser();
  },

  login: function (loginName, user) {
    // TODO: this needs to be hooked up with server authentication
    console.log("In login function");
    this.setAuthenticated(true);
    this.setLoginName(loginName);

//    refactor to get user attributes
//    this.setUser(user);

    this.transitionTo(this.getSection());

  },

  defaultSection: 'installer',

  getSection: function () {
    var section = App.db.getSection();
    console.log("The section is: " + section);
    var section = localStorage.getItem(this.getLoginName() + 'section');

    return section || this.defaultSection;

  },

  setSection: function (section) {
    App.db.setSection(section);
  },

  root: Em.Route.extend({
    index: Em.Route.extend({
      route: '/',
      redirectsTo: 'login'
    }),

    login: Em.Route.extend({
      route: '/login',

      /**
       *  If the user is already logged in, redirect to where the user was previously
       */
      enter: function (router, context) {
        if (router.getAuthenticated()) {
          Ember.run.next(function () {
            console.log(router.getLoginName() + ' already authenticated.  Redirecting...');
            router.transitionTo(router.getSection(), context);
          });
        }
      },

      connectOutlets: function (router, context) {
        console.log('/login:connectOutlet');
        console.log('currentStep is: ' + router.getInstallerCurrentStep());
        console.log('authenticated is: ' + router.getAuthenticated());
        router.get('applicationController').connectOutlet('login', App.LoginView);
      }
    }),

    installer: require('routes/installer'),

    main: require('routes/main'),

    logoff: function (router, context) {
      console.log('logging off');
      App.db.cleanUp();
      router.set('loginController.loginName', '');
      router.set('loginController.password', '');
      router.transitionTo('login', context);
    }

  })
})
