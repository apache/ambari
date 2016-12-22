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
require('controllers/global/configuration_controller');
var testHelpers = require('test/helpers');


describe('App.ConfigurationController', function () {
  var controller = App.ConfigurationController.create();

  describe('#checkTagsChanges()', function () {
    var testCases = [
      {
        title: 'Tags haven\'t been uploaded',
        content: {
          tags: [],
          storedTags: []
        },
        result: false
      },
      {
        title: 'New tag uploaded',
        content: {
          tags: [
            {
              siteName: 'site1',
              tagName: 1
            }
          ],
          storedTags: []
        },
        result: true
      },
      {
        title: 'Existing tag with with new tagName',
        content: {
          tags: [
            {
              siteName: 'site1',
              tagName: 1
            }
          ],
          storedTags: [
            {
              siteName: 'site1',
              tagName: 2
            }
          ]
        },
        result: true
      },
      {
        title: 'Tags with different tagNames',
        content: {
          tags: [
            {
              siteName: 'site1',
              tagName: 1
            }
          ],
          storedTags: [
            {
              siteName: 'site2',
              tagName: 1
            }
          ]
        },
        result: true
      },
      {
        title: 'One new tag uploaded',
        content: {
          tags: [
            {
              siteName: 'site2',
              tagName: 1
            },
            {
              siteName: 'site1',
              tagName: 1
            }
          ],
          storedTags: [
            {
              siteName: 'site2',
              tagName: 1
            }
          ]
        },
        result: true
      },
      {
        title: 'Tags haven\'t been changed',
        content: {
          tags: [
            {
              siteName: 'site2',
              tagName: 1
            }
          ],
          storedTags: [
            {
              siteName: 'site2',
              tagName: 1
            }
          ]
        },
        result: false
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        expect(controller.checkTagsChanges(test.content.tags, test.content.storedTags)).to.equal(test.result);
      });
    });
  });

  describe('#getConfigsByTags()', function() {

    beforeEach(function() {
      sinon.stub(App.db, 'getConfigs').returns([
        {
          type: 't1',
          tag: 'tag1'
        }
      ]);
      this.mockCheck = sinon.stub(controller, 'checkTagsChanges');
      sinon.stub(controller, 'loadFromServer');
      sinon.stub(controller, 'loadFromDB');
    });

    afterEach(function() {
      App.db.getConfigs.restore();
      this.mockCheck.restore();
      controller.loadFromServer.restore();
      controller.loadFromDB.restore();
    });

    it('checkTagsChanges should be called', function() {
      controller.getConfigsByTags([]);
      expect(controller.checkTagsChanges.calledWith([], [{
        siteName: 't1',
        tagName: 'tag1'
      }])).to.be.true;
    });

    it('loadFromServer should be called', function() {
      this.mockCheck.returns(true);
      controller.getConfigsByTags([]);
      expect(controller.loadFromServer.calledWith([])).to.be.true;
    });

    it('loadFromDB should be called', function() {
      this.mockCheck.returns(false);
      controller.getConfigsByTags([{siteName: 'site1'}]);
      expect(controller.loadFromDB.calledWith(['site1'])).to.be.true;
    });
  });

  describe('#loadFromServer()', function() {

    beforeEach(function() {
      sinon.stub(controller, 'loadConfigTags').returns({
        done: function(callback) {
          callback({
            Clusters: {
              desired_configs: {
                's1': {
                  siteName: 's1',
                  tag: 'tag2'
                }
              }
            }
          });
        }
      });
      sinon.stub(controller, 'loadConfigsByTags');
    });

    afterEach(function() {
      controller.loadConfigTags.restore();
      controller.loadConfigsByTags.restore();
    });

    it('tags data is correct', function() {
      expect(controller.loadFromServer([{tagName: 'tag1', siteName: 's1'}])).to.be.an.object;
      expect(controller.loadConfigsByTags.calledWith([{tagName: 'tag1', siteName: 's1'}])).to.be.true;
    });

    it('tags data is corrupted', function() {
      expect(controller.loadFromServer([{siteName: 's1'}])).to.be.an.object;
      expect(controller.loadConfigsByTags.calledWith([{tagName: 'tag2', siteName: 's1'}])).to.be.true;
    });
  });

  describe('#loadConfigsByTags()', function() {
    var dfd = {
      resolve: Em.K
    };

    beforeEach(function() {
      sinon.stub(App.config, 'loadConfigsByTags').returns({
        done: function(callback) {
          callback({items: [{}]});
          return {
            complete: Em.clb
          }
        }
      });
      sinon.stub(controller, 'saveToDB');
      sinon.spy(dfd, 'resolve');
      controller.loadConfigsByTags([], dfd);
    });

    afterEach(function() {
      App.config.loadConfigsByTags.restore();
      controller.saveToDB.restore();
      dfd.resolve.restore();
    });

    it('saveToDB should be called', function() {
      expect(controller.saveToDB.calledWith([{}])).to.be.true;
    });

    it('Deferred should be resolved', function() {
      expect(dfd.resolve.calledWith([{}])).to.be.true;
    });
  });

  describe('#loadConfigTags()', function() {

    it('App.ajax.send should be called', function() {
      controller.loadConfigTags();
      var args = testHelpers.findAjaxRequest('name', 'config.tags');
      expect(args).to.exist;
    });
  });

  describe('#saveToDB()', function() {

    beforeEach(function() {
      sinon.stub(App.db, 'getConfigs').returns([{
        type: 't1'
      }]);
      sinon.stub(App.db, 'setConfigs');
    });

    afterEach(function() {
      App.db.getConfigs.restore();
      App.db.setConfigs.restore();
    });

    it('App.db.setConfigs should be called', function() {
      var loadedConfigs = [
        {
          type: 't1',
          tag: 'tag1',
          properties: {},
          properties_attributes: {}
        },
        {
          type: 't2'
        }
      ];
      controller.saveToDB(loadedConfigs);
      expect(JSON.stringify(App.db.setConfigs.getCall(0).args[0])).to.be.equal(JSON.stringify([
        {
          type: 't1',
          tag: 'tag1',
          properties: {},
          properties_attributes: {}
        },
        {
          type: 't2'
        }
      ]));
    });
  });
});