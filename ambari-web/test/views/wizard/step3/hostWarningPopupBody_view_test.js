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
var lazyloading = require('utils/lazy_loading');
require('views/wizard/step3/hostWarningPopupBody_view');
var view;

describe('App.WizardStep3HostWarningPopupBody', function() {

  beforeEach(function() {
    view = App.WizardStep3HostWarningPopupBody.create({
      didInsertElement: Em.K,
      $: function() {
        return Em.Object.create({
          toggle: Em.K
        })
      }
    });
  });

  describe('#onToggleBlock', function() {
    it('should toggle', function() {
      var context = Em.Object.create({isCollapsed: false});
      view.onToggleBlock({context: context});
      expect(context.get('isCollapsed')).to.equal(true);
      view.onToggleBlock({context: context});
      expect(context.get('isCollapsed')).to.equal(false);
    });
  });

  describe('#showHostsPopup', function() {
    it('should call App.ModalPopup.show', function() {
      sinon.stub(App.ModalPopup, 'show', Em.K);
      view.showHostsPopup({context: []});
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
      App.ModalPopup.show.restore();
    });
  });

  describe('#categoryWarnings', function() {
    it('should return empty array', function() {
      var warningsByHost = null;
      view.reopen({warningsByHost: warningsByHost});
      expect(view.get('categoryWarnings')).to.eql([]);
    });
    it('should return filtered warnings', function() {
      var warningsByHost = [
        {name: 'c', warnings: [{}, {}, {}]},
        {name: 'd', warnings: [{}]}
      ];
      view.reopen({warningsByHost: warningsByHost, category: 'c'});
      expect(view.get('categoryWarnings.length')).to.equal(3);
    });
  });

  describe('#warningHostsNamesCount', function() {
    it('should parse warnings', function() {
      view.set('bodyController', Em.Object.create({
        repoCategoryWarnings: [
          {hostsNames: ['h1', 'h4']}
        ],
        thpCategoryWarnings: [
          {hostsNames: ['h2', 'h3']}
        ],
        jdkCategoryWarnings: [
          {hostsNames: ['h3', 'h5']}
        ],
        hostCheckWarnings: [
          {hostsNames: ['h1', 'h2']}
        ],
        diskCategoryWarnings: [
          {hostsNames: ['h2', 'h5']}
        ],
        warningsByHost: [
          {},
          { name: 'h1', warnings: [{}, {}, {}] },
          { name: 'h2', warnings: [{}, {}, {}] },
          { name: 'h3', warnings: [] }
        ]
      }));
      expect(view.warningHostsNamesCount()).to.equal(5);
    });
  });

  describe('#hostSelectView', function() {

    var v;

    beforeEach(function() {
      v = view.get('hostSelectView').create();
    });

    describe('#click', function() {
      Em.A([
          {
            isLoaded: false,
            isLazyLoading: true,
            e: true
          },
          {
            isLoaded: true,
            isLazyLoading: true,
            e: false
          },
          {
            isLoaded: false,
            isLazyLoading: false,
            e: false
          },
          {
            isLoaded: true,
            isLazyLoading: false,
            e: false
          }
        ]).forEach(function (test) {
          it('isLoaded: ' + test.isLoaded.toString() + ', isLazyLoading: ' + test.isLazyLoading.toString(), function () {
            v.reopen({
              isLoaded: test.isLoaded,
              isLazyLoading: test.isLazyLoading
            });
            sinon.spy(lazyloading, 'run');
            v.click();
            if (test.e) {
              expect(lazyloading.run.calledOnce).to.equal(true);
            }
            else {
              expect(lazyloading.run.called).to.equal(false);
            }
            lazyloading.run.restore();
          });
        });
    });

  });

  describe('#contentInDetails', function() {
    var content = [
      {category: 'firewall', warnings: [{name: 'n1'}, {name: 'n2'}, {name: 'n3'}]},
      {category: 'fileFolders', warnings: [{name: 'n4'}, {name: 'n5'}, {name: 'n6'}]},
      {category: 'reverseLookup', warnings: [{name: 'n19', hosts: ["h1"]}]},
      {
        category: 'process',
        warnings: [
          {name: 'n7', hosts:['h1', 'h2'], user: 'u1', pid: 'pid1'},
          {name: 'n8', hosts:['h2'], user: 'u2', pid: 'pid2'},
          {name: 'n9', hosts:['h3'], user: 'u1', pid: 'pid3'}
        ]
      },
      {category: 'package', warnings: [{name: 'n10'}, {name: 'n11'}, {name: 'n12'}]},
      {category: 'service', warnings: [{name: 'n13'}, {name: 'n14'}, {name: 'n15'}]},
      {category: 'user', warnings: [{name: 'n16'}, {name: 'n17'}, {name: 'n18'}]},
      {category: 'jdk', warnings: []},
      {category: 'disk', warnings: []},
      {category: 'repositories', warnings: []},
      {category: 'hostNameResolution', warnings: []},
      {category: 'thp', warnings: []}
    ];
    beforeEach(function() {
      view.reopen({content: content, warningsByHost: [], hostNamesWithWarnings: ['c', 'd']});
    });
    it('should map hosts', function() {
      var newContent = view.get('contentInDetails');
      expect(newContent.contains('c d')).to.equal(true);
    });
    it('should map firewall warnings', function() {
      var newContent = view.get('contentInDetails');
      expect(newContent.contains('n1<br>n2<br>n3')).to.equal(true);
    });
    it('should map fileFolders warnings', function() {
      var newContent = view.get('contentInDetails');
      expect(newContent.contains('n4 n5 n6')).to.equal(true);
    });
    it('should map process warnings', function() {
      var newContent = view.get('contentInDetails');
      expect(newContent.contains('(h1,u1,pid1)')).to.equal(true);
      expect(newContent.contains('(h2,u1,pid1)')).to.equal(true);
      expect(newContent.contains('(h2,u2,pid2)')).to.equal(true);
      expect(newContent.contains('(h3,u1,pid3)')).to.equal(true);
    });
    it('should map package warnings', function() {
      var newContent = view.get('contentInDetails');
      expect(newContent.contains('n10 n11 n12')).to.equal(true);
    });
    it('should map service warnings', function() {
      var newContent = view.get('contentInDetails');
      expect(newContent.contains('n13 n14 n15')).to.equal(true);
    });
    it('should map user warnings', function() {
      var newContent = view.get('contentInDetails');
      expect(newContent.contains('n16 n17 n18')).to.equal(true);
    });
    it('should map reverse lookup warnings', function() {
      var newContent = view.get('contentInDetails');
      expect(newContent.contains('h1')).to.equal(true);
    });
  });

});