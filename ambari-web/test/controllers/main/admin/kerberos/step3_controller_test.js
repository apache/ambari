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

describe('App.KerberosWizardStep3Controller', function() {
  
  describe('#onTestKerberosError', function() {
    var controller = App.KerberosWizardStep3Controller.create({});

    beforeEach(function(){
      sinon.stub(App.ajax, 'defaultErrorHandler', Em.K);
      sinon.stub(controller, 'onTaskError', Em.K);
    });

    afterEach(function(){
      App.ajax.defaultErrorHandler.restore();
      controller.onTaskError.restore();
    });

    it('should call App.ajax.defaultErrorHandler and onTaskError', function () {
      controller.onTestKerberosError({status: 3}, null, null, {url: 1, type: 2});
      expect(App.ajax.defaultErrorHandler.calledWith({status: 3}, 1, 2, 3)).to.be.true;
      expect(controller.onTaskError.calledOnce).to.be.true;
    });
  });

});