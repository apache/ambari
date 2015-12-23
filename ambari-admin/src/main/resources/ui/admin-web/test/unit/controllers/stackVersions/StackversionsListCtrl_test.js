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
  describe('StackVersionsListCtrl', function() {
    var scope, ctrl;

    beforeEach(module('ambariAdminConsole', function($provide) {

    }));

    beforeEach(inject(function($rootScope, $controller) {
      scope = $rootScope.$new();
      ctrl = $controller('StackVersionsListCtrl', {$scope: scope});
    }));

    describe('fetchRepos()', function () {

      it('saves list of stacks', function() {
        scope.fetchRepos().then(function() {
          expect(Array.isArray(scope.repos)).toBe(true);
        });
      });

    });

    describe('fillClusters()', function () {

      var clusters = [
          {
            Clusters: {
              cluster_name: 'c0'
            }
          }
        ],
        cases = [
          {
            prev: null,
            current: {
              label: 'All',
              value: ''
            },
            title: 'no cluster selected before'
          },
          {
            prev: {
              label: 'c0',
              value: 'c0'
            },
            current: {
              label: 'c0',
              value: 'c0'
            },
            title: 'cluster was selected before'
          }
        ];

      angular.forEach(cases, function (item) {
        it(item.title, function() {
          scope.filter.cluster.current = item.prev;
          scope.fillClusters(clusters);
          expect(scope.dropDownClusters).toEqual(clusters);
          expect(scope.filter.cluster.current).toEqual(item.current);
        });
      });

    });

  });
});
