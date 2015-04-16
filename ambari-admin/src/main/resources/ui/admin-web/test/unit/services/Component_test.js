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

describe('#Component', function () {
  var componentService,
    httpBackend,
    Settings;

  beforeEach(function() {
    module('ambariAdminConsole');

    inject(function(_Component_, $httpBackend, _Settings_) {
      componentService = _Component_;
      httpBackend = $httpBackend;
      Settings = _Settings_;
    });
  });

  afterEach(function() {
    httpBackend.verifyNoOutstandingExpectation();
    httpBackend.verifyNoOutstandingRequest();
  });

  var returnData = {
    RootServiceComponents: {
      'component_name': "AMBARI_SERVER",
      'component_version': "2.0.0",
      'service_name': "AMBARI"
    }
  };
  it('should get the information of the Ambari server', function() {
    httpBackend.whenGET(/\/api\/v1\/services\/AMBARI\/components\/AMBARI_SERVER\?.*/).respond(returnData);

    componentService.getAmbariServer().then(function(response) {
      expect(response).toEqual(returnData);
      expect(response.RootServiceComponents.component_version).toEqual('2.0.0');
    });

    httpBackend.flush();
  });
});
