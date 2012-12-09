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
  backBtnForHigherStep: false,

  setNavigationFlow: function (step) {
    var matches = step.match(/\d+$/);
    var newStep;
    if (matches) {
      newStep = parseInt(matches[0]);
    }
    var previousStep = parseInt(this.getInstallerCurrentStep());
    this.set('isFwdNavigation', newStep >= previousStep);
  },

  clearAllSteps: function() {
    this.set('installerController.content', []);
    this.set('installerController.currentStep', 0);
    this.set('wizardStep2Controller.hasSubmitted', false);
  },

  /**
   * Temporary fix for getting cluster name
   * @return {*}
   */
  getClusterName: function(){
    return App.router.get('clusterController').get('clusterName');
  },


  /**
   * Get current step of Installer wizard
   * @return {*}
   */
  getInstallerCurrentStep: function () {
    return this.getWizardCurrentStep('installer');
  },

  /**
   * Get current step for <code>wizardType</code> wizard
   * @param wizardType one of <code>installer</code>, <code>addHost</code>, <code>addServices</code>
   */
  getWizardCurrentStep: function (wizardType) {
    var loginName = this.getLoginName();
    var currentStep = App.db.getWizardCurrentStep(wizardType);
    console.log('getWizardCurrentStep: loginName=' + loginName + ", currentStep=" + currentStep);
    if (!currentStep) {
      currentStep = '1';
    }
    console.log('returning currentStep=' + currentStep);
    return currentStep;
  },

  loggedIn: false,

  getAuthenticated: function () {
    // TODO: this needs to be hooked up with server authentication
//    this.authenticated();
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

  },

  setLoginName: function (loginName) {
    // TODO: this needs to be hooked up with server authentication
    App.db.setLoginName(loginName);
  },

  // that works incorrectly
  setUser: function (user) {
    App.db.setUser(user);
  },
  // that works incorrectly
  getUser: function () {
    return App.db.getUser();
  },

  resetAuth: function (authenticated) {
    if (!authenticated){
      App.db.cleanUp();
      this.set('loggedIn', false);
      this.set('loginController.loginName', '');
      this.set('loginController.password', '');
      this.transitionTo('login');
    }
    return authenticated;
  },

  login: function (postLogin) {
    var controller = this.get('loginController');
    var loginName = controller.get('loginName');
    var hash = window.btoa(loginName + ":" + controller.get('password'));
    var router = this;

    $.ajax({
      url : '/api/users/' + loginName,
      dataType : 'text',
      type: 'GET',
      beforeSend: function(xhr) {
        xhr.setRequestHeader("Authorization", "Basic " + hash);
      },
      statusCode: {
        200: function() {
          console.log('Authorization status: 200');
        },
        401: function() {
          console.log('Authorization status: 401');
        },
        403: function(){
          console.log('Authorization status: 403');
        }
      },
      success: function (data) {
        console.log('login success');

        var resp = $.parseJSON(data);
        var isAdmin = resp.Users.roles.indexOf('admin') >= 0;

        router.setAuthenticated(true);
        router.setLoginName(loginName);

        router.setUser(App.store.createRecord(App.User, { userName: loginName, admin: isAdmin }));
        router.transitionTo(router.getSection());
        postLogin(true);
      },
      error: function (req) {
        console.log("login error: " + req.statusCode);
        router.setAuthenticated(false);
        postLogin(false);
      }
    });
  },

  mockLogin: function (postLogin) {
    var controller = this.get('loginController');
    var loginName = controller.get('loginName');

    if ((loginName === 'admin' && controller.get('password') === 'admin') ||
        (loginName === 'user' && controller.get('password') === 'user')) {
      this.setAuthenticated(true);
      this.setLoginName(loginName);
      this.setUser(App.store.createRecord(App.User, { userName: loginName, admin: loginName === 'admin' }));
      this.transitionTo(this.getSection());
      postLogin(true);
    } else {
      this.setAuthenticated(false);
      postLogin(false);
    }
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
      // App.db.cleanUp() must be called before router.clearAllSteps().
      // otherwise, this.set('installerController.currentStep, 0) would have no effect
      // since it's a computed property but we are not setting it as a dependent of App.db.
      App.db.cleanUp();
      router.clearAllSteps();
      console.log("Log off: " + App.db.getClusterName());
      router.set('loginController.loginName', '');
      router.set('loginController.password', '');
      router.transitionTo('login', context);
    }

  })
})
