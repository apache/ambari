"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""
import os
from resource_management import *
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import get_unique_id_and_date

class SolrServerUpgrade(Script):
  def pre_upgrade_conf41(self, env):
    """
    Create /etc/solr/4.1.0.0/0 directory and copies Solr config files here.
    Create symlinks accordingly.

    conf-select create-conf-dir --package solr --stack-version 4.1.0.0 --conf-version 0
    cp -r /usr/iop/4.1.0.0/solr/conf/* /etc/solr/4.1.0.0/0/.
    unlink or rm -r /usr/iop/4.1.0.0/solr/conf
    ln -s /etc/solr/4.1.0.0/0 /usr/iop/4.1.0.0/solr/conf
    conf-select set-conf-dir --package solr --stack-version 4.1.0.0 --conf-version 0
    """
    import params
    env.set_params(params)

    solr41_conf_dir="/usr/iop/4.1.0.0/solr/conf"
    solr41_etc_dir="/etc/solr/4.1.0.0/0"

    content_path=solr41_conf_dir
    if not os.path.isfile("/usr/iop/4.1.0.0/solr/conf/solr.in.sh"):
      content_path = "/etc/solr/conf.backup"

    for each in os.listdir(content_path):
      File(os.path.join(solr41_etc_dir, each),
           owner=params.solr_user,
           content = StaticFile(os.path.join(content_path, each)))

    if not os.path.islink(solr41_conf_dir):
      Directory(solr41_conf_dir,
                action="delete",
                create_parents = True)

    if os.path.islink(solr41_conf_dir):
      os.unlink(solr41_conf_dir)

    if not os.path.islink(solr41_conf_dir):
      Link(solr41_conf_dir,
           to=solr41_etc_dir
      )

  def pre_stop_backup_cores(self, env):
    """
    Backs up the Solr cores under Solr's home directory.
    cp -r /var/lib/solr/data/* /tmp/solr/cores
    """
    import params
    env.set_params(params)

    if compare_versions(format_stack_version(params.version), '4.2.0.0') >= 0:
      solr_home_dir=params.solr_data_dir
    else: #4.1.0.0
      solr_home_dir=params.old_lib_dir + "/data"

    unique = get_unique_id_and_date()
    backup_solr_dir="/tmp/upgrades/{0}/solr_{1}".format(params.version, unique)
    backup_solr_cores="/tmp/solr/cores"

    if os.path.isdir(solr_home_dir) and not os.path.isdir(backup_solr_dir):
      os.makedirs(backup_solr_dir)
      Execute(('cp', '-r', solr_home_dir+"/.", backup_solr_dir),
              sudo=True
      )

    if params.upgrade_direction is not None and params.upgrade_direction == Direction.UPGRADE:
      Directory(backup_solr_cores,
                action="delete",
                create_parents = True)

      Directory(backup_solr_cores,
                mode=0755,
                cd_access='a',
                owner=params.solr_user,
                create_parents = True,
                group=params.user_group
      )

      Execute(('cp', '-r', solr_home_dir+"/.", backup_solr_cores),
              user=params.solr_user
      )

  def pre_start_migrate_cores(self, env):
    """
    Copy the Solr cores from previous version to the new Solr home directory if solr_home is a differnet directory.
    cp -r /tmp/solr/cores/* /opt/solr/data/.
    """
    import params
    env.set_params(params)

    if params.upgrade_direction is not None and params.upgrade_direction == Direction.UPGRADE:
      backup_solr_cores="/tmp/solr/cores"
      solr_home_dir=params.solr_data_dir

      Directory(format(solr_home_dir),
                owner=params.solr_user,
                create_parents = True,
                group=params.user_group
      )

      if os.path.isdir(solr_home_dir) and os.path.isdir(backup_solr_cores):
        Execute(('cp', '-rn', backup_solr_cores+"/.", solr_home_dir),
                 user=params.solr_user,
                 logoutput=True
        )

if __name__ == "__main__":
  SolrServerUpgrade().execute()
