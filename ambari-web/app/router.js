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

  /**
   * Is true, if cluster.provisioning_state is equal to 'INSTALLED'
   * @type {Boolean}
   */
  clusterInstallCompleted: false,
  /**
   * user prefered path to route
   */
  preferedPath: null,

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
    this.get('backgroundOperationsController').clear();
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
    var dfd = $.Deferred();
    var self = this;
    var auth = App.db.getAuthenticated();
    var authResp = (auth && auth === true);
    if (authResp) {
      App.ajax.send({
        name: 'router.authentication',
        sender: this,
        success: 'onAuthenticationSuccess',
        error: 'onAuthenticationError'
      }).complete(function () {
          dfd.resolve(self.get('loggedIn'));
        });
    } else {
      this.set('loggedIn', false);
      dfd.resolve(false);
    }
    return dfd.promise();
  },

  onAuthenticationSuccess: function (data) {
    this.setAuthenticated(true);
    if (data.items.length) {
      this.setClusterInstalled(data);
    }
  },

  onAuthenticationError: function (data) {
    if (data.status === 403) {
      this.setAuthenticated(false);
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

  setUserLoggedIn: function(userName) {
    this.setAuthenticated(true);
    this.setLoginName(userName);
    this.setUser(App.User.find().findProperty('id', userName));
  },

  /**
   * Set `clusterInstallCompleted` property based on cluster info response.
   *
   * @param {Object} clusterObject
   **/
  setClusterInstalled: function(clusterObject) {
    this.set('clusterInstallCompleted', clusterObject.items[0].Clusters.provisioning_state === 'INSTALLED')
  },

  login: function () {
    var controller = this.get('loginController');
    var loginName = controller.get('loginName').toLowerCase();
    controller.set('loginName', loginName);
    var hash = window.btoa(loginName + ":" + controller.get('password'));
    var usr = '';

    if (App.get('testMode')) {
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
        loginName: encodeURIComponent(loginName)
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
    App.usersMapper.map({"items": [data]});
    this.setUserLoggedIn(params.loginName);
    App.router.get('mainViewsController').loadAmbariViews();
    App.ajax.send({
      name: 'router.login.clusters',
      sender: this,
      data: {
        loginName: params.loginName,
        loginData: data
      },
      success: 'loginGetClustersSuccessCallback',
      error: 'loginGetClustersErrorCallback'
    });
  },

  loginErrorCallback: function(request, ajaxOptions, error, opt) {
    var controller = this.get('loginController');
    console.log("login error: " + error);
    this.setAuthenticated(false);
    if (request.status == 403) {
      var responseMessage = request.responseText;
      try{
        responseMessage = JSON.parse(request.responseText).message;
      }catch(e){}
      controller.postLogin(true, false, responseMessage);
    } else {
      controller.postLogin(false, false, null);
    }

  },

  loginGetClustersSuccessCallback: function (clustersData, opt, params) {
    var adminViewUrl = '/views/ADMIN_VIEW/1.0.0/INSTANCE/#/';
    //TODO: Replace hard coded value with query. Same in templates/application.hbs
    var loginController = this.get('loginController');
    var loginData = params.loginData;
    var privileges = loginData.privileges;
    var router = this;
    var permissionList = privileges.mapProperty('PrivilegeInfo.permission_name');
      var isAdmin = permissionList.contains('AMBARI.ADMIN');
      var transitionToApp = false;
      if (isAdmin) {
        App.set('isAdmin', true);
        if (clustersData.items.length) {
          router.setClusterInstalled(clustersData);
          transitionToApp = true;
        } else {
          window.location = adminViewUrl;
          return;
        }
      } else {
        if (clustersData.items.length) {
          router.setClusterInstalled(clustersData);
          //TODO: Iterate over clusters
          var clusterName = clustersData.items[0].Clusters.cluster_name;
          var clusterPermissions = privileges.filterProperty('PrivilegeInfo.cluster_name', clusterName).mapProperty('PrivilegeInfo.permission_name');
          if (clusterPermissions.contains('CLUSTER.OPERATE')) {
            App.set('isAdmin', true);
            App.set('isOperator', true);
            transitionToApp = true;
          } else if (clusterPermissions.contains('CLUSTER.READ')) {
            transitionToApp = true;
          }
        }
      }
      if (transitionToApp) {
        if (!Em.isNone(router.get('preferedPath'))) {
          window.location = router.get('preferedPath');
          router.set('preferedPath', null);
        } else {
          router.getSection(function (route) {
            router.transitionTo(route);
            loginController.postLogin(true, true);
          });
        }
      } else {
        router.transitionTo('main.views.index');
        loginController.postLogin(true,true);
      }
  },

  loginGetClustersErrorCallback: function (req) {
    console.log("Get clusters error: " + req.statusCode);
  },

  getSection: function (callback) {
    if (App.get('testMode')) {
      if (App.alwaysGoToInstaller) {
        callback('installer');
      } else {
        callback('main.dashboard.index');
      }
    } else {
      if (this.get('clusterInstallCompleted')) {
        App.clusterStatus.updateFromServer(false).complete(function () {
          var clusterStatusOnServer = App.clusterStatus.get('value');
          var route = 'main.dashboard.index';
          if (clusterStatusOnServer && clusterStatusOnServer.wizardControllerName === App.router.get('addHostController.name')) {
            // if wizardControllerName == "addHostController", then it means someone closed the browser or the browser was crashed when we were last in Add Hosts wizard
            route = 'main.hostAdd';
          } else if (clusterStatusOnServer && (clusterStatusOnServer.wizardControllerName === App.router.get('addSecurityController.name') || clusterStatusOnServer.wizardControllerName === App.router.get('mainAdminSecurityDisableController.name'))) {
            // if wizardControllerName == "addSecurityController", then it means someone closed the browser or the browser was crashed when we were last in Add Security wizard
            route = 'main.admin.adminSecurity';
          } else if (clusterStatusOnServer && clusterStatusOnServer.wizardControllerName === App.router.get('addServiceController.name')) {
            // if wizardControllerName == "addHostController", then it means someone closed the browser or the browser was crashed when we were last in Add Hosts wizard
            route = 'main.serviceAdd';
          } else if (clusterStatusOnServer && clusterStatusOnServer.wizardControllerName === App.router.get('stackUpgradeController.name')) {
            // if wizardControllerName == "stackUpgradeController", then it means someone closed the browser or the browser was crashed when we were last in Stack Upgrade wizard
            route = 'main.stackUpgrade';
          } else if (clusterStatusOnServer && clusterStatusOnServer.wizardControllerName === App.router.get('reassignMasterController.name')) {
            // if wizardControllerName == "reassignMasterController", then it means someone closed the browser or the browser was crashed when we were last in Reassign Master wizard
            route = 'main.reassign';
          } else if (clusterStatusOnServer && clusterStatusOnServer.wizardControllerName === App.router.get('highAvailabilityWizardController.name')) {
            // if wizardControllerName == "highAvailabilityWizardController", then it means someone closed the browser or the browser was crashed when we were last in NameNode High Availability wizard
            route = 'main.services.enableHighAvailability';
          } else if (clusterStatusOnServer && clusterStatusOnServer.wizardControllerName === App.router.get('rMHighAvailabilityWizardController.name')) {
            // if wizardControllerName == "highAvailabilityWizardController", then it means someone closed the browser or the browser was crashed when we were last in NameNode High Availability wizard
            route = 'main.services.enableRMHighAvailability';
          } else if (clusterStatusOnServer && clusterStatusOnServer.wizardControllerName === App.router.get('rollbackHighAvailabilityWizardController.name')) {
            // if wizardControllerName == "highAvailabilityRollbackController", then it means someone closed the browser or the browser was crashed when we were last in NameNode High Availability Rollback wizard
            route = 'main.services.rollbackHighAvailability';
          }
          callback(route);
        });
      } else {
        callback('installer');
      }
    }
  },

  logOff: function (context) {
    $('title').text(Em.I18n.t('app.name'));
    var hash = window.btoa(this.get('loginController.loginName') + ":" + this.get('loginController.password'));

    App.router.get('mainController').stopPolling();
    // App.db.cleanUp() must be called before router.clearAllSteps().
    // otherwise, this.set('installerController.currentStep, 0) would have no effect
    // since it's a computed property but we are not setting it as a dependent of App.db.
    App.db.cleanUp();
    App.set('isAdmin', false);
    App.set('isOperator', false);
    this.set('loggedIn', false);
    this.clearAllSteps();
    console.log("Log off: " + App.router.getClusterName());
    this.set('loginController.loginName', '');
    this.set('loginController.password', '');
    // When logOff is called by Sign Out button, context contains event object. As it is only case we should send logoff request, we are checking context below.
    if (!App.get('testMode') && context) {
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
    if (App.router.get('clusterController.isLoaded')) {
      window.location.reload();
    } else {
      this.transitionTo('login', context);
    }
  },

  logOffSuccessCallback: function (data) {
    console.log("invoked logout on the server successfully");
    var applicationController = App.router.get('applicationController');
    applicationController.set('isPollerRunning',false);
  },

  logOffErrorCallback: function (req) {
    console.log("failed to invoke logout on the server");
  },

  /**
   * initialize isAdmin if user is administrator
   */
  initAdmin: function(){
    if (App.db) {
      var user = App.db.getUser();
      if (user) {
        if (user.admin) {
          App.set('isAdmin', true);
          console.log('Administrator logged in');
        }
        if (user.operator) {
          App.set('isOperator', true);
        }
      }
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
        router.getAuthenticated().done(function (loggedIn) {
          if (loggedIn) {
            Ember.run.next(function () {
              console.log(router.getLoginName() + ' already authenticated.  Redirecting...');
              router.getSection(function (route) {
                router.transitionTo(route, context);
              });
            });
          }
        });
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

    adminView: Em.Route.extend({
      route: '/adminView',
      enter: function (router) {
        if (!router.get('loggedIn') || !App.get('isAdmin') || App.get('isOperator')) {
          Em.run.next(function () {
            router.transitionTo('login');
          });
        } else {
            window.location.replace('/views/ADMIN_VIEW/1.0.0/INSTANCE/#/');
        }
      }
    }),

    experimental: Em.Route.extend({
      route: '/experimental',
      enter: function (router, context) {
        if (App.get('isOperator')) {
          Em.run.next(function () {
            if (router.get('clusterInstallCompleted')) {
              router.transitionTo("main.dashboard.widgets");
            } else {
              router.route("installer");
            }
          });
        } else if (!App.get('isAdmin')) {
          Em.run.next(function () {
            router.transitionTo("main.views.index");
          });
        }
      },
      connectOutlets: function (router, context) {
        if (App.get('isAdmin') && !App.get('isOperator')) {
          $('title').text(Em.I18n.t('app.name.subtitle.experimental'));
          console.log('/experimental:connectOutlet');
          router.get('applicationController').connectOutlet('experimental');
        }
      }
    }),

    logoff: function (router, context) {
      router.logOff(context);
    }

  })
});
