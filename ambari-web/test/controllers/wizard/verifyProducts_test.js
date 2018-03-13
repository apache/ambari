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
var controller = App.WizardVerifyProductsController.create();

describe('App.WizardVerifyProductsController', function () {
  describe('#setRepoState', function () {
    it('Sets repo state correctly', function () {
      var actualInProgress = Em.Object.create({});
      var actualSucceeded = Em.Object.create({});
      var actualFailed = Em.Object.create({});

      var expectedInProgress = Em.Object.create({
        succeeded: false,
        failed: false,
        inProgress: true
      });
      var expectedSucceeded = Em.Object.create({
        succeeded: true,
        failed: false,
        inProgress: false
      });
      var expectedFailed = Em.Object.create({
        succeeded: false,
        failed: true,
        inProgress: false
      });

      controller.setRepoState(actualInProgress, controller.get('VERIFYREPO_INPROGRESS'));
      controller.setRepoState(actualSucceeded, controller.get('VERIFYREPO_SUCCEEDED'));
      controller.setRepoState(actualFailed, controller.get('VERIFYREPO_FAILED'));
      
      expect(actualInProgress).to.deep.equal(expectedInProgress);
      expect(actualSucceeded).to.deep.equal(expectedSucceeded);
      expect(actualFailed).to.deep.equal(expectedFailed);
    });
  });
});