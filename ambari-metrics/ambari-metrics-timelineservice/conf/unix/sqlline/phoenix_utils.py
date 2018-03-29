#!/usr/bin/env python
############################################################################
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
############################################################################

import os
import fnmatch
from ambari_commons import subprocess32

def find(pattern, classPaths):
    paths = classPaths.split(os.pathsep)

    # for each class path
    for path in paths:
        # remove * if it's at the end of path
        if ((path is not None) and (len(path) > 0) and (path[-1] == '*')) :
            path = path[:-1]
    
        for root, dirs, files in os.walk(path):
            # sort the file names so *-client always precedes *-thin-client
            files.sort()
            for name in files:
                if fnmatch.fnmatch(name, pattern):
                    return os.path.join(root, name)
                
    return ""

def findFileInPathWithoutRecursion(pattern, path):
    if not os.path.exists(path):
        return ""
    files = [f for f in os.listdir(path) if os.path.isfile(os.path.join(path,f))]
    # sort the file names so *-client always precedes *-thin-client
    files.sort()
    for name in files:
        if fnmatch.fnmatch(name, pattern):
            return os.path.join(path, name)

    return ""

def which(file):
    for path in os.environ["PATH"].split(os.pathsep):
        if os.path.exists(os.path.join(path, file)):
            return os.path.join(path, file)
    return None

def findClasspath(file):
    aPath = which(file)
    command = "%s%s" %(aPath, ' classpath')
    return subprocess32.Popen(command, shell=True, stdout=subprocess32.PIPE).stdout.read()

