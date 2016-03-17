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
        deletePrivilege: _$q_.defer(),
        createPrivileges: _$q_.defer()
      };
      ctrl = $controller('UserAccessListCtrl', {
        $scope: scope
      });
      mock = {
        Cluster: Cluster,
        Alert: Alert,
        scope: scope
      };
      spyOn(Cluster, 'deletePrivilege').andReturn(deferred.deletePrivilege.promise);
      spyOn(Cluster, 'createPrivileges').andReturn(deferred.createPrivileges.promise);
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
  });

});
