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

describe('App.MainServiceItemView', function () {
  var view;

  beforeEach(function () {
    view = App.ReassignMasterWizardStep2View.create({});
  });

  describe('#alertMessage', function () {
    it('should return services.reassign.step2.body if component_name is not equal to NAMENODE or isHAEnabled is false', function () {
      view.set('controller', Em.Object.create({
        content: {
          reassign: {
            display_name: 'test'
          }
        }
      }));
      expect(view.get('alertMessage')).to.be.equal(Em.I18n.t('services.reassign.step2.body').format('test'));
    });

    it('should return services.reassign.step2.body if component_name is not equal to NAMENODE or isHAEnabled is false', function () {
      view.set('controller', Em.Object.create({
        content: {
          reassign: {
            display_name: 'test_display',
            component_name: 'NAMENODE'
          }
        }
      }));
      sinon.stub(App, 'get').returns(true);
      expect(view.get('alertMessage')).to.be.equal(Em.I18n.t('services.reassign.step2.body.namenodeHA').format('test_display'));
      App.get.restore();
    });
  });
});