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

describe('#CreateViewInstanceCtrl', function () {

  describe('loadMeta', function () {

    var scope, ctrl, $window, $q, deferred;

    beforeEach(module('ambariAdminConsole', function ($provide) {
      $provide.value('$window', {
        location: {
          pathname: 'http://c6401.ambari.apache.org:8080/views/ADMIN_VIEW/1.0.0/INSTANCE/#/'
        }
      });
      $provide.value('Auth', {
        getCurrentUser: function () {
          return 'admin';
        }
      });
      $provide.value('View', {
        getMeta: function () {
          return deferred.promise;
        },
        getVersions: function () {
          var dfd = $q.defer();
          return dfd.promise;
        }
      });
      $provide.value('$routeParams', {
        viewId: 'ADMIN_VIEW'
      });
    }));

    beforeEach(inject(function ($rootScope, $controller, _$window_, _$q_) {
      $q = _$q_;
      $window = _$window_;
      scope = $rootScope.$new();
      deferred = $q.defer();
      ctrl = $controller('CreateViewInstanceCtrl', {
        $scope: scope
      });
    }));

    it('should parse {username}', function () {
      deferred.resolve({
        data: {
          ViewVersionInfo: {
            parameters: [{
              description: '{username}'
            }]
          }
        }
      });
      scope.version = '1.0.0';
      scope.$digest();
      chai.expect(scope.view.ViewVersionInfo.parameters[0].description).to.equal('admin');
    });

  });

});
