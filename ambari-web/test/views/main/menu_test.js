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
require('views/main/menu');

var mainMenuView = App.MainMenuView.create();
describe('App.MainMenuView', function () {

  describe('#itemViewClass', function () {

    beforeEach(function () {
      mainMenuView.reopen({
        content: [
          mainMenuView.get('itemViewClass').create({
            content: {
              routing: 'dashboard'
            }
          }),
          mainMenuView.get('itemViewClass').create({
            content: {
              routing: 'admin'
            }
          })
        ]
      });
    });

    describe('#dropdownCategories', function () {

      var cases = [
        {
          itemName: 'dashboard',
          dropdownCategories: [],
          title: 'not Admin item'
        },
        {
          itemName: 'admin',
          isHadoopWindowsStack: true,
          dropdownCategories: [
            {
              name: 'stackAndUpgrade',
              url: 'stack',
              label: Em.I18n.t('admin.stackUpgrade.title')
            },
            {
              name: 'adminServiceAccounts',
              url: 'serviceAccounts',
              label: Em.I18n.t('common.serviceAccounts')
            }
          ],
          title: 'Admin item, HDPWIN'
        },
        {
          itemName: 'admin',
          isHadoopWindowsStack: false,
          dropdownCategories: [
            {
              name: 'stackAndUpgrade',
              url: 'stack',
              label: Em.I18n.t('admin.stackUpgrade.title')
            },
            {
              name: 'adminServiceAccounts',
              url: 'serviceAccounts',
              label: Em.I18n.t('common.serviceAccounts')
            },
            {
              name: 'kerberos',
              url: 'kerberos/',
              label: Em.I18n.t('common.kerberos')
            }
          ],
          title: 'Admin item, not HDPWIN'
        }
      ];

      afterEach(function () {
        App.get.restore();
      });

      cases.forEach(function (item) {
        it(item.title, function () {
          sinon.stub(App, 'get').withArgs('isHadoopWindowsStack').returns(item.isHadoopWindowsStack);
          var menuItem = mainMenuView.get('content').findProperty('content.routing', item.itemName);
          menuItem.propertyDidChange('dropdownCategories');
          expect(menuItem.get('dropdownCategories')).to.eql(item.dropdownCategories);
        });
      });

    });

  });

});
