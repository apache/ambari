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
  var scope, ctrl, $httpBackend, View;

  beforeEach(module('ambariAdminConsole', function($provide){
    $provide.value('$routeParams', {viewId: 'TestView'});
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  beforeEach(inject(function (_$httpBackend_, $rootScope, $controller, _View_, $q) {
    View = _View_;
    spyOn(View, 'createInstance').andReturn($q.defer().promise);

    $httpBackend = _$httpBackend_;
    $httpBackend.whenGET(/\/api\/v1\/views\/TestView\?.+/).respond(200, {
      "versions": [{"ViewVersionInfo": {}}]
    });
    $httpBackend.whenGET(/\/api\/v1\/views\/TestView\/versions\/1\.0\.0/).respond(200, {
      "ViewVersionInfo": {"parameters": [{"name": "n", "defaultValue": "d"}]}
    });
    $httpBackend.whenGET('template/modal/backdrop.html').respond(200, '<div></div>');
    $httpBackend.whenGET('template/modal/window.html').respond(200, '<div></div>');
    scope = $rootScope.$new();
    ctrl = $controller('CreateViewInstanceCtrl', {$scope: scope});
  }));

  it('it should invoke View.createInstance on save', function () {
    scope.form = {
      instanceCreateForm: {
        submitted: false,
        $valid: true
      }
    };
    $httpBackend.flush();
    scope.instance = {};
    scope.save();
    expect(View.createInstance).toHaveBeenCalled();
  });

  it('should set default property value before creating view instance', function () {
    scope.form = {
      instanceCreateForm: {
        $dirty: true
      }
    };
    scope.instance = {};
    scope.version = '1.0.0';
    $httpBackend.expectGET('template/modal/backdrop.html');
    $httpBackend.expectGET('template/modal/window.html');
    $httpBackend.whenGET(/\/api\/v1\/clusters\?_=\d+/).respond(200, {
      "items" : [
        {
          "Clusters" : {
            "cluster_name" : "c1",
            "version" : "HDP-2.2"
          }
        }
      ]
    });
    scope.$digest();
    $httpBackend.flush();
    chai.expect(scope.view.ViewVersionInfo.parameters[0].value).to.equal('d');
  });
});
