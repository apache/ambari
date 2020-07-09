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
require('views/wizard/step7/credentials_tab_view');
var view;

function getView() {
  return App.CredentialsTabOnStep7View.create({
    controller: Em.Object.create()
  });
}

var tab = Em.Object.create({
  name: 'credentials',
  sections: Em.A([
    Em.Object.create({
      name: 'credentials',
      subSections: [
        Em.Object.create({
          configProperties: [
            {name: 'name1'},
            {name: 'name2'}
          ],
          displayName: 'C1'
        })
      ]
    }),
  ]),
  isCategorized: true
});

var stepConfigs = Em.A([
  Em.Object.create({
    serviceName: 'S1',
    configs: Em.A([
      Em.Object.create({
        name: 'name1',
        displayType: 'password'
      }),
        Em.Object.create({
        name: 'name2',
        displayType: 'username'
      })
    ])
  })
]);

describe('App.CredentialsTabOnStep7View', function () {

  beforeEach(function () {
    view = getView();
  });

  describe("#didInsertElement", function () {

    beforeEach(function () {
      sinon.stub(view, 'setRows');
    });

    afterEach(function () {
      view.setRows.restore();
    });

    it("should call setRows()", function () {
      view.didInsertElement();
      expect(view.setRows.calledOnce).to.be.true;
    });
  });

  describe("#updateNextDisabled", function () {

    it("should update appropriate credentialsTabNextEnabled flag with true", function () {
      view.set('rows', Em.A([
        {
          passwordProperty: {
            error: false
          },
          usernameProperty: {
            error: false
          }
        }
      ]));
      view.updateNextDisabled();
      expect(view.get('controller.credentialsTabNextEnabled')).to.be.true;
    });

    it("should update appropriate credentialsTabNextEnabled flag with false", function () {
      view.set('rows', Em.A([
        {
          passwordProperty: {
            error: true
          },
          usernameProperty: {
            error: false
          }
        }
      ]));
      view.updateNextDisabled();
      expect(view.get('controller.credentialsTabNextEnabled')).to.be.false;
    });
  });

  describe("#setRows", function () {

    beforeEach(function () {
      sinon.stub(App.Tab, 'find').returns([tab]);
      sinon.stub(App.configsCollection, 'getConfig', function(id){
        if (id.name === 'name1') {
          return Em.Object.create({
            serviceName: 'S1',
            name: 'name1'
          });
        }
        return Em.Object.create({
          serviceName: 'S1',
          name: 'name2'
        });
      })
    });

    afterEach(function () {
      App.Tab.find.restore();
      App.configsCollection.getConfig.restore();
    });

    it("should set rows and properties", function () {
      view.set('controller.stepConfigs', stepConfigs);
      view.set('controller.stepConfigsCreated', true);
      view.setRows();
      expect(view.get('rows').mapProperty('displayName')[0]).to.equal('C1');
      expect(view.get('rows').mapProperty('passwordProperty')[0]).to.eql(Em.Object.create({
        name: 'name1',
        displayType: 'password'
      }));
      expect(view.get('rows').mapProperty('usernameProperty')[0]).to.eql(Em.Object.create({
        name: 'name2',
        displayType: 'username'
      }));
      expect(view.get('properties').mapProperty('displayType')).to.eql(['password', 'username']);
    });

    it("shouldn't set rows and properties", function () {
      view.set('controller.stepConfigsCreated', false);
      view.setRows();
      expect(view.get('rows')).to.be.empty;
      expect(view.get('properties')).to.be.empty;
    });
  });

});