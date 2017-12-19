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
  describe('ViewsListCtrl', function() {
    var scope, ctrl;

    beforeEach(function () {
      module('ambariAdminConsole');
      inject(function($rootScope, $controller) {
        scope = $rootScope.$new();
        ctrl = $controller('ViewsListCtrl', {$scope: scope});
      });
      scope.instances = [
        {
          short_url_name: 'sun1',
          url: 'url1',
          view_name: 'vn1',
          instance_name: 'in1',
          short_url: 'su1'
        },
        {
          short_url_name: 'sun2',
          url: 'url2',
          view_name: 'vn2',
          instance_name: 'in2',
          short_url: 'su2'
        }
      ];
    });

    describe('#initFilterOptions()', function () {
      beforeEach(function() {
        scope.initFilterOptions();
      });

      it('should fill short_url_name options', function() {
        expect(scope.filters[0].options).toEqual([
          {
            key: 'sun1',
            label: 'sun1'
          },
          {
            key: 'sun2',
            label: 'sun2'
          }
        ]);
      });

      it('should fill url options', function() {
        expect(scope.filters[1].options).toEqual([
          {
            key: '/main/view/vn1/su1',
            label: '/main/view/vn1/su1'
          },
          {
            key: '/main/view/vn2/su2',
            label: '/main/view/vn2/su2'
          }
        ]);
      });

      it('should fill view_name options', function() {
        expect(scope.filters[2].options).toEqual([
          {
            key: 'vn1',
            label: 'vn1'
          },
          {
            key: 'vn2',
            label: 'vn2'
          }
        ]);
      });

      it('should fill instance_name options', function() {
        expect(scope.filters[3].options).toEqual([
          {
            key: 'in1',
            label: 'in1'
          },
          {
            key: 'in2',
            label: 'in2'
          }
        ]);
      });
    });


    describe('#filterInstances', function() {
      beforeEach(function() {
        spyOn(scope, 'resetPagination');
      });

      it('all should be filtered when filters not applied', function() {
        scope.filterInstances();
        expect(scope.tableInfo.filtered).toEqual(2);
        scope.filterInstances([]);
        expect(scope.tableInfo.filtered).toEqual(2);
      });

      it('resetPagination should be called', function() {
        scope.filterInstances();
        expect(scope.resetPagination).toHaveBeenCalled();
      });

      it('one view should be filtered', function() {
        var appliedFilters = [
          {
            key: 'view_name',
            values: ['vn1']
          }
        ];
        scope.filterInstances(appliedFilters);
        expect(scope.tableInfo.filtered).toEqual(1);
        expect(scope.instances[0].isFiltered).toBeTruthy();
        expect(scope.instances[1].isFiltered).toBeFalsy();
      });

      it('two views should be filtered', function() {
        var appliedFilters = [
          {
            key: 'view_name',
            values: ['vn1', 'vn2']
          }
        ];
        scope.filterInstances(appliedFilters);
        expect(scope.tableInfo.filtered).toEqual(2);
        expect(scope.instances[0].isFiltered).toBeTruthy();
        expect(scope.instances[1].isFiltered).toBeTruthy();
      });

      it('one views should be filtered with combo filter', function() {
        var appliedFilters = [
          {
            key: 'view_name',
            values: ['vn1', 'vn2']
          },
          {
            key: 'instance_name',
            values: ['in2']
          }
        ];
        scope.filterInstances(appliedFilters);
        expect(scope.tableInfo.filtered).toEqual(1);
        expect(scope.instances[0].isFiltered).toBeFalsy();
        expect(scope.instances[1].isFiltered).toBeTruthy();
      });
    });
  });
});
