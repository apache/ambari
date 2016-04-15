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
var controller;

function getView() {
  return App.WizardStep1View.create();
}

describe('App.WizardStep1View', function () {

  describe('#operatingSystems', function () {
    beforeEach(function () {
      sinon.stub(App.Stack, 'find', function () {
        return [
          Ember.Object.create({
            id: 'HDP-1.3-1234',
            stackName: 'HDP',
            stackVersion: '1.3',
            active: true,
            operatingSystems: [
              Ember.Object.create({
                id: 'HDP-1.3-1234-redhat5',
                osType: 'redhat5',
                isSelected: false,
                repositories: [
                  Ember.Object.create({
                    id: 'redhat5-HDP-1.3',
                    isSelected: false
                  }),
                  Ember.Object.create({
                    id: 'redhat5-HDP-UTILS-1.1.0.19',
                    isSelected: false
                  })
                ]
              }),
              Ember.Object.create({
                id: 'HDP-1.3-1234-redhat6',
                osType: 'redhat6',
                isSelected: false,
                repositories: [
                  Ember.Object.create({
                    id: 'redhat6-HDP-1.3',
                    isSelected: false
                  }),
                  Ember.Object.create({
                    id: 'redhat6-HDP-UTILS-1.1.0.19',
                    isSelected: false
                  })
                ]
              })
            ],
            isSelected: false
          }),
          Ember.Object.create({
            id: 'HDP-2.1',
            stackName: 'HDP',
            stackVersion: '2.1',
            active: true,
            operatingSystems: [
              Ember.Object.create({
                id: 'HDP-2.1-redhat5',
                osType: 'redhat5',
                isSelected: true,
                repositories: [
                  Ember.Object.create({
                    id: 'redhat5-HDP-2.1',
                    isSelected: true,
                    baseUrl: "http://public-repo-1.hortonworks.com/HDP/centos5/2.x/updates/2.1.5.0",
                    latestBaseUrl: "http://public-repo-1.hortonworks.com/HDP/centos5/2.x/updates/2.1.5.0"
                  }),
                  Ember.Object.create({
                    id: 'redhat5-HDP-UTILS-1.1.0.19',
                    isSelected: true,
                    baseUrl: "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.19/repos/centos5",
                    latestBaseUrl: "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.19/repos/centos5"
                  })
                ]
              }),
              Ember.Object.create({
                id: 'HDP-2.1-redhat6',
                osType: 'redhat6',
                isSelected: true,
                repositories: [
                  Ember.Object.create({
                    id: 'redhat6-HDP-2.1',
                    isSelected: true,
                    baseUrl: "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.1.5.0",
                    latestBaseUrl: "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.1.5.0"
                  }),
                  Ember.Object.create({
                    id: 'redhat6-HDP-UTILS-1.1.0.19',
                    isSelected: true,
                    baseUrl: "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.19/repos/centos6",
                    latestBaseUrl: "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.19/repos/centos6"
                  })
                ]
              })
            ],
            repositories: [
              Ember.Object.create({
                id: 'redhat5-HDP-2.1',
                isSelected: true,
                baseUrl: "http://public-repo-1.hortonworks.com/HDP/centos5/2.x/updates/2.1.5.0",
                latestBaseUrl: "http://public-repo-1.hortonworks.com/HDP/centos5/2.x/updates/2.1.5.0"
              }),
              Ember.Object.create({
                id: 'redhat5-HDP-UTILS-1.1.0.19',
                isSelected: true,
                baseUrl: "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.19/repos/centos5",
                latestBaseUrl: "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.19/repos/centos5"
              }),
              Ember.Object.create({
                id: 'redhat6-HDP-2.1',
                isSelected: true,
                baseUrl: "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.1.5.0",
                latestBaseUrl: "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.1.5.0"
              }),
              Ember.Object.create({
                id: 'redhat6-HDP-UTILS-1.1.0.19',
                isSelected: true,
                baseUrl: "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.19/repos/centos6",
                latestBaseUrl: "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.19/repos/centos6"
              })
            ],
            isSelected: true
          })
        ];
      });
    });

    afterEach(function () {
      App.Stack.find.restore();
    });

    describe('should create repo groups from repo list', function () {

      var repositories;
      beforeEach(function () {
        controller = App.WizardStep1Controller.create({
          content: {
            stacks: App.Stack.find()
          }
        });

        view = App.WizardStep1View.create({'controller': controller});
        view.set('$', function () {
          return Em.Object.create({hide: Em.K, toggle: Em.K});
        });
        repositories = view.get('allRepositories');
      });

      it('operatingSystems.length', function () {
        expect(view.get('operatingSystems.length')).to.equal(2);
      });

      it('operatingSystems.0.osType', function () {
        expect(view.get('operatingSystems')[0].get('osType')).to.equal('redhat5');
      });

      it('operatingSystems.1.osType', function () {
        expect(view.get('operatingSystems')[1].get('osType')).to.equal('redhat6');
      });

      it('operatingSystems.0.isSelected', function () {
        expect(view.get('operatingSystems')[0].get('isSelected')).to.be.true;
      });

      it('operatingSystems.1.isSelected', function () {
        expect(view.get('operatingSystems')[1].get('isSelected')).to.be.true;
      });

      it('operatingSystems.0.repositories', function () {
        expect(view.get('operatingSystems')[0].get('repositories')).to.eql([repositories[0], repositories[1]]);
      });

      it('operatingSystems.1.repositories', function () {
        expect(view.get('operatingSystems')[1].get('repositories')).to.eql([repositories[2], repositories[3]]);
      });

    });

  });

  describe('#invalidFormatUrlExist', function () {

    controller = App.WizardStep1Controller.create({
      content: {
        stacks: App.Stack.find()
      }
    });
    view = App.WizardStep1View.create();
    view.reopen({
      controller: controller
    });
    view.set('$', function () {
      return Em.Object.create({hide: Em.K, toggle: Em.K});
    });

    it(view.get('allRepositories').mapProperty('invalidFormatError').join(', '), function () {
      expect(view.get('invalidFormatUrlExist')).to.equal(false);
    });
  });

  App.TestAliases.testAsComputedEveryBy(getView(), 'isNoOsChecked', 'operatingSystems', 'isSelected', false);

  App.TestAliases.testAsComputedOr(getView(), 'isSubmitDisabled', ['controller.content.isCheckInProgress']);

  App.TestAliases.testAsComputedSomeBy(getView(), 'invalidUrlExist', 'allRepositories', 'validation', App.Repository.validation.INVALID);

  App.TestAliases.testAsComputedSomeBy(getView(), 'invalidFormatUrlExist', 'allRepositories', 'invalidFormatError', true);


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