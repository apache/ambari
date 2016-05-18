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
require('/views/main/service/widgets/create/step3_view');


describe('App.WidgetWizardStep3View', function () {
  var view;

  beforeEach(function() {
    view = App.WidgetWizardStep3View.create({
      controller: Em.Object.create({
        initPreviewData: Em.K
      })
    });
  });

  describe("#didInsertElement()", function() {

    beforeEach(function () {
      sinon.stub(view.get('controller'), 'initPreviewData');
    });
    afterEach(function () {
      view.get('controller').initPreviewData.restore();
    });

    it("initPreviewData should be called", function() {
      view.didInsertElement();
      expect(view.get('controller').initPreviewData.calledOnce).to.be.true;
    });
  });
});
