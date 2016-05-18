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

  describe('GroupsListCtrl', function() {

    var scope, ctrl, $t, $httpBackend;

    beforeEach(module('ambariAdminConsole', function () {}));

    beforeEach(inject(function($rootScope, $controller, _$translate_, _$httpBackend_) {
      scope = $rootScope.$new();
      $t = _$translate_.instant;
      $httpBackend = _$httpBackend_;
      ctrl = $controller('GroupsListCtrl', {
        $scope: scope
      });
    }));

    describe('#clearFilters()', function () {

      it('should clear filters and reset pagination', function () {
        scope.currentPage = 2;
        scope.currentNameFilter = 'a';
        scope.currentTypeFilter = {
          label: $t('common.local'),
          value: false
        };
        scope.clearFilters();
        expect(scope.currentNameFilter).toEqual('');
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
          isNotEmptyFilter: false,
          title: 'no filters'
        },
        {
          currentNameFilter: '',
          currentTypeFilter: {
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
          isNotEmptyFilter: true,
          title: 'name filter'
        },
        {
          currentNameFilter: '0',
          currentTypeFilter: {
            value: '*'
          },
          isNotEmptyFilter: true,
          title: 'name filter with "0" as string'
        },
        {
          currentNameFilter: '',
          currentTypeFilter: {
            value: false
          },
          isNotEmptyFilter: true,
          title: 'type filter'
        },
        {
          currentNameFilter: 'a',
          currentTypeFilter: {
            value: false
          },
          isNotEmptyFilter: true,
          title: 'both filters'
        },
        {
          currentNameFilter: '0',
          currentTypeFilter: {
            value: false
          },
          isNotEmptyFilter: true,
          title: 'both filters with "0" as string'
        }
      ];

      cases.forEach(function (item) {
        it(item.title, function () {
          $httpBackend.expectGET(/\/api\/v1\/groups/).respond(200);
          scope.currentNameFilter = item.currentNameFilter;
          scope.currentTypeFilter = item.currentTypeFilter;
          scope.$digest();
          expect(scope.isNotEmptyFilter).toEqual(item.isNotEmptyFilter);
        });
      });

    });

  });

});
