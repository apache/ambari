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
.factory('PermissionSaver', ['Cluster', 'View', '$q', 'getDifference', function(Cluster, View, $q, getDifference) {

  function savePermissionsFor(resource, permissions, params){
    var arr = [];

    angular.forEach(permissions, function(permission) {
      // Sanitaize input
      var users = permission.USER.toString().split(',').filter(function(item) {return item.trim();}).map(function(item) {return item.trim()});
      var groups = permission.GROUP.toString().split(',').filter(function(item) {return item.trim();}).map(function(item) {return item.trim()});
      // Build array
      arr = arr.concat(users.map(function(user) {
        return {
          'PrivilegeInfo':{
            'permission_name': permission.PermissionInfo.permission_name,
            'principal_name': user,
            'principal_type': 'USER'
          }
        }
      }));

      arr = arr.concat(groups.map(function(group) {
        return {
          'PrivilegeInfo':{
            'permission_name': permission.PermissionInfo.permission_name,
            'principal_name': group,
            'principal_type': 'GROUP'
          }
        }
      }));
    });

    return resource.updatePrivileges(params, arr);
  }
  
  function savePermissionsForOld(resource, oldPermissions, newPermissions, params){
    var deferred = $q.defer();

    var addArr = [];
    var delArr = [];
    angular.forEach(newPermissions, function(permission) {
      // Sanitize input
      var users = permission.USER.toString().split(',').filter(function(item) {return item.trim();}).map(function(item) {return item.trim()});
      var groups = permission.GROUP.toString().split(',').filter(function(item) {return item.trim();}).map(function(item) {return item.trim()});

      var userObj = getDifference(angular.copy(oldPermissions[permission.PermissionInfo.permission_name].USER) , users);
      var groupObj = getDifference(angular.copy(oldPermissions[permission.PermissionInfo.permission_name].GROUP) , groups);

      // Build Add array
      addArr = addArr.concat(userObj.add.map(function(user) {
        return {
          'PrivilegeInfo':{
            'permission_name': permission.PermissionInfo.permission_name,
            'principal_name': user,
            'principal_type': 'USER'
          }
        }
      }));
      addArr = addArr.concat(groupObj.add.map(function(group) {
        return {
          'PrivilegeInfo':{
            'permission_name': permission.PermissionInfo.permission_name,
            'principal_name': group,
            'principal_type': 'GROUP'
          }
        }
      }));

      // Build del array
      delArr = delArr.concat(userObj.del.map(function(user) {
        return {
          'PrivilegeInfo':{
            'permission_name': permission.PermissionInfo.permission_name,
            'principal_name': user,
            'principal_type': 'USER'
          }
        }
      }));
      delArr = delArr.concat(groupObj.del.map(function(group) {
        return {
          'PrivilegeInfo':{
            'permission_name': permission.PermissionInfo.permission_name,
            'principal_name': group,
            'principal_type': 'GROUP'
          }
        }
      }));
    });

    if(addArr.length){
      resource.createPrivileges(params, addArr)
      .then(function() {
        deferred.resolve();
      })
      .catch(function(data) {
        deferred.reject(data);
      });
    }

    if(delArr.length){
      resource.deletePrivileges(params, delArr)
      .then(function() {
        deferred.resolve();
      })
      .catch(function(data) {
        deferred.resolve(data);
      });
    }

    return deferred.promise;
  }

  return {
    saveClusterPermissions: function(oldPermissions, newPermissions, params) {
      return savePermissionsFor(Cluster, oldPermissions, newPermissions, params);
    },
    saveViewPermissions: function(permissions, params) {
      return savePermissionsFor(View, permissions, params);
    }
  };
}]);
