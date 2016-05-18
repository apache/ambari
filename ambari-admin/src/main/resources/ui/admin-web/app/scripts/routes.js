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
'use strict';

angular.module('ambariAdminConsole')
.constant('ROUTES', {
  root: {
    url: '/',
    templateUrl: 'views/main.html',
    controller: 'MainCtrl'
  },
  authentication: {
    main: {
      url: '/authentication',
      templateUrl: 'views/authentication/main.html',
      controller: 'AuthenticationMainCtrl'
    }
  },
  loginActivities: {
    loginMessage:{
      url: '/loginMessage',
      templateUrl: 'views/loginActivities/main.html',
      controller: 'LoginActivitiesMainCtrl'
    },
    homeDirectory: {
      url: '/homeDirectory',
      templateUrl: 'views/loginActivities/main.html',
      controller: 'LoginActivitiesMainCtrl'
    }
  },
  users: {
    list: {
      url: '/users',
      templateUrl: 'views/users/list.html',
      controller: 'UsersListCtrl'
    },
    edit: {
      url: '/users/:id/edit',
      templateUrl: 'views/users/create.html',
      controller: 'UsersCreateCtrl'
    },
    create: {
      url: '/users/new',
      templateUrl: 'views/users/create.html',
      controller: 'UsersCreateCtrl'
    },
    show: {
      url: '/users/:id*',
      templateUrl: 'views/users/show.html',
      controller: 'UsersShowCtrl'
    }
  },
  groups: {
    list: {
      url: '/groups',
      templateUrl: 'views/groups/list.html',
      controller: 'GroupsListCtrl'
    },
    edit: {
      url: '/groups/:id/edit',
      templateUrl: 'views/groups/edit.html',
      controller: 'GroupsEditCtrl'
    },
    create: {
      url: '/groups/new',
      templateUrl: 'views/groups/create.html',
      controller: 'GroupsCreateCtrl'
    }
  },
  views: {
    list: {
      url: '/views',
      templateUrl: 'views/ambariViews/listTable.html',
      controller: 'ViewsListCtrl'
    },
    listViewUrls: {
      url: '/viewUrls',
      templateUrl: 'views/ambariViews/listUrls.html',
      controller: 'ViewsListCtrl'
    },
    createViewUrl:{
      url: '/urls/new',
      templateUrl: 'views/urls/create.html',
      controller: 'ViewUrlCtrl'
    },
    linkViewUrl:{
      url: '/urls/link/:viewName/:viewVersion/:viewInstanceName',
      templateUrl: 'views/urls/create.html',
      controller: 'ViewUrlCtrl'
    },
    editViewUrl:{
      url: '/urls/edit/:urlName',
      templateUrl: 'views/urls/edit.html',
      controller: 'ViewUrlEditCtrl'
    },
    edit: {
      url: '/views/:viewId/versions/:version/instances/:instanceId/edit',
      templateUrl: 'views/ambariViews/edit.html',
      controller: 'ViewsEditCtrl'
    },
    create: {
      url: '/views/:viewId/new',
      templateUrl: 'views/ambariViews/create.html',
      controller: 'CreateViewInstanceCtrl'
    }
  },
  stackVersions: {
    list: {
      url: '/stackVersions',
      templateUrl: 'views/stackVersions/list.html',
      controller: 'StackVersionsListCtrl'
    },
    create: {
      url: '/stackVersions/create',
      templateUrl: 'views/stackVersions/stackVersionPage.html',
      controller: 'StackVersionsCreateCtrl'
    },
    edit: {
      url: '/stackVersions/:stackName/:versionId/edit',
      templateUrl: 'views/stackVersions/stackVersionPage.html',
      controller: 'StackVersionsEditCtrl'
    }
  },
  remoteClusters: {
    list: {
      url: '/remoteClusters',
      templateUrl: 'views/remoteClusters/list.html',
      controller: 'RemoteClustersListCtrl'
    },
    create: {
      url: '/remoteClusters/create',
      templateUrl: 'views/remoteClusters/remoteClusterPage.html',
      controller: 'RemoteClustersCreateCtrl'
    },
     edit: {
     url: '/remoteClusters/:clusterName/edit',
     templateUrl: 'views/remoteClusters/editRemoteClusterPage.html',
     controller: 'RemoteClustersEditCtrl'
     }
  },
  clusters: {
    manageAccess: {
      url: '/clusters/:id/manageAccess',
      templateUrl: 'views/clusters/manageAccess.html',
      controller: 'ClustersManageAccessCtrl'
    },
    manageAccess2: {
      url: '/clusters/:id/userAccessList',
      templateUrl: 'views/clusters/userAccessList.html',
      controller: 'UserAccessListCtrl'
    }
  },
  dashboard:{
    url: '/dashboard',
    controller: ['$window', function($window) {
      $window.location.replace('/#/main/dashboard');
    }],
    template: ''
  }
})
.config(['$routeProvider', '$locationProvider', 'ROUTES', function($routeProvider, $locationProvider, ROUTES) {
  var createRoute = function(routeObj) {
    if(routeObj.url){
      $routeProvider.when(routeObj.url, routeObj);
    } else {
      angular.forEach(routeObj, createRoute);
    }
  };
  angular.forEach(ROUTES, createRoute);
}])
.run(['$rootScope', 'ROUTES', 'Settings', function($rootScope, ROUTES, Settings) {
  // Make routes available in every template and controller
  $rootScope.ROUTES = ROUTES;
  $rootScope.$on('$locationChangeStart', function (e, nextUrl) {
    if (/\/authentication$/.test(nextUrl) && !Settings.isLDAPConfigurationSupported) {
      e.preventDefault();
    }
  });
  $rootScope.$on('$locationChangeStart', function (e, nextUrl) {
    if ((/\/loginMessage$/.test(nextUrl) || /\/homeDirectory$/.test(nextUrl)) && !Settings.isLoginActivitiesSupported) {
      e.preventDefault();
    }
  });
}]);
