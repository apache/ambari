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

describe('App.KerberosWizardController', function() {
  var controller = App.KerberosWizardController.create({});

  describe('#warnBeforeExitPopup()', function () {
    beforeEach(function () {
      sinon.stub(App, "showConfirmationPopup", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
    });
    it('should open warning confirmation popup', function () {
      var f = Em.K;
      controller.warnBeforeExitPopup(f, false);
      expect(App.showConfirmationPopup.calledWith(f, Em.I18n.t('admin.kerberos.wizard.exit.warning.msg'), null, null, Em.I18n.t('common.exitAnyway'), false)).to.be.true;
    });

    it('should open critical confirmation popup', function () {
      var f = Em.K;
      controller.warnBeforeExitPopup(f, true);
      expect(App.showConfirmationPopup.calledWith(f, Em.I18n.t('admin.kerberos.wizard.exit.critical.msg'), null, null, Em.I18n.t('common.exitAnyway'), true)).to.be.true;
    });
  });

});


