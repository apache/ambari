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
.controller('GroupEditCtrl',
['$scope', 'Group', '$routeParams', 'Cluster', 'View', 'Alert', 'ConfirmationModal', '$location', 'GroupConstants', '$translate',
function($scope, Group, $routeParams, Cluster, View, Alert, ConfirmationModal, $location, GroupConstants, $translate) {
  var $t = $translate.instant;
  $scope.constants = {
    group: $t('common.group'),
    view: $t('common.view').toLowerCase(),
    cluster: $t('common.cluster').toLowerCase()
  };
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
        Alert.error($t('groups.alerts.cannotUpdateGroupMembers'), "<div class='break-word'>" + data.message + "</div>");
      }).finally(function() {
        loadMembers();
      });
    $scope.isMembersEditing = false;
  };


  function loadMembers(){
    $scope.group.getMembers().then(function(members) {
      $scope.group.groupTypeName = $t(GroupConstants.TYPES[$scope.group.group_type].LABEL_KEY);
      $scope.groupMembers = members;
      $scope.group.editingUsers = angular.copy($scope.groupMembers);
    });
  }    
  
  $scope.group.isLDAP().then(function(isLDAP) {
    $scope.group.ldap_group = isLDAP;
    $scope.group.getGroupType().then(function() {
      $scope.group.groupTypeName = $t(GroupConstants.TYPES[$scope.group.group_type].LABEL_KEY);
    });
    loadMembers();
  });

  $scope.group.getGroupType();

  $scope.deleteGroup = function(group) {
    ConfirmationModal.show(
      $t('common.delete', {
        term: $t('common.group')
      }),
      $t('common.deleteConfirmation', {
        instanceType: $t('common.group').toLowerCase(),
        instanceName: '"' + group.group_name + '"'
      })
    ).then(function() {
      Cluster.getPrivilegesForResource({
        nameFilter : group.group_name,
        typeFilter : {value: 'GROUP'}
      }).then(function(data) {
        var clusterPrivilegesIds = [];
        var viewsPrivileges = [];
        if (data.items && data.items.length) {
          angular.forEach(data.items[0].privileges, function(privilege) {
            if (privilege.PrivilegeInfo.principal_type === 'GROUP') {
              if (privilege.PrivilegeInfo.type === 'VIEW') {
                viewsPrivileges.push({
                  id: privilege.PrivilegeInfo.privilege_id,
                  view_name: privilege.PrivilegeInfo.view_name,
                  version: privilege.PrivilegeInfo.version,
                  instance_name: privilege.PrivilegeInfo.instance_name
                });
              } else {
                clusterPrivilegesIds.push(privilege.PrivilegeInfo.privilege_id);
              }
            }
          });
        }
        group.destroy().then(function() {
          $location.path('/userManagement');
          if (clusterPrivilegesIds.length) {
            Cluster.getAllClusters().then(function (clusters) {
              var clusterName = clusters[0].Clusters.cluster_name;
              Cluster.deleteMultiplePrivileges(clusterName, clusterPrivilegesIds);
            });
          }
          angular.forEach(viewsPrivileges, function(privilege) {
            View.deletePrivilege(privilege);
          });
        });
      });
    });
  };


  $scope.removePrivilege = function(name, privilege) {
    var privilegeObject = {
        id: privilege.privilege_id,
        view_name: privilege.view_name,
        version: privilege.version,
        instance_name: name
    };
    View.deletePrivilege(privilegeObject).then(function() {
      loadPrivileges();
    });
  };

function loadPrivileges() {
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
        privileges.clusters[privilege.cluster_name].push(privilege.permission_label);
      } else if ( privilege.type === 'VIEW'){
        privileges.views[privilege.instance_name] = privileges.views[privilege.instance_name] || { privileges:[]};
        privileges.views[privilege.instance_name].version = privilege.version;
        privileges.views[privilege.instance_name].view_name = privilege.view_name;
        privileges.views[privilege.instance_name].privilege_id = privilege.privilege_id;
        privileges.views[privilege.instance_name].privileges.push(privilege.permission_label);
      }
    });

    $scope.privileges = data.data.items.length ? privileges : null;
    $scope.noClusterPriv = $.isEmptyObject(privileges.clusters);
    $scope.noViewPriv = $.isEmptyObject(privileges.views);
    $scope.hidePrivileges = $scope.noClusterPriv && $scope.noViewPriv;
    $scope.dataLoaded = true;
  }).catch(function(data) {
    Alert.error($t('common.alerts.cannotLoadPrivileges'), data.data.message);
  });
}
loadPrivileges();
}]);
