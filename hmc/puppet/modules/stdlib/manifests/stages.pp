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
# Class: stdlib::stages
#
# This class manages a standard set of Run Stages for Puppet.
#
# The high level stages are (In order):
#
#  * setup
#  * main
#  * runtime
#  * setup_infra
#  * deploy_infra
#  * setup_app
#  * deploy_app
#  * deploy
#
# Parameters:
#
# Actions:
#
#   Declares various run-stages for deploying infrastructure,
#   language runtimes, and application layers.
#
# Requires:
#
# Sample Usage:
#
#  node default {
#    include stdlib::stages
#    class { java: stage => 'runtime' }
#  }
#
class stdlib::stages {

  stage { 'setup':  before => Stage['main'] }
  stage { 'runtime': require => Stage['main'] }
  -> stage { 'setup_infra': }
  -> stage { 'deploy_infra': }
  -> stage { 'setup_app': }
  -> stage { 'deploy_app': }
  -> stage { 'deploy': }

}
