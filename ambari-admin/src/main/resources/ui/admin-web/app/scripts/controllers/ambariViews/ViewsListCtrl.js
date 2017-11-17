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
.controller('ViewsListCtrl',['$scope', 'View','$modal', 'Alert', 'ConfirmationModal', '$translate', function($scope, View, $modal, Alert, ConfirmationModal, $translate) {
  var $t = $translate.instant;
  var VIEWS_VERSION_STATUS_TIMEOUT = 5000;
  $scope.isLoading = false;
  $scope.minInstanceForPagination = 10;

  function checkViewVersionStatus(view, versionObj, versionNumber) {
    var deferred = View.checkViewVersionStatus(view.view_name, versionNumber);

    deferred.promise.then(function (status) {
      if (versionNeedStatusUpdate(status)) {
        setTimeout(function() {
          checkViewVersionStatus(view, versionObj, versionNumber);
        }, VIEWS_VERSION_STATUS_TIMEOUT);
      } else {
        versionObj.status = status;
        angular.forEach(view.versions, function (version) {
          if (version.status === 'DEPLOYED') {
            view.canCreateInstance = true;
          }
        })
      }
    });
  }

  function versionNeedStatusUpdate(status) {
    return status !== 'DEPLOYED' && status !== 'ERROR';
  }

  function loadViews() {
    $scope.isLoading = true;
    View.all().then(function (views) {
      $scope.isLoading = false;
      $scope.views = views;
      $scope.instances = [];
      angular.forEach(views, function (view) {
        angular.forEach(view.versions, function (versionObj, versionNumber) {
          if (versionNeedStatusUpdate(versionObj.status)) {
            checkViewVersionStatus(view, versionObj, versionNumber);
          }
        });
        angular.forEach(view.instances, function (instance) {
          instance.ViewInstanceInfo.short_url_name = instance.ViewInstanceInfo.short_url_name || '';
          instance.ViewInstanceInfo.short_url = instance.ViewInstanceInfo.short_url || '';
          instance.ViewInstanceInfo.versionObj = view.versions[instance.ViewInstanceInfo.version] || {};
          $scope.instances.push(instance.ViewInstanceInfo);
        });
      });
      initTypeFilter();
      $scope.filterInstances();
    }).catch(function (data) {
      Alert.error($t('views.alerts.cannotLoadViews'), data.data.message);
    });
  }

  function initTypeFilter() {
    var uniqTypes = $.unique($scope.instances.map(function(instance) {
      return instance.view_name;
    }));
    $scope.typeFilterOptions = [ { label: $t('common.all'), value: '*'} ]
      .concat(uniqTypes.map(function(type) {
        return {
          label: type,
          value: type
        };
      }));
    $scope.instanceTypeFilter = $scope.typeFilterOptions[0];
  }

  function showInstancesOnPage() {
    var startIndex = ($scope.currentPage - 1) * $scope.instancesPerPage + 1;
    var endIndex = $scope.currentPage * $scope.instancesPerPage;
    var showedCount = 0;
    var filteredCount = 0;

    angular.forEach($scope.instances, function(instance) {
      instance.isShowed = false;
      if (instance.isFiltered) {
        filteredCount++;
        if (filteredCount >= startIndex && filteredCount <= endIndex) {
          instance.isShowed = true;
          showedCount++;
        }
      }
    });
    $scope.tableInfo.showed = showedCount;
  }

  $scope.views = [];
  $scope.instances = [];
  $scope.instancesPerPage = 10;
  $scope.currentPage = 1;
  $scope.instanceNameFilter = '';
  $scope.instanceUrlFilter = '';
  $scope.maxVisiblePages = 10;
  $scope.isNotEmptyFilter = true;
  $scope.instanceTypeFilter = '';
  $scope.tableInfo = {
    filtered: 0,
    showed: 0
  };

  loadViews();

  $scope.filterInstances = function() {
    var filteredCount = 0;
    angular.forEach($scope.instances, function(instance) {
      if ($scope.instanceNameFilter && instance.short_url_name.indexOf($scope.instanceNameFilter) === -1) {
        return instance.isFiltered = false;
      }
      if ($scope.instanceUrlFilter && ('/main/view/'+ instance.view_name + '/' + instance.short_url).indexOf($scope.instanceUrlFilter) === -1) {
        return instance.isFiltered = false;
      }
      if ($scope.instanceTypeFilter.value !== '*' && instance.view_name.indexOf($scope.instanceTypeFilter.value) === -1) {
        return instance.isFiltered = false;
      }
      filteredCount++;
      instance.isFiltered = true;
    });
    $scope.tableInfo.filtered = filteredCount;
    $scope.resetPagination();
  };

  $scope.pageChanged = function() {
    showInstancesOnPage();
  };

  $scope.resetPagination = function() {
    $scope.currentPage = 1;
    showInstancesOnPage();
  };

  $scope.clearFilters = function () {
    $scope.instanceNameFilter = '';
    $scope.instanceUrlFilter = '';
    $scope.instanceTypeFilter = $scope.typeFilterOptions[0];
    $scope.resetPagination();
  };

  $scope.$watch(
    function (scope) {
      return Boolean(scope.instanceNameFilter || scope.instanceUrlFilter || (scope.instanceTypeFilter && scope.instanceTypeFilter.value !== '*'));
    },
    function (newValue, oldValue, scope) {
      scope.isNotEmptyFilter = newValue;
    }
  );

  $scope.cloneInstance = function(instanceClone) {
    $scope.createInstance(instanceClone);
  };

  $scope.createInstance = function (instanceClone) {
    var modalInstance = $modal.open({
      templateUrl: 'views/ambariViews/modals/create.html',
      controller: 'CreateViewInstanceCtrl',
      resolve: {
        views: function() {
          return $scope.views;
        },
        instanceClone: function() {
          return instanceClone;
        }
      },
      backdrop: 'static'
    });

    modalInstance.result.then(loadViews);
  };

  $scope.deleteInstance = function (instance) {
    ConfirmationModal.show(
      $t('common.delete', {
        term: $t('views.viewInstance')
      }),
      $t('common.deleteConfirmation', {
        instanceType: $t('views.viewInstance'),
        instanceName: instance.label
      })
    ).then(function () {
      View.deleteInstance(instance.view_name, instance.version, instance.instance_name)
        .then(function () {
          loadViews();
        })
        .catch(function (data) {
          Alert.error($t('views.alerts.cannotDeleteInstance'), data.data.message);
        });
    });
  };
}]);
