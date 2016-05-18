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
.controller('ViewsListCtrl',['$scope', 'View','$modal', 'Alert', 'ConfirmationModal', '$location', '$translate', function($scope, View, $modal, Alert, ConfirmationModal, $location, $translate) {
  var deferredList = [],
    $t = $translate.instant;
  $scope.constants = {
    unable: $t('views.alerts.unableToCreate'),
    views: $t('common.views').toLowerCase()
  };
  $scope.$on('$locationChangeStart', function() {
    deferredList.forEach(function(def) {
      def.reject();
    })
  });

  $scope.createUrlDisabled = false;

  function checkViewVersionStatus(view, versionObj, versionNumber){
    var deferred = View.checkViewVersionStatus(view.view_name, versionNumber);
    deferredList.push(deferred);

    deferred.promise.then(function(status) {
      deferredList.splice(deferredList.indexOf(deferred), 1);
      if (status !== 'DEPLOYED' && status !== 'ERROR') {
        checkViewVersionStatus(view, versionObj, versionNumber);
      } else {
        $scope.$evalAsync(function() {
          versionObj.status = status;
          angular.forEach(view.versions, function(version) {
            if(version.status === 'DEPLOYED'){
              view.canCreateInstance = true;
            }
          })
        });
      }
    });
  }

  function loadViews(){
    View.all().then(function(views) {
      $scope.views = views;
      $scope.getFilteredViews();
      angular.forEach(views, function(view) {
        angular.forEach(view.versions, function(versionObj, versionNumber) {
          if (versionObj.status !== 'DEPLOYED' || versionObj.status !== 'ERROR'){
            checkViewVersionStatus(view, versionObj, versionNumber);
          }
        });
      })
    }).catch(function(data) {
      Alert.error($t('views.alerts.cannotLoadViews'), data.data.message);
    });
  }

  loadViews();

  $scope.createInstance = function(view) {
    var modalInstance = $modal.open({
      templateUrl: 'views/ambariViews/modals/create.html',
      size: 'lg',
      controller: 'CreateViewInstanceCtrl',
      resolve: {
        viewVersion: function(){
          return view.versionsList[ view.versionsList.length-1];
        }
      }
    });

    modalInstance.result.then(loadViews);
  };

  $scope.viewsFilter = '';
  $scope.filteredViews = [];
  $scope.getFilteredViews = function(views) {
    var result = [];
    var filter = $scope.viewsFilter.toLowerCase();
    if(!filter){  // if no filter return all views
      result = $scope.views.map(function(view) {
        view.isOpened = false;
        return view;
      });
    } else {
      result = $scope.views.map(function(view) {
        view.isOpened = true;
        if(view.view_name.toLowerCase().indexOf(filter) >= 0){
          return view; // if filter matched with view name -- return whole view
        } else {
          var instances = [];
          angular.forEach(view.instances, function(instance) {
            if(instance.ViewInstanceInfo.label.toLowerCase().indexOf(filter) >= 0){
              instances.push(instance);
            }
          });
          if( instances.length ){ // If inside view exists instances with matched filter - show only this instances
            var v = angular.copy(view);
            v.instances = instances;
            return v;
          }
        }
      }).filter(function(view) {
        return !!view; // Remove 'undefined'
      });
    }
    $scope.filteredViews = result;
  };

  $scope.gotoCreate = function(viewName, isAllowed) {
    if(isAllowed){
      $location.path('/views/'+viewName+'/new');
    }
  };

  $scope.reloadViews = function () {
    loadViews();
  };

  /**
   * Url listing
   */

  $scope.loadedUrls = [];
  $scope.urlsPerPage = 10;
  $scope.currentPage = 1;
  $scope.totalUrls = 1;
  $scope.urlNameFilter = '';
  $scope.urlSuffixfilter = '';
  $scope.maxVisiblePages=20;
  $scope.tableInfo = {
    total: 0,
    showed: 0
  };

  $scope.isNotEmptyFilter = true;


  $scope.pageChanged = function() {
    $scope.listViewUrls();
  };

  $scope.urlsPerPageChanged = function() {
    $scope.resetPagination();
  };


  $scope.resetPagination = function() {
    $scope.currentPage = 1;
    $scope.listViewUrls();
  };


  $scope.getVersions = function(instances) {
    var names = [];

    instances.map(function(view){
      var name = view.view_name;
      names.push(name);
    });

    var output = [],
        keys = [];

    angular.forEach(names, function(item) {
      var key = item;
      if(keys.indexOf(key) === -1) {
        keys.push(key);
        output.push(item);
      }
    });
    return output;
    };



  $scope.clearFilters = function () {
    $scope.urlNameFilter = '';
    $scope.urlSuffixfilter = '';
    $scope.instanceTypeFilter = $scope.typeFilterOptions[0];
    $scope.resetPagination();
  };



  $scope.$watch(
      function (scope) {
        return Boolean(scope.urlNameFilter || scope.urlSuffixfilter || (scope.instanceTypeFilter && scope.instanceTypeFilter.value !== '*'));
      },
      function (newValue, oldValue, scope) {
        scope.isNotEmptyFilter = newValue;
      }
  );




  $scope.listViewUrls = function(){
    View.allUrls({
      currentPage: $scope.currentPage,
      urlsPerPage: $scope.urlsPerPage,
      searchString: $scope.urlNameFilter,
      suffixSearch: $scope.urlSuffixfilter,
      instanceType: $scope.instanceTypeFilter?$scope.instanceTypeFilter.value:'*'
    }).then(function(urls) {
      $scope.urls = urls;
      $scope.ViewNameFilterOptions = urls.items.map(function(url){
        return url.ViewUrlInfo.view_instance_common_name;
      });

      $scope.totalUrls = urls.itemTotal;
      $scope.tableInfo.showed = urls.items.length;
      $scope.tableInfo.total = urls.itemTotal;

      // get all view instances to enable/disable creation if empty

    }).catch(function(data) {
      Alert.error($t('views.alerts.cannotLoadViewUrls'), data.message);
    });
  };


  $scope.initViewUrls = function(){
    $scope.listViewUrls();
    View.getAllVisibleInstance().then(function(instances){
      // if no instances then disable the create button
      if(!instances.length){
        $scope.createUrlDisabled = true;
      } else {
        $scope.typeFilterOptions = [{ label: $t('common.all'), value: '*'}]
            .concat($scope.getVersions(instances).map(function(key) {
              return {
                label: key,
                value: key
              };
            }));

        $scope.instanceTypeFilter = $scope.typeFilterOptions[0];
      }

    }).catch(function(data) {
      // Make the create button enabled, and swallow the error
      $scope.createUrlDisabled = false;
    });

  };

}]);