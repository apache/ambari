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

var view;

describe('App.FormatDateView', function () {

  beforeEach(function () {
    view = App.FormatDateView.create();
  });

  describe('#result', function () {
    Em.A([
      {content: new Date(2018, 1, 13), expected: "Tue, Feb 13, 2018 00:00"},
      {content: new Date(2018, 1, 13), format: "D MMM YYYY", expected: "13 Feb 2018"}
    ]).forEach(function (test) {
      var message = 'content: {0}, format: {1}, expected: {2}'.format(JSON.stringify(test.content), JSON.stringify(test.format), JSON.stringify(test.expected));
      it(message, function () {
        view.set('content', test.content);
        view.set('format', test.format);
        expect(view.get('result')).to.be.equal(test.expected);
      });
    });

  });

});