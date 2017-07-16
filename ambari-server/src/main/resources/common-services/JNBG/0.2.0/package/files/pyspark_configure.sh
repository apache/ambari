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

PY_EXEC=$1
PY_VENV_PATH_PREFIX=$2
PY_VENV_OWNER=$3
KINIT_CMD=$4
SPARK_HOME=$5
PYTHONPATH=$6
SPARK_OPTS=$7

if [ ! -d "${PY_VENV_PATH_PREFIX}/python2.7" ]; then
  echo "Unexpected state of installation. No Python client installation detected while trying to install PySpark kernel."
  exit 0
fi

source ${PY_VENV_PATH_PREFIX}/python2.7/bin/activate

if [ -z "${VIRTUAL_ENV}" ]; then
  echo "Unexpected condition detected; Unable to find virtualenv environment."
  exit 1
fi

# assume --sys-prefix used for Toree kernel installs
kernel_dir=${VIRTUAL_ENV}/share/jupyter/kernels/spark_2.1_python2
kernel_run_file=$kernel_dir/bin/run.sh
kernel_json_file=$kernel_dir/kernel.json

mkdir -p $kernel_dir/bin
rm -f $kernel_json_file $kernel_run_file

cat <<'EOF' >> $kernel_run_file
#!/usr/bin/env bash
echo
echo "Starting Python 2 kernel with Spark 2.1 for user ${KERNEL_USERNAME}"
echo

CONF_ARGS="--name '${KERNEL_USERNAME:-Notebook} Python' \
           --conf spark.sql.catalogImplementation=in-memory"

PYSPARK_SUBMIT_ARGS="${CONF_ARGS} ${PYSPARK_SUBMIT_ARGS}"

# replace generic log file name with user-specific log file name based on authenticated end-user
PYSPARK_SUBMIT_ARGS="${PYSPARK_SUBMIT_ARGS//spark-driver-USER.log/spark-driver-${KERNEL_USERNAME:-all}.log}"

echo "PYSPARK_SUBMIT_ARGS=\"${PYSPARK_SUBMIT_ARGS}\""

EOF

# For kerberized clusters
if [ -n "${KINIT_CMD}" ]; then
  sed -i "$ a ${KINIT_CMD}\n" $kernel_run_file
fi

sed -i "$ a ${PY_VENV_PATH_PREFIX}/python2.7/bin/python2 -m ipykernel -f \${2}" $kernel_run_file

chmod 755 $kernel_run_file

# Escape double-quotes in the user specified SPARK_OPTS value
SPARK_OPTS="${SPARK_OPTS//\"/\\\"}"

cat <<EOF >> $kernel_json_file
{
  "language": "python",
  "display_name": "Spark 2.1 - Python 2",
  "env": {
    "SPARK_HOME": "${SPARK_HOME}",
    "PYTHONPATH": "${PYTHONPATH}",
    "PYTHONSTARTUP": "${SPARK_HOME}/python/pyspark/shell.py",
    "PYSPARK_SUBMIT_ARGS": "${SPARK_OPTS} pyspark-shell"
  },
  "argv": [
    "$kernel_run_file",
    "-f",
    "{connection_file}"
  ]
}
EOF

# Set ownership of the created virtualenv if configured via python_virtualenv_restrictive
if [ "${PY_VENV_OWNER}" != "root" ]; then
  echo ====== Virtualenv owner = $PY_VENV_OWNER =========
  chown -R ${PY_VENV_OWNER}: ${PY_VENV_PATH_PREFIX}/python2.7
fi
