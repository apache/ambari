#!/usr/bin/env python
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
import sys
import fileinput
import subprocess
import json
import re
from resource_management import *
from resource_management.libraries.functions.ranger_functions import Rangeradmin
from resource_management.core.logger import Logger

def setup_ranger_knox(env):
    import params
    env.set_params(params)

    if params.has_ranger_admin:
        try:
            command = 'hdp-select status knox-server'
            return_code, hdp_output = shell.call(command, timeout=20)
        except Exception, e:
            Logger.error(str(e))
            raise Fail('Unable to execute hdp-select command to retrieve the version.')

        if return_code != 0:
            raise Fail('Unable to determine the current version because of a non-zero return code of {0}'.format(str(return_code)))

        hdp_version = re.sub('knox-server - ', '', hdp_output)
        match = re.match('[0-9]+.[0-9]+.[0-9]+.[0-9]+-[0-9]+', hdp_version)

        if match is None:
            raise Fail('Failed to get extracted version')

        file_path = '/usr/hdp/'+ hdp_version +'/ranger-knox-plugin/install.properties'

        ranger_knox_dict = ranger_knox_properties(params)
        knox_repo_data = knox_repo_properties(params)       

        write_properties_to_file(file_path, ranger_knox_dict)

        if params.enable_ranger_knox:
            cmd = format('cd /usr/hdp/{hdp_version}/ranger-knox-plugin/ && sh enable-knox-plugin.sh')
            ranger_adm_obj = Rangeradmin(url=ranger_knox_dict['POLICY_MGR_URL'])
            response_code, response_recieved = ranger_adm_obj.check_ranger_login_urllib2(ranger_knox_dict['POLICY_MGR_URL'] + '/login.jsp', 'test:test')

            if response_code is not None and response_code == 200:
                repo = ranger_adm_obj.get_repository_by_name_urllib2(ranger_knox_dict['REPOSITORY_NAME'], 'knox', 'true', 'admin:admin')

                if repo and repo['name'] == ranger_knox_dict['REPOSITORY_NAME']:
                    Logger.info('Knox Repository exist')
                else:
                    response = ranger_adm_obj.create_repository_urllib2(knox_repo_data, 'admin:admin')
                    if response is not None:
                        Logger.info('Knox Repository created in Ranger Admin')
                    else:
                        Logger.info('Knox Repository creation failed in Ranger Admin')
            else:
                Logger.info('Ranger service is not started on given host')
        else:
            cmd = format('cd /usr/hdp/{hdp_version}/ranger-knox-plugin/ && sh disable-knox-plugin.sh')

        Execute(cmd, environment={'JAVA_HOME': params.java_home}, logoutput=True)
    else:
        Logger.info('Ranger admin not installed') 


def write_properties_to_file(file_path, value):
    for key in value:
      modify_config(file_path, key, value[key])


def modify_config(filepath, variable, setting):
    var_found = False
    already_set = False
    V=str(variable)
    S=str(setting)
    # use quotes if setting has spaces #
    if ' ' in S:
        S = '%s' % S

    for line in fileinput.input(filepath, inplace = 1):
        # process lines that look like config settings #
        if not line.lstrip(' ').startswith('#') and '=' in line:
            _infile_var = str(line.split('=')[0].rstrip(' '))
            _infile_set = str(line.split('=')[1].lstrip(' ').rstrip())
            # only change the first matching occurrence #
            if var_found == False and _infile_var.rstrip(' ') == V:
                var_found = True
                # don't change it if it is already set #
                if _infile_set.lstrip(' ') == S:
                    already_set = True
                else:
                    line = "%s=%s\n" % (V, S)

        sys.stdout.write(line)

    # Append the variable if it wasn't found #
    if not var_found:
        with open(filepath, "a") as f:
            f.write("%s=%s\n" % (V, S))
    elif already_set == True:
        pass
    else:
        pass

    return

