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
    var totalSteps = 10
    for (var step = 1; step <= totalSteps; step++){
      this.get('installerStep' + step + 'Controller').clearStep();
    }
  },

  /*
  loadAllPriorSteps: function(step) {
    var stepVal = parseInt(step);
    switch(step){
      case '10':
        this.get('installerStep9Controller').loadStep();
      case '9':
        this.get('installerStep8Controller').loadStep();
      case '8':
        this.get('installerStep7Controller').loadStep();
      case '7':
        this.get('installerStep6Controller').loadStep();
      case '6':
        this.get('installerStep5Controller').loadStep();
      case '5':
        this.get('installerStep4Controller').loadStep();
      case '4':
        this.get('installerStep3Controller').loadStep();
      case '3':
        this.get('installerStep2Controller').loadStep();
      case '2':
        this.get('installerStep1Controller').loadStep();
      case '1':

    }
  },
  */

  setInstallerCurrentStep: function (currentStep, completed) {
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

  /**
   * Get current step for <code>wizardType</code> wizard
   * @param wizardType one of <code>installer</code>, <code>addHost</code>, <code>addServices</code>
   */
  getWizardCurrentStep: function (wizardType) {
    var loginName = this.getLoginName();
    var currentStep = App.db.getWizardCurrentStep(wizardType);
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

  login: function (loginName, user) {
    // TODO: this needs to be hooked up with server authentication
    console.log("In login function");
    this.setAuthenticated(true);
    this.setLoginName(loginName);

//    refactor to get user attributes
    this.setUser(user);

    this.transitionTo(this.getSection());

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

  authenticated: function () {
    var authenticated = false;
    var controller = this.get('loginController');
    var hash = ''; //window.btoa(controller.get('loginName') + ":" + controller.get('password'));
    $.ajax({
      url : '/api/check',
      dataType : 'json',
      type: 'GET',
      beforeSend: function(xhr) {
        xhr.setRequestHeader("Authorization", "Basic " + hash);
      },
      statusCode:{
        200:function(){
          console.log('Authorization status: 200');
          authenticated = true;
        },
        401:function(){
          console.log('Authorization status: 401');
        },
        403:function(){
          console.log('Authorization status: 403');
        }
      },
      success: function(data){
        console.log('Success: ');
      },
      error:function (req){
        console.log("Error: " + req.statusText);
      }
    });
//    this.resetAuth(authenticated);
    this.setAuthenticated(authenticated);

    return this.getAuthenticated();
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
      router.clearAllSteps();
      App.db.cleanUp();
      router.set('loginController.loginName', '');
      router.set('loginController.password', '');
      router.transitionTo('login', context);
    }

  })
})
