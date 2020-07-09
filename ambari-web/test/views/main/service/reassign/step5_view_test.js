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
require('/views/main/service/reassign/step5_view');


describe('App.ReassignMasterWizardStep5View', function () {
  var view;

  beforeEach(function() {
    view = App.ReassignMasterWizardStep5View.create({
      controller: Em.Object.create({
        content: Em.Object.create({
          reassignHosts: Em.Object.create(),
          reassign: Em.Object.create(),
          configs: Em.Object.create({
            'yarn-env': Em.Object.create(),
            'yarn-site': Em.Object.create()
          })
        }),
      })
    });
  });

  describe("#manualCommands", function() {

    beforeEach(function () {
      this.stackServiceStub = sinon.stub(App.StackService, 'find');
      this.haEnabled = sinon.stub(App, 'get');
    });

    afterEach(function () {
      this.stackServiceStub.restore();
      this.haEnabled.restore();
    });

    it("should return empty string if component not in componentsWithManualCommands list", function() {
      view.set('controller.content.reassign.component_name', 'NAMENODE');
      view.set('controller.content.componentsWithManualCommands', ['JOURNALNODE']);
      expect(view.get('manualCommands')).to.equal('');
    });

    it("should return formatted string for NAMENODE with HA enabled", function() {
      this.stackServiceStub.returns(Em.Object.create({
        serviceName: 'YARN',
        serviceVersion: '1.0',
        compareCurrentVersion: App.StackService.proto().compareCurrentVersion
      }));
      this.haEnabled.returns(true);
      view.set('controller.content.reassignHosts.source', 'host1');
      view.set('controller.content.reassignHosts.target', 'host2');
      view.set('controller.content.reassign.component_name', 'NAMENODE');
      view.set('controller.content.componentsWithManualCommands', ['JOURNALNODE', 'NAMENODE']);
      view.set('controller.content.hdfsUser', 'user1');
      view.set('controller.content.masterComponentHosts', [{
        component: 'NAMENODE',
        isInstalled: true,
        hostName: 'host1'
      }]);
      expect(view.get('manualCommands')).to.equal('<div class="alert alert-info"><ol><li>Login to the NameNode host <b></b>.</li><li>Reset automatic failover information in ZooKeeper by running:<div class="code-snippet">sudo su user1 -l -c \'hdfs zkfc -formatZK\'</div></li></ol></div><div class="alert alert-info"><ol start="3"><li>Login to the newly installed NameNode host <b>host2</b>.<br><div class="alert alert-warning"><strong>Important!</strong> Be sure to login to the newly installed NameNode host.<br>This is a different host from the Steps 1 and 2 above.</div></li><li>Initialize the metadata by running:<div class=\'code-snippet\'>sudo su user1 -l -c \'hdfs namenode -bootstrapStandby\'</div></li></ol></div>');
    });

    it("should return formatted string for NAMENODE with HA disabled", function() {
      this.stackServiceStub.returns(Em.Object.create({
        serviceName: 'YARN',
        serviceVersion: '1.0',
        compareCurrentVersion: App.StackService.proto().compareCurrentVersion
      }));
      this.haEnabled.returns(false);
      view.set('controller.content.reassignHosts.source', 'host1');
      view.set('controller.content.reassignHosts.target', 'host2');
      view.set('controller.content.reassign.component_name', 'NAMENODE');
      view.set('controller.content.componentsWithManualCommands', ['JOURNALNODE', 'NAMENODE']);
      view.set('controller.content.hdfsUser', 'user1');
      view.set('controller.content.masterComponentHosts', [{
        component: 'NAMENODE',
        isInstalled: true,
        hostName: 'host1'
      }]);
      expect(view.get('manualCommands')).to.equal('<div class="alert alert-info"><ol><li>Copy the contents of <b></b> on the source host <b>host1</b> to <b></b> on the target host <b>host2</b>.</li><li>Login to the target host <b>host2</b> and change permissions for the NameNode dirs by running:<div class="code-snippet">chown -R user1:{5} </div></li><li>Create marker directory by running:<div class="code-snippet">mkdir -p /var/lib/hdfs/namenode/formatted</div></li></ol></div>');
    });

    it("should return formatted string for APP_TIMELINE_SERVER with Yarn version < 2.7", function() {
      this.stackServiceStub.returns(Em.Object.create({
        serviceName: 'YARN',
        serviceVersion: '1.0',
        compareCurrentVersion: App.StackService.proto().compareCurrentVersion
      }));
      view.set('controller.content.reassignHosts.source', 'host1');
      view.set('controller.content.reassignHosts.target', 'host2');
      view.set('controller.content.reassign.component_name', 'APP_TIMELINE_SERVER');
      view.set('controller.content.componentsWithManualCommands', ['APP_TIMELINE_SERVER', 'NAMENODE']);
      view.set('controller.content.hdfsUser', 'user1');
      view.set('controller.content.configs.yarn-env.yarn_user', 'user2');
      view.set('controller.content.configs.yarn-site', {'yarn.timeline-service.leveldb-timeline-store.path': '/path'});
      expect(view.get('manualCommands')).to.equal('<div class="alert alert-info"><ol><li>Copy <b>/path/leveldb-timeline-store.ldb</b> from the source host <b>host1</b> to <b>/path/leveldb-timeline-store.ldb</b> on the target host <b>host2</b>.</li><li>Login to the target host <b>host2</b> and change permissions by running:<div class="code-snippet">chown -R user2:{5} /path/leveldb-timeline-store.ldb</div></li><div class="code-snippet">chmod -R 700 /path/leveldb-timeline-store.ldb</div></li></ol></div>');
    });

    it("should return formatted string for APP_TIMELINE_SERVER with Yarn version > 2.7", function() {
      this.stackServiceStub.returns(Em.Object.create({
        serviceName: 'YARN',
        serviceVersion: '3.0',
        compareCurrentVersion: App.StackService.proto().compareCurrentVersion
      }));
      view.set('controller.content.reassignHosts.source', 'host1');
      view.set('controller.content.reassignHosts.target', 'host2');
      view.set('controller.content.reassign.component_name', 'APP_TIMELINE_SERVER');
      view.set('controller.content.componentsWithManualCommands', ['APP_TIMELINE_SERVER', 'NAMENODE']);
      view.set('controller.content.hdfsUser', 'user1');
      view.set('controller.content.configs.yarn-env.yarn_user', 'user2');
      view.set('controller.content.configs.yarn-site', {'yarn.timeline-service.leveldb-timeline-store.path': '/path'});
      expect(view.get('manualCommands')).to.equal('<div class="alert alert-info"><ol><li>Copy <b>/path/timeline-state-store.ldb</b> from the source host <b>host1</b> to <b>/path/timeline-state-store.ldb</b> on the target host <b>host2</b>.</li><li>Login to the target host <b>host2</b> and change permissions by running:<div class="code-snippet">chown -R user2:{5} /path/timeline-state-store.ldb</div></li><div class="code-snippet">chmod -R 700 /path/timeline-state-store.ldb</div></li></ol></div>');
    });
  });

  describe("#securityNotice", function() {

    beforeEach(function () {
      this.kerberosEnabled = sinon.stub(App, 'get');
    });

    afterEach(function () {
      this.kerberosEnabled.restore();
    });

    it("should return notice with secure configs and Kerberos enabled", function() {
      this.kerberosEnabled.returns(true);
      view.set('controller.content.secureConfigs', [
        {
          site: 's1',
          keytab: 'k1',
          principal: 'p1_HOST'
        },
        {
          site: 's2',
          keytab: 'k2',
          principal: 'p2_HOST'
        }
      ]);
      view.set('controller.content.reassign.component_name', 'NAMENODE');
      view.set('controller.content.componentsWithoutSecurityConfigs', ['JOURNALNODE']);
      view.set('controller.content.reassignHosts.target', 'host2');
      expect(view.get('securityNotice')).to.equal('<div class="alert alert-info"> <div class="alert alert-warning"> <strong>Note: </strong> Secure cluster requires generating necessary principals for reassigned component and creating keytab files with the principal on the target host. The keytab file should be accessible to the service user.</div> <ul><li>Create keytab file <b>k1</b> with principal <b>p1host2</b> on <b>host2</b> host.</li><li>Create keytab file <b>k2</b> with principal <b>p2host2</b> on <b>host2</b> host.</li></ul> </div>Please proceed once you have completed the steps above');
    });

    it("should return notice with secure configs and Kerberos disabled", function() {
      this.kerberosEnabled.returns(false);
      view.set('controller.content.secureConfigs', [
        {
          site: 's1',
          keytab: 'k1',
          principal: 'p1_HOST'
        },
        {
          site: 's2',
          keytab: 'k2',
          principal: 'p2_HOST'
        }
      ]);
      view.set('controller.content.reassign.component_name', 'NAMENODE');
      view.set('controller.content.componentsWithoutSecurityConfigs', ['JOURNALNODE']);
      view.set('controller.content.reassignHosts.target', 'host2');
      expect(view.get('securityNotice')).to.equal('Please proceed once you have completed the steps above');
    });

    it("should return notice without secure configs and Kerberos enabled", function() {
      this.kerberosEnabled.returns(true);
      view.set('controller.content.reassign.component_name', 'NAMENODE');
      view.set('controller.content.componentsWithoutSecurityConfigs', ['JOURNALNODE', 'NAMENODE']);
      view.set('controller.content.reassignHosts.target', 'host2');
      expect(view.get('securityNotice')).to.equal('Please proceed once you have completed the steps above');
    });

    it("should return notice without secure configs and Kerberos disabled", function() {
      this.kerberosEnabled.returns(false);
      view.set('controller.content.reassign.component_name', 'NAMENODE');
      view.set('controller.content.componentsWithoutSecurityConfigs', ['JOURNALNODE', 'NAMENODE']);
      view.set('controller.content.reassignHosts.target', 'host2');
      expect(view.get('securityNotice')).to.equal('Please proceed once you have completed the steps above');
    });
  });
});