def setPath():
    PHOENIX_CLIENT_JAR_PATTERN = "phoenix-*-client.jar"
    PHOENIX_THIN_CLIENT_JAR_PATTERN = "phoenix-*-thin-client.jar"
    PHOENIX_QUERYSERVER_JAR_PATTERN = "phoenix-server-*-runnable.jar"
    PHOENIX_TESTS_JAR_PATTERN = "phoenix-core-*-tests*.jar"

    # Backward support old env variable PHOENIX_LIB_DIR replaced by PHOENIX_CLASS_PATH
    global phoenix_class_path
    phoenix_class_path = os.getenv('PHOENIX_LIB_DIR','')
    if phoenix_class_path == "":
        phoenix_class_path = os.getenv('PHOENIX_CLASS_PATH','')

    global hbase_conf_dir
    # if HBASE_CONF_DIR set explicitly, use that
    hbase_conf_dir = os.getenv('HBASE_CONF_DIR', os.getenv('HBASE_CONF_PATH'))
    if not hbase_conf_dir:
        # else fall back to HBASE_HOME
        if os.getenv('HBASE_HOME'):
            hbase_conf_dir = os.path.join(os.getenv('HBASE_HOME'), "conf")
        elif os.name == 'posix':
            # default to the bigtop configuration dir
            hbase_conf_dir = '/etc/hbase/conf'
        else:
            # Try to provide something valid
            hbase_conf_dir = '.'
    global hbase_conf_path # keep conf_path around for backward compatibility
    hbase_conf_path = hbase_conf_dir

    global current_dir
    current_dir = os.path.dirname(os.path.abspath(__file__))

    global phoenix_jar_path
    phoenix_jar_path = os.path.join(current_dir, "..", "phoenix-assembly", "target","*")

    global phoenix_client_jar
    phoenix_client_jar = find("phoenix-*-client.jar", phoenix_jar_path)
    if phoenix_client_jar == "":
        phoenix_client_jar = findFileInPathWithoutRecursion(PHOENIX_CLIENT_JAR_PATTERN, os.path.join(current_dir, ".."))
    if phoenix_client_jar == "":
        phoenix_client_jar = find(PHOENIX_CLIENT_JAR_PATTERN, phoenix_class_path)

    global phoenix_test_jar_path
    phoenix_test_jar_path = os.path.join(current_dir, "..", "phoenix-core", "target","*")

    global hadoop_conf
    hadoop_conf = os.getenv('HADOOP_CONF_DIR', None)
    if not hadoop_conf:
        if os.name == 'posix':
            # Try to provide a sane configuration directory for Hadoop if not otherwise provided.
            # If there's no jaas file specified by the caller, this is necessary when Kerberos is enabled.
            hadoop_conf = '/etc/hadoop/conf'
        else:
            # Try to provide something valid..
            hadoop_conf = '.'

    global hadoop_classpath
    if (os.name != 'nt'):
        hadoop_classpath = findClasspath('hadoop')
    else:
        hadoop_classpath = os.getenv('HADOOP_CLASSPATH', '')

    global hadoop_common_jar_path
    hadoop_common_jar_path = os.path.join(current_dir, "..", "phoenix-assembly", "target","*")

    global hadoop_common_jar
    hadoop_common_jar = find("hadoop-common*.jar", hadoop_common_jar_path)

    global hadoop_hdfs_jar_path
    hadoop_hdfs_jar_path = os.path.join(current_dir, "..", "phoenix-assembly", "target","*")

    global hadoop_hdfs_jar
    hadoop_hdfs_jar = find("hadoop-hdfs*.jar", hadoop_hdfs_jar_path)

    global testjar
    testjar = find(PHOENIX_TESTS_JAR_PATTERN, phoenix_test_jar_path)
    if testjar == "":
        testjar = findFileInPathWithoutRecursion(PHOENIX_TESTS_JAR_PATTERN, os.path.join(current_dir, ".."))
    if testjar == "":
        testjar = find(PHOENIX_TESTS_JAR_PATTERN, phoenix_class_path)

    global phoenix_queryserver_jar
    phoenix_queryserver_jar = find(PHOENIX_QUERYSERVER_JAR_PATTERN, os.path.join(current_dir, "..", "phoenix-server", "target", "*"))
    if phoenix_queryserver_jar == "":
        phoenix_queryserver_jar = findFileInPathWithoutRecursion(PHOENIX_QUERYSERVER_JAR_PATTERN, os.path.join(current_dir, "..", "lib"))
    if phoenix_queryserver_jar == "":
        phoenix_queryserver_jar = findFileInPathWithoutRecursion(PHOENIX_QUERYSERVER_JAR_PATTERN, os.path.join(current_dir, ".."))


    global phoenix_thin_client_jar
    phoenix_thin_client_jar = find(PHOENIX_THIN_CLIENT_JAR_PATTERN, os.path.join(current_dir, "..", "phoenix-server-client", "target", "*"))
    if phoenix_thin_client_jar == "":
        phoenix_thin_client_jar = findFileInPathWithoutRecursion(PHOENIX_THIN_CLIENT_JAR_PATTERN, os.path.join(current_dir, ".."))

    return ""

def shell_quote(args):
    """
    Return the platform specific shell quoted string. Handles Windows and *nix platforms.

    :param args: array of shell arguments
    :return: shell quoted string
    """
    if os.name == 'nt':
        from ambari_commons import subprocess32
        return subprocess32.list2cmdline(args)
    else:
        # pipes module isn't available on Windows
        import pipes
        return " ".join([pipes.quote(v) for v in args])

if __name__ == "__main__":
    setPath()
    print "phoenix_class_path:", phoenix_class_path
    print "hbase_conf_dir:", hbase_conf_dir
    print "hbase_conf_path:", hbase_conf_path
    print "current_dir:", current_dir
    print "phoenix_jar_path:", phoenix_jar_path
    print "phoenix_client_jar:", phoenix_client_jar
    print "phoenix_test_jar_path:", phoenix_test_jar_path
    print "hadoop_common_jar_path:", hadoop_common_jar_path
    print "hadoop_common_jar:", hadoop_common_jar
    print "hadoop_hdfs_jar_path:", hadoop_hdfs_jar_path
    print "hadoop_hdfs_jar:", hadoop_hdfs_jar
    print "testjar:", testjar
    print "phoenix_queryserver_jar:", phoenix_queryserver_jar
    print "phoenix_thin_client_jar:", phoenix_thin_client_jar
    print "hadoop_classpath:", hadoop_classpath
