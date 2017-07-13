#!/usr/bin/env bash
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#
set -x

SPARK_CONFIG_DIR=$1

log4j_properties_file="${SPARK_CONFIG_DIR}/log4j.properties"

cat <<'EOF' > "${log4j_properties_file}"

# default log file, overridden by Java System property -Dlog4j.logFile=...
log4j.logFile=/var/log/jnbg/spark-driver-${user.name}.log

# default (root) log level, overridable by Java System property -Dlog4j.logLevel=...
log4j.logLevel=INFO

# default log file size limit, overridable by Java System property -Dlog4j.fileSize=... (KB, MB, GB)
log4j.fileSize=10MB

# default max number of log file backups, overridable by Java System property -Dlog4j.backupFiles=...
log4j.backupFiles=10

# log to file using rolling log strategy with one backup file
# NOTE: Spark REPL overrides rootCategory, set log4j.logLevel above
log4j.rootCategory=${log4j.logLevel}, logfile
log4j.appender.logfile=org.apache.log4j.RollingFileAppender
log4j.appender.logfile.File=${log4j.logFile}
log4j.appender.logfile.MaxFileSize=${log4j.fileSize}
log4j.appender.logfile.MaxBackupIndex=${log4j.backupFiles}
log4j.appender.logfile.encoding=UTF-8
log4j.appender.logfile.layout=org.apache.log4j.PatternLayout
log4j.appender.logfile.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{1}: %m%n

# Reduce Toree related "noise"
log4j.logger.org.apache.toree.kernel.protocol.v5.stream.KernelOutputStream=ERROR

# Modified Spark 2.1 default settings:

# Spark overrides rootCategory level with the level set for the Scala & PySpark REPLs (default=WARN)
# This is intended to reduce log verbosity while working with a Spark shell or PySpark shell.
# However, notebook kernels internally use the spark-shell and pyspark shell implementation, but
# since notebooks are logging to a log file, we want potentially more verbose logs.
# We need to set the spark-shell and pyspark shell log level to the same level as the rootCategory.
# See: org.apache.spark.internal.Logging#initializeLogging(isInterpreter=true)
log4j.logger.org.apache.spark.repl.Main=${log4j.rootCategory}
log4j.logger.org.apache.spark.api.python.PythonGatewayServer=${log4j.rootCategory}

# Settings to quiet third party logs that are too verbose
log4j.logger.org.spark_project.jetty=WARN
log4j.logger.org.spark_project.jetty.util.component.AbstractLifeCycle=ERROR
log4j.logger.org.apache.spark.repl.SparkIMain$exprTyper=INFO
log4j.logger.org.apache.spark.repl.SparkILoop$SparkILoopInterpreter=INFO
log4j.logger.org.apache.parquet=ERROR
log4j.logger.parquet=ERROR

# SPARK-9183: Settings to avoid annoying messages when looking up nonexistent UDFs in SparkSQL with Hive support
log4j.logger.org.apache.hadoop.hive.metastore.RetryingHMSHandler=FATAL
log4j.logger.org.apache.hadoop.hive.ql.exec.FunctionRegistry=ERROR

EOF
