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

var modelSetup = require('test/init_model_test');
require('models/job');

var job,
  jobData = {
   id: 'job'
  };

describe('App.Job', function () {

  beforeEach(function () {
    job = App.Job.createRecord(jobData);
  });

  afterEach(function () {
    modelSetup.deleteRecord(job);
  });

  describe('#duration', function () {
    it('should convert elapsedTime into time format', function () {
      job.set('elapsedTime', 1000);
      expect(job.get('duration')).to.equal('1.00 secs');
    });
  });

  describe('#inputFormatted', function () {
    it('should convert input into bandwidth format', function () {
      job.set('input', 1024);
      expect(job.get('inputFormatted')).to.equal('1.0KB');
    });
  });

  describe('#outputFormatted', function () {
    it('should convert output into bandwidth format', function () {
      job.set('output', 1024);
      expect(job.get('outputFormatted')).to.equal('1.0KB');
    });
  });

});
