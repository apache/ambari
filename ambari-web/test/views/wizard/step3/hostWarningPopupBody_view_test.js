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
      {category: 'reverseLookup', warnings: [{name: 'n19', hosts: ["h1"], hostsLong: ["h1"]}]},
      {
        category: 'process',
        warnings: [
          {name: 'n7', hosts:['h1', 'h2'], hostsLong:['h1', 'h2'], user: 'u1', pid: 'pid1'},
          {name: 'n8', hosts:['h2'], hostsLong:['h2'], user: 'u2', pid: 'pid2'},
          {name: 'n9', hosts:['h3'], hostsLong:['h3'], user: 'u1', pid: 'pid3'}
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

  describe('#content', function () {

    it('should return array with warning objects', function () {
      view.set('bodyController', Em.Object.create({
        hostCheckWarnings: [
          {
            hosts: ['h0', 'h1', 'h2', 'h3', 'h4', 'h5', 'h5', 'h7', 'h8', 'h9', 'h10']
          }
        ],
        repoCategoryWarnings: [
          {
            hosts: ['h11', 'h12']
          }
        ],
        diskCategoryWarnings: [
          {
            hosts: ['h13']
          }
        ],
        jdkCategoryWarnings: [
          {
            hosts: ['h14']
          }
        ],
        thpCategoryWarnings: [
          {
            hosts: ['h15']
          }
        ]
      }));
      view.reopen({
        categoryWarnings: [
          {
            category: 'firewall',
            hosts: ['h16']
          },
          {
            category: 'firewall',
            hosts: ['h17']
          },
          {
            category: 'processes',
            hosts: ['h18']
          },
          {
            category: 'packages',
            hosts: ['h19']
          },
          {
            category: 'fileFolders',
            hosts: ['h20']
          },
          {
            category: 'services',
            hosts: ['h21']
          },
          {
            category: 'users',
            hosts: ['h22']
          },
          {
            category: 'misc',
            hosts: ['h23']
          },
          {
            category: 'alternatives',
            hosts: ['h24']
          },
          {
            category: 'reverseLookup',
            hosts: ['h25']
          },
          {
            category: 'reverseLookup',
            hosts: ['h26']
          },
          {
            category: 'reverseLookup',
            hosts: ['h27']
          },
          {
            category: 'reverseLookup',
            hosts: ['h28']
          },
          {
            category: 'reverseLookup',
            hosts: ['h29']
          },
          {
            category: 'reverseLookup',
            hosts: ['h30']
          },
          {
            category: 'reverseLookup',
            hosts: ['h31']
          },
          {
            category: 'reverseLookup',
            hosts: ['h32']
          },
          {
            category: 'reverseLookup',
            hosts: ['h33']
          },
          {
            category: 'reverseLookup',
            hosts: ['h34']
          },
          {
            category: 'reverseLookup',
            hosts: ['h35', 'h36']
          }
        ]
      });
      var content = view.get('content');
      expect(content.mapProperty('isCollapsed').uniq()).to.eql([true]);
      expect(content.findProperty('category', 'hostNameResolution').get('warnings')[0].hostsList).
        to.equal('h0<br>h1<br>h2<br>h3<br>h4<br>h5<br>h5<br>h7<br>h8<br>h9<br> ' + Em.I18n.t('installer.step3.hostWarningsPopup.moreHosts').format(1));
      expect(content.findProperty('category', 'repositories').get('warnings')[0].hostsList).to.equal('h11<br>h12');
      expect(content.findProperty('category', 'disk').get('warnings')[0].hostsList).to.equal('h13');
      expect(content.findProperty('category', 'jdk').get('warnings')[0].hostsList).to.equal('h14');
      expect(content.findProperty('category', 'thp').get('warnings')[0].hostsList).to.equal('h15');
      expect(content.findProperty('category', 'firewall').get('warnings').mapProperty('hostsList')).to.eql(['h16', 'h17']);
      expect(content.findProperty('category', 'process').get('warnings')[0].hostsList).to.equal('h18');
      expect(content.findProperty('category', 'package').get('warnings')[0].hostsList).to.equal('h19');
      expect(content.findProperty('category', 'fileFolders').get('warnings')[0].hostsList).to.equal('h20');
      expect(content.findProperty('category', 'service').get('warnings')[0].hostsList).to.equal('h21');
      expect(content.findProperty('category', 'user').get('warnings')[0].hostsList).to.equal('h22');
      expect(content.findProperty('category', 'misc').get('warnings')[0].hostsList).to.equal('h23');
      expect(content.findProperty('category', 'alternatives').get('warnings')[0].hostsList).to.equal('h24');
      expect(content.findProperty('category', 'reverseLookup').get('warnings').mapProperty('hostsList')).to.eql([
        'h25', 'h26', 'h27', 'h28', 'h29', 'h30', 'h31', 'h32', 'h33', 'h34', 'h35<br>h36'
      ]);
    });

  });

});