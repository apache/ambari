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
    var scope, ctrl, $httpBackend;

    beforeEach(module('ambariAdminConsole', function($provide) {

    }));

    beforeEach(inject(function($rootScope, $controller, _$httpBackend_) {
      scope = $rootScope.$new();
      ctrl = $controller('StackVersionsListCtrl', {$scope: scope});
      $httpBackend = _$httpBackend_;
    }));

    describe('#fetchRepos()', function () {

      it('saves list of stacks', function() {
        scope.fetchRepos().then(function() {
          expect(Array.isArray(scope.repos)).toBe(true);
        });
      });

    });

    describe('#fillClusters()', function () {

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

    describe('#isNotEmptyFilter', function () {

      var cases = [
        {
          filter: {
            version: '',
            cluster: {
              current: null
            }
          },
          isNotEmptyFilter: false,
          title: 'no filters'
        },
        {
          filter: {
            version: '',
            cluster: {
              current: {
                value: ''
              }
            }
          },
          isNotEmptyFilter: false,
          title: 'empty filters'
        },
        {
          filter: {
            version: 'a',
            cluster: {
              current: {
                value: ''
              }
            }
          },
          isNotEmptyFilter: true,
          title: 'version filter'
        },
        {
          filter: {
            version: '0',
            cluster: {
              current: {
                value: ''
              }
            }
          },
          isNotEmptyFilter: true,
          title: 'version filter with "0" as string'
        },
        {
          filter: {
            version: '',
            cluster: {
              current: {
                value: 'a'
              }
            }
          },
          isNotEmptyFilter: true,
          title: 'cluster filter'
        },
        {
          filter: {
            version: '',
            cluster: {
              current: {
                value: '0'
              }
            }
          },
          isNotEmptyFilter: true,
          title: 'cluster filter with "0" as string'
        },
        {
          filter: {
            version: 'a',
            cluster: {
              current: {
                value: 'a'
              }
            }
          },
          isNotEmptyFilter: true,
          title: 'both filters'
        },
        {
          filter: {
            version: '0',
            cluster: {
              current: {
                value: '0'
              }
            }
          },
          isNotEmptyFilter: true,
          title: 'both filters with "0" as string'
        }
      ];

      cases.forEach(function (item) {
        it(item.title, function () {
          $httpBackend.expectGET(/\/api\/v1\/clusters\?_=\d+/).respond(200);
          scope.filter = item.filter;
          scope.$digest();
          expect(scope.isNotEmptyFilter).toEqual(item.isNotEmptyFilter);
        });
      });

    });

  });
});
