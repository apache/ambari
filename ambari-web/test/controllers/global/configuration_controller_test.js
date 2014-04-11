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
});