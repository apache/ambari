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
require('views/main/service/info/summary');

describe('App.MainServiceInfoSummaryView', function() {

  var view = App.MainServiceInfoSummaryView.create({
    monitorsLiveTextView: Em.View.create(),
    controller: Em.Object.create({
      content: Em.Object.create({
        id: 'HDFS',
        serviceName: 'HDFS',
        hostComponents: []
      })
    }),
    alertsController: Em.Object.create()
  });

  describe('#servers', function () {
    it('services shuldn\'t have servers except FLUME and ZOOKEEPER', function () {
      expect(view.get('servers')).to.be.empty;
    });

    it('if one server exists then first server should have isComma and isAnd property false', function () {
      view.set('controller.content', Em.Object.create({
        id: 'ZOOKEEPER',
        serviceName: 'ZOOKEEPER',
        hostComponents: [
          Em.Object.create({
            displayName: '',
            isMaster: true
          })
        ]
      }));
      expect(view.get('servers').objectAt(0).isComma).to.equal(false);
      expect(view.get('servers').objectAt(0).isAnd).to.equal(false);
    });

    it('if more than one servers exist then first server should have isComma - true and isAnd - false', function () {
      view.set('controller.content', Em.Object.create({
        id: 'ZOOKEEPER',
        serviceName: 'ZOOKEEPER',
        hostComponents: [
          Em.Object.create({
            displayName: '',
            isMaster: true
          }),
          Em.Object.create({
            displayName: '',
            isMaster: true
          })
        ]
      }));
      expect(view.get('servers').objectAt(0).isComma).to.equal(true);
      expect(view.get('servers').objectAt(0).isAnd).to.equal(false);
      expect(view.get('servers').objectAt(1).isComma).to.equal(false);
      expect(view.get('servers').objectAt(1).isAnd).to.equal(false);
    });

    it('if more than two servers exist then second server should have isComma - false and isAnd - true', function () {
      view.set('controller.content', Em.Object.create({
        id: 'ZOOKEEPER',
        serviceName: 'ZOOKEEPER',
        hostComponents: [
          Em.Object.create({
            displayName: '',
            isMaster: true
          }),
          Em.Object.create({
            displayName: '',
            isMaster: true
          }),
          Em.Object.create({
            displayName: '',
            isMaster: true
          })
        ]
      }));
      expect(view.get('servers').objectAt(0).isComma).to.equal(true);
      expect(view.get('servers').objectAt(0).isAnd).to.equal(false);
      expect(view.get('servers').objectAt(1).isComma).to.equal(false);
      expect(view.get('servers').objectAt(1).isAnd).to.equal(true);
      expect(view.get('servers').objectAt(2).isComma).to.equal(false);
      expect(view.get('servers').objectAt(2).isAnd).to.equal(false);
    });

  });

  describe('#hasAlertDefinitions', function () {

    beforeEach(function () {
      sinon.stub(App.AlertDefinition, 'getAllDefinitions', function () {
        return [
          {
            serviceName: 'HDFS'
          },
          {
            serviceName: 'YARN'
          }
        ];
      });
    });

    afterEach(function () {
      App.AlertDefinition.getAllDefinitions.restore();
    });

    it('should return true if at least one alert definition for this service exists', function () {
      view.set('controller.content', Em.Object.create({
        serviceName: 'HDFS'
      }));
      expect(view.get('hasAlertDefinitions')).to.be.true;

      it('should return false if there is no alert definition for this service', function () {
        view.set('controller.content', Em.Object.create({
          serviceName: 'ZOOKEEPER'
        }));
        expect(view.get('hasAlertDefinitions')).to.be.false;
      });
    })
  });
});