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
.controller('ViewsListCtrl',['$scope', 'View', '$modal', 'uiAlert', 'ConfirmationModal', function($scope, View, $modal, uiAlert, ConfirmationModal) {
  function loadViews(){
    View.all().then(function(views) {
      $scope.views = views;
      $scope.getFilteredViews();
    }).catch(function(data) {
      uiAlert.danger(data.data.status, data.data.message);
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

  $scope.deleteInstance = function(instance) {
    ConfirmationModal.show('Delete View Instance', 'Are you sure you want to delete View Instance '+ instance.ViewInstanceInfo.label +'?').then(function() {
      View.deleteInstance(instance.ViewInstanceInfo.view_name, instance.ViewInstanceInfo.version, instance.ViewInstanceInfo.instance_name)
      .then(function() {
        loadViews();
      })
      .catch(function(data) {
        uiAlert.danger(data.data.status, data.data.message);
      });
    });
  };

  $scope.viewsFilter = '';
  $scope.filteredViews = [];
  $scope.getFilteredViews = function(views) {
    var result = [];
    var filter = $scope.viewsFilter.toLowerCase();
    if(!filter){  // if no filter return all views
      result = $scope.views;
    } else {
      result = $scope.views.map(function(view) {
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
}]);