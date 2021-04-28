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
require('controllers/main/service/info/metric');
var testHelpers = require('test/helpers');
function getController() {
  return App.MainServiceInfoMetricsController.create();
}

describe('App.MainServiceInfoMetricsController', function () {

  var controller;

  beforeEach(function () {
    controller = App.MainServiceInfoMetricsController.create();
  });

  App.TestAliases.testAsComputedOr(getController(), 'showTimeRangeControl', ['!isServiceWithEnhancedWidgets', 'someWidgetGraphExists']);


  describe("#getActiveWidgetLayout() for Enhanced Dashboard", function () {

    it("make GET call", function () {
      controller.reopen({
        isServiceWithEnhancedWidgets: true,
        content: Em.Object.create({serviceName: 'HDFS'})
      });
      controller.getActiveWidgetLayout();
      expect(testHelpers.findAjaxRequest('name', 'widgets.layouts.active.get')).to.exists;
    });
  });

  describe("#getActiveWidgetLayoutSuccessCallback()", function () {
    beforeEach(function () {
      sinon.stub( App.widgetLayoutMapper, 'map');
      sinon.stub( App.widgetMapper, 'map');
    });
    afterEach(function () {
      App.widgetLayoutMapper.map.restore();
      App.widgetMapper.map.restore();
    });
    it("isWidgetLayoutsLoaded should be set to true", function () {
      controller.reopen({
        isServiceWithEnhancedWidgets: true,
        content: Em.Object.create({serviceName: 'HDFS'})
      });
      controller.getActiveWidgetLayoutSuccessCallback({items:[{
        WidgetLayoutInfo: {}
      }]});
      expect(controller.get('isWidgetsLoaded')).to.be.true;
    });

  });

  describe("#hideWidgetSuccessCallback()", function () {
    beforeEach(function () {
      sinon.stub(App.widgetLayoutMapper, 'map');
      sinon.stub(controller, 'propertyDidChange');
      var params = {
        data: {
          WidgetLayoutInfo: {
            widgets: [
              {id: 1}
            ]
          }
        }
      };
      controller.hideWidgetSuccessCallback({}, {}, params);
    });
    afterEach(function () {
      App.widgetLayoutMapper.map.restore();
      controller.propertyDidChange.restore();
    });
    it("mapper is called with valid data", function () {
      expect(App.widgetLayoutMapper.map.calledWith({
        items: [{
          WidgetLayoutInfo: {
            widgets: [
              {
                WidgetInfo: {
                  id: 1
                }
              }
            ]
          }
        }]
      })).to.be.true;
    });
    it('`widgets` is forced to be recalculated', function () {
      expect(controller.propertyDidChange.calledWith('widgets')).to.be.true;
    });
  });

  describe('#loadWidgetLayouts', function () {

    it('should call ajax send request', function () {
      controller.set('isWidgetLayoutsLoaded', true);
      controller.loadWidgetLayouts();
      expect(App.ajax.send.calledOnce).to.be.true;
      expect(controller.get('isWidgetLayoutsLoaded')).to.be.false;
    });
  });

  describe('#loadWidgetLayoutsSuccessCallback', function () {

    beforeEach(function () {
      sinon.stub(App.widgetLayoutMapper, 'map');
    });

    afterEach(function () {
      App.widgetLayoutMapper.map.restore();
    });

    it('should map widget layout data', function () {
      controller.set('isWidgetLayoutsLoaded', false);
      controller.loadWidgetLayoutsSuccessCallback({});
      expect(App.widgetLayoutMapper.map.calledOnce).to.be.true;
      expect(controller.get('isWidgetLayoutsLoaded')).to.be.true;
    });
  });

  describe('#loadAllSharedWidgets', function () {

    it('should call ajax send request', function () {
      controller.set('isAllSharedWidgetsLoaded', true);
      controller.loadAllSharedWidgets();
      expect(App.ajax.send.calledOnce).to.be.true;
      expect(controller.get('isAllSharedWidgetsLoaded')).to.be.false;
    });
  });

  describe('#loadAllSharedWidgetsSuccessCallback', function () {

    it('should set all shared widgets', function () {
      var data = {
        items: [
          {
            WidgetInfo: {
              tag: '1',
              id: 1,
              widget_name: 'w1',
              widget_type: 'GRAPH',
              metrics: JSON.stringify([{service_name: 's1'}, {service_name: 's2'}]),
              description: 'some text',
              scope: 'CLUSTER'
            }
          },
          {
            WidgetInfo: {
              tag: '2',
              id: 2,
              widget_name: 'w2',
              widget_type: 'HEATMAP',
              metrics: '',
              description: 'some text',
              scope: 'CLUSTER'
            }
          }
        ]
      };

      controller.set('widgets', [
        {
          id: 1
        },
        {
          id: 2
        }
      ]);
      controller.set('activeNSWidgetLayouts', [
        Em.Object.create({
          nameServiceId: 'all',
          widgets: []
        }),
        Em.Object.create({
          nameServiceId: '1',
          widgets: []
        })
      ]);
      controller.set('isAllSharedWidgetsLoaded', false);
      controller.loadAllSharedWidgetsSuccessCallback(data);
      expect(JSON.stringify(controller.get('allSharedWidgets'))).to.equal('[{"id":1,"widgetName":"w1","tag":"1","metrics":"[{\\"service_name\\":\\"s1\\"},{\\"service_name\\":\\"s2\\"}]","description":"some text","widgetType":"GRAPH","iconPath":"/img/widget-graph.png","serviceName":"s1-s2","added":false,"isShared":true}]');
      expect(controller.get('isAllSharedWidgetsLoaded')).to.be.true;
    });
  });

  describe('#loadMineWidgets', function () {

    it('should call ajax send request', function () {
      controller.set('isMineWidgetsLoaded', true);
      controller.loadMineWidgets();
      expect(App.ajax.send.calledOnce).to.be.true;
      expect(controller.get('isMineWidgetsLoaded')).to.be.false;
    });
  });

  describe('#loadMineWidgetsSuccessCallback', function () {

    it('should set mine widgets', function () {
      var data = {
        items: [
          {
            WidgetInfo: {
              tag: '1',
              id: 1,
              widget_name: 'w1',
              widget_type: 'GRAPH',
              metrics: JSON.stringify([{service_name: 's1'}, {service_name: 's2'}]),
              description: 'some text',
              scope: 'CLUSTER'
            }
          },
          {
            WidgetInfo: {
              tag: '2',
              id: 2,
              widget_name: 'w2',
              widget_type: 'HEATMAP',
              metrics: '',
              description: 'some text',
              scope: 'CLUSTER'
            }
          }
        ]
      };

      controller.set('widgets', [
        {
          id: 1
        },
        {
          id: 2
        }
      ]);
      controller.set('activeNSWidgetLayouts', [
        Em.Object.create({
          nameServiceId: 'all',
          widgets: []
        }),
        Em.Object.create({
          nameServiceId: '1',
          widgets: []
        })
      ]);
      controller.set('isMineWidgetsLoaded', false);
      controller.loadMineWidgetsSuccessCallback(data);
      expect(JSON.stringify(controller.get('mineWidgets'))).to.equal('[{"id":1,"widgetName":"w1","tag":"1","metrics":"[{\\"service_name\\":\\"s1\\"},{\\"service_name\\":\\"s2\\"}]","description":"some text","widgetType":"GRAPH","iconPath":"/img/widget-graph.png","serviceName":"s1-s2","added":false,"isShared":true}]');
      expect(controller.get('isMineWidgetsLoaded')).to.be.true;
    });

    it('should set mine widgets as empty array', function () {
      controller.set('isMineWidgetsLoaded', false);
      controller.loadMineWidgetsSuccessCallback({items: []});
      expect(controller.get('mineWidgets')).to.eql([]);
      expect(controller.get('isMineWidgetsLoaded')).to.be.true;
    });
  });

  describe('#addWidget', function () {

    it('should call ajax send request', function () {
      var event = {
        context: Em.Object.create({
          tag: '1',
          id: 2
        })
      };

      controller.set('activeNSWidgetLayouts', [
        Em.Object.create({
          id: 1,
          nameServiceId: '1',
          scope: 'CLUSTER',
          sectionName: 'sn1',
          layoutName: 'l1',
          displayName: 'w1',
          widgets: [
            Em.Object.create({
              id: 1
            })
          ]
        })
      ]);
      controller.addWidget(event);
      var args = testHelpers.findAjaxRequest('name', 'widget.layout.edit');
      expect(JSON.stringify(args[0].data)).to.be.equal('{"layoutId":1,"data":{"WidgetLayoutInfo":{"display_name":"w1","id":1,"layout_name":"l1","scope":"CLUSTER","section_name":"sn1","widgets":[{"id":1},{"id":2}]}}}');
      expect(event.context.get('added')).to.be.true;
    });
  });

  describe('#hideWidgetBrowser', function () {

    beforeEach(function () {
      sinon.spy(controller, 'hideWidget');
    });

    afterEach(function () {
      controller.hideWidget.restore();
    });

    it('should find and hide widgets from all layouts', function () {
      var event = {
        context: Em.Object.create({
          tag: '1',
          id: 1
        })
      };

      controller.set('activeNSWidgetLayouts', [
        Em.Object.create({
          widgets: [
            Em.Object.create({
              id: 1
            })
          ]
        })
      ]);
      controller.hideWidgetBrowser(event);
      expect(controller.hideWidget.calledOnce).to.be.true;
    });
  });

  describe('#updateActiveLayout', function () {

    beforeEach(function () {
      sinon.stub(controller, 'getActiveWidgetLayout');
    });

    afterEach(function () {
      controller.getActiveWidgetLayout.restore();
    });

    it('should call "getActiveWidgetLayout"', function () {
      controller.updateActiveLayout();
      expect(controller.getActiveWidgetLayout.calledOnce).to.be.true;
    });
  });

  describe('#deleteWidget', function () {

    beforeEach(function () {
      sinon.stub(App, 'showConfirmationFeedBackPopup', function(callback) {
        return callback()
      });
    });

    afterEach(function () {
      App.showConfirmationFeedBackPopup.restore();
    });

    it('should update widget browser content', function () {
      var event = {
        context: Em.Object.create({
          isShared: true,
          widgetName: 'w1',
          id: 1
        })
      };

      controller.deleteWidget(event);
      expect(App.showConfirmationFeedBackPopup.calledOnce).to.be.true;
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#updateWidgetBrowser', function () {

    beforeEach(function () {
      sinon.stub(controller, 'loadAllSharedWidgets');
      sinon.stub(controller, 'loadMineWidgets');
    });

    afterEach(function () {
      controller.loadAllSharedWidgets.restore();
      controller.loadMineWidgets.restore();
    });

    it('should update widget browser content', function () {
      controller.updateWidgetBrowser();
      expect(controller.loadAllSharedWidgets.calledOnce).to.be.true;
      expect(controller.loadMineWidgets.calledOnce).to.be.true;
    });
  });

  describe('#shareWidget', function () {

    beforeEach(function () {
      sinon.stub(App, 'showConfirmationFeedBackPopup', function(callback) {
        return callback()
      });
    });

    afterEach(function () {
      App.showConfirmationFeedBackPopup.restore();
    });

    it('should share widgets', function () {
      var event = {
        context: Em.Object.create({
          widgetName: 'w1',
          id: 1
        })
      };

      controller.shareWidget(event);
      expect(App.showConfirmationFeedBackPopup.calledOnce).to.be.true;
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#createWidget', function () {

    beforeEach(function () {
      sinon.stub(App.router, 'send');
    });

    afterEach(function () {
      App.router.send.restore();
    });

    it('should call router send request', function () {
      controller.set('activeWidgetLayout', {});
      controller.set('content', Em.Object.create({serviceName: 's1'}));
      controller.createWidget();
      expect(App.router.send.calledOnce).to.be.true;
    });
  });

  describe('#editWidget', function () {

    beforeEach(function () {
      sinon.stub(App.router, 'send');
    });

    afterEach(function () {
      App.router.send.restore();
    });

    it('should call router send request', function () {
      var content = Em.Object.create();

      controller.set('content', Em.Object.create({serviceName: 's1'}));
      controller.editWidget(content);
      expect(App.router.send.calledOnce).to.be.true;
      expect(content.get('serviceName')).to.equal('s1');
    });
  });

  describe('#goToWidgetsBrowser', function () {

    beforeEach(function () {
      sinon.stub(App.ModalPopup, 'show');
    });

    afterEach(function () {
      App.ModalPopup.show.restore();
    });

    it('should show modal popup', function () {
      controller.goToWidgetsBrowser();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });
});