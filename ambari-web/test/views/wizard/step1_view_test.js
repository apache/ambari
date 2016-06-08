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
require('views/wizard/step1_view');

var view;

function getView() {
  return App.WizardStep1View.create();
}

describe('App.WizardStep1View', function () {

  beforeEach(function () {
    view = getView();
  });

  App.TestAliases.testAsComputedEveryBy(getView(), 'isNoOsChecked', 'controller.selectedStack.operatingSystems', 'isSelected', false);

  App.TestAliases.testAsComputedOr(getView(), 'isSubmitDisabled', ['invalidFormatUrlExist', 'isNoOsChecked', 'isNoOsFilled', 'controller.content.isCheckInProgress', 'App.router.btnClickInProgress']);

  App.TestAliases.testAsComputedSomeBy(getView(), 'invalidUrlExist', 'allRepositories', 'validation', App.Repository.validation.INVALID);

  describe('#editLocalRepository', function () {

    it('should update repository', function () {
      view.reopen({
        allRepositories: [
          Em.Object.create({
            isSelected: true,
            baseUrl: 'b1',
            validation: 'icon-exclamation-sign'
          })
        ]
      });
      view.editLocalRepository();
      var repository = view.get('allRepositories.firstObject');
      expect(repository.get('lastBaseUrl')).to.equal(repository.get('baseUrl'));
      expect(repository.get('validation')).to.be.empty;
    });
  });
});