# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License

# stop on all errors
$ErrorActionPreference = 'Stop';

$packageName = $Env:chocolateyPackageName
$packageVersion = $Env:chocolateyPackageVersion
$toolsDir = "$(Split-Path -parent $MyInvocation.MyCommand.Definition)"
$rootDir = "$(Split-Path -parent $toolsDir)"
$contentDir = "$(Join-Path $rootDir content)"

$zipFile = "$(Join-Path $contentDir $packageName-$packageVersion-dist.zip)"
$ambariRoot = "C:\ambari"
$specificFolder = ""

Get-ChocolateyUnzip "$zipFile" $ambariRoot $specificFolder $packageName
cmd /c mklink /D "$ambariRoot\$packageName" "$ambariRoot\$packageName-$packageVersion"