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
