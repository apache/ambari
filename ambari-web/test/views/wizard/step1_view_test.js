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

var controller = Em.Object.create({
  content: {
    stacks: []
  }
});

describe('App.WizardStep1View', function () {

  beforeEach(function () {
    view = App.WizardStep1View.create({'controller': controller});
    view.set('$', function () {
      return Em.Object.create({hide: Em.K, toggle: Em.K});
    });
  });

  describe('#osTypeToGroup', function () {

    var tests = Em.A([
      {os: 'redhat5', e: 0},
      {os: 'redhat6', e: 1},
      {os: 'suse11', e: 2},
      {os: 'debian12', e: 3},
      {os: 'bulgen', e: -1}
    ]);

    tests.forEach(function (test) {
      it(test.os, function () {
        expect(view.osTypeToGroup(test.os)).to.equal(test.e);
      });
    });

  });

  describe('#groupToOsType', function () {

    var tests = Em.A([
      {type: 0, e: ['redhat5']},
      {type: 1, e: ['redhat6']},
      {type: 2, e: ['suse11']},
      {type: 3, e: ['debian12']},
      {type: -1, e: []}
    ]);

    tests.forEach(function (test) {
      it(test.type, function () {
        expect(view.groupToOsType(test.type)).to.eql(test.e);
      });
    });

  });

  describe('#emptyRepoExist', function () {

    var tests = Em.A([
      {
        allRepositoriesGroup: [
          {'empty-error': false},
          {'empty-error': false},
          {'empty-error': false}
        ],
        e: false
      },
      {
        allRepositoriesGroup: [
          {'empty-error': true},
          {'empty-error': false},
          {'empty-error': false}
        ],
        e: true
      },
      {
        allRepositoriesGroup: [
          {'empty-error': true},
          {'empty-error': true},
          {'empty-error': true}
        ],
        e: true
      }
    ]);

    tests.forEach(function (test) {
      it(test.allRepositoriesGroup.mapProperty('empty-error').join(', '), function () {
        view.set('allRepositoriesGroup', test.allRepositoriesGroup);
        expect(view.get('emptyRepoExist')).to.equal(test.e);
      });
    });

  });

  describe('#allRepoUnchecked', function () {

    var tests = Em.A([
      {
        allRepositoriesGroup: [
          {'checked': false},
          {'checked': false},
          {'checked': false}
        ],
        e: true
      },
      {
        allRepositoriesGroup: [
          {'checked': true},
          {'checked': false},
          {'checked': false}
        ],
        e: false
      },
      {
        allRepositoriesGroup: [
          {'checked': true},
          {'checked': true},
          {'checked': true}
        ],
        e: false
      }
    ]);

    tests.forEach(function (test) {
      it(test.allRepositoriesGroup.mapProperty('checked').join(', '), function () {
        view.set('allRepositoriesGroup', test.allRepositoriesGroup);
        expect(view.get('allRepoUnchecked')).to.equal(test.e);
      });
    });

  });

  describe('#stacks', function () {

    var tests = Em.A([
      {
        m: 'Stack with 2 HDP',
        stacks: [
          Em.Object.create({isSelected: true, name: 'HDP-2.0.1'}),
          Em.Object.create({isSelected: false, name: 'HDP-1.3.3'})
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

  describe('#isSubmitDisabled', function () {

    var tests = Em.A([
      {
        emptyRepoExist: false,
        allRepoUnchecked: false,
        invalidUrlExist: false,
        e: false
      },
      {
        emptyRepoExist: true,
        allRepoUnchecked: false,
        invalidUrlExist: false,
        e: true
      },
      {
        emptyRepoExist: false,
        allRepoUnchecked: true,
        invalidUrlExist: false,
        e: true
      },
      {
        emptyRepoExist: false,
        allRepoUnchecked: false,
        invalidUrlExist: true,
        e: true
      },
      {
        emptyRepoExist: true,
        allRepoUnchecked: false,
        invalidUrlExist: true,
        e: true
      },
      {
        emptyRepoExist: true,
        allRepoUnchecked: true,
        invalidUrlExist: false,
        e: true
      },
      {
        emptyRepoExist: false,
        allRepoUnchecked: true,
        invalidUrlExist: true,
        e: true
      },
      {
        emptyRepoExist: true,
        allRepoUnchecked: true,
        invalidUrlExist: true,
        e: true
      }
    ]);

    tests.forEach(function (test) {
      it(test.emptyRepoExist.toString() + ' ' + test.allRepoUnchecked.toString() + ' ' + test.invalidUrlExist.toString(), function () {
        view = App.WizardStep1View.create();
        view.reopen({
          emptyRepoExist: test.emptyRepoExist,
          allRepoUnchecked: test.allRepoUnchecked,
          invalidUrlExist: test.invalidUrlExist
        });
        expect(view.get('isSubmitDisabled')).to.equal(test.e);
      });
    });

  });

  describe('#invalidUrlExist', function () {
    var tests = Em.A([
      {
        stacks: [Em.Object.create({isSelected: true, invalidCnt: 1})],
        allRepositoriesGroup: [Em.Object.create({validation: 'icon-exclamation-sign'})],
        m: 'invalidCnt: 1, validation: icon-exclamation-sign',
        e: true
      },
      {
        stacks: [Em.Object.create({isSelected: true, invalidCnt: 1})],
        allRepositoriesGroup: [Em.Object.create({validation: ''})],
        m: 'invalidCnt: 1, validation: ""',
        e: false
      },
      {
        stacks: [Em.Object.create({isSelected: true, invalidCnt: 0})],
        allRepositoriesGroup: [Em.Object.create({validation: ''})],
        m: 'invalidCnt: 0, validation: ""',
        e: false
      },
      {
        stacks: [Em.Object.create({isSelected: true, invalidCnt: 0})],
        allRepositoriesGroup: [Em.Object.create({validation: 'icon-exclamation-sign'})],
        m: 'invalidCnt: 0, validation: icon-exclamation-sign',
        e: false
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        view.set('controller.content.stacks', test.stacks);
        view.set('allRepositoriesGroup', test.allRepositoriesGroup);
        expect(view.get('invalidUrlExist')).to.equal(test.e);
      });
    });
  });

  describe('#totalErrorCnt', function () {
    var tests = Em.A([
      {
        allRepositoriesGroup: [
          {checked: false}
        ],
        m: 'allRepoUnchecked',
        e: 1
      },
      {
        allRepositoriesGroup: [
          {checked: true, 'empty-error': true},
          {checked: false, 'empty-error': true}
        ],
        m: 'two with empty-error',
        e: 2
      },
      {
        allRepositoriesGroup: [
          {checked: true, 'validation': 'icon-exclamation-sign'},
          {checked: false, 'validation': 'icon-exclamation-sign'}
        ],
        m: 'two with validation="icon-exclamation-sign"',
        e: 2
      },
      {
        allRepositoriesGroup: [
          {checked: true, 'empty-error': true, 'validation': 'icon-exclamation-sign'},
          {checked: false, 'empty-error': true, 'validation': 'icon-exclamation-sign'}
        ],
        m: 'two with empty-error, two with validation="icon-exclamation-sign"',
        e: 4
      },
      {
        allRepositoriesGroup: [
          {checked: true}
        ],
        m: 'no errors/warnings etc',
        e: 0
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        view.set('allRepositoriesGroup', test.allRepositoriesGroup);
        expect(view.get('totalErrorCnt')).to.equal(test.e);
      });
    });
  });

  describe('#didInsertElement', function () {
    it('should create tooltip', function () {
      sinon.stub($.fn, 'tooltip', Em.K);
      view.set('isRLCollapsed', false);
      view.didInsertElement();
      expect($.fn.tooltip.calledOnce).to.equal(true);
      $.fn.tooltip.restore();
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

    describe('#checked', function () {
      it('should be equal content.isSelected', function () {
        v.set('content.isSelected', true);
        expect(v.get('checked')).to.equal(true);
        v.set('content.isSelected', false);
        expect(v.get('checked')).to.equal(false);
      });
    });

    describe('#click', function () {
      it('should select proper stack', function () {
        v.set('controller.content.stacks', Em.A([Em.Object.create({name: 'n-1'}), Em.Object.create({name: 'n-2'}), Em.Object.create({name: 'n-3'})]));
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

  describe('#setGroupByOs', function () {
    Em.A([
        {
          allGroupsCheckbox: [true, false, true],
          groupNumber: 1,
          m: 'should update group',
          os: {
            baseUrl: 'baseUrl',
            latestBaseUrl: 'latestBaseUrl',
            defaultBaseUrl: 'defaultBaseUrl',
            validation: 'icon-exclamation-sign',
            errorTitle: 'errorTitle',
            errorContent: 'errorContent'
          },
          e: {
            'checked': false,
            'baseUrl': 'baseUrl',
            'latestBaseUrl': 'latestBaseUrl',
            'defaultBaseUrl': 'defaultBaseUrl',
            'empty-error': false,
            'invalid-error': true,
            'validation': 'icon-exclamation-sign',
            'undo': true,
            'clearAll': 'baseUrl',
            'errorTitle': 'errorTitle',
            'errorContent': 'errorContent',
            'group-number': 1
          }
        },
        {
          allGroupsCheckbox: [true, false, true],
          groupNumber: 0,
          m: 'should update group (2)',
          os: {
            baseUrl: '',
            latestBaseUrl: 'latestBaseUrl',
            defaultBaseUrl: 'defaultBaseUrl',
            validation: 'validation',
            errorTitle: 'errorTitle',
            errorContent: 'errorContent'
          },
          e: {
            'checked': true,
            'baseUrl': '',
            'latestBaseUrl': 'latestBaseUrl',
            'defaultBaseUrl': 'defaultBaseUrl',
            'empty-error': true,
            'invalid-error': false,
            'validation': 'validation',
            'undo': true,
            'clearAll': '',
            'errorTitle': 'errorTitle',
            'errorContent': 'errorContent',
            'group-number': 0
          }
        },
        {
          allGroupsCheckbox: [true, false, true],
          groupNumber: 0,
          m: 'should update group (3)',
          os: {
            baseUrl: 'latestBaseUrl',
            latestBaseUrl: 'latestBaseUrl',
            defaultBaseUrl: 'defaultBaseUrl',
            validation: 'validation',
            errorTitle: 'errorTitle',
            errorContent: 'errorContent'
          },
          e: {
            'checked': true,
            'baseUrl': 'latestBaseUrl',
            'latestBaseUrl': 'latestBaseUrl',
            'defaultBaseUrl': 'defaultBaseUrl',
            'empty-error': false,
            'invalid-error': false,
            'validation': 'validation',
            'undo': false,
            'clearAll': 'latestBaseUrl',
            'errorTitle': 'errorTitle',
            'errorContent': 'errorContent',
            'group-number': 0
          }
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          var group = Em.Object.create({});
          view.set('allGroupsCheckbox', test.allGroupsCheckbox);
          view.setGroupByOs(group, test.os, test.groupNumber);
          Em.keys(test.e).forEach(function (k) {
            expect(group.get(k)).to.equal(test.e[k]);
          });
        });
      });
  });

  describe('#updateByCheckbox', function () {

    it('shouldn\'t do nothing if no stack selected', function () {
      var groups = [
        {},
        {},
        {}
      ];
      view.reopen({
        allRepositoriesGroup: groups,
        controller: {
          content: {
            stacks: [
              {isSelected: false}
            ]
          }
        }
      });
      view.updateByCheckbox();
      view.get('allRepositoriesGroup').forEach(function(g) {
        expect(g).to.eql({});
      });
    });

    it('target group isn\'t checked', function() {
      view.reopen({
        allGroupsCheckbox: [true],
        allRepositoriesGroup: [
          Em.Object.create({
            'group-number': 0,
            checked: false
          })
        ],
        controller: {
          content: {
            stacks: [
              {
                isSelected: true,
                operatingSystems: [
                  {
                    osType: 'redhat5',
                    baseUrl: 'baseUrl',
                    latestBaseUrl: 'latestBaseUrl',
                    validation: '',
                    selected: ''
                  }
                ]
              }
            ]
          }
        }
      });
      view.updateByCheckbox();
      var os = view.get('controller.content.stacks')[0].operatingSystems[0],
        targetGroup = view.get('allRepositoriesGroup.firstObject');
      expect(os.baseUrl).to.equal(os.latestBaseUrl);
      expect(os.selected).to.equal(false);
      expect(os.validation).to.be.null;
      expect(view.get('allGroupsCheckbox')).to.eql([false]);
      expect(targetGroup.get('baseUrl')).to.equal('latestBaseUrl');
      expect(targetGroup.get('latestBaseUrl')).to.equal('latestBaseUrl');
      expect(targetGroup.get('undo')).to.equal(false);
      expect(targetGroup.get('invalid-error')).to.equal(false);
      expect(targetGroup.get('clearAll')).to.equal(false);
      expect(targetGroup.get('empty-error')).to.equal(false);
      expect(targetGroup.get('validation')).to.be.null;
    });

    it('target group is checked, skipValidationChecked = true', function() {
      view.reopen({
        allGroupsCheckbox: [false],
        skipValidationChecked: true,
        allRepositoriesGroup: [
          Em.Object.create({
            'group-number': 0,
            checked: true,
            baseUrl: ''
          })
        ],
        controller: {
          content: {
            stacks: [
              {
                isSelected: true,
                operatingSystems: [
                  {
                    osType: 'redhat5',
                    baseUrl: 'baseUrl',
                    latestBaseUrl: 'latestBaseUrl',
                    validation: '',
                    selected: ''
                  }
                ]
              }
            ]
          }
        }
      });
      view.updateByCheckbox();
      var os = view.get('controller.content.stacks')[0].operatingSystems[0],
        targetGroup = view.get('allRepositoriesGroup.firstObject');
      expect(os.selected).to.equal(true);
      expect(os.skipValidation).to.equal(true);
      expect(view.get('allGroupsCheckbox')).to.eql([true]);
      expect(targetGroup.get('invalid-error')).to.equal(false);
      expect(targetGroup.get('empty-error')).to.equal(true);
      expect(targetGroup.get('clearAll')).to.equal('');
      expect(targetGroup.get('validation')).to.be.null;
    });

  });

  describe('#clearGroupLocalRepository', function() {
    it('should be proxy for doActionForGroupLocalRepository', function() {
      sinon.stub(view, 'doActionForGroupLocalRepository', Em.K);
      view.clearGroupLocalRepository({});
      expect(view.doActionForGroupLocalRepository.calledWith({}, '')).to.equal(true);
      view.doActionForGroupLocalRepository.restore();
    });
  });

  describe('#undoGroupLocalRepository', function() {
    it('should be proxy for doActionForGroupLocalRepository', function() {
      sinon.stub(view, 'doActionForGroupLocalRepository', Em.K);
      view.undoGroupLocalRepository({});
      expect(view.doActionForGroupLocalRepository.calledWith({}, 'latestBaseUrl')).to.equal(true);
      view.doActionForGroupLocalRepository.restore();
    });
  });

  describe('#doActionForGroupLocalRepository', function() {

    beforeEach(function() {
      sinon.stub(view, 'loadRepositories', Em.K);
    });

    afterEach(function() {
      view.loadRepositories.restore();
    });

    it('should update OS in selected stack', function() {
      var event = {context: Em.Object.create({'group-number': 0})};
      view.reopen({
        allGroupsCheckbox: [true],
        allRepositoriesGroup: [
          Em.Object.create({
            'group-number': 0,
            checked: false
          })
        ],
        controller: {
          content: {
            stacks: [
              {
                isSelected: true,
                operatingSystems: [
                  {
                    osType: 'redhat5',
                    baseUrl: 'baseUrl',
                    latestBaseUrl: 'latestBaseUrl',
                    validation: '',
                    selected: ''
                  }
                ]
              }
            ]
          }
        }
      });
      view.doActionForGroupLocalRepository(event, '');
      var os = view.get('controller.content.stacks')[0].operatingSystems[0];
      expect(os.baseUrl).to.equal('');
      expect(os.validation).to.be.null;
      expect(view.loadRepositories.calledOnce).to.equal(true);
    });

    it('should update OS in selected stack (2)', function() {
      var event = {context: Em.Object.create({'group-number': 0})};
      view.reopen({
        allGroupsCheckbox: [true],
        allRepositoriesGroup: [
          Em.Object.create({
            'group-number': 0,
            checked: false
          })
        ],
        controller: {
          content: {
            stacks: [
              {
                isSelected: true,
                operatingSystems: [
                  {
                    osType: 'redhat5',
                    baseUrl: 'baseUrl',
                    latestBaseUrl: 'latestBaseUrl',
                    validation: '',
                    selected: ''
                  }
                ]
              }
            ]
          }
        }
      });
      view.doActionForGroupLocalRepository(event, 'latestBaseUrl');
      var os = view.get('controller.content.stacks')[0].operatingSystems[0];
      expect(os.baseUrl).to.equal('latestBaseUrl');
      expect(os.validation).to.be.null;
      expect(view.loadRepositories.calledOnce).to.equal(true);
    });

  });

  describe('#editGroupLocalRepository', function() {

    it('should update os and group', function() {
      view.reopen({
        allGroupsCheckbox: [true],
        allRepositoriesGroup: [
          Em.Object.create({
            'group-number': 0,
            checked: false,
            baseUrl: 'b1'
          })
        ],
        controller: {
          content: {
            stacks: [
              {
                isSelected: true,
                operatingSystems: [
                  {
                    osType: 'redhat5',
                    baseUrl: 'baseUrl',
                    latestBaseUrl: 'latestBaseUrl',
                    validation: '',
                    selected: ''
                  }
                ]
              }
            ]
          }
        }
      });
      view.editGroupLocalRepository();
      var os = view.get('controller.content.stacks')[0].operatingSystems[0],
        targetGroup = view.get('allRepositoriesGroup.firstObject');
      expect(os.baseUrl).to.equal(targetGroup.get('baseUrl'));
      expect(os.validation).to.be.null;


      expect(targetGroup.get('undo')).to.equal(true);
      expect(targetGroup.get('invalid-error')).to.equal(false);
      expect(targetGroup.get('empty-error')).to.equal(false);
      expect(targetGroup.get('validation')).to.be.null;
    });

  });

  describe('#loadRepositories', function() {
    beforeEach(function() {
      sinon.stub(view, 'setGroupByOs', Em.K);
      sinon.stub(view, 'updateByCheckbox', Em.K);
      sinon.stub(view, 'editGroupLocalRepository', Em.K);
      sinon.stub(App, 'get', function(k) {
        if('supports.ubuntu' == k) return true;
        return Em.get(App, k);
      });
    });
    afterEach(function() {
      view.setGroupByOs.restore();
      view.updateByCheckbox.restore();
      view.editGroupLocalRepository.restore();
      App.get.restore();
    });
    Em.A([
        {
          osType: 'redhat5',
          e: {
            i: 0,
            o: 'Red Hat 5'
          }
        },
        {
          osType: 'redhat6',
          e: {
            i: 1,
            o: 'Red Hat 6'
          }
        },
        {
          osType: 'suse11',
          e: {
            i: 2,
            o: 'SLES 11'
          }
        },
        {
          osType: 'debian12',
          e: {
            i: 3,
            o: 'Ubuntu 12'
          }
        }
      ]).forEach(function (test) {
        it(test.osType, function () {
          view.reopen({
            allGroupsCheckbox: [true],
            allRepositoriesGroup: [
              Em.Object.create({
                'group-number': 0,
                checked: false,
                baseUrl: 'b1'
              })
            ],
            controller: {
              content: {
                stacks: [
                  {
                    isSelected: true,
                    operatingSystems: [
                      {
                        osType: test.osType,
                        baseUrl: 'baseUrl'
                      }
                    ]
                  }
                ]
              }
            }
          });
          view.loadRepositories();
          expect(view.get('allRepositoriesGroup')[test.e.i][0].get('osType')).to.equal(test.e.o);
        });
      });
  });

});
