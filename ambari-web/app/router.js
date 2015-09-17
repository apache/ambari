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

var misc = require('utils/misc');
var App = require('app');

App.WizardRoute = Em.Route.extend({

  gotoStep0: Em.Router.transitionTo('step0'),

  gotoStep1: Em.Router.transitionTo('step1'),

  gotoStep2: Em.Router.transitionTo('step2'),

  gotoStep3: Em.Router.transitionTo('step3'),

  gotoStep4: Em.Router.transitionTo('step4'),

  gotoStep5: Em.Router.transitionTo('step5'),

  gotoStep6: Em.Router.transitionTo('step6'),

  gotoStep7: Em.Router.transitionTo('step7'),

  gotoStep8: Em.Router.transitionTo('step8'),

  gotoStep9: Em.Router.transitionTo('step9'),

  gotoStep10: Em.Router.transitionTo('step10'),

  isRoutable: function() {
    return (typeof this.get('route') === 'string' && App.router.get('loggedIn'));
  }.property('App.router.loggedIn')

});

App.Router = Em.Router.extend({

  enableLogging: true,
  isFwdNavigation: true,
  backBtnForHigherStep: false,
  transitionInProgress: false,


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

  loggedIn: !!App.db.getAuthenticated(),

  loginName: function() {
    return this.getLoginName();
  }.property('loggedIn'),

  getAuthenticated: function () {
    var dfd = $.Deferred();
    var self = this;
    var auth = App.db.getAuthenticated();
    App.ajax.send({
      name: 'router.login.clusters',
      sender: this,
      success: 'onAuthenticationSuccess',
      error: 'onAuthenticationError'
    }).complete(function (xhr) {
      if (xhr.isResolved()) {
        // if server knows the user and user authenticated by UI
        if (auth && auth === true) {
          dfd.resolve(self.get('loggedIn'));
          // if server knows the user but UI don't, check the response header
          // and try to authorize
        } else if (xhr.getResponseHeader('User')) {
          var user = xhr.getResponseHeader('User');
          App.ajax.send({
            name: 'router.login',
            sender: self,
            data: {
              usr: user,
              loginName: encodeURIComponent(user)
            },
            success: 'loginSuccessCallback',
            error: 'loginErrorCallback'
          });
        } else {
          self.setAuthenticated(false);
          dfd.resolve(false);
        }
      }
    });
    return dfd.promise();
  },

  /**
   * Response for <code>/clusters?fields=Clusters/provisioning_state</code>
   * @type {null|object}
   */
  clusterData: null,

  onAuthenticationSuccess: function (data) {
    if (App.db.getAuthenticated() === true) {
      this.set('clusterData', data);
      this.setAuthenticated(true);
      if (data.items.length) {
        this.setClusterInstalled(data);
      }
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
    var hash = misc.utf8ToB64(loginName + ":" + controller.get('password'));
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
    this.setUserLoggedIn(decodeURIComponent(params.loginName));
    var requestData = {
      loginName: params.loginName,
      loginData: data
    };
    // no need to load cluster data if it's already loaded
    if (this.get('clusterData')) {
      this.loginGetClustersSuccessCallback(this.get('clusterData'), {}, requestData);
    }
    else {
      App.ajax.send({
        name: 'router.login.clusters',
        sender: this,
        data: requestData,
        success: 'loginGetClustersSuccessCallback'
      });
    }
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
    var loginController = this.get('loginController');
    var loginData = params.loginData;
    var privileges = loginData.privileges || [];
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
          App.ajax.send({
            name: 'ambari.service.load_server_version',
            sender: this,
            success: 'adminViewInfoSuccessCallback'
          });
        }
      } else {
        if (clustersData.items.length) {
          router.setClusterInstalled(clustersData);
          //TODO: Iterate over clusters
          var clusterName = clustersData.items[0].Clusters.cluster_name;
          var clusterPermissions = privileges.filterProperty('PrivilegeInfo.cluster_name', clusterName).mapProperty('PrivilegeInfo.permission_name');
          if (clusterPermissions.contains('CLUSTER.OPERATE')) {
            App.setProperties({
              isAdmin: true,
              isOperator: true
            });
            transitionToApp = true;
          } else if (clusterPermissions.contains('CLUSTER.READ')) {
            transitionToApp = true;
          }
        }
      }
      App.set('isPermissionDataLoaded', true);
      if (transitionToApp) {
        var preferredPath = router.get('preferedPath');
        // If the preferred path is relative, allow a redirect to it.
        // If the path is not relative, silently ignore it - if the path is an absolute URL, the user
        // may be routed to a different server where the [possibility exists for a phishing attack.
        if (!Em.isNone(preferredPath)) {
          if (preferredPath.startsWith('/') || preferredPath.startsWith('#')) {
            console.log("INFO: Routing to preferred path: " + preferredPath);
          }
          else {
            console.log("WARNING: Ignoring preferred path since it is not a relative URL: " + preferredPath);
            preferredPath = null;
          }

          // Unset preferedPath
          router.set('preferedPath', null);
        }

        if (!Em.isNone(preferredPath)) {
          window.location = preferredPath;
        } else {
          router.getSection(function (route) {
            router.transitionTo(route);
            loginController.postLogin(true, true);
          });
        }
      } else {
        App.router.get('mainViewsController').loadAmbariViews();
        router.transitionTo('main.views.index');
        loginController.postLogin(true,true);
      }
  },
  adminViewInfoSuccessCallback: function(data) {
    var components = Em.get(data,'components');
    if (Em.isArray(components)) {
      var mappedVersions = components.map(function(component) {
          if (Em.get(component, 'RootServiceComponents.component_version')) {
            return Em.get(component, 'RootServiceComponents.component_version');
          }
        }),
        sortedMappedVersions = mappedVersions.sort(),
        latestVersion = sortedMappedVersions[sortedMappedVersions.length-1];
      window.location.replace('/views/ADMIN_VIEW/' + latestVersion + '/INSTANCE/#/');
    }
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
        App.router.get('wizardWatcherController').getUser().complete(function() {
          App.clusterStatus.updateFromServer(false).complete(function () {
            var route = 'main.dashboard.index';
            var clusterStatusOnServer = App.clusterStatus.get('value');
            if (clusterStatusOnServer) {
              var wizardControllerRoutes = require('data/controller_route');
              var wizardControllerRoute =  wizardControllerRoutes.findProperty('wizardControllerName', clusterStatusOnServer.wizardControllerName);
              if (wizardControllerRoute && !App.router.get('wizardWatcherController').get('isNonWizardUser')) {
                route = wizardControllerRoute.route;
              }
            }
            if (wizardControllerRoute && wizardControllerRoute.wizardControllerName === 'mainAdminStackAndUpgradeController')  {
              var clusterController =   App.router.get('clusterController');
              clusterController.loadClusterName().done(function(){
                clusterController.restoreUpgradeState().done(function(){
                  callback(route);
                });
              });
            } else {
              callback(route);
            }
          });
        });
      } else {
        callback('installer');
      }
    }
  },

  logOff: function (context) {
    var self = this;

    $('title').text(Em.I18n.t('app.name'));
    App.router.get('mainController').stopPolling();
    // App.db.cleanUp() must be called before router.clearAllSteps().
    // otherwise, this.set('installerController.currentStep, 0) would have no effect
    // since it's a computed property but we are not setting it as a dependent of App.db.
    App.db.cleanUp();
    App.setProperties({
      isAdmin: false,
      isOperator: false,
      isPermissionDataLoaded: false
    });
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
        success: 'logOffSuccessCallback',
        error: 'logOffErrorCallback'
      }).complete(function() {
        self.logoffRedirect(context);
      });
    } else {
      this.logoffRedirect();
    }
  },

  logOffSuccessCallback: function () {
    console.log("invoked logout on the server successfully");
    var applicationController = App.router.get('applicationController');
    applicationController.set('isPollerRunning', false);
  },

  logOffErrorCallback: function () {
    console.log("failed to invoke logout on the server");
  },

  /**
   * Redirect function on sign off request.
   *
   * @param {$.Event} [context=undefined] - triggered event context
   */
  logoffRedirect: function(context) {
    if (App.router.get('clusterController.isLoaded')) {
      window.location.reload();
    } else {
      this.transitionTo('login', context);
    }
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
        App.set('isPermissionDataLoaded', true);
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
          var location = router.location.location.hash;
          //key to parse URI for prefered path to route
          var key = '?targetURI=';

          if (loggedIn) {
            Ember.run.next(function () {
              console.log(router.getLoginName() + ' already authenticated.  Redirecting...');
              router.getSection(function (route) {
                router.transitionTo(route, context);
              });
            });
          } else {
            if (location.contains(key)) {
              router.set('preferedPath', location.slice(location.indexOf(key) + key.length));
            }
          }
        });
      },

      connectOutlets: function (router, context) {
        $('title').text(Em.I18n.t('app.name'));
        console.log('/login:connectOutlet');
        console.log('currentStep is: ' + router.getInstallerCurrentStep());
        router.get('applicationController').connectOutlet('login');
      }
    }),

    installer: require('routes/installer'),

    main: require('routes/main'),

    adminView: Em.Route.extend({
      route: '/adminView',
      enter: function (router) {
        if (!router.get('loggedIn') || !App.isAccessible('upgrade_ADMIN') || App.isAccessible('upgrade_OPERATOR')) {
          Em.run.next(function () {
            router.transitionTo('login');
          });
        } else {
          App.ajax.send({
            name: 'ambari.service.load_server_version',
            sender: router,
            success: 'adminViewInfoSuccessCallback'
          });
        }
      }
    }),

    experimental: Em.Route.extend({
      route: '/experimental',
      enter: function (router, context) {
        if (App.isAccessible('upgrade_OPERATOR')) {
          Em.run.next(function () {
            if (router.get('clusterInstallCompleted')) {
              router.transitionTo("main.dashboard.widgets");
            } else {
              router.route("installer");
            }
          });
        } else if (!App.isAccessible('upgrade_ADMIN')) {
          Em.run.next(function () {
            router.transitionTo("main.views.index");
          });
        }
      },
      connectOutlets: function (router, context) {
        if (App.isAccessible('upgrade_ONLY_ADMIN')) {
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
