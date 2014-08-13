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
.controller('GroupsEditCtrl',['$scope', 'Group', '$routeParams', 'uiAlert', 'ConfirmationModal', '$location', function($scope, Group, $routeParams, uiAlert, ConfirmationModal, $location) {
  $scope.editMode = false;
  $scope.group = new Group($routeParams.id);
  $scope.group.editingUsers = [];
  $scope.groupMembers = [];
  $scope.dataLoaded = false;


  function loadMembers(){
    $scope.group.getMembers().then(function(members) {
      $scope.groupMembers = members;
    });
  }    
  
  $scope.group.isLDAP().then(function(isLDAP) {
    $scope.group.ldap_group = isLDAP;
    loadMembers();
  });

  $scope.toggleEditMode = function() {
    $scope.editMode = !$scope.editMode;

    if( $scope.editMode ){
      // $scope.group.editingUsers = $scope.group.members.join(', ');
      $scope.group.editingUsers = $scope.groupMembers;
    } else {
      var newMembers = $scope.group.editingUsers.toString().split(',').filter(function(item) {return item.trim();}).map(function(item) {return item.trim()});
      $scope.group.members = newMembers;
      $scope.group.saveMembers().then(loadMembers)
      .catch(function(data) {
        uiAlert.danger(data.status, data.message);
      });
    }
  };

  $scope.deleteGroup = function(group) {
    ConfirmationModal.show('Delete Group', 'Are you sure you want to delete group "'+ group.group_name +'"?').then(function() {
      group.destroy().then(function() {
        $location.path('/groups');
      }).catch(function() {
        
      });
    });
  };

  // Load privilegies
  Group.getPrivilegies($routeParams.id).then(function(data) {
    var privilegies = {
      clusters: {},
      views: {}
    };
    angular.forEach(data.data.items, function(privilegie) {
      privilegie = privilegie.PrivilegeInfo;
      if(privilegie.type === 'CLUSTER'){
        // This is cluster
        privilegies.clusters[privilegie.cluster_name] = privilegies.clusters[privilegie.cluster_name] || '';
        privilegies.clusters[privilegie.cluster_name] += privilegies.clusters[privilegie.cluster_name] ? ', ' + privilegie.permission_name : privilegie.permission_name;
      } else if ( privilegie.type === 'VIEW'){
        privilegies.views[privilegie.instance_name] = privilegies.views[privilegie.instance_name] || { privileges:''};
        privilegies.views[privilegie.instance_name].version = privilegie.version;
        privilegies.views[privilegie.instance_name].view_name = privilegie.view_name;
        privilegies.views[privilegie.instance_name].privileges += privilegies.views[privilegie.instance_name].privileges ? ', ' + privilegie.permission_name : privilegie.permission_name;

      }
    });

    $scope.privileges = data.data.items.length ? privilegies : null;
    $scope.dataLoaded = true;
  }).catch(function(data) {
    uiAlert.danger(data.data.status, data.data.message);
  });


}]);
