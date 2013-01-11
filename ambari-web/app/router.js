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
    this.get('installerController').clear();
    this.get('addHostController').clear();
    this.get('addServiceController').clear();
    for (i = 1; i<11; i++) {
      this.set('wizardStep' + i + 'Controller.hasSubmitted', false);
      this.set('wizardStep' + i + 'Controller.isDisabled', true);
    }
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

  resetAuth: function (authenticated) {
    if (!authenticated) {
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
    var loginName = controller.get('loginName').toLowerCase();
    controller.set('loginName', loginName);
    var hash = window.btoa(loginName + ":" + controller.get('password'));
    var router = this;
    var url = '';

    if(loginName === "admin" && controller.get('password') === 'admin')
    {
      url = '/data/users/user_admin.json';
    }else if(loginName === 'user' && controller.get('password') === 'user'){
      url = '/data/users/user_user.json';
    }

    $.ajax({
      url : (App.testMode) ? url  : App.apiPrefix + '/users/' + loginName ,
      dataType : 'json',
      type: 'GET',
      beforeSend: function (xhr) {
        xhr.setRequestHeader("Authorization", "Basic " + hash);
      },
      statusCode: {
        200: function () {
          console.log("Status code 200: Success.");
        },
        401: function () {
          console.log("Error code 401: Unauthorized.");
        },
        403: function () {
          console.log("Error code 403: Forbidden.");
        }
      },
      success: function (data) {
        console.log('login success');

        var resp = data;
        var isAdmin = resp.Users.roles.indexOf('admin') >= 0;
        if(isAdmin){
          router.setAuthenticated(true);
          router.setLoginName(loginName);
          App.usersMapper.map({"items":[data]});
          router.setUser(App.User.find(loginName));
          router.transitionTo(router.getSection());
          postLogin(true);
        } else {
          $.ajax({
            url:  (App.testMode) ? '/data/clusters/info.json' : App.apiPrefix + '/clusters',
            dataType: 'text',
            type: 'GET',
            success: function (data) {
              var clusterResp = $.parseJSON(data);
              if (clusterResp.items.length) {
                router.setAuthenticated(true);
                router.setLoginName(loginName);
                App.usersMapper.map({"items":[resp]});
                router.setUser(App.User.find(loginName));
                router.transitionTo(router.getSection());
                postLogin(true);
              } else {
                controller.set('errorMessage', "Your administrator has not set up a Hadoop cluster yet.");
              }
            },
            error: function (req) {
              console.log("Server not responding: " + req.statusCode);
            }
          });
        }
      },
      error: function (req) {
        console.log("login error: " + req.statusCode);
        router.setAuthenticated(false);
        postLogin(false);
      }
    });

  },

  setAmbariStacks: function () {
    var self = this;
    var method = 'GET';
    var url = (App.testMode) ? '/data/wizard/stack/stacks.json' : App.apiPrefix + '/stacks';
    $.ajax({
      type: method,
      url: url,
      async: false,
      dataType: 'text',
      timeout: App.timeout,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: In success function for the setAmbariStacks call");
        console.log("TRACE: value of the url is: " + url);
        var stacks = [];
        jsonData.forEach(function (_stack) {
         stacks.pushObject({
           name:_stack.name,
           version: _stack.version
         });
        }, this);
        App.db.setAmbariStacks(stacks);
        console.log('TRACEIINNGG: ambaristacks: ' + JSON.stringify(App.db.getAmbariStacks()));
      },

      error: function (request, ajaxOptions, error) {
        console.log("TRACE: In error function for the setAmbariStacks call");
        console.log("TRACE: value of the url is: " + url);
        console.log("TRACE: error code status is: " + request.status);
        console.log('Error message is: ' + request.responseText);
      },

      statusCode: require('data/statusCodes')
    });
  },

  getSection: function () {
    if (App.alwaysGoToInstaller) {
      return 'installer';
    }
    var clusterStatusOnServer = App.clusterStatus.get('value');
    if (clusterStatusOnServer && (clusterStatusOnServer.clusterState === 'CLUSTER_STARTED_5' || clusterStatusOnServer.clusterState === 'ADD_HOSTS_COMPLETED_5' )) {
      return 'main.index';
    } else if (clusterStatusOnServer && clusterStatusOnServer.wizardControllerName === App.router.get('addHostController.name')) {
      // if wizardControllerName == "addHostController", then it means someone closed the browser or the browser was crashed when we were last in Add Hosts wizard
      return 'main.hostAdd';
    } else {
      // if wizardControllerName == "installerController", then it means someone closed the browser or the browser was crashed when we were last in Installer wizard
      return 'installer';
    }
  },

  logOff: function(context){
    $('title').text('Ambari');
    var hash = window.btoa(this.get('loginController.loginName') + ":" + this.get('loginController.password'));

    App.router.get('mainController').stopPolling();
    // App.db.cleanUp() must be called before router.clearAllSteps().
    // otherwise, this.set('installerController.currentStep, 0) would have no effect
    // since it's a computed property but we are not setting it as a dependent of App.db.
    App.db.cleanUp();
    this.clearAllSteps();
    console.log("Log off: " + App.router.getClusterName());
    this.set('loginController.loginName', '');
    this.set('loginController.password', '');

    if (!App.testMode) {
      $.ajax({
        url: App.apiPrefix + '/logout',
        dataType: 'json',
        type: 'GET',
        beforeSend: function (xhr) {
          xhr.setRequestHeader("Authorization", "Basic " + hash);
        },
        statusCode: {
          200: function () {
            console.log("Status code 200: Success.");
          },
          401: function () {
            console.log("Error code 401: Unauthorized.");
          },
          403: function () {
            console.log("Error code 403: Forbidden.");
          }
        },
        success: function (data) {
          console.log("invoked logout on the server successfully");
        },
        error: function (data) {
          console.log("failed to invoke logout on the server");
        },
        complete: function () {
          console.log('done');
        }
      });
    }

    this.transitionTo('login', context);
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
        $('title').text('Ambari');
        console.log('/login:connectOutlet');
        console.log('currentStep is: ' + router.getInstallerCurrentStep());
        console.log('authenticated is: ' + router.getAuthenticated());
        router.get('applicationController').connectOutlet('login', App.LoginView);
      }
    }),

    installer: require('routes/installer'),

    main: require('routes/main'),

    logoff: function (router, context) {
      router.logOff(context);
    }

  })
})
