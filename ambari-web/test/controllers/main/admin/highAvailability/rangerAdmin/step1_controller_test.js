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


var App = require('app');
require('controllers/main/admin/highAvailability/rangerAdmin/step1_controller');

describe('App.RAHighAvailabilityWizardStep1Controller', function () {

  var controller;

  beforeEach(function () {
    controller = App.RAHighAvailabilityWizardStep1Controller.create({content: Em.Object.create()});
  });

  describe('#isloadBalancerURLValid', function () {
    it('should check if balancer url is valid', function () {
      controller.set('content.loadBalancerURL', 'https://domain-name_0.com/');
      expect(controller.get('isloadBalancerURLValid')).to.be.true;
    });

    it('should check if balancer url is not valid', function () {
      controller.set('content.loadBalancerURL', 'https//domain-name_0.com/');
      expect(controller.get('isloadBalancerURLValid')).to.be.false;
    });
  });

});