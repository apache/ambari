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

var ui_utils = require('utils/ui_effects');
var App = require('app');

describe('utils/ui_effects', function(){
  describe('#pulsate()', function(){
    beforeEach(function(){
      $('body').append('<div id="pulsate-test-dom"></div>');
      this.clock = sinon.useFakeTimers();
    });

    it('opacity should be 0.2 on 5-th iteration', function() {
      var domEl = $('#pulsate-test-dom');
      ui_utils.pulsate(domEl, 1000);
      this.clock.tick(300);
      expect(parseFloat(domEl.css('opacity')).toFixed(1)).to.eql('0.2');
    });
    it('should call callback at the end', function() {
      var domEl = $('#pulsate-test-dom');
      var stub = sinon.stub();
      ui_utils.pulsate(domEl, 1000, stub);
      this.clock.tick(2000);
      expect(stub.calledOnce).to.be.ok;
    });

    afterEach(function(){
      $('#pulsate-test-dom').remove();
      this.clock.restore();
    });
  });
});
