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

App.WizardRoute = Em.Route.extend({
  isRoutable: function() {
    return (typeof this.get('route') === 'string' && App.router.get('loggedIn'));
  }.property('App.router.loggedIn')
});

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


  clearAllSteps: function () {
    this.get('installerController').clear();
    this.get('addHostController').clear();
    this.get('addServiceController').clear();
    this.get('stackUpgradeController').clear();
    for (var i = 1; i < 11; i++) {
      this.set('wizardStep' + i + 'Controller.hasSubmitted', false);
      this.set('wizardStep' + i + 'Controller.isDisabled', true);
    }
  },

  /**
   * Temporary fix for getting cluster name
   * @return {*}
   */

  getClusterName: function () {
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
      currentStep = wizardType === 'installer' ? '0' : '1';
    }
    console.log('returning currentStep=' + currentStep);
    return currentStep;
  },

  loggedIn: App.db.getAuthenticated(),

  loginName: function() {
    return this.getLoginName();
  }.property('loggedIn'),

  getAuthenticated: function () {
    var auth = App.db.getAuthenticated();
    var authResp = (auth && auth === true);
    if (authResp) {
      App.ajax.send({
        name: 'router.authentication',
        sender: this,
        success: 'onAuthenticationSuccess',
        error: 'onAuthenticationError'
      });
    } else {
      this.set('loggedIn', false);
    }
    return this.get('loggedIn');
  },

  onAuthenticationSuccess: function (data) {
    this.set('loggedIn', true);
  },

  onAuthenticationError: function (data) {
    if (data.status === 403) {
      this.set('loggedIn', false);
    } else {
      console.log('error in getAuthenticated');
    }
  },

  setAuthenticated: function (authenticated) {
    console.log("TRACE: Entering router:setAuthenticated function");
    App.db.setAuthenticated(authenticated);
    this.set('loggedIn', authenticated);
  },

  getLoginName: function () {
    return App.db.getLoginName();
  },

  setLoginName: function (loginName) {
    App.db.setLoginName(loginName);
  },

  /**
   * Set user model to local storage
   * @param user
   */
  setUser: function (user) {
    App.db.setUser(user);
  },

  /**
   * Get user model from local storage
   * @return {*}
   */
  getUser: function () {
    return App.db.getUser();
  },

  login: function () {
    var controller = this.get('loginController');
    var loginName = controller.get('loginName').toLowerCase();
    controller.set('loginName', loginName);
    var hash = window.btoa(loginName + ":" + controller.get('password'));
    var usr = '';

    if (App.testMode) {
      if (loginName === "admin" && controller.get('password') === 'admin') {
        usr = 'admin';
      } else if (loginName === 'user' && controller.get('password') === 'user') {
        usr = 'user';
      }
    }

    App.ajax.send({
      name: 'router.login',
      sender: this,
      data: {
        auth: "Basic " + hash,
        usr: usr,
        loginName: loginName
      },
      beforeSend: 'authBeforeSend',
      success: 'loginSuccessCallback',
      error: 'loginErrorCallback'
    });

  },

  authBeforeSend: function(opt, xhr, data) {
    xhr.setRequestHeader("Authorization", data.auth);
  },

  loginSuccessCallback: function(data, opt, params) {
    console.log('login success');
    var d = data;
    var isAdmin = data.Users.roles.indexOf('admin') >= 0;
    if (isAdmin) {
      App.set('isAdmin', true);
      var controller = this.get('loginController');
      this.setAuthenticated(true);
      this.setLoginName(params.loginName);
      App.usersMapper.map({"items": [data]});
      this.setUser(App.User.find(params.loginName));
      this.transitionTo(this.getSection());
      controller.postLogin(true);
    }
    else {
      App.ajax.send({
        name: 'router.login2',
        sender: this,
        data: {
          loginName: params.loginName,
          loginData: data
        },
        success: 'login2SuccessCallback',
        error: 'login2ErrorCallback'
      });
    }
  },

  loginErrorCallback: function(request, ajaxOptions, error, opt) {
    var controller = this.get('loginController');
    console.log("login error: " + error);
    this.setAuthenticated(false);
    controller.postLogin(false);
  },

  login2SuccessCallback: function (clusterResp, opt, params) {
    var controller = this.get('loginController');
    if (clusterResp.items.length) {
      this.setAuthenticated(true);
      this.setLoginName(params.loginName);
      App.usersMapper.map({"items": [params.loginData]});
      this.setUser(App.User.find(params.loginName));
      this.transitionTo(this.getSection());
      controller.postLogin(true);
    }
    else {
      controller.set('errorMessage', Em.I18n.t('router.hadoopClusterNotSetUp'));
    }
  },

  login2ErrorCallback: function (req) {
    console.log("Server not responding: " + req.statusCode);
  },

  setAmbariStacks: function () {
    App.ajax.send({
      name: 'router.set_ambari_stacks',
      sender: this,
      success: 'setAmbariStacksSuccessCallback',
      error: 'setAmbariStacksErrorCallback'
    });
  },

  setAmbariStacksSuccessCallback: function (jsonData) {
    console.log("TRACE: In success function for the setAmbariStacks call");
    var stacks = [];
    jsonData.forEach(function (_stack) {
      stacks.pushObject({
        name: _stack.name,
        version: _stack.version
      });
    }, this);
    App.db.setAmbariStacks(stacks);
    console.log('TRACEIINNGG: ambaristacks: ' + JSON.stringify(App.db.getAmbariStacks()));
  },

  setAmbariStacksErrorCallback: function (request, ajaxOptions, error) {
    console.log("TRACE: In error function for the setAmbariStacks call");
    console.log("TRACE: error code status is: " + request.status);
    console.log('Error message is: ' + request.responseText);
  },

  getSection: function () {
    if (App.testMode) {
      if (App.alwaysGoToInstaller) {
        return 'installer';
      } else {
        return 'main.dashboard';
      }
    }
    App.clusterStatus.updateFromServer(false, false);
    var clusterStatusOnServer = App.clusterStatus.get('value');
    if (!App.get('isAdmin') || clusterStatusOnServer && clusterStatusOnServer.clusterState === 'DEFAULT') {
      return 'main.dashboard';
    } else if (clusterStatusOnServer && clusterStatusOnServer.wizardControllerName === App.router.get('addHostController.name')) {
      // if wizardControllerName == "addHostController", then it means someone closed the browser or the browser was crashed when we were last in Add Hosts wizard
      return 'main.hostAdd';
    } else if (clusterStatusOnServer && (clusterStatusOnServer.wizardControllerName === App.router.get('addSecurityController.name') || clusterStatusOnServer.wizardControllerName === App.router.get('mainAdminSecurityDisableController.name'))) {
      // if wizardControllerName == "addSecurityController", then it means someone closed the browser or the browser was crashed when we were last in Add Security wizard
      return 'main.admin.adminSecurity';
    } else if (clusterStatusOnServer && clusterStatusOnServer.wizardControllerName === App.router.get('addServiceController.name')) {
      // if wizardControllerName == "addHostController", then it means someone closed the browser or the browser was crashed when we were last in Add Hosts wizard
      return 'main.serviceAdd';
    } else if (clusterStatusOnServer && clusterStatusOnServer.wizardControllerName === App.router.get('stackUpgradeController.name')) {
      // if wizardControllerName == "stackUpgradeController", then it means someone closed the browser or the browser was crashed when we were last in Stack Upgrade wizard
      return 'main.stackUpgrade';
    } else if (clusterStatusOnServer && clusterStatusOnServer.wizardControllerName === App.router.get('reassignMasterController.name')) {
      // if wizardControllerName == "reassignMasterController", then it means someone closed the browser or the browser was crashed when we were last in Reassign Master wizard
      return 'main.reassign';
    } else if (clusterStatusOnServer && clusterStatusOnServer.wizardControllerName === App.router.get('highAvailabilityWizardController.name')) {
      // if wizardControllerName == "highAvailabilityWizardController", then it means someone closed the browser or the browser was crashed when we were last in NameNode High Availability wizard
      return 'main.admin.enableHighAvailability';
    }else if (clusterStatusOnServer && clusterStatusOnServer.wizardControllerName === App.router.get('rollbackHighAvailabilityWizardController.name')) {
      // if wizardControllerName == "highAvailabilityRollbackController", then it means someone closed the browser or the browser was crashed when we were last in NameNode High Availability Rollback wizard
      return 'main.admin.rollbackHighAvailability';
    } else {
      // if wizardControllerName == "installerController", then it means someone closed the browser or the browser was crashed when we were last in Installer wizard
      return 'installer';
    }
  },

  logOff: function (context) {
    $('title').text('Ambari');
    var hash = window.btoa(this.get('loginController.loginName') + ":" + this.get('loginController.password'));

    App.router.get('mainController').stopPolling();
    // App.db.cleanUp() must be called before router.clearAllSteps().
    // otherwise, this.set('installerController.currentStep, 0) would have no effect
    // since it's a computed property but we are not setting it as a dependent of App.db.
    App.db.cleanUp();
    App.set('isAdmin', false);
    this.set('loggedIn', false);
    this.clearAllSteps();
    console.log("Log off: " + App.router.getClusterName());
    this.set('loginController.loginName', '');
    this.set('loginController.password', '');
    // When logOff is called by Sign Out button, context contains event object. As it is only case we should send logoff request, we are checking context below.
    if (!App.testMode && context) {
      App.ajax.send({
        name: 'router.logoff',
        sender: this,
        data: {
          auth: "Basic " + hash
        },
        beforeSend: 'authBeforeSend',
        success: 'logOffSuccessCallback',
        error:'logOffErrorCallback'
      });
    }
    this.transitionTo('login', context);
  },

  logOffSuccessCallback: function (data) {
    console.log("invoked logout on the server successfully");
  },

  logOffErrorCallback: function (req) {
    console.log("failed to invoke logout on the server");
  },

  /**
   * initialize isAdmin if user is administrator
   */
  initAdmin: function(){
    if(App.db && App.db.getUser() && App.db.getUser().admin) {
      App.set('isAdmin', true);
      console.log('Administrator logged in');
    }
  },

  root: Em.Route.extend({
    index: Em.Route.extend({
      route: '/',
      redirectsTo: 'login'
    }),

    enter: function(router){
      router.initAdmin();
    },

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
        $('title').text(Em.I18n.t('app.name'));
        console.log('/login:connectOutlet');
        console.log('currentStep is: ' + router.getInstallerCurrentStep());
        console.log('authenticated is: ' + router.getAuthenticated());
        router.get('applicationController').connectOutlet('login');
      }
    }),

    installer: require('routes/installer'),

    main: require('routes/main'),
    
    experimental: Em.Route.extend({
      route: '/experimental',
      enter: function (router, context) {
        
      },
      connectOutlets: function (router, context) {
        $('title').text("Ambari Experimental");
        console.log('/experimental:connectOutlet');
        router.get('applicationController').connectOutlet('experimental');
      }
    }),

    logoff: function (router, context) {
      router.logOff(context);
    }

  })
});
