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
.controller('GroupsEditCtrl',['$scope', 'Group', '$routeParams', 'Alert', 'ConfirmationModal', '$location', function($scope, Group, $routeParams, Alert, ConfirmationModal, $location) {
  $scope.editMode = false;
  $scope.group = new Group($routeParams.id);
  $scope.group.editingUsers = [];
  $scope.groupMembers = [];
  $scope.dataLoaded = false;
  
  $scope.isMembersEditing = false;

  $scope.$watch(function() {
    return $scope.group.editingUsers;
  }, function(newValue) {
    if(newValue && !angular.equals(newValue, $scope.groupMembers)){
      $scope.updateMembers();  
    }
  }, true);
  
  $scope.enableMembersEditing = function() {
    $scope.isMembersEditing = true;
    $scope.group.editingUsers = angular.copy($scope.groupMembers);
  };
  $scope.cancelUpdate = function() {
    $scope.isMembersEditing = false;
    $scope.group.editingUsers = '';
  };
  $scope.updateMembers = function() {
    var newMembers = $scope.group.editingUsers.toString().split(',').filter(function(item) {
      return item.trim();}
    ).map(function(item) {
        return item.trim()
      }
    );
    $scope.group.members = newMembers;
    $scope.group.saveMembers().catch(function(data) {
        Alert.error('Cannot update group members', "<div class='break-word'>" + data.message + "</div>");
      }).finally(function() {
        loadMembers();
      });
    $scope.isMembersEditing = false;
  };


  function loadMembers(){
    $scope.group.getMembers().then(function(members) {
      $scope.groupMembers = members;
      $scope.group.editingUsers = angular.copy($scope.groupMembers);
    });
  }    
  
  $scope.group.isLDAP().then(function(isLDAP) {
    $scope.group.ldap_group = isLDAP;
    loadMembers();
  });

  $scope.deleteGroup = function(group) {
    ConfirmationModal.show('Delete Group', 'Are you sure you want to delete group "'+ group.group_name +'"?').then(function() {
      group.destroy().then(function() {
        $location.path('/groups');
      }).catch(function() {
        
      });
    });
  };

  // Load privileges
  Group.getPrivileges($routeParams.id).then(function(data) {
    var privileges = {
      clusters: {},
      views: {}
    };
    angular.forEach(data.data.items, function(privilege) {
      privilege = privilege.PrivilegeInfo;
      if(privilege.type === 'CLUSTER'){
        // This is cluster
        privileges.clusters[privilege.cluster_name] = privileges.clusters[privilege.cluster_name] || [];
        privileges.clusters[privilege.cluster_name].push(privilege.permission_name);
      } else if ( privilege.type === 'VIEW'){
        privileges.views[privilege.instance_name] = privileges.views[privilege.instance_name] || { privileges:[]};
        privileges.views[privilege.instance_name].version = privilege.version;
        privileges.views[privilege.instance_name].view_name = privilege.view_name;
        privileges.views[privilege.instance_name].privileges.push(privilege.permission_name);
      }
    });

    $scope.privileges = data.data.items.length ? privileges : null;
    $scope.dataLoaded = true;
  }).catch(function(data) {
    Alert.error('Cannot load privileges', data.data.message);
  });


}]);
