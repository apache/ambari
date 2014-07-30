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
.controller('GroupsEditCtrl',['$scope', 'Group', '$routeParams', function($scope, Group, $routeParams) {
  $scope.editMode = false;
  $scope.group = new Group($routeParams.id);
  $scope.group.editingUsers = "";

    
  $scope.group.getMembers();
  
  $scope.removeMember = function(member) {
    $scope.group.removeMember(member).finally(function() {
      $scope.group.getMembers();
    });
  };

  $scope.toggleEditMode = function() {
    $scope.editMode = !$scope.editMode;
    if( $scope.editMode ){
      $scope.group.editingUsers = $scope.group.members.join(', ');
    } else {
      var oldMembers = $scope.group.members;
      $scope.group.members = [];
      var members = $scope.group.editingUsers.split(',');
      var member;
      angular.forEach(members,function(member) {
        if(member && $scope.group.members.indexOf(member) < 0){
          $scope.group.members.push(member.trim());
        }
      });

      if(!angular.equals(oldMembers, $scope.group.members)){
        $scope.group.saveMembers().finally(function() {
          $scope.group.getMembers();
        });
      }
    }
  };


}]);