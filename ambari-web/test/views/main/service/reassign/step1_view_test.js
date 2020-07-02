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
require('/views/main/service/reassign/step1_view');
var stringUtils = require('utils/string_utils');

describe('App.ReassignMasterWizardStep1View', function () {
  var view;

  beforeEach(function() {
    view = App.ReassignMasterWizardStep1View.create({
      controller: Em.Object.create({
        content: Em.Object.create({
          reassignHosts: Em.Object.create(),
          reassign: Em.Object.create()
        }),
        target: Em.Object.create({
          reassignMasterController: Em.Object.create({
            relatedServicesMap: Em.Object.create()
          })
        })
      })
    });
  });

  describe("#message", function() {

    beforeEach(function () {
      sinon.stub(App.Service, 'find').returns([{serviceName: 'HDFS'},{serviceName: 'HIVE'},{serviceName: 'YARN'}]);
    });
    afterEach(function () {
      App.Service.find.restore();
    });

    it("should return proper message", function() {
      view.set('controller.content.componentsToStopAllServices', ['NAMENODE']);
      view.set('controller.target.reassignMasterController.relatedServicesMap', {NAMENODE:[]});
      view.set('controller.content.reassign.component_name', 'NAMENODE');
      view.set('controller.content.reassign.display_name', 'NameNode');
      view.set('controller.content.hasManualSteps', true);
      view.propertyDidChange('message');
      expect(view.get('message')).to.eql([
        Em.I18n.t('services.reassign.step1.message1').format('NameNode'),
        Em.I18n.t('services.reassign.step1.message2').format('NameNode'),
        Em.I18n.t('services.reassign.step1.message3').format(stringUtils.getFormattedStringFromArray(['HDFS', 'HIVE', 'YARN']), 'NameNode')
      ]);
    });

    it("should return proper message without manual steps", function() {
      view.set('controller.content.componentsToStopAllServices', ['NAMENODE']);
      view.set('controller.target.reassignMasterController.relatedServicesMap', {NAMENODE:[]});
      view.set('controller.content.reassign.component_name', 'NAMENODE');
      view.set('controller.content.reassign.display_name', 'NameNode');
      view.set('controller.content.hasManualSteps', false);
      view.propertyDidChange('message');
      expect(view.get('message')).to.eql([
        Em.I18n.t('services.reassign.step1.message1').format('NameNode'),
        Em.I18n.t('services.reassign.step1.message3').format(stringUtils.getFormattedStringFromArray(['HDFS', 'HIVE', 'YARN']), 'NameNode')
      ]);
    });

    it("should return proper message with component not in componentsToStopAllServices list", function() {
      view.set('controller.content.componentsToStopAllServices', ['JORNALNODE']);
      view.set('controller.target.reassignMasterController.relatedServicesMap', {NAMENODE:[]});
      view.set('controller.content.reassign.component_name', 'NAMENODE');
      view.set('controller.content.reassign.display_name', 'NameNode');
      view.set('controller.content.hasManualSteps', false);
      view.propertyDidChange('message');
      expect(view.get('message')).to.eql([
        Em.I18n.t('services.reassign.step1.message1').format('NameNode'),
        Em.I18n.t('services.reassign.step1.message3').format(stringUtils.getFormattedStringFromArray(['HIVE', 'YARN']), 'NameNode')
      ]);
    });

    it("should return proper message with component has services in relatedServicesMap list", function() {
      view.set('controller.content.componentsToStopAllServices', ['JORNALNODE']);
      view.set('controller.target.reassignMasterController.relatedServicesMap', {NAMENODE:['HIVE']});
      view.set('controller.content.reassign.component_name', 'NAMENODE');
      view.set('controller.content.reassign.display_name', 'NameNode');
      view.set('controller.content.hasManualSteps', false);
      view.propertyDidChange('message');
      expect(view.get('message')).to.eql([
        Em.I18n.t('services.reassign.step1.message1').format('NameNode'),
        Em.I18n.t('services.reassign.step1.message3').format(stringUtils.getFormattedStringFromArray(['HIVE']), 'NameNode')
      ]);
    });
  });

});
