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
  return App.WizardStep1View.create({
    controller: Em.Object.create()
  });
}

describe('App.WizardStep1View', function () {

  beforeEach(function () {
    view = getView();
  });

  App.TestAliases.testAsComputedEveryBy(getView(), 'isNoOsChecked', 'controller.selectedStack.operatingSystems', 'isSelected', false);

  App.TestAliases.testAsComputedOr(getView(), 'isSubmitDisabled', ['invalidFormatUrlExist', 'isNoOsChecked', 'isAnyOsEmpty', 'controller.content.isCheckInProgress', 'App.router.btnClickInProgress', '!controller.isLoadingComplete']);

  App.TestAliases.testAsComputedSomeBy(getView(), 'invalidUrlExist', 'allRepositories', 'validation', 'INVALID');

  describe('#editLocalRepository', function () {

    var repository;

    beforeEach(function () {
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
      repository = view.get('allRepositories.firstObject');
    });

    it('should update repository base URL', function () {
      expect(repository.get('lastBaseUrl')).to.equal(repository.get('baseUrl'));
    });

    it('should update repository validation status', function () {
      expect(repository.get('validation')).to.equal('PENDING');
    });
  });

  describe('#isAnyOsEmpty', function() {

    it('should be true when useRedhatSatellite is true and redhat os is empty', function() {
      view.set('controller.selectedStack', Em.Object.create({
        useRedhatSatellite: true,
        operatingSystems: [
          Em.Object.create({
            isSelected: true,
            isNotFilled: true,
            osType: 'redhat'
          })
        ]
      }));
      expect(view.get('isAnyOsEmpty')).to.be.true;
    });

    it('should be false when operatingSystems is null', function() {
      view.set('controller.selectedStack', Em.Object.create({
        operatingSystems: null
      }));
      expect(view.get('isAnyOsEmpty')).to.be.false;
    });

    it('should be false when operatingSystem is filled', function() {
      view.set('controller.selectedStack', Em.Object.create({
        useRedhatSatellite: false,
        operatingSystems: [
          Em.Object.create({
            isSelected: true,
            isNotFilled: false
          })
        ]
      }));
      expect(view.get('isAnyOsEmpty')).to.be.false;
    });

    it('should be true when operatingSystem is not filled', function() {
      view.set('controller.selectedStack', Em.Object.create({
        useRedhatSatellite: false,
        operatingSystems: [
          Em.Object.create({
            isSelected: true,
            isNotFilled: true
          }),
          Em.Object.create({
            isSelected: true,
            isNotFilled: false
          })
        ]
      }));
      expect(view.get('isAnyOsEmpty')).to.be.true;
    });
  });

  describe('#isRedhat', function() {

    it('should be false when osType not specified', function() {
      expect(view.isRedhat(Em.Object.create())).to.be.false;
    });

    it('should be false when osType not redhat', function() {
      expect(view.isRedhat(Em.Object.create({osType: 'debian7'}))).to.be.false;
    });

    it('should be true when osType is redhat7', function() {
      expect(view.isRedhat(Em.Object.create({osType: 'redhat7'}))).to.be.true;
    });

    it('should be true when osType is redhat-ppc7', function() {
      expect(view.isRedhat(Em.Object.create({osType: 'redhat-ppc7'}))).to.be.true;
    });
  });
});