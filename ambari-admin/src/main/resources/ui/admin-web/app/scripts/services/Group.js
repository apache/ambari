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
.factory('Group', ['$http', '$q', 'Settings', function($http, $q, Settings) {
  function Group(item){
    if(typeof item === 'string'){
      this.group_name = item;
    } else if(typeof item === 'object'){
      angular.extend(this, item.Groups);
      this.getMembers();
    }
  }

  Group.prototype.isLDAP = function() {
    var deferred = $q.defer();
    var self = this;
    if( typeof this.ldap_group === 'boolean' ){
      deferred.resolve(this.ldap_group)
    } else {
      $http({
        method: 'GET',
        url: Settings.baseUrl + '/groups/'+this.group_name
      }).
      success(function(data) {
        self.ldap_group = data.Groups.ldap_group;
        deferred.resolve(self.ldap_group);
      });
    }

    return deferred.promise;
  }

  Group.prototype.save = function() {
    return $http({
      method : 'POST',
      url: Settings.baseUrl + '/groups',
      data:{
        'Groups/group_name': this.group_name
      }
    });
  };

  Group.prototype.destroy = function() {
    var deferred = $q.defer();
    $http.delete(Settings.baseUrl + '/groups/' +this.group_name)
    .success(function() {
      deferred.resolve();
    })
    .error(function(data) {
      deferred.reject(data);
    });

    return deferred.promise;
  };

  Group.prototype.getMembers = function() {
    var deferred = $q.defer();
    var self = this;

    $http({
      method: 'GET',
      url: Settings.baseUrl + '/groups/' + this.group_name + '/members'
    })
    .success(function(data) {
      self.members = [];
      angular.forEach(data.items, function(member) {
        self.members.push(member.MemberInfo.user_name);
      });
      deferred.resolve(self.members);
    })
    .error(function(data) {
      deferred.reject(data);
    });

    return deferred.promise;
  };

  Group.prototype.saveMembers = function() {
    var self = this;
    var deferred = $q.defer();

    var members = [];
    angular.forEach(this.members, function(member) {
      members.push({
        'MemberInfo/user_name' : member,
        'MemberInfo/group_name' : self.group_name
      });
    });

    $http({
      method: 'PUT',
      url: Settings.baseUrl + '/groups/' + this.group_name + '/members',
      data: members
    })
    .success(function(data) {
      deferred.resolve(data);
    })
    .error(function(data) {
      deferred.reject(data);
    });
    return deferred.promise;
  }

  Group.prototype.addMember = function(memberName) {
    var deferred = $q.defer();

    $http({
      method: 'POST',
      url: Settings.baseUrl + '/groups/' + this.group_name + '/members' + '/'+ encodeURIComponent(member.user_name)
    })
    .success(function(data) {
      deferred.resolve(data)
    })
    .error(function(data) {
      deferred.reject(data);
    });

    return deferred.promise;
  };

  Group.prototype.removeMember = function(memberId) {
    return $http.delete(Settings.baseUrl + '/groups/'+this.group_name+'/members/'+memberId);
  };

  Group.removeMemberFromGroup = function(groupName, memberName) {
    return $http.delete(Settings.baseUrl + '/groups/'+groupName + '/members/'+memberName);
  };

  Group.addMemberToGroup = function(groupName, memberName) {
    return $http.post(Settings.baseUrl + '/groups/' + groupName + '/members/'+memberName);
  };

  Group.all = function(params) {
    var deferred = $q.defer();

    $http.get(Settings.baseUrl + '/groups?'
      + 'Groups/group_name.matches(.*'+params.searchString+'.*)'
      + '&fields=*'
      + '&from='+ (params.currentPage-1)*params.groupsPerPage
      + '&page_size=' + params.groupsPerPage
      + (params.ldap_group === '*' ? '' : '&Groups/ldap_group='+params.ldap_group)
    )
    .success(function(data) {
      var groups = [];
      if(Array.isArray(data.items)){
        angular.forEach(data.items, function(item) {
          groups.push(new Group(item));
        });
      }
      groups.itemTotal = data.itemTotal;
      deferred.resolve(groups);
    })
    .error(function(data) {
      deferred.reject(data);
    });

    return deferred.promise;
  };

  Group.listByName = function(name) {
    return $http.get(Settings.baseUrl + '/groups?'
      + 'Groups/group_name.matches(.*'+name+'.*)'
    );
  };

  Group.getPrivileges = function(groupId) {
    return $http.get(Settings.baseUrl + '/privileges', {
        params:{
          'PrivilegeInfo/principal_type': 'GROUP',
          'PrivilegeInfo/principal_name': groupId,
          'fields': '*'
        }
      });
  };

  return Group;
}]);