def ranger_knox_properties(params):
    ranger_knox_properties = dict()

    ranger_knox_properties['POLICY_MGR_URL']           = params.config['configurations']['admin-properties']['policymgr_external_url']
    ranger_knox_properties['SQL_CONNECTOR_JAR']        = params.config['configurations']['admin-properties']['SQL_CONNECTOR_JAR']
    ranger_knox_properties['XAAUDIT.DB.FLAVOUR']       = params.config['configurations']['admin-properties']['DB_FLAVOR']
    ranger_knox_properties['XAAUDIT.DB.DATABASE_NAME'] = params.config['configurations']['admin-properties']['audit_db_name']
    ranger_knox_properties['XAAUDIT.DB.USER_NAME']     = params.config['configurations']['admin-properties']['audit_db_user']
    ranger_knox_properties['XAAUDIT.DB.PASSWORD']      = params.config['configurations']['admin-properties']['audit_db_password']
    ranger_knox_properties['XAAUDIT.DB.HOSTNAME']      = params.config['configurations']['admin-properties']['db_host']
    ranger_knox_properties['REPOSITORY_NAME']          = params.config['clusterName'] + '_knox'

    ranger_knox_properties['KNOX_HOME'] = params.config['configurations']['ranger-knox-plugin-properties']['KNOX_HOME']

    ranger_knox_properties['XAAUDIT.DB.IS_ENABLED']   = params.config['configurations']['ranger-knox-plugin-properties']['XAAUDIT.DB.IS_ENABLED']

    ranger_knox_properties['XAAUDIT.HDFS.IS_ENABLED'] = params.config['configurations']['ranger-knox-plugin-properties']['XAAUDIT.HDFS.IS_ENABLED']
    ranger_knox_properties['XAAUDIT.HDFS.DESTINATION_DIRECTORY'] = params.config['configurations']['ranger-knox-plugin-properties']['XAAUDIT.HDFS.DESTINATION_DIRECTORY']
    ranger_knox_properties['XAAUDIT.HDFS.LOCAL_BUFFER_DIRECTORY'] = params.config['configurations']['ranger-knox-plugin-properties']['XAAUDIT.HDFS.LOCAL_BUFFER_DIRECTORY']
    ranger_knox_properties['XAAUDIT.HDFS.LOCAL_ARCHIVE_DIRECTORY'] = params.config['configurations']['ranger-knox-plugin-properties']['XAAUDIT.HDFS.LOCAL_ARCHIVE_DIRECTORY']
    ranger_knox_properties['XAAUDIT.HDFS.DESTINTATION_FILE'] = params.config['configurations']['ranger-knox-plugin-properties']['XAAUDIT.HDFS.DESTINTATION_FILE']
    ranger_knox_properties['XAAUDIT.HDFS.DESTINTATION_FLUSH_INTERVAL_SECONDS'] = params.config['configurations']['ranger-knox-plugin-properties']['XAAUDIT.HDFS.DESTINTATION_FLUSH_INTERVAL_SECONDS']
    ranger_knox_properties['XAAUDIT.HDFS.DESTINTATION_ROLLOVER_INTERVAL_SECONDS'] = params.config['configurations']['ranger-knox-plugin-properties']['XAAUDIT.HDFS.DESTINTATION_ROLLOVER_INTERVAL_SECONDS']
    ranger_knox_properties['XAAUDIT.HDFS.DESTINTATION_OPEN_RETRY_INTERVAL_SECONDS'] = params.config['configurations']['ranger-knox-plugin-properties']['XAAUDIT.HDFS.DESTINTATION_OPEN_RETRY_INTERVAL_SECONDS']
    ranger_knox_properties['XAAUDIT.HDFS.LOCAL_BUFFER_FILE'] = params.config['configurations']['ranger-knox-plugin-properties']['XAAUDIT.HDFS.LOCAL_BUFFER_FILE']
    ranger_knox_properties['XAAUDIT.HDFS.LOCAL_BUFFER_FLUSH_INTERVAL_SECONDS'] = params.config['configurations']['ranger-knox-plugin-properties']['XAAUDIT.HDFS.LOCAL_BUFFER_FLUSH_INTERVAL_SECONDS']
    ranger_knox_properties['XAAUDIT.HDFS.LOCAL_BUFFER_ROLLOVER_INTERVAL_SECONDS'] = params.config['configurations']['ranger-knox-plugin-properties']['XAAUDIT.HDFS.LOCAL_BUFFER_ROLLOVER_INTERVAL_SECONDS']
    ranger_knox_properties['XAAUDIT.HDFS.LOCAL_ARCHIVE_MAX_FILE_COUNT'] = params.config['configurations']['ranger-knox-plugin-properties']['XAAUDIT.HDFS.LOCAL_ARCHIVE_MAX_FILE_COUNT']
    

    ranger_knox_properties['SSL_KEYSTORE_FILE_PATH'] = params.config['configurations']['ranger-knox-plugin-properties']['SSL_KEYSTORE_FILE_PATH']
    ranger_knox_properties['SSL_KEYSTORE_PASSWORD'] = params.config['configurations']['ranger-knox-plugin-properties']['SSL_KEYSTORE_PASSWORD']
    ranger_knox_properties['SSL_TRUSTSTORE_FILE_PATH'] = params.config['configurations']['ranger-knox-plugin-properties']['SSL_TRUSTSTORE_FILE_PATH']
    ranger_knox_properties['SSL_TRUSTSTORE_PASSWORD'] = params.config['configurations']['ranger-knox-plugin-properties']['SSL_TRUSTSTORE_PASSWORD']
    

    return ranger_knox_properties    

def knox_repo_properties(params):

    knoxHost = params.config['clusterHostInfo']['knox_gateway_hosts'][0]
    knoxPort = params.config['configurations']['gateway-site']['gateway.port']

    config_dict = dict()
    config_dict['username'] = params.config['configurations']['ranger-knox-plugin-properties']['REPOSITORY_CONFIG_USERNAME']
    config_dict['password'] = params.config['configurations']['ranger-knox-plugin-properties']['REPOSITORY_CONFIG_USERNAME']
    config_dict['knox.url'] = 'https://' + knoxHost + ':' + str(knoxPort) +'/gateway/admin/api/v1/topologies'
    config_dict['commonNameForCertificate'] = params.config['configurations']['ranger-knox-plugin-properties']['common.name.for.certificate']

    repo= dict()
    repo['isActive']                = "true"
    repo['config']                  = json.dumps(config_dict)
    repo['description']             = "knox repo"
    repo['name']                    = params.config['clusterName'] + "_knox"
    repo['repositoryType']          = "Knox"
    repo['assetType']               = '5'

    data = json.dumps(repo)

    return data
