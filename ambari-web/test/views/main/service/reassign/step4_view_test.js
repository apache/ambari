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

describe('App.ReassignMasterWizardStep4View', function () {
  var view;

  beforeEach(function () {
    view = App.ReassignMasterWizardStep4View.create({});
  });

  describe('#noticeCompleted', function () {
    it('should return formatted services.reassign.step4.status.success.withManualSteps msg if hasManualSteps is true', function () {
      view.set('controller', Em.Object.create({
        content: {
          reassign: {
            component_name: 'test'
          },
          hasManualSteps: true
        }
      }));
      expect(view.get('noticeCompleted')).to.be.equal(Em.I18n.t('services.reassign.step4.status.success.withManualSteps').format('test'));
    });

    it('should return formatted services.reassign.step4.status.success msg if hasManualSteps is false', function () {
      view.set('controller', Em.Object.create({
        content: {
          reassign: {
            component_name: 'test'
          },
          hasManualSteps: false
        }
      }));
      expect(view.get('noticeCompleted')).to.be.equal(Em.I18n.t('services.reassign.step4.status.success').format('Test'));
    });
  });
});