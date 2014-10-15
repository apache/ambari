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
.controller('GroupsCreateCtrl',['$scope', 'Group', '$location', 'Alert', 'UnsavedDialog', function($scope, Group, $location, Alert, UnsavedDialog) {
  $scope.group = new Group();
  var targetUrl = '/groups';

  $scope.createGroup = function() {
    $scope.form.submitted = true;
    if ($scope.form.$valid){
      $scope.group.save().then(function() {
        Alert.success('Created group <a href="#/groups/' + $scope.group.group_name + '/edit">' + $scope.group.group_name + '</a>');
        $scope.form.$setPristine();
        $location.path(targetUrl);
      })
      .catch(function(data) {
        Alert.error('Group creation error', data.data.message);
      });
    }
  };

  $scope.cancel = function() {
    $scope.form.$setPristine();
    $location.path('/groups');
  };

  $scope.$on('$locationChangeStart', function(event, __targetUrl) {
    if( $scope.form.$dirty ){
      UnsavedDialog().then(function(action) {
        targetUrl = __targetUrl.split('#').pop();
        switch(action){
          case 'save':
            $scope.createGroup();
            break;
          case 'discard':
            $scope.form.$setPristine();
            $location.path(targetUrl);
            break;
          case 'cancel':
            targetUrl = '/groups';
            break;
        }
      });
      event.preventDefault();
    }
  });
}]);