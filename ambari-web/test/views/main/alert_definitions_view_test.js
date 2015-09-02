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
require('messages');
require('views/main/alert_definitions_view');

var view;

describe('App.MainAlertDefinitionsView', function () {

  beforeEach(function () {
    view = App.MainAlertDefinitionsView.create({});
  });

  describe('#serviceFilterView', function () {
    it('Add Ambari service to filters', function () {
      var serviceFilterClass = view.serviceFilterView;
      var content = serviceFilterClass.create({}).get('content');
      expect(content[0].label==Em.I18n.t('common.all'));
      expect(content[content.length-1].label==Em.I18n.t('app.name'));
    });
  });

  describe('#willInsertElement', function () {

    beforeEach(function(){
      sinon.stub(view, 'clearFilterConditionsFromLocalStorage', Em.K);
      sinon.stub(view, 'clearStartIndex', Em.K);
    });

    afterEach(function(){
      view.clearFilterConditionsFromLocalStorage.restore();
      view.clearStartIndex.restore();
    });

    it('should call clearFilterCondition, clearStartIndex if controller.showFilterConditionsFirstLoad is false', function () {
      view.set('controller', {showFilterConditionsFirstLoad: false, content: []});
      view.willInsertElement();
      expect(view.clearFilterConditionsFromLocalStorage.calledOnce).to.be.true;
      expect(view.clearStartIndex.calledOnce).to.be.true;
    });

    it('should not call clearFilterCondition, clearStartIndex if controller.showFilterConditionsFirstLoad is true', function () {
      view.set('controller', {showFilterConditionsFirstLoad: true, content: []});
      view.willInsertElement();
      expect(view.clearFilterConditionsFromLocalStorage.called).to.be.false;
      expect(view.clearStartIndex.called).to.be.false;
    });
  });

});
