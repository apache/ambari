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

describe('#Auth', function () {

  describe('signout', function () {
    var scope, ctrl, $httpBackend, $window, clusterService,deferred;
    beforeEach(module('ambariAdminConsole', function($provide){
      $provide.value('$window', {location: {pathname: 'http://c6401.ambari.apache.org:8080/views/ADMIN_VIEW/1.0.0/INSTANCE/#/'}});
      localStorage.ambari = JSON.stringify({app: {authenticated: true, loginName: 'admin', user: 'user'}});
    }));
    afterEach(function() {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });
    beforeEach(inject(function (_$httpBackend_, $rootScope, $controller, _$window_, _Cluster_,_$q_) {
      clusterService =  _Cluster_;
      deferred = _$q_.defer();
      spyOn(clusterService, 'getStatus').andReturn(deferred.promise);
      deferred.resolve('c1');
      $window = _$window_;
      $httpBackend = _$httpBackend_;
      $httpBackend.whenGET('/api/v1/logout').respond(200,{message: "successfully logged out"});
      scope = $rootScope.$new();
      scope.$apply();
      ctrl = $controller('MainCtrl', {$scope: scope});
    }));

    it('should reset window.location and ambari localstorage', function () {
      scope.signOut();
      $httpBackend.flush();
      chai.expect($window.location.pathname).to.be.empty;
      var data = JSON.parse(localStorage.ambari);
      chai.expect(data.app.authenticated).to.equal(undefined);
      chai.expect(data.app.loginName).to.equal(undefined);
      chai.expect(data.app.user).to.equal(undefined);
    });
  });
});