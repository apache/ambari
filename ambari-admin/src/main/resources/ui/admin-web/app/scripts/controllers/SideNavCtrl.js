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
.controller('SideNavCtrl', ['$scope', '$location', 'ROUTES', '$rootScope', 'Stack', 'Settings', function($scope, $location, ROUTES, $rootScope, Stack, Settings) {
  $scope.totalRepos = 0;
  $scope.settings = Settings;

  $scope.$watch(function() {
    return $rootScope.cluster;
  }, function() {
    $scope.cluster = $rootScope.cluster;
  }, true);

  function loadRepos() {
    Stack.allRepos().then(function (repos) {
      $scope.totalRepos = repos.itemTotal;
    });
  }

  function initNavigationBar () {
    const observer = new MutationObserver(mutations => {
      var targetNode
      if (mutations.some((mutation) => mutation.type === 'childList' && (targetNode = $('.navigation-bar')).length)) {
        observer.disconnect();
        //initTooltips();
        targetNode.navigationBar({
          fitHeight: true,
          collapseNavBarClass: 'fa-angle-double-left',
          expandNavBarClass: 'fa-angle-double-right'
        });
      }
    });

    setTimeout(() => {
      // remove observer if selected element is not found in 10secs.
      observer.disconnect();
    }, 10000)

    observer.observe(document.body, {
      childList: true,
      subtree: true
    });
  }

  function initTooltips () {
    $('[rel="tooltip"]').tooltip();
  }

  initNavigationBar();
  loadRepos();

  $scope.isActive = function(path) {
    var route = ROUTES;
    angular.forEach(path.split('.'), function(routeObj) {
      route = route[routeObj];
    });
    var r = new RegExp( route.url.replace(/(:\w+)/, '\\w+'));
    return r.test($location.path());
  };
}]);
