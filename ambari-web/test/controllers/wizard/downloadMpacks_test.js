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
var controller = App.WizardDownloadMpacksController.create();

describe('App.WizardConfigureDownloadController', function () {
  beforeEach(function () {
    var mpacks = [
      Em.Object.create({
        name: 'alpha',
        succeeded: false,
        failed: false,
        inProgress: true
      }),
      Em.Object.create({
        name: 'bravo',
        succeeded: false,
        failed: false,
        inProgress: true
      })
    ];

    controller.set('mpacks', mpacks);
  })

  describe('#downloadMpackSuccess', function () {
    it('Sets succeeded to true, failed to false, and inProgress to false', function () {
      var expected = Em.Object.create({
        name: 'alpha',
        succeeded: true,
        failed: false,
        inProgress: false
      });

      controller.downloadMpackSuccess({ status: 200 }, null, { name: 'alpha' });
      var actual = controller.get('mpacks').objectAt(0);
      
      expect(actual).to.deep.equal(expected);
    });
  });

  describe('#downloadMpackError', function () {
    it('Sets succeeded to false, failed to true, and inProgress to false', function () {
      var expected = Em.Object.create({
        name: 'alpha',
        succeeded: false,
        failed: true,
        inProgress: false
      });

      controller.downloadMpackError({ status: 500 }, null, null, null, { name: 'alpha' });
      var actual = controller.get('mpacks').objectAt(0);

      expect(actual).to.deep.include(expected);
    });

    it('Sets succeeded to true, failed to false, and inProgress to false on 409 response', function () {
      var expected = Em.Object.create({
        name: 'alpha',
        succeeded: true,
        failed: false,
        inProgress: false
      });

      controller.downloadMpackError({ status: 409 }, null, null, null, { name: 'alpha' });
      var actual = controller.get('mpacks').objectAt(0);

      expect(actual).to.deep.equal(expected);
    });
  });

  describe('#retryDownload', function () {
    it('Retries the download and sets the status flags correctly if the download failed.', function () {
      var expected = Em.Object.create({
        succeeded: false,
        failed: false,
        inProgress: true
      });

      var actual = Em.Object.create({
        succeeded: false,
        failed: true,
        inProgress: false
      });

      sinon.stub(controller, 'downloadMpack');

      controller.retryDownload({ context: actual });

      expect(actual).to.deep.equal(expected);
      expect(controller.downloadMpack).to.be.called;

      controller.downloadMpack.restore();
    })
  });

  describe('#showError', function () {
    it('Displays the error if the download failed.', function () {
      var mpack = Em.Object.create({
        failed: true
      });

      sinon.stub(App.ModalPopup, 'show');

      controller.showError({ context: mpack });

      expect(App.ModalPopup.show).to.be.called;

      App.ModalPopup.show.restore();
    })
  });
});