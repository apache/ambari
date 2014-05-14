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

  describe('#allRepositoriesGroups', function () {

    var controller = Em.Object.create({
      content: {
        stacks: [
          {
            operatingSystems: [
              {
                osType: 'redhat5',
                selected: true
              },
              {
                osType: 'redhat5',
                selected: true
              },
              {
                osType: 'redhat6',
                selected: false
              }
            ],
            isSelected: true
          }
        ]
      }
    });

    var allRepositories = [
      Ember.Object.create({osType: 'redhat5', 'empty-error': true}),
      Ember.Object.create({osType: 'redhat5', 'empty-error': true}),
      Ember.Object.create({osType: 'redhat6', 'empty-error': true})
    ];

    it('should create repo groups from repo list', function () {
      view.reopen({
        controller: controller
      });
      view.set('allRepositories', allRepositories);
      expect(view.get('allRepositoriesGroups.length')).to.equal(2);
      expect(view.get('allRepositoriesGroups')[0].get('name')).to.equal('redhat5');
      expect(view.get('allRepositoriesGroups')[1].get('name')).to.equal('redhat6');
      expect(view.get('allRepositoriesGroups')[0].get('checked')).to.be.true;
      expect(view.get('allRepositoriesGroups')[1].get('checked')).to.be.false;
      expect(view.get('allRepositoriesGroups')[0].get('repositories')).to.eql([allRepositories[0], allRepositories[1]]);
      expect(view.get('allRepositoriesGroups')[1].get('repositories')).to.eql([allRepositories[2]]);
    });

    it('should create empty array if there is no stacks', function () {
      view.reopen({
        controller: controller
      });
      view.set('controller.content.stacks', []);
      view.set('allRepositories', allRepositories);
      expect(view.get('allRepositoriesGroups.length')).to.equal(0);
    });
  });

  describe('#emptyRepoExist', function () {

    var tests = Em.A([
      {
        allRepositories: [
          {'empty-error': false},
          {'empty-error': false},
          {'empty-error': false}
        ],
        e: false
      },
      {
        allRepositories: [
          {'empty-error': true},
          {'empty-error': false},
          {'empty-error': false}
        ],
        e: true
      },
      {
        allRepositories: [
          {'empty-error': true},
          {'empty-error': true},
          {'empty-error': true}
        ],
        e: true
      }
    ]);

    tests.forEach(function (test) {
      it(test.allRepositories.mapProperty('empty-error').join(', '), function () {
        view.set('allRepositories', test.allRepositories);
        expect(view.get('emptyRepoExist')).to.equal(test.e);
      });
    });
  });

  describe('#allRepoUnchecked', function () {

    var tests = Em.A([
      {
        allRepositoriesGroups: [
          {'checked': false},
          {'checked': false},
          {'checked': false}
        ],
        e: true
      },
      {
        allRepositoriesGroups: [
          {'checked': true},
          {'checked': false},
          {'checked': false}
        ],
        e: false
      },
      {
        allRepositoriesGroups: [
          {'checked': true},
          {'checked': true},
          {'checked': true}
        ],
        e: false
      }
    ]);

    tests.forEach(function (test) {
      it(test.allRepositoriesGroups.mapProperty('checked').join(', '), function () {
        view.reopen({
          allRepositoriesGroups: test.allRepositoriesGroups
        });
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
        allRepositories: [Em.Object.create({validation: 'icon-exclamation-sign'})],
        m: 'invalidCnt: 1, validation: icon-exclamation-sign',
        e: true
      },
      {
        stacks: [Em.Object.create({isSelected: true, invalidCnt: 1})],
        allRepositories: [Em.Object.create({validation: ''})],
        m: 'invalidCnt: 1, validation: ""',
        e: false
      },
      {
        stacks: [Em.Object.create({isSelected: true, invalidCnt: 0})],
        allRepositories: [Em.Object.create({validation: ''})],
        m: 'invalidCnt: 0, validation: ""',
        e: false
      },
      {
        stacks: [Em.Object.create({isSelected: true, invalidCnt: 0})],
        allRepositories: [Em.Object.create({validation: 'icon-exclamation-sign'})],
        m: 'invalidCnt: 0, validation: icon-exclamation-sign',
        e: false
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        view.reopen({
          allRepositoriesGroups: null
        });
        view.set('controller.content.stacks', test.stacks);
        view.set('allRepositories', test.allRepositories);
        expect(view.get('invalidUrlExist')).to.equal(test.e);
      });
    });
  });

  describe('#totalErrorCnt', function () {
    var tests = Em.A([
      {
        allRepositories: [
          {}
        ],
        m: 'allRepoUnchecked',
        allRepoUnchecked: true,
        e: 1
      },
      {
        allRepositories: [
          {'empty-error': true},
          {'empty-error': true}
        ],
        allRepoUnchecked: false,
        m: 'two with empty-error',
        e: 2
      },
      {
        allRepositories: [
          {'validation': 'icon-exclamation-sign'},
          {'validation': 'icon-exclamation-sign'}
        ],
        allRepoUnchecked: false,
        m: 'two with validation="icon-exclamation-sign"',
        e: 2
      },
      {
        allRepositories: [
          {'empty-error': true, 'validation': 'icon-exclamation-sign'},
          {'empty-error': true, 'validation': 'icon-exclamation-sign'}
        ],
        allRepoUnchecked: false,
        m: 'two with empty-error, two with validation="icon-exclamation-sign"',
        e: 4
      },
      {
        allRepositories: [
          {}
        ],
        allRepoUnchecked: false,
        m: 'no errors/warnings etc',
        e: 0
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        view.reopen({
          allRepositoriesGroups: null,
          allRepoUnchecked: test.allRepoUnchecked
        });
        view.set('allRepositories', test.allRepositories);
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

  describe('#updateByCheckbox', function () {

    var allRepositories = [
      Em.Object.create({
        id: 'id',
        osType: 'redhat5',
        baseUrl: 'baseUrl',
        latestBaseUrl: 'latestBaseUrl',
        validation: '',
        selected: ''
      })
    ];

    var allRepositoriesGroups = [
      Em.Object.create({
        name: 'redhat5',
        checked: false,
        repositories: [Em.Object.create({
          id: 'id',
          osType: 'redhat5',
          baseUrl: 'baseUrl',
          latestBaseUrl: 'latestBaseUrl',
          validation: '',
          selected: ''
        })
        ]
      })
    ];

    var controller = {
      content: {
        stacks: [
          {
            isSelected: true,
            operatingSystems: [
              {
                id: 'id',
                osType: 'redhat5',
                baseUrl: 'baseUrl',
                latestBaseUrl: 'latestBaseUrl',
                validation: '',
                selected: false
              }
            ]
          }
        ]
      }
    };

    it('target group isn\'t checked', function () {
      view.reopen({
        allRepositories: allRepositories,
        allRepositoriesGroups: allRepositoriesGroups,
        controller: controller
      });
      view.updateByCheckbox();
      var os = view.get('controller.content.stacks')[0].operatingSystems[0],
          targetGroup = view.get('allRepositories.firstObject');
      expect(os.baseUrl).to.equal(os.latestBaseUrl);
      expect(os.selected).to.equal(false);
      expect(os.validation).to.be.null;
      expect(targetGroup.get('baseUrl')).to.equal('latestBaseUrl');
      expect(targetGroup.get('latestBaseUrl')).to.equal('latestBaseUrl');
      expect(targetGroup.get('undo')).to.equal(false);
      expect(targetGroup.get('invalid-error')).to.equal(false);
      expect(targetGroup.get('clearAll')).to.equal(false);
      expect(targetGroup.get('empty-error')).to.equal(false);
      expect(targetGroup.get('validation')).to.be.null;
    });

    it('target group is checked, skipValidationChecked = true', function () {
      controller.content.stacks[0].operatingSystems[0].selected = true;
      allRepositoriesGroups[0].set('checked', true);
      view.reopen({
        allRepositories: allRepositories,
        allRepositoriesGroups: allRepositoriesGroups,
        controller: controller,
        skipValidationChecked: true
      });
      view.updateByCheckbox();
      var os = view.get('controller.content.stacks')[0].operatingSystems[0],
          targetGroup = view.get('allRepositories.firstObject');
      expect(os.selected).to.equal(true);
      expect(os.skipValidation).to.equal(true);
      expect(targetGroup.get('invalid-error')).to.equal(false);
      expect(targetGroup.get('empty-error')).to.equal(false);
      expect(targetGroup.get('clearAll')).to.equal('latestBaseUrl');
      expect(targetGroup.get('validation')).to.be.null;
    });
  });

  describe('#clearGroupLocalRepository', function () {
    it('should be proxy for doActionForGroupLocalRepository', function () {
      sinon.stub(view, 'doActionForGroupLocalRepository', Em.K);
      view.clearGroupLocalRepository({});
      expect(view.doActionForGroupLocalRepository.calledWith({}, '')).to.equal(true);
      view.doActionForGroupLocalRepository.restore();
    });
  });

  describe('#undoGroupLocalRepository', function () {
    it('should be proxy for doActionForGroupLocalRepository', function () {
      sinon.stub(view, 'doActionForGroupLocalRepository', Em.K);
      view.undoGroupLocalRepository({});
      expect(view.doActionForGroupLocalRepository.calledWith({}, 'latestBaseUrl')).to.equal(true);
      view.doActionForGroupLocalRepository.restore();
    });
  });

  describe('#doActionForGroupLocalRepository', function () {

    beforeEach(function () {
      sinon.stub(view, 'loadRepositories', Em.K);
    });

    afterEach(function () {
      view.loadRepositories.restore();
    });

    it('should update OS in selected stack', function () {
      var event = {context: Em.Object.create({'group-number': 0})};
      view.reopen({
        allRepositories: [
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

    it('should update OS in selected stack (2)', function () {
      var event = {context: Em.Object.create({'group-number': 0})};
      view.reopen({
        allRepositories: [
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

  describe('#editLocalRepository', function () {

    it('should update os and group', function () {
      view.reopen({
        allRepositories: [
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
      view.editLocalRepository();
      var os = view.get('controller.content.stacks')[0].operatingSystems[0],
          targetGroup = view.get('allRepositories.firstObject');
      expect(os.baseUrl).to.equal(targetGroup.get('baseUrl'));
      expect(os.validation).to.be.null;


      expect(targetGroup.get('undo')).to.equal(true);
      expect(targetGroup.get('invalid-error')).to.equal(false);
      expect(targetGroup.get('empty-error')).to.equal(false);
      expect(targetGroup.get('validation')).to.be.null;
    });

  });

  describe('#loadRepositories', function () {
    beforeEach(function () {
      sinon.stub(view, 'updateByCheckbox', Em.K);
      sinon.stub(view, 'editLocalRepository', Em.K);
    });
    afterEach(function () {
      view.updateByCheckbox.restore();
      view.editLocalRepository.restore();
    });
    it('Should create repository object from controller content stack data', function () {
      controller = {
        content: {
          stacks: [
            {
              isSelected: true,
              operatingSystems: [
                {
                  'id': 'test',
                  'repoId': 'HDP',
                  'baseUrl': 'http://test1',
                  'osType': 'RedHat',
                  'latestBaseUrl': 'http://test1',
                  'defaultBaseUrl': 'http://test3',
                  'validation': 'icon-exclamation-sign',
                  'errorTitle': 'test',
                  'errorContent': 'test'
                }
              ]
            }
          ]
        }
      };
      result = Ember.Object.create({
        'id': 'test',
        'repoId': 'HDP',
        'baseUrl': 'http://test1',
        'osType': 'RedHat',
        'latestBaseUrl': 'http://test1',
        'defaultBaseUrl': 'http://test3',
        'empty-error': false,
        'invalid-error': true,
        'validation': 'icon-exclamation-sign',
        'undo': false,
        'clearAll': 'http://test1',
        'errorTitle': 'test',
        'errorContent': 'test'
      });
      view.reopen({
        controller: controller
      });
      view.loadRepositories();
      var allRepositories = view.get('allRepositories');
      Em.keys(allRepositories).forEach(function (key) {
        expect(allRepositories[0].get(key)).to.equal(result.get(key));
      });
    });
  });

});
