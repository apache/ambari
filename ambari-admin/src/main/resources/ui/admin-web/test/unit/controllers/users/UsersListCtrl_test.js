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

  describe('UsersListCtrl', function() {

    var scope, ctrl, $t, $httpBackend;

    beforeEach(module('ambariAdminConsole', function () {}));

    beforeEach(inject(function($rootScope, $controller, _$translate_, _$httpBackend_) {
      scope = $rootScope.$new();
      $t = _$translate_.instant;
      $httpBackend = _$httpBackend_;
      ctrl = $controller('UsersListCtrl', {
        $scope: scope
      });
    }));

    describe('#clearFilters()', function () {

      it('should clear filters and reset pagination', function () {
        scope.currentPage = 2;
        scope.currentNameFilter = 'a';
        scope.currentActiveFilter = {
          label: $t('common.local'),
          value: false
        };
        scope.currentTypeFilter = {
          label: $t('common.local'),
          value: 'LOCAL'
        };
        scope.clearFilters();
        expect(scope.currentNameFilter).toEqual('');
        expect(scope.currentActiveFilter).toEqual({
          label: $t('common.all'),
          value: '*'
        });
        expect(scope.currentTypeFilter).toEqual({
          label: $t('common.all'),
          value: '*'
        });
        expect(scope.currentPage).toEqual(1);
      });

    });

    describe('#isNotEmptyFilter', function () {

      var cases = [
        {
          currentNameFilter: '',
          currentTypeFilter: null,
          currentActiveFilter: null,
          isNotEmptyFilter: false,
          title: 'no filters'
        },
        {
          currentNameFilter: '',
          currentTypeFilter: {
            value: '*'
          },
          currentActiveFilter: {
            value: '*'
          },
          isNotEmptyFilter: false,
          title: 'empty filters'
        },
        {
          currentNameFilter: 'a',
          currentTypeFilter: {
            value: '*'
          },
          currentActiveFilter: {
            value: '*'
          },
          isNotEmptyFilter: true,
          title: 'name filter'
        },
        {
          currentNameFilter: '0',
          currentTypeFilter: {
            value: '*'
          },
          currentActiveFilter: {
            value: '*'
          },
          isNotEmptyFilter: true,
          title: 'name filter with "0" as string'
        },
        {
          currentNameFilter: '',
          currentTypeFilter: {
            value: 'LOCAL'
          },
          currentActiveFilter: {
            value: '*'
          },
          isNotEmptyFilter: true,
          title: 'type filter'
        },
        {
          currentNameFilter: '',
          currentTypeFilter: {
            value: '*'
          },
          currentActiveFilter: {
            value: false
          },
          isNotEmptyFilter: true,
          title: 'activity filter'
        },
        {
          currentNameFilter: 'a',
          currentTypeFilter: {
            value: 'LOCAL'
          },
          currentActiveFilter: {
            value: '*'
          },
          isNotEmptyFilter: true,
          title: 'name and type filters'
        },
        {
          currentNameFilter: 'a',
          currentTypeFilter: {
            value: '*'
          },
          currentActiveFilter: {
            value: false
          },
          isNotEmptyFilter: true,
          title: 'name and activity filters'
        },
        {
          currentNameFilter: '0',
          currentTypeFilter: {
            value: 'LOCAL'
          },
          currentActiveFilter: {
            value: '*'
          },
          isNotEmptyFilter: true,
          title: 'name and type filters with "0" as string'
        },
        {
          currentNameFilter: '0',
          currentTypeFilter: {
            value: '*'
          },
          currentActiveFilter: {
            value: false
          },
          isNotEmptyFilter: true,
          title: 'name and activity filters with "0" as string'
        },
        {
          currentNameFilter: '',
          currentTypeFilter: {
            value: 'LOCAL'
          },
          currentActiveFilter: {
            value: false
          },
          isNotEmptyFilter: true,
          title: 'type and activity filters'
        },
        {
          currentNameFilter: 'a',
          currentTypeFilter: {
            value: false
          },
          currentActiveFilter: {
            value: 'LOCAL'
          },
          isNotEmptyFilter: true,
          title: 'all filters'
        },
        {
          currentNameFilter: '0',
          currentTypeFilter: {
            value: false
          },
          currentActiveFilter: {
            value: 'LOCAL'
          },
          isNotEmptyFilter: true,
          title: 'all filters with "0" as string'
        }
      ];

      cases.forEach(function (item) {
        it(item.title, function () {
          $httpBackend.expectGET(/\/api\/v1\/users/).respond(200);
          scope.currentNameFilter = item.currentNameFilter;
          scope.currentActiveFilter = item.currentActiveFilter;
          scope.currentTypeFilter = item.currentTypeFilter;
          scope.$digest();
          expect(scope.isNotEmptyFilter).toEqual(item.isNotEmptyFilter);
        });
      });

    });

  });

});
