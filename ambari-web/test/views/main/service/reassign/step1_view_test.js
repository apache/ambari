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
var stringUtils = require('utils/string_utils');

describe('App.MainServiceItemView', function () {
  var view;

  beforeEach(function() {
    view = App.ReassignMasterWizardStep1View.create({});
  });

  describe('#message', function () {
    var listOfServices = [
      Em.Object.create({serviceName: 'test'}),
      Em.Object.create({serviceName: 'test2'}),
      Em.Object.create({serviceName: 'HDFS'})
    ];
    it('should return 2 msgs if componentsToStopAllServices contains componentName and hasManualSteps is false with installed services', function () {
      view.set('controller', Em.Object.create({
        content: Em.Object.create({
          reassign: Em.Object.create({component_name: 'test', display_name: 'test_display'}),
          componentsToStopAllServices: ['test'],
          hasManualSteps: false
        })
      }));
      sinon.stub(App.Service, 'find').returns(listOfServices);
      expect(view.get('message')).to.be.eql([
        Em.I18n.t('services.reassign.step1.message1').format('test_display'),
        Em.I18n.t('services.reassign.step1.message3').format(
          stringUtils.getFormattedStringFromArray(listOfServices.mapProperty('serviceName')),
          'test_display')
      ]);
      App.Service.find.restore();
    });

    it('should return 3 msgs if componentsToStopAllServices contains componentName and hasManualSteps is true with installed services', function () {
      view.set('controller', Em.Object.create({
        content: Em.Object.create({
          reassign: Em.Object.create({component_name: 'test', display_name: 'test_display'}),
          componentsToStopAllServices: ['test'],
          hasManualSteps: true
        })
      }));
      sinon.stub(App.Service, 'find').returns(listOfServices);
      expect(view.get('message')).to.be.eql([
        Em.I18n.t('services.reassign.step1.message1').format('test_display'),
        Em.I18n.t('services.reassign.step1.message2').format('test_display'),
        Em.I18n.t('services.reassign.step1.message3').format(
          stringUtils.getFormattedStringFromArray(listOfServices.mapProperty('serviceName')),
          'test_display')
      ]);
      App.Service.find.restore();
    });

    it('should return 2 msgs if componentsToStopAllServices contains componentName and hasManualSteps is false with installed services without HDFS', function () {
      view.set('controller', Em.Object.create({
        content: Em.Object.create({
          reassign: Em.Object.create({component_name: 'test', display_name: 'test_display'}),
          componentsToStopAllServices: [],
          hasManualSteps: false
        }),
        target: Em.Object.create({
          reassignMasterController: Em.Object.create({
            relatedServicesMap: {
              'test': []
            }
          })
        })
      }));

      sinon.stub(App.Service, 'find').returns(listOfServices);
      expect(view.get('message')).to.be.eql([
        Em.I18n.t('services.reassign.step1.message1').format('test_display'),
        Em.I18n.t('services.reassign.step1.message3').format(
          stringUtils.getFormattedStringFromArray(listOfServices.slice(0, 2).mapProperty('serviceName')),
          'test_display')
      ]);
      App.Service.find.restore();
    });

    it('should return 3 msgs if componentsToStopAllServices contains componentName and hasManualSteps is false with installed services without HDFS', function () {
      view.set('controller', Em.Object.create({
        content: Em.Object.create({
          reassign: Em.Object.create({component_name: 'test', display_name: 'test_display'}),
          componentsToStopAllServices: [],
          hasManualSteps: true
        }),
        target: Em.Object.create({
          reassignMasterController: Em.Object.create({
            relatedServicesMap: {
              'test': []
            }
          })
        })
      }));

      sinon.stub(App.Service, 'find').returns(listOfServices);
      expect(view.get('message')).to.be.eql([
        Em.I18n.t('services.reassign.step1.message1').format('test_display'),
        Em.I18n.t('services.reassign.step1.message2').format('test_display'),
        Em.I18n.t('services.reassign.step1.message3').format(
          stringUtils.getFormattedStringFromArray(listOfServices.slice(0, 2).mapProperty('serviceName')),
          'test_display')
      ]);
      App.Service.find.restore();
    });

    it('should return 2 msgs if componentsToStopAllServices contains componentName and hasManualSteps is false with installed services without HDFS', function () {
      view.set('controller', Em.Object.create({
        content: Em.Object.create({
          reassign: Em.Object.create({component_name: 'test', display_name: 'test_display'}),
          componentsToStopAllServices: [],
          hasManualSteps: false
        }),
        target: Em.Object.create({
          reassignMasterController: Em.Object.create({
            relatedServicesMap: {
              'test': ['HDFS', 'test2']
            }
          })
        })
      }));

      sinon.stub(App.Service, 'find').returns(listOfServices);
      expect(view.get('message')).to.be.eql([
        Em.I18n.t('services.reassign.step1.message1').format('test_display'),
        Em.I18n.t('services.reassign.step1.message3').format(
          stringUtils.getFormattedStringFromArray(['HDFS', 'test2']),
          'test_display')
      ]);
      App.Service.find.restore();
    });
  });
});