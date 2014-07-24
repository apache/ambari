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
.controller('ClustersManageAccessCtrl', ['$scope', function($scope) {
  $scope.isEditMode = false;
  $scope.permissions = {
    read:{
      users: ['bill', 'kat'],
      groups: ['users', 'contractors']
    },
    operate:{
      users: ['jeff', 'tom', 'john', 'mike', 'steve'],
      groups: ['sysadmins', 'hadoopadmins']
    }
  };

  var processInput = function(obj) {
    var result = [], item;
    if(typeof obj === 'string'){
      obj = obj.split(',');
    } else if(!Array.isArray(obj)){
      throw 'processInput:: argument must be Array or string!';
    }
    // Remove doubles
    for(var i=0, max = obj.length; i < max; i++){
      item = obj[i];
      if(item != false && result.indexOf(item) < 0){

        result.push(item);
      }
    }
    return result;
  };

  $scope.toggleEditMode = function() {
    if($scope.isEditMode){
      $scope.permissions.read.users = processInput($scope.permissions.read.users);
      $scope.permissions.read.groups = processInput($scope.permissions.read.groups);

      $scope.permissions.operate.users = processInput($scope.permissions.operate.users);
      $scope.permissions.operate.groups = processInput($scope.permissions.operate.groups);
    }
    $scope.isEditMode = !$scope.isEditMode;
  };
}]);