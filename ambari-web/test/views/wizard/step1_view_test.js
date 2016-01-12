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

  App.TestAliases.testAsComputedAnd(getView(), 'showErrorsWarningCount', ['isSubmitDisabled', 'totalErrorCnt']);

  describe('#operatingSystems', function () {
    beforeEach(function () {
      sinon.stub(App.Stack, 'find', function () {
        return [
          Ember.Object.create({
            id: 'HDP-1.3',
            stackName: 'HDP',
            stackVersion: '1.3',
            active: true,
            operatingSystems: [
              Ember.Object.create({
                id: 'HDP-1.3-redhat5',
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
                id: 'HDP-1.3-redhat6',
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

    it('should create empty array if there is no stacks', function () {
      controller = App.WizardStep1Controller.create({
        content: {
          stacks: []
        },
        selectedStack: []
      });
      view = App.WizardStep1View.create();
      view.reopen({
        controller: controller
      });
      expect(view.get('allRepositories.length')).to.equal(0);
      expect(view.get('operatingSystems.length')).to.equal(0);
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

  App.TestAliases.testAsComputedOr(getView(), 'isSubmitDisabled', ['invalidFormatUrlExist', 'isNoOsChecked', 'invalidUrlExist', 'controller.content.isCheckInProgress']);

  describe('#stacks', function () {

    var tests = Em.A([
      {
        m: 'Stack with 2 HDP',
        stacks: [
          Em.Object.create({isSelected: true, id: 'HDP-2.0.1'}),
          Em.Object.create({isSelected: false, id: 'HDP-1.3.3'})
        ],
        e: {
          names: ['HDP 2.0.1', 'HDP 1.3.3'],
          selected: [true, false]
        }
      },
      {
        m: 'No HDP',
        stacks: [],
        e: {
          names: [],
          selected: []
        }
      }
    ]);

    tests.forEach(function (test) {
      it(test.m, function () {
        view.set('controller.content.stacks', test.stacks);
        var stacks = view.get('stacks');
        expect(stacks.mapProperty('name')).to.eql(test.e.names);
        expect(stacks.mapProperty('isSelected')).to.eql(test.e.selected);
      });
    });

  });

  App.TestAliases.testAsComputedSomeBy(getView(), 'invalidUrlExist', 'allRepositories', 'validation', App.Repository.validation.INVALID);

  App.TestAliases.testAsComputedSomeBy(getView(), 'invalidFormatUrlExist', 'allRepositories', 'invalidFormatError', true);

  describe('#totalErrorCnt', function () {
    var tests = Em.A([
      {
        allRepositories: [
          {}
        ],
        m: 'isNoOsChecked',
        isNoOsChecked: true,
        e: 1
      },
      {
        allRepositories: [
          {'invalidFormatError': true},
          {'invalidFormatError': true}
        ],
        isNoOsChecked: false,
        m: 'two with empty-error',
        e: 2
      },
      {
        allRepositories: [
          {'validation': 'icon-exclamation-sign'},
          {'validation': 'icon-exclamation-sign'}
        ],
        isNoOsChecked: false,
        m: 'two with validation="icon-exclamation-sign"',
        e: 2
      },
      {
        allRepositories: [
          {'invalidFormatError': true, 'validation': 'icon-exclamation-sign'},
          {'invalidFormatError': true, 'validation': 'icon-exclamation-sign'}
        ],
        isNoOsChecked: false,
        m: 'two with empty-error, two with validation="icon-exclamation-sign"',
        e: 4
      },
      {
        allRepositories: [
          {}
        ],
        isNoOsChecked: false,
        m: 'no errors/warnings etc',
        e: 0
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        view = App.WizardStep1View.create();
        view.reopen({
          isNoOsChecked: test.isNoOsChecked,
          allRepositories: test.allRepositories
        });
        expect(view.get('totalErrorCnt')).to.equal(test.e);
      });
    });
  });

  describe('#didInsertElement', function () {

    beforeEach(function () {
      sinon.stub($.fn, 'tooltip', Em.K);
    });

    afterEach(function () {
      $.fn.tooltip.restore();
    });

    it('should create tooltip', function () {
      view.set('isRLCollapsed', false);
      view.didInsertElement();
      expect($.fn.tooltip.calledOnce).to.equal(true);
    });
  });

  describe('#stackRadioButton', function () {

    var v;
    beforeEach(function () {
      v = view.get('stackRadioButton').create({
        content: Em.Object.create({
          name: ''
        }),
        controller: Em.Object.create({
          content: Em.Object.create({
            stacks: []
          })
        })
      });
    });

    describe('#isSelected', function () {
      it('should be equal content.isSelected', function () {
        v.set('content.isSelected', true);
        expect(v.get('checked')).to.equal(true);
        v.set('content.isSelected', false);
        expect(v.get('checked')).to.equal(false);
      });
    });

    describe('#click', function () {
      it('should select proper stack', function () {
        v.set('controller.content.stacks', Em.A([Em.Object.create({id: 'n-1'}), Em.Object.create({id: 'n-2'}), Em.Object.create({id: 'n-3'})]));
        v.set('content.name', 'n 2');
        v.click();
        expect(v.get('controller.content.stacks').getEach('isSelected')).to.eql([false, true, false]);
      });
    });

  });

  describe('#popoverView', function () {

    var v;
    beforeEach(function () {
      v = view.get('popoverView').create();
      sinon.stub(App, 'popover', Em.K);
      view = App.WizardStep1View.create({'controller': controller});
      view.set('$', function () {
        return Em.Object.create({hide: Em.K, toggle: Em.K});
      });
    });

    afterEach(function () {
      App.popover.restore();
    });

    describe('#didInsertElement', function () {
      it('should create popover', function () {
        v.didInsertElement();
        expect(App.popover.calledOnce).to.equal(true);
      });
    });

  });

  describe('#onToggleBlock', function () {

    it('should toggle isRLCollapsed', function () {
      view.set('isRLCollapsed', true);
      view.onToggleBlock();
      expect(view.get('isRLCollapsed')).to.equal(false);
      view.onToggleBlock();
      expect(view.get('isRLCollapsed')).to.equal(true);
    });
  });

  describe('#updateByCheckbox', function () {

    var operatingSystems = [
      Em.Object.create({
        name: 'redhat5',
        isSelected: false,
        repositories: [Em.Object.create({
          id: 'id',
          osType: 'redhat5',
          baseUrl: 'baseUrl',
          latestBaseUrl: 'latestBaseUrl',
          validation: '',
          isSelected: false
        })
        ]
      })
    ];

    var ctrl = {
      content: {
        stacks: [
          Em.Object.create({
            isSelected: true,
            operatingSystems: [
              Em.Object.create({
                id: 'id',
                osType: 'redhat5',
                baseUrl: 'baseUrl',
                latestBaseUrl: 'latestBaseUrl',
                validation: '',
                isSelected: false
              })
            ]
          })
        ]
      },
      selectedStack: Em.Object.create({
        isSelected: true,
        operatingSystems: [
          Em.Object.create({
            id: 'id',
            osType: 'redhat5',
            baseUrl: 'baseUrl',
            latestBaseUrl: 'latestBaseUrl',
            validation: '',
            isSelected: true
          })
        ]
      }),
      skipValidationChecked: true
    };

    it('target group isn\'t isSelected', function () {
      view.reopen({
        operatingSystems: operatingSystems,
        controller: ctrl
      });
      view.updateByCheckbox();
      var targetGroup = view.get('operatingSystems.firstObject.repositories.firstObject');
      expect(targetGroup.get('baseUrl')).to.equal('latestBaseUrl');
      expect(targetGroup.get('latestBaseUrl')).to.equal('latestBaseUrl');
      expect(targetGroup.get('validation')).to.be.empty;

    });

    it('target group is isSelected, skipValidationisSelected = true', function () {
      ctrl.content.stacks[0].operatingSystems[0].selected = true;
      operatingSystems[0].set('isSelected', true);
      view.reopen({
        operatingSystems: operatingSystems,
        controller: ctrl
      });
      view.updateByCheckbox();
      var targetGroup = view.get('operatingSystems.firstObject.repositories.firstObject');
      expect(targetGroup.get('validation')).to.be.empty;
    });
  });

  describe('#clearGroupLocalRepository', function () {
    var context = {'group-number': 0, id: 'HDP-redhat5', repoId: 'HDP-redhat5', baseUrl: 'baseUrl', validation: 'validation'};
    it('should empty base url and validation', function () {
      var event = {context: Em.Object.create(context, {isSelected: true})};
      view.clearGroupLocalRepository(event);
      expect(event.context.get('baseUrl')).to.be.empty;
      expect(event.context.get('validation')).to.be.empty;
    });
    it('should do nothing if corresponding OS is not selected', function () {
      var event = {context: Em.Object.create(context, {isSelected: false})};
      view.clearGroupLocalRepository(event);
      expect(event.context.get('baseUrl')).to.equal('baseUrl');
      expect(event.context.get('validation')).to.equal('validation');
    });
  });

  describe('#undoGroupLocalRepository', function () {
    it('should reset base url and validation', function () {
      var event = {context: Em.Object.create({'group-number': 0, id: 'HDP-redhat5', repoId: 'HDP-redhat5', latestBaseUrl: 'latestBaseUrl', validation: 'validation'})};
      view.undoGroupLocalRepository(event);
      expect(event.context.get('baseUrl')).to.equal(event.context.get('latestBaseUrl'));
      expect(event.context.get('validation')).to.be.empty;
    });
  });

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