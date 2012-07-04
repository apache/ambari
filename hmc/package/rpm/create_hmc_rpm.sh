#!/bin/bash
#
#/*
# * Licensed to the Apache Software Foundation (ASF) under one
# * or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information
# * regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the
# * "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */

CUR_DIR=`pwd`

BASEDIR="$( cd "$( dirname "$0" )" && pwd )"

if [[ -z "${BUILD_DIR}" ]]; then
  BUILD_DIR="${BASEDIR}/build/"
fi

if [[ -z "${VERSION}" ]]; then
  VERSION="0.9.0"
fi

if [[ -z "${RELEASE}" ]]; then
  RELEASE="0"
fi

rm -rf ${BUILD_DIR}/*

PKG_NAME="ambari"

HMC_DIR="${BUILD_DIR}/${PKG_NAME}-$VERSION/"

mkdir -p "${HMC_DIR}"
cp -rf ${BASEDIR}/../../css ${HMC_DIR}
cp -rf ${BASEDIR}/../../db ${HMC_DIR}
cp -rf ${BASEDIR}/../../ShellScripts ${HMC_DIR}
cp -rf ${BASEDIR}/../../html ${HMC_DIR}
cp -rf ${BASEDIR}/../../images ${HMC_DIR}
cp -rf ${BASEDIR}/../../licenses ${HMC_DIR}
cp -rf ${BASEDIR}/../../js ${HMC_DIR}
#cp -rf ${BASEDIR}/../../src/php ${HMC_DIR}
#cp -rf ${BASEDIR}/../../src/puppet ${HMC_DIR}
cp -rf ${BASEDIR}/../../puppet ${HMC_DIR}
cp -rf ${BASEDIR}/../../php ${HMC_DIR}
cp -rf ${BASEDIR}/../../conf ${HMC_DIR}
cp -rf ${BASEDIR}/../../fonts ${HMC_DIR}
cp -f ${BASEDIR}/../../fileCombinator.php ${HMC_DIR}
cp -f ${BASEDIR}/../../../NOTICE.txt ${HMC_DIR}/licenses
TAR_DEST="${BUILD_DIR}/${PKG_NAME}-$VERSION.tar.gz"

cd ${BUILD_DIR};
tar -zcf "${TAR_DEST}" "${PKG_NAME}-$VERSION/"

RPM_BUILDDIR=${BUILD_DIR}/rpmbuild/

mkdir -p ${RPM_BUILDDIR}
mkdir -p ${RPM_BUILDDIR}/SOURCES/
mkdir -p ${RPM_BUILDDIR}/SPECS/
mkdir -p ${RPM_BUILDDIR}/BUILD/
mkdir -p ${RPM_BUILDDIR}/RPMS/
mkdir -p ${RPM_BUILDDIR}/SRPMS/

cp -f ${BASEDIR}/SPECS/${PKG_NAME}.spec ${RPM_BUILDDIR}/SPECS/
cp -f ${TAR_DEST} ${RPM_BUILDDIR}/SOURCES/
cp -f ${BASEDIR}/SOURCES/${PKG_NAME}.init.in ${RPM_BUILDDIR}/SOURCES/
cp -f ${BASEDIR}/SOURCES/${PKG_NAME}-agent.init.in ${RPM_BUILDDIR}/SOURCES/



cd ${RPM_BUILDDIR}

cmd="rpmbuild --define \"_topdir ${RPM_BUILDDIR}\" \
  -ba ${RPM_BUILDDIR}/SPECS/${PKG_NAME}.spec"

echo $cmd
eval $cmd
ret=$?
if [[ "$ret" != "0" ]]; then
  echo "Error: rpmbuild failed, error=$ret"
  exit 1
fi

cd ${CUR_DIR}
RPM_DEST=`find ${RPM_BUILDDIR}/{SRPMS,RPMS} -name *.noarch.rpm`
if [[ -z "${RPM_DEST}" ]]; then
  echo "Error: RPM_DEST dir is empty"
  exit 1
fi

exit 0
