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

describe('#Cluster', function () {

  describe('UserAccessListCtrl', function() {

    var scope, ctrl, $t, $httpBackend, Cluster, deferred, Alert, mock;

    beforeEach(module('ambariAdminConsole', function () {}));

    beforeEach(inject(function($rootScope, $controller, _$translate_, _$httpBackend_, _Cluster_, _$q_, _Alert_) {
      scope = $rootScope.$new();
      $t = _$translate_.instant;
      $httpBackend = _$httpBackend_;
      Cluster = _Cluster_;
      Alert = _Alert_;
      deferred = {
        createPrivileges: _$q_.defer(),
        getPrivilegesForResource: _$q_.defer(),
        getPrivilegesWithFilters: _$q_.defer(),
        deletePrivileges: _$q_.defer(),
        deleteMultiplePrivileges: _$q_.defer()
      };
      ctrl = $controller('UserAccessListCtrl', {
        $scope: scope
      });
      mock = {
        Cluster: Cluster,
        Alert: Alert,
        scope: scope
      };
      spyOn(Cluster, 'createPrivileges').andReturn(deferred.createPrivileges.promise);
      spyOn(Cluster, 'deletePrivileges').andReturn(deferred.deletePrivileges.promise);
      spyOn(Cluster, 'getPrivilegesForResource').andReturn(deferred.getPrivilegesForResource.promise);
      spyOn(Cluster, 'getPrivilegesWithFilters').andReturn(deferred.getPrivilegesWithFilters.promise);
      spyOn(Alert, 'success').andCallFake(angular.noop);
      spyOn(Alert, 'error').andCallFake(angular.noop);
      spyOn(scope, 'loadRoles').andCallFake(angular.noop);

      $httpBackend.expectGET(/\/api\/v1\/permissions/).respond(200, {
        items: []
      });
      $httpBackend.expectGET(/\/api\/v1\/users?.*/).respond(200, {
        items:[]
      });
      $httpBackend.flush();
    }));

    describe('#clearFilters()', function () {

      it('should clear filters and reset pagination', function () {
        scope.currentPage = 2;
        scope.currentNameFilter = 'a';
        scope.roleFilterOptions = [
          {
            label: $t('common.all'),
            value: ''
          },
          {
            label: $t('users.roles.clusterUser'),
            value: 'CLUSTER.USER'
          }
        ];
        scope.typeFilterOptions = [
          {label: $t('common.user'), value: 'USER'},
          {label: $t('common.group'), value: 'GROUP'}
        ];
        scope.currentRoleFilter = scope.roleFilterOptions[1];
        scope.clearFilters();
        expect(scope.currentNameFilter).toEqual('');
        expect(scope.currentRoleFilter).toEqual({
          label: $t('common.all'),
          value: ''
        });
        expect(scope.currentPage).toEqual(1);
      });

    });

    describe('#isNotEmptyFilter', function () {

      var cases = [
        {
          currentNameFilter: '',
          currentTypeFilter: null,
          currentRoleFilter: null,
          isNotEmptyFilter: false,
          title: 'no filters'
        },
        {
          currentNameFilter: '',
          currentTypeFilter: {
            value: ''
          },
          currentRoleFilter: {
            value: ''
          },
          isNotEmptyFilter: false,
          title: 'empty filters'
        },
        {
          currentNameFilter: 'a',
          currentTypeFilter: {
            value: ''
          },
          currentRoleFilter: {
            value: ''
          },
          isNotEmptyFilter: true,
          title: 'name filter'
        },
        {
          currentNameFilter: '0',
          currentTypeFilter: {
            value: ''
          },
          currentRoleFilter: {
            value: ''
          },
          isNotEmptyFilter: true,
          title: 'name filter with "0" as string'
        },
        {
          currentNameFilter: '',
          currentTypeFilter: {
            value: 'GROUP'
          },
          currentRoleFilter: {
            value: ''
          },
          isNotEmptyFilter: true,
          title: 'type filter'
        },
        {
          currentNameFilter: '',
          currentTypeFilter: {
            value: ''
          },
          currentRoleFilter: {
            value: 'CLUSTER.USER'
          },
          isNotEmptyFilter: true,
          title: 'role filter'
        },
        {
          currentNameFilter: 'a',
          currentTypeFilter: {
            value: 'GROUP'
          },
          currentRoleFilter: {
            value: ''
          },
          isNotEmptyFilter: true,
          title: 'name and type filters'
        },
        {
          currentNameFilter: 'a',
          currentTypeFilter: {
            value: ''
          },
          currentRoleFilter: {
            value: 'CLUSTER.USER'
          },
          isNotEmptyFilter: true,
          title: 'name and role filters'
        },
        {
          currentNameFilter: '0',
          currentTypeFilter: {
            value: 'GROUP'
          },
          currentRoleFilter: {
            value: ''
          },
          isNotEmptyFilter: true,
          title: 'name and type filters with "0" as string'
        },
        {
          currentNameFilter: '0',
          currentTypeFilter: {
            value: ''
          },
          currentRoleFilter: {
            value: 'CLUSTER.USER'
          },
          isNotEmptyFilter: true,
          title: 'name and role filters with "0" as string'
        },
        {
          currentNameFilter: '',
          currentTypeFilter: {
            value: 'GROUP'
          },
          currentRoleFilter: {
            value: 'CLUSTER.USER'
          },
          isNotEmptyFilter: true,
          title: 'type and role filters'
        },
        {
          currentNameFilter: 'a',
          currentTypeFilter: {
            value: 'CLUSTER.USER'
          },
          currentRoleFilter: {
            value: 'GROUP'
          },
          isNotEmptyFilter: true,
          title: 'all filters'
        },
        {
          currentNameFilter: '0',
          currentTypeFilter: {
            value: 'CLUSTER.USER'
          },
          currentRoleFilter: {
            value: 'GROUP'
          },
          isNotEmptyFilter: true,
          title: 'all filters with "0" as string'
        }
      ];

      cases.forEach(function (item) {
        it(item.title, function () {
          scope.currentNameFilter = item.currentNameFilter;
          scope.currentRoleFilter = item.currentRoleFilter;
          scope.currentTypeFilter = item.currentTypeFilter;
          scope.$digest();
          expect(scope.isNotEmptyFilter).toEqual(item.isNotEmptyFilter);
        });
      });

    });

    describe('#save() for Users', function(){
      var user = {};

      beforeEach(function() {
        items1 = {
            "href" : "http://abc.com:8080/api/v1/users/user1",
            "Users" : { "user_name" : "user1" },
            "privileges" : [
              {
                "href" : "http://abc.com:8080/api/v1/users/user1/privileges/222",
                "PrivilegeInfo" : {
                  "cluster_name" : "myCluster",
                  "permission_label" : "Service Administrator",
                  "permission_name" : "SERVICE.ADMINISTRATOR",
                  "principal_name" : "mygroup2",
                  "principal_type" : "GROUP",
                  "privilege_id" : 222,
                  "type" : "CLUSTER",
                  "user_name" : "user1"
                }
              }, {
                "href" : "http://abc.com:8080/api/v1/users/user1/privileges/111",
                "PrivilegeInfo" : {
                  "cluster_name" : "myCluster",
                  "permission_label" : "Service Administrator",
                  "permission_name" : "SERVICE.ADMINISTRATOR",
                  "principal_name" : "mygroup",
                  "principal_type" : "GROUP",
                  "privilege_id" : 111,
                  "type" : "CLUSTER",
                  "user_name" : "user1"
                }
              }, {
                "href" : "http://abc.com:8080/api/v1/users/user1/privileges/11",
                "PrivilegeInfo" : {
                  "cluster_name" : "myCluster",
                  "permission_label" : "Cluster Administrator",
                  "permission_name" : "CLUSTER.ADMINISTRATOR",
                  "principal_name" : "user1",
                  "principal_type" : "USER",
                  "privilege_id" : 11,
                  "type" : "CLUSTER",
                  "user_name" : "user1"
                }
              }
            ]
          };

        items2 =
          {
            "href" : "http://abc.com:8080/api/v1/users/user2",
            "Users" : { "user_name" : "user2" },
            "privileges" : [
              {
                "href" : "http://abc.com:8080/api/v1/users/user2/privileges/111",
                "PrivilegeInfo" : {
                  "cluster_name" : "myCluster",
                  "permission_label" : "Service Administrator",
                  "permission_name" : "SERVICE.ADMINISTRATOR",
                  "principal_name" : "mygroup",
                  "principal_type" : "GROUP",
                  "privilege_id" : 111,
                  "type" : "CLUSTER",
                  "user_name" : "user2"
                }
              }, {
                "href" : "http://abc.com:8080/api/v1/users/user2/privileges/22",
                "PrivilegeInfo" : {
                  "cluster_name" : "myCluster",
                  "permission_label" : "Service Administrator",
                  "permission_name" : "SERVICE.ADMINISTRATOR",
                  "principal_name" : "user2",
                  "principal_type" : "USER",
                  "privilege_id" : 22,
                  "type" : "CLUSTER",
                  "user_name" : "user2"
                }
              }
            ]
          };

        all_items = { "items": [items1, items2]};

        scope.loadUsers();

        spyOn(Cluster, 'deleteMultiplePrivileges').andCallFake(function(clusterId, privilege_ids) {
          privilege_ids.forEach(function(privilege_id) {
            items1.privileges.forEach(function(p, index) {
              if (p.PrivilegeInfo.privilege_id === privilege_id) {
                //Remove from array
                items1.privileges.splice(index, 1);
              }
            });
          });
        });
        spyOn(scope, 'addPrivilege').andCallFake(function(user) {
          var p = {};
          p.PrivilegeInfo = {};
          p.PrivilegeInfo.privilege_id = user.privilege_id + 1;
          p.PrivilegeInfo.permission_name = user.permission_name;
          p.PrivilegeInfo.principal_type = 'USER';

          items1.privileges.push(p);
          scope.loadUsers();
        });

        deferred.getPrivilegesWithFilters.resolve(all_items);
        deferred.getPrivilegesForResource.promise.then(function(data) {
          var arrayOfPrivileges = data.items[0].privileges;
          var privilegesOfTypeUser = [];
          var privilegesOfTypeGroup = [];
          for (var i = 0; i < arrayOfPrivileges.length; i++) {
            if(arrayOfPrivileges[i].PrivilegeInfo.principal_type === "GROUP"){
              privilegesOfTypeGroup.push(arrayOfPrivileges[i]);
            } else {
              privilegesOfTypeUser.push(arrayOfPrivileges[i].PrivilegeInfo);
            }
          }

          var effectivePrivilege = scope.pickEffectivePrivilege(arrayOfPrivileges);
          var effectivePrivilegeFromGroups = scope.pickEffectivePrivilege(privilegesOfTypeGroup);
          user.principal_type = 'USER';
          user.original_perm = effectivePrivilege.permission_name;
          user.editable = (Cluster.ineditableRoles.indexOf(effectivePrivilege.permission_name) === -1);

          //add a new privilege of type USER only if it is also the effective privilege considering the user's Group privileges
          var curIndex = scope.getRoleRank(user.permission_name);
          var prevIndex = -1;
          if (privilegesOfTypeGroup.length !== 0) {
            prevIndex = scope.getRoleRank(effectivePrivilegeFromGroups.permission_name);
          }
          if ((curIndex === 6) || (curIndex <= prevIndex)) {
            var privilege_ids = [];
            privilegesOfTypeUser.forEach(function(privilegeOfTypeUser) {
              privilege_ids.push(privilegeOfTypeUser.privilege_id);
            });

            //delete all privileges of type USER, if they exist
            //then add the privilege for the user, after which the user displays the effective privilege
            if(privilege_ids.length !== 0) {
              Cluster.deleteMultiplePrivileges(
                123,
                privilege_ids
              );
            }
            scope.addPrivilege(user);
          } else {
            Alert.error($t('common.alerts.cannotSavePermissions'), "User's effective privilege through its Group(s) is higher than your selected privilege.");
            scope.loadUsers();
          }
        });

        scope.$apply();
      });

      it('Should save the Privilege equal to the user\'s group privileges. Should also remove any individual user privileges', function() {
        //using 'user1' for updating the new privilege
        user.principal_name = scope.users[0].principal_name;
        user.principal_type = scope.users[0].principal_type;
        user.privilege_id = scope.users[0].privilege_id;
        user.permission_name = "SERVICE.ADMINISTRATOR";

        deferred.getPrivilegesForResource.resolve({ "items" : [items1] });

        scope.$apply();

        expect(scope.users[0].permission_name).toEqual(user.permission_name);
        expect(scope.users[0].privilege_id).toEqual(user.privilege_id + 1);

        var oldPrivilege = {
          "href" : "http://abc.com:8080/api/v1/users/user1/privileges/11",
          "PrivilegeInfo" : {
            "cluster_name" : "myCluster",
            "permission_label" : "Cluster Administrator",
            "permission_name" : "CLUSTER.ADMINISTRATOR",
            "principal_name" : "user1",
            "principal_type" : "USER",
            "privilege_id" : 11,
            "type" : "CLUSTER",
            "user_name" : "user1"
          }
        };
        var newPrivilege = {
          "PrivilegeInfo" : {
            "permission_name" : user.permission_name,
            "privilege_id" : user.privilege_id+1,
            "principal_type" : "USER",
            "principal_name" : "user1",
            "encoded_name" : "user1",
            "original_perm" : user.permission_name,
            "url" : "users/user1",
            "editable" : true
          }
        };

        //test if the individual user privilege CLUSTER.ADMINISTRATOR is removed from 'items1' by deletePrivilege()
        expect(items1.privileges).toNotContain(oldPrivilege);

        //test if the new privilege got added to 'items1' by addPrivilege()
        expect(items1.privileges).toContain(newPrivilege);
      });

      it('Should save the Privilege greater than the user\'s group privileges. Should also remove any individual user privileges', function() {
        //using 'user1' for updating the new privilege
        user.principal_name = scope.users[0].principal_name;
        user.principal_type = scope.users[0].principal_type;
        user.privilege_id = scope.users[0].privilege_id;
        user.permission_name = "CLUSTER.OPERATOR";

        deferred.getPrivilegesForResource.resolve({ "items" : [items1] });
        scope.$apply();

        expect(scope.users[0].permission_name).toEqual(user.permission_name);
        expect(scope.users[0].privilege_id).toEqual(user.privilege_id + 1);

        var oldPrivilege = {
          "href" : "http://abc.com:8080/api/v1/users/user1/privileges/11",
          "PrivilegeInfo" : {
            "cluster_name" : "myCluster",
            "permission_label" : "Cluster Administrator",
            "permission_name" : "CLUSTER.ADMINISTRATOR",
            "principal_name" : "user1",
            "principal_type" : "USER",
            "privilege_id" : 11,
            "type" : "CLUSTER",
            "user_name" : "user1"
          }
        };
        var newPrivilege = {
          "PrivilegeInfo" : {
            "permission_name" : user.permission_name,
            "principal_name" : "user1",
            "principal_type" : "USER",
            "privilege_id" : user.privilege_id + 1,
            "encoded_name" : "user1",
            "original_perm" : user.permission_name,
            "url" : "users/user1",
            "editable" : true
          }
        };

        //test if the individual user privilege CLUSTER.ADMINISTRATOR is removed from 'items1' by deletePrivilege()
        expect(items1.privileges).toNotContain(oldPrivilege);

        //test if the new privilege got added to 'items1' by addPrivilege()
        expect(items1.privileges).toContain(newPrivilege);
      });

      it('Should NOT save the Privilege smaller than the user\'s group privileges. Should keep the user\'s original privileges intact', function() {
        //using 'user1' for updating the new privilege
        user.principal_name = scope.users[0].principal_name;
        user.principal_type = scope.users[0].principal_type;
        user.privilege_id = scope.users[0].privilege_id;
        user.permission_name = "CLUSTER.USER";

        deferred.getPrivilegesForResource.resolve({ "items" : [items1] });
        scope.$apply();

        expect(scope.users[0].permission_name).toEqual(user.original_perm);
        expect(scope.users[0].privilege_id).toEqual(user.privilege_id);

        var oldPrivilege = {
          "href" : "http://abc.com:8080/api/v1/users/user1/privileges/11",
          "PrivilegeInfo" : {
            "cluster_name" : "myCluster",
            "permission_label" : "Cluster Administrator",
            "permission_name" : "CLUSTER.ADMINISTRATOR",
            "principal_name" : "user1",
            "principal_type" : "USER",
            "privilege_id" : 11,
            "type" : "CLUSTER",
            "user_name" : "user1",
            "encoded_name" : "user1",
            "original_perm" : "CLUSTER.ADMINISTRATOR",
            "url" : "users/user1",
            "editable" : true
          }
        };
        var newPrivilege = {
          "PrivilegeInfo" : {
            "permission_name" : user.permission_name,
            "principal_name" : "user1",
            "principal_type" : "USER",
            "privilege_id" : user.privilege_id+1,
            "encoded_name" : "user1",
            "original_perm" : user.permission_name,
            "url" : "users/user1",
            "editable" : true
          }
        };

        //test if the individual user privilege CLUSTER.ADMINISTRATOR is NOT removed from 'items1'
        expect(items1.privileges).toContain(oldPrivilege);

        //test if the new privilege is NOT added to 'items1'
        expect(items1.privileges).toNotContain(newPrivilege);
      });

    });

    describe('#save() for Groups', function() {
      var user = {};

      beforeEach(function() {
        items1 = {
          "href" : "http://abc.com:8080/api/v1/groups/mygroup",
          "Groups" : { "group_name" : "mygroup" },
          "privileges" : [
            {
              "href" : "http://abc.com:8080/api/v1/groups/mygroup/privileges/3359",
              "PrivilegeInfo" : {
                "cluster_name" : "myCluster",
                "group_name" : "mygroup",
                "permission_label" : "Service Administrator",
                "permission_name" : "SERVICE.ADMINISTRATOR",
                "principal_name" : "mygroup",
                "principal_type" : "GROUP",
                "privilege_id" : 3359,
                "type" : "CLUSTER"
              }
            }
          ]
        };

        items2 = {
          "href" : "http://abc.com:8080/api/v1/groups/mygroup2",
          "Groups" : { "group_name" : "mygroup2" },
          "privileges" : [
            {
              "href" : "http://abc.com:8080/api/v1/groups/mygroup2/privileges/3356",
              "PrivilegeInfo" : {
                "cluster_name" : "myCluster",
                "group_name" : "mygroup2",
                "permission_label" : "Service Administrator",
                "permission_name" : "SERVICE.ADMINISTRATOR",
                "principal_name" : "mygroup2",
                "principal_type" : "GROUP",
                "privilege_id" : 3356,
                "type" : "CLUSTER"
              }
            }
          ]
        };

        all_items = { "items": [items1, items2]};

        scope.loadUsers();

        spyOn(Cluster, 'deleteMultiplePrivileges').andCallFake(function(clusterId, privilege_ids) {
          privilege_ids.forEach(function(privilege_id) {
            items1.privileges.forEach(function(p, index) {
              if (p.PrivilegeInfo.privilege_id === privilege_id) {
                //Remove from array
                items1.privileges.splice(index, 1);
              }
            });
          });
        });
        spyOn(scope, 'addPrivilege').andCallFake(function(user) {
          var p = {};
          p.PrivilegeInfo = {};
          p.PrivilegeInfo.privilege_id = user.privilege_id + 1;
          p.PrivilegeInfo.permission_name = user.permission_name;
          p.PrivilegeInfo.principal_type = 'GROUP';

          items1.privileges.push(p);
          scope.loadUsers();
        });

        deferred.getPrivilegesWithFilters.resolve(all_items);
        deferred.getPrivilegesForResource.promise.then(function(data) {
          var arrayOfPrivileges = data.items[0].privileges;
          var privilegesOfTypeGroup = [];
          var privilege = scope.pickEffectivePrivilege(arrayOfPrivileges);
          user.principal_type = 'GROUP';
          user.original_perm = privilege.permission_name;
          user.editable = (Cluster.ineditableRoles.indexOf(privilege.permission_name) === -1);

          arrayOfPrivileges.forEach(function(privilegeOfTypeGroup){
            if (privilegeOfTypeGroup.PrivilegeInfo.principal_type === "GROUP") {
              privilegesOfTypeGroup.push(privilegeOfTypeGroup.PrivilegeInfo);
            }
          });

          var privilege_ids = [];
          privilegesOfTypeGroup.forEach(function(privilegeOfTypeGroup) {
            privilege_ids.push(privilegeOfTypeGroup.privilege_id);
          });

          //delete all privileges of type GROUP, if they exist
          //then add the privilege for the group, after which the group displays the effective privilege
          if(privilege_ids.length !== 0) {
            Cluster.deleteMultiplePrivileges(
                123,
                privilege_ids
            );
          }
          scope.addPrivilege(user);
        });

        scope.$apply();
      });

      it('Should save the Privilege equal to the group\'s effective privilege. Should remove any other privileges of the group',function(){
        //using 'mygroup' for updating the new privilege
        user.principal_name = scope.users[0].principal_name;
        user.principal_type = scope.users[0].principal_type;
        user.privilege_id = scope.users[0].privilege_id;
        user.permission_name = "SERVICE.ADMINISTRATOR";

        deferred.getPrivilegesForResource.resolve({ "items" : [items1] });

        scope.$apply();

        expect(scope.users[0].permission_name).toEqual(user.permission_name);
        expect(scope.users[0].privilege_id).toEqual(user.privilege_id + 1);

        var oldPrivilege = {
          "PrivilegeInfo" : {
            "cluster_name" : "myCluster",
            "group_name" : "mygroup",
            "permission_label" : "Service Administrator",
            "permission_name" : "SERVICE.ADMINISTRATOR",
            "principal_name" : "mygroup",
            "principal_type" : "GROUP",
            "privilege_id" : 3359,
            "type" : "CLUSTER"
          }
        };
        var newPrivilege = {
          "PrivilegeInfo" : {
            "privilege_id" : user.privilege_id + 1,
            "permission_name" : user.permission_name,
            "principal_type" : "GROUP",
            "principal_name" : "mygroup",
            "encoded_name" : "mygroup",
            "original_perm" : user.permission_name,
            "url" : "groups/mygroup/edit",
            "editable" : true
          }
        };

        //test if the older privilege is no longer present in 'items1'
        expect(items1.privileges).toNotContain(oldPrivilege);

        //test if the new privilege is added to 'items1'
        expect(items1.privileges).toContain(newPrivilege);
      });

      it('Should save the Privilege greater than the group\'s effective privilege. Should remove any other privileges of the group',function(){
        //using 'mygroup' for updating the new privilege
        user.principal_name = scope.users[0].principal_name;
        user.principal_type = scope.users[0].principal_type;
        user.privilege_id = scope.users[0].privilege_id;
        user.permission_name = "CLUSTER.ADMINISTRATOR";

        deferred.getPrivilegesForResource.resolve({ "items" : [items1] });

        scope.$apply();

        expect(scope.users[0].permission_name).toEqual(user.permission_name);
        expect(scope.users[0].privilege_id).toEqual(user.privilege_id + 1);

        var oldPrivilege = {
          "PrivilegeInfo" : {
            "cluster_name" : "myCluster",
            "group_name" : "mygroup",
            "permission_label" : "Service Administrator",
            "permission_name" : "SERVICE.ADMINISTRATOR",
            "principal_name" : "mygroup",
            "principal_type" : "GROUP",
            "privilege_id" : 3359,
            "type" : "CLUSTER"
          }
        };
        var newPrivilege = {
          "PrivilegeInfo" : {
            "privilege_id" : user.privilege_id + 1,
            "permission_name" : user.permission_name,
            "principal_type" : "GROUP",
            "principal_name" : "mygroup",
            "encoded_name" : "mygroup",
            "original_perm" : user.permission_name,
            "url" : "groups/mygroup/edit",
            "editable" : true
          }
        };

        //test if the older privilege is no longer present in 'items1'
        expect(items1.privileges).toNotContain(oldPrivilege);

        //test if the new privilege is added to 'items1'
        expect(items1.privileges).toContain(newPrivilege);
      });

      it('Should save the Privilege lesser than the group\'s effective privilege. Should remove any other privileges of the group', function() {
        //using 'mygroup' for updating the new privilege
        user.principal_name = scope.users[0].principal_name;
        user.principal_type = scope.users[0].principal_type;
        user.privilege_id = scope.users[0].privilege_id;
        user.permission_name = "CLUSTER.USER";

        deferred.getPrivilegesForResource.resolve({ "items" : [items1] });

        scope.$apply();

        expect(scope.users[0].permission_name).toEqual(user.permission_name);
        expect(scope.users[0].privilege_id).toEqual(user.privilege_id + 1);

        var oldPrivilege = {
          "PrivilegeInfo" : {
            "cluster_name" : "myCluster",
            "group_name" : "mygroup",
            "permission_label" : "Service Administrator",
            "permission_name" : "SERVICE.ADMINISTRATOR",
            "principal_name" : "mygroup",
            "principal_type" : "GROUP",
            "privilege_id" : 3359,
            "type" : "CLUSTER"
          }
        };
        var newPrivilege = {
          "PrivilegeInfo" : {
            "privilege_id" : user.privilege_id + 1,
            "permission_name" : user.permission_name,
            "principal_type" : "GROUP",
            "principal_name" : "mygroup",
            "encoded_name" : "mygroup",
            "original_perm" : user.permission_name,
            "url" : "groups/mygroup/edit",
            "editable" : true
          }
        };

        //test if the older privilege is no longer present in 'items1'
        expect(items1.privileges).toNotContain(oldPrivilege);

        //test if the new privilege is added to 'items1'
        expect(items1.privileges).toContain(newPrivilege);
      });

    });

    describe('#pickEffectivePrivilege()', function() {
      var cases = [{
          "test" : [{
            "href" : "http://abc.com:8080/api/v1/groups/mygroup1",
            "PrivilegeInfo" : {
              "instance_name" : "jobs_view",
              "permission_label" : "View User",
              "permission_name" : "VIEW.USER",
              "principal_name" : "mygroup1",
              "principal_type" : "GROUP",
              "privilege_id" : 111,
              "type" : "VIEW",
              "user_name" : "mygroup1",
              "view_name" : "JOBS"
            }
          }],
          "result":{
            "permission_name": "CLUSTER.NONE"
          }
        }, {
          "test": [{
            "href" : "http://abc.com:8080/api/v1/groups/mygroup2",
            "PrivilegeInfo" : {
              "cluster_name":"mycluster",
              "permission_label" : "Cluster User",
              "permission_name" : "CLUSTER.USER",
              "principal_name" : "mygroup2",
              "principal_type" : "GROUP",
              "privilege_id" : 222,
              "type" : "CLUSTER",
              "user_name":"mygroup2"
            }
          }],
          "result":{
            "permission_name": "CLUSTER.USER"
          }
        }, {
          "test":[{
            "href" : "http://abc.com:8080/api/v1/groups/mygroup3",
            "PrivilegeInfo" : {
              "cluster_name":"mycluster",
              "permission_label" : "Cluster User",
              "permission_name" : "CLUSTER.USER",
              "principal_name" : "mygroup3",
              "principal_type" : "GROUP",
              "privilege_id" : 333,
              "type" : "CLUSTER",
              "user_name":"mygroup3"
            }
          }, {
            "href" : "http://abc.com:8080/api/v1/groups/mygroup3",
            "PrivilegeInfo" : {
              "instance_name": "jobs_view",
              "permission_label" : "View User",
              "permission_name" : "VIEW.USER",
              "principal_name" : "mygroup3",
              "principal_type" : "GROUP",
              "privilege_id" : 3333,
              "type" : "VIEW",
              "user_name":"mygroup3",
              "view_name":"JOBS"
            }
          }],
          "result":{
            "permission_name": "CLUSTER.USER"
          }
        }, {
          "test": [{
            "href" : "http://abc.com:8080/api/v1/users/myuser1/privileges/11",
            "PrivilegeInfo" : {
              "instance_name": "jobs_view",
              "permission_label" : "View User",
              "permission_name" : "VIEW.USER",
              "principal_name" : "myuser1",
              "principal_type" : "USER",
              "privilege_id" : 11,
              "type" : "VIEW",
              "user_name":"myuser1",
              "view_name":"JOBS"
            }
          }],
          "result":{
            "permission_name": "CLUSTER.NONE"
          }
        }, {
          "test":[{
            "href" : "http://abc.com:8080/api/v1/users/myuser2/privileges/22",
            "PrivilegeInfo" : {
              "cluster_name":"mycluster",
              "permission_label" : "Cluster Administrator",
              "permission_name" : "CLUSTER.ADMINISTRATOR",
              "principal_name" : "myuser2",
              "principal_type" : "USER",
              "privilege_id" : 22,
              "type" : "CLUSTER",
              "user_name":"myuser2"
            }
          }],
          "result":{
            "permission_name": "CLUSTER.ADMINISTRATOR"
          }
        }
      ];

      it('User/Group with only View User permission must show \'None\' as the Cluster Permission, otherwise show the effective privilege', function(){
        var effectivePrivilege;
        cases.forEach(function (item){
          effectivePrivilege = scope.pickEffectivePrivilege(item.test);
          scope.$apply();
          expect(effectivePrivilege.permission_name).toEqual(item.result.permission_name);
        });
      });
    });

  });
});
