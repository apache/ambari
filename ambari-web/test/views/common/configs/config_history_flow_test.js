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
require('views/common/configs/config_history_flow');
var testHelpers = require('test/helpers');

describe.skip('App.ConfigHistoryFlowView', function () {

  var view = App.ConfigHistoryFlowView.create({
    controller: Em.Object.create({
      loadSelectedVersion: Em.K,
      loadStep: Em.K
    }),
    displayedServiceVersion: Em.Object.create(),
    serviceVersions: []
  });

  App.TestAliases.testAsComputedAlias(view, 'serviceName', 'controller.selectedService.serviceName', 'string');

  App.TestAliases.testAsComputedOr(view, 'isSaveDisabled', ['controller.isSubmitDisabled', '!controller.versionLoaded', '!controller.isPropertiesChanged']);

  App.TestAliases.testAsComputedGt(view, 'displayedServiceVersion.notes.length', 100);

  App.TestAliases.testAsComputedTruncate(view, 'shortNotes', 'displayedServiceVersion.notes', 100, 100);

  describe('#visibleServiceVersion', function () {
    var testCases = [
      {
        params: {
          startIndex: 0,
          serviceVersions: []
        },
        result: []
      },
      {
        params: {
          startIndex: 0,
          serviceVersions: [1, 2, 3]
        },
        result: [1, 2, 3]
      },
      {
        params: {
          startIndex: 0,
          serviceVersions: [1, 2, 3, 4, 5, 6, 7]
        },
        result: [1, 2, 3, 4, 5]
      },
      {
        params: {
          startIndex: 3,
          serviceVersions: [1, 2, 3, 4, 5, 6, 7]
        },
        result: [4, 5, 6, 7]
      }
    ];
    testCases.forEach(function (test) {
      it('start index - ' + test.params.startIndex + ', serviceVersions length - ' + test.params.serviceVersions.length, function () {
        view.set('serviceVersions', test.params.serviceVersions);
        view.set('startIndex', test.params.startIndex);
        view.propertyDidChange('visibleServiceVersion');
        expect(view.get('visibleServiceVersion')).to.eql(test.result);
      });
    });
  });

  describe('#versionActionsDisabled', function () {
    it('versionLoaded is false', function () {
      view.set('controller.versionLoaded', false);
      expect(view.get('versionActionsDisabled')).to.be.true;
    });
    it('versionLoaded is true', function () {
      view.set('controller.versionLoaded', true);
      expect(view.get('versionActionsDisabled')).to.be.false;
    });
  });

  describe('#dropDownList', function () {
    var displayedServiceVersion = {version: 1};

    it('Only one service version is present', function () {
      view.set('serviceVersions', [displayedServiceVersion]);
      view.set('displayedServiceVersion', displayedServiceVersion);
      view.propertyDidChange('dropDownList');
      expect(view.get('dropDownList')).to.be.empty;
    });
    it('Three service version', function () {
      view.set('serviceVersions', [displayedServiceVersion, {version: 2}, {version: 3}]);
      view.set('displayedServiceVersion', displayedServiceVersion);
      view.propertyDidChange('dropDownList');
      expect(view.get('dropDownList')).to.eql([{version: 3}, {version: 2}]);
    });
    it('Seven service version, showFullList is false', function () {
      view.set('serviceVersions', [
        displayedServiceVersion,
        {version: 2},
        {version: 3},
        {version: 4},
        {version: 5},
        {version: 6},
        {version: 7},
        {version: 8}
      ]);
      view.set('displayedServiceVersion', displayedServiceVersion);
      view.set('showFullList', false);
      view.propertyDidChange('dropDownList');
      expect(view.get('dropDownList')).to.eql([
        {version: 8},
        {version: 7},
        {version: 6},
        {version: 5},
        {version: 4},
        {version: 3}
      ]);
    });
    it('Seven service version, showFullList is true', function () {
      view.set('serviceVersions', [
        displayedServiceVersion,
        {version: 2},
        {version: 3},
        {version: 4},
        {version: 5},
        {version: 6},
        {version: 7},
        {version: 8}
      ]);
      view.set('displayedServiceVersion', displayedServiceVersion);
      view.set('showFullList', true);
      view.propertyDidChange('dropDownList');
      expect(view.get('dropDownList')).to.eql([
        {version: 8},
        {version: 7},
        {version: 6},
        {version: 5},
        {version: 4},
        {version: 3},
        {version: 2}
      ]);
    });
  });

  describe('#openFullList()', function () {
    var event;
    beforeEach(function () {
      event = {
        stopPropagation: Em.K
      };
      sinon.spy(event, 'stopPropagation');
      view.openFullList(event);
    });

    afterEach(function () {
      event.stopPropagation.restore();
    });

    it('stopPropagation is called once', function () {
      expect(event.stopPropagation.calledOnce).to.be.true;
    });

    it('stopPropagation is true', function () {
      expect(view.get('stopPropagation')).to.be.true;
    });
  });

  describe('#hideFullList()', function () {
    var testCases = [
      {
        params: {
          serviceVersions: new Array(0)
        },
        result: true
      },
      {
        params: {
          serviceVersions: new Array(6)
        },
        result: true
      },
      {
        params: {
          serviceVersions: new Array(7)
        },
        result: false
      }
    ];
    testCases.forEach(function (test) {
      it('notes length - ' + test.params.count, function () {
        view.set('serviceVersions', test.params.serviceVersions);
        view.hideFullList();
        expect(view.get('showFullList')).to.equal(test.result);
      });
    });
  });

  describe('#didInsertElement()', function () {

    beforeEach(function () {
      sinon.stub(App, 'tooltip');
      view.didInsertElement();
    });

    afterEach(function () {
      App.tooltip.restore();
    });

    it('App.tooltip is called on�e', function () {
      expect(App.tooltip.calledOnce).to.be.true;
    });
  });

  describe('#willInsertElement()', function () {

    beforeEach(function () {
      sinon.stub(view, 'adjustFlowView', Em.K);
      sinon.stub(view, 'keepInfoBarAtTop', Em.K);
    });

    afterEach(function () {
      view.adjustFlowView.restore();
      view.keepInfoBarAtTop.restore();
    });

    describe('Only current version is present', function () {

      beforeEach(function () {
        view.set('serviceVersions', [Em.Object.create({isCurrent: true})]);
        view.willInsertElement();
      });

      it('adjustFlowView is called once', function () {
        expect(view.adjustFlowView.calledOnce).to.be.true;
      });
      it('keepInfoBarAtTop is called once', function () {
        expect(view.keepInfoBarAtTop.calledOnce).to.be.true;
      });
      it('startIndex = 0', function () {
        expect(view.get('startIndex')).to.equal(0);
      });
      it('serviceVersions.@each.isDisplayed = [true]', function () {
        expect(view.get('serviceVersions').mapProperty('isDisplayed')).to.eql([true]);
      });
    });

    describe('Five service versions are present', function () {

      beforeEach(function () {
        view.set('serviceVersions', [
          Em.Object.create({isCurrent: true}),
          Em.Object.create(),
          Em.Object.create(),
          Em.Object.create(),
          Em.Object.create()
        ]);
        view.willInsertElement();
      });
      it('adjustFlowView is called once', function () {
        expect(view.adjustFlowView.calledOnce).to.be.true;
      });
      it('keepInfoBarAtTop is called once', function () {
        expect(view.keepInfoBarAtTop.calledOnce).to.be.true;
      });
      it('startIndex = 0', function () {
        expect(view.get('startIndex')).to.equal(0);
      });
      it('serviceVersions.@each.isDisplayed = [true, false, false, false, false]', function () {
        expect(view.get('serviceVersions').mapProperty('isDisplayed')).to.eql([true, false, false, false, false]);
      });
    });

    describe('Six service versions are present', function () {
      beforeEach(function () {
        view.set('serviceVersions', [
          Em.Object.create({isCurrent: true}),
          Em.Object.create(),
          Em.Object.create(),
          Em.Object.create(),
          Em.Object.create(),
          Em.Object.create()
        ]);
        view.willInsertElement();
      });

      it('adjustFlowView is called once', function () {
        expect(view.adjustFlowView.calledOnce).to.be.true;
      });
      it('keepInfoBarAtTop is called once', function () {
        expect(view.keepInfoBarAtTop.calledOnce).to.be.true;
      });
      it('startIndex is 1', function () {
        expect(view.get('startIndex')).to.equal(1);
      });
      it('serviceVersions.@each.isDisplayed = [true, false, false, false, false, false]', function () {
        expect(view.get('serviceVersions').mapProperty('isDisplayed')).to.eql([true, false, false, false, false, false]);
      });
    });

  });

  describe('#setInfoBarPosition()', function () {
    var testCases = [
      {
        params: {
          scrollTop: 0,
          defaultTop: 0
        },
        result: 'auto'
      },
      {
        params: {
          scrollTop: 1,
          defaultTop: 11
        },
        result: '10px'
      },
      {
        params: {
          scrollTop: 1,
          defaultTop: 0
        },
        result: '10px'
      }
    ];
    var infoBar = {
      css: Em.K
    };

    beforeEach(function () {
      sinon.spy(infoBar, 'css');
    });

    afterEach(function () {
      infoBar.css.restore();
    });

    testCases.forEach(function (test) {
      it('scroll top - ' + test.params.scrollTop + ', default top - ' + test.params.defaultTop, function () {
        view.setInfoBarPosition(infoBar, test.params.defaultTop, test.params.scrollTop);
        expect(infoBar.css.calledWith('top', test.result)).to.be.true;
      });
    });
  });

  describe('#adjustFlowView()', function () {
    var testCases = [
      {
        params: {
          serviceVersions: [Em.Object.create()],
          startIndex: 0
        },
        result: {
          first: [true],
          showLeftArrow: false,
          showRightArrow: false
        }
      },
      {
        params: {
          serviceVersions: [
            Em.Object.create(),
            Em.Object.create(),
            Em.Object.create(),
            Em.Object.create(),
            Em.Object.create()
          ],
          startIndex: 0
        },
        result: {
          first: [true, false, false, false, false],
          showLeftArrow: false,
          showRightArrow: false
        }
      },
      {
        params: {
          serviceVersions: [
            Em.Object.create(),
            Em.Object.create(),
            Em.Object.create(),
            Em.Object.create(),
            Em.Object.create(),
            Em.Object.create()
          ],
          startIndex: 0
        },
        result: {
          first: [true, false, false, false, false, false],
          showLeftArrow: false,
          showRightArrow: true
        }
      },
      {
        params: {
          serviceVersions: [
            Em.Object.create(),
            Em.Object.create(),
            Em.Object.create(),
            Em.Object.create(),
            Em.Object.create(),
            Em.Object.create()
          ],
          startIndex: 1
        },
        result: {
          first: [false, true, false, false, false, false],
          showLeftArrow: true,
          showRightArrow: false
        }
      },
      {
        params: {
          serviceVersions: [
            Em.Object.create(),
            Em.Object.create(),
            Em.Object.create(),
            Em.Object.create(),
            Em.Object.create(),
            Em.Object.create(),
            Em.Object.create()
          ],
          startIndex: 1
        },
        result: {
          first: [false, true, false, false, false, false, false],
          showLeftArrow: true,
          showRightArrow: true
        }
      }
    ];

    testCases.forEach(function (test) {
      it('start index - ' + test.params.startIndex + ', serviceVersions length - ' + test.params.serviceVersions.length, function () {
        view.set('startIndex', test.params.startIndex);
        view.set('serviceVersions', test.params.serviceVersions);

        view.adjustFlowView();
        expect(view.get('serviceVersions').mapProperty('first')).to.eql(test.result.first);
        expect(view.get('showLeftArrow')).to.eql(test.result.showLeftArrow);
        expect(view.get('showRightArrow')).to.eql(test.result.showRightArrow);
      });
    });
  });

  describe('#switchVersion()', function () {
    var event = {
      context: Em.Object.create({
        version: 2
      })
    };
    beforeEach(function(){
      sinon.stub(view, 'shiftFlowOnSwitch', Em.K);
      sinon.spy(view.get('controller'), 'loadSelectedVersion');
    });
    afterEach(function(){
      view.shiftFlowOnSwitch.restore();
      view.get('controller').loadSelectedVersion.restore();
    });
    it('Only one service version is present', function () {
      view.set('serviceVersions', [Em.Object.create({version: 2})]);
      view.switchVersion(event);
      expect(view.get('serviceVersions').mapProperty('isDisplayed')).to.eql([true]);
      expect(view.get('controller').loadSelectedVersion.calledWith(2)).to.be.true;
      expect(view.shiftFlowOnSwitch.calledWith(0)).to.be.true;
    });
    it('Two service versions are present', function () {
      view.set('serviceVersions', [
        Em.Object.create({version: 1}),
        Em.Object.create({version: 2})
      ]);
      view.switchVersion(event);
      expect(view.get('serviceVersions').mapProperty('isDisplayed')).to.eql([false, true]);
      expect(view.get('controller').loadSelectedVersion.calledWith(2)).to.be.true;
      expect(view.shiftFlowOnSwitch.calledWith(1)).to.be.true;
    });
  });

  describe('#compare()', function () {
    it('should set compareServiceVersion', function () {
      view.compare({context: Em.Object.create({version: 1})});

      expect(view.get('controller.compareServiceVersion')).to.eql(Em.Object.create({version: 1}));
    });
  });

  describe('#revert()', function () {
    beforeEach(function () {
      sinon.stub(App.ModalPopup, 'show', function (options) {
        options.onPrimary.call(Em.Object.create({
          serviceConfigNote: 'note',
          hide: Em.K
        }));
      });
      sinon.stub(view, 'sendRevertCall', Em.K);
    });
    afterEach(function () {
      App.ModalPopup.show.restore();
      view.sendRevertCall.restore();
    });
    it('context passed', function () {
      view.revert({context: Em.Object.create({
        version: 1,
        serviceName: 'S1'
      })});

      expect(App.ModalPopup.show.calledOnce).to.be.true;
      expect(view.sendRevertCall.calledWith(Em.Object.create({
        version: 1,
        serviceName: 'S1',
        serviceConfigNote: 'note'
      }))).to.be.true;
    });
    it('context is not passed', function () {
      view.set('displayedServiceVersion', Em.Object.create({
        version: 1,
        serviceName: 'S1'
      }));
      view.revert({});

      expect(App.ModalPopup.show.calledOnce).to.be.true;
      expect(view.sendRevertCall.calledWith(Em.Object.create({
        version: 1,
        serviceName: 'S1',
        serviceConfigNote: 'note',
        notes: ''
      }))).to.be.true;
    });
  });

  describe('#sendRevertCall()', function () {

    beforeEach(function () {
      view.sendRevertCall(Em.Object.create());
    });

    it('request is sent', function () {
      var args = testHelpers.findAjaxRequest('name', 'service.serviceConfigVersion.revert');
      expect(args).exists;
    });
  });

  describe('#sendRevertCallSuccess()', function () {
    beforeEach(function () {
      sinon.spy(view.get('controller'), 'loadStep');
      sinon.stub(App.router.get('updateController'), 'updateComponentConfig', Em.K);
      view.sendRevertCallSuccess();
    });
    afterEach(function () {
      view.get('controller').loadStep.restore();
      App.router.get('updateController').updateComponentConfig.restore();
    });

    it('loadStep is called', function () {
      expect(view.get('controller').loadStep.calledOnce).to.be.true;
    });

    it('updateComponentConfig is called', function () {
      expect(App.router.get('updateController').updateComponentConfig.calledOnce).to.be.true;
    });
  });

  describe('#save()', function () {

    beforeEach(function () {
      sinon.stub(App.ModalPopup, 'show', Em.K);
      sinon.stub(App.ServiceConfigVersion, 'find').returns([
        { serviceName: 'service'}
      ]);
    });

    afterEach(function () {
      App.ModalPopup.show.restore();
      App.ServiceConfigVersion.find.restore();
    });

    it('modal popup should be displayed', function () {
      view.save();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });

    it('controller properties should be modified on save', function () {
      view.setProperties({
        'serviceName': 'service',
        'controller.saveConfigsFlag': false,
        'controller.serviceConfigVersionNote': '',
        'controller.serviceConfigNote': '',
        'controller.preSelectedConfigVersion': null,
        'serviceConfigNote': 'note',
        'displayedServiceVersion.serviceName': 'service',
        'controller.selectedConfigGroup.name': 'group'
      });
      var popup = view.save();
      popup.onSave();
      expect(view.get('controller.saveConfigsFlag')).to.be.true;
      expect(view.get('controller').getProperties(['saveConfigsFlag', 'serviceConfigVersionNote', 'serviceConfigNote', 'preSelectedConfigVersion'])).to.eql({
        saveConfigsFlag: true,
        serviceConfigVersionNote: 'note',
        serviceConfigNote: this.get('serviceConfigNote'),
        preSelectedConfigVersion: Em.Object.create({
          version: 2,
          serviceName: 'service',
          groupName: 'group'
        })
      });
    });
  });

  describe('#shiftBack()', function () {

    beforeEach(function () {
      sinon.stub(view, 'decrementProperty', Em.K);
      sinon.stub(view, 'adjustFlowView', Em.K);
      view.shiftBack();
    });

    afterEach(function () {
      view.adjustFlowView.restore();
      view.decrementProperty.restore();
    });

    it('decrementProperty is called with correct data', function () {
      expect(view.decrementProperty.calledWith('startIndex')).to.be.true;
    });

    it('adjustFlowView is called once', function () {
      expect(view.adjustFlowView.calledOnce).to.be.true;
    });
  });

  describe('#shiftForward()', function () {

    beforeEach(function () {
      sinon.stub(view, 'incrementProperty', Em.K);
      sinon.stub(view, 'adjustFlowView', Em.K);
      view.shiftForward();
    });

    afterEach(function () {
      view.adjustFlowView.restore();
      view.incrementProperty.restore();
    });

    it('startIndex++', function () {
      expect(view.incrementProperty.calledWith('startIndex')).to.be.true;
    });

    it('adjustFlowView is called once', function () {
      expect(view.adjustFlowView.calledOnce).to.be.true;
    });
  });

  describe('#adjustFlowView()', function () {
    var testCases = [
      {
        params: {
          serviceVersions: [],
          startIndex: 0,
          versionIndex: 1
        },
        result: {
          startIndex: 0,
          adjustFlowViewCall: false
        }
      },
      {
        params: {
          serviceVersions: new Array(6),
          startIndex: 7,
          versionIndex: 6
        },
        result: {
          startIndex: 1,
          adjustFlowViewCall: true
        }
      },
      {
        params: {
          serviceVersions: new Array(12),
          startIndex: 7,
          versionIndex: 6
        },
        result: {
          startIndex: 6,
          adjustFlowViewCall: true
        }
      },
      {
        params: {
          serviceVersions: new Array(12),
          startIndex: 0,
          versionIndex: 6
        },
        result: {
          startIndex: 6,
          adjustFlowViewCall: true
        }
      },
      {
        params: {
          serviceVersions: new Array(6),
          startIndex: 0,
          versionIndex: 6
        },
        result: {
          startIndex: 1,
          adjustFlowViewCall: true
        }
      }
    ];

    beforeEach(function () {
      sinon.stub(view, 'adjustFlowView', Em.K);
    });

    afterEach(function () {
      view.adjustFlowView.restore();
    });

    testCases.forEach(function (test) {
      it('start index - ' + test.params.startIndex + ', serviceVersions length - ' + test.params.serviceVersions.length + ', versionIndex - ' + test.params.versionIndex, function () {
        view.set('serviceVersions', test.params.serviceVersions);
        view.set('startIndex', test.params.startIndex);
        view.shiftFlowOnSwitch(test.params.versionIndex);

        expect(view.get('startIndex')).to.eql(test.result.startIndex);
        expect(view.adjustFlowView.calledOnce).to.eql(test.result.adjustFlowViewCall);
      });
    });
  });
});

function getView() {
  return App.ConfigsServiceVersionBoxView.create();
}

describe('App.ConfigsServiceVersionBoxView', function () {

  App.TestAliases.testAsComputedAlias(getView(), 'disabledActionAttr', 'serviceVersion.disabledActionAttr', 'object');

  App.TestAliases.testAsComputedAlias(getView(), 'disabledActionMessages', 'serviceVersion.disabledActionMessages', 'object');

});