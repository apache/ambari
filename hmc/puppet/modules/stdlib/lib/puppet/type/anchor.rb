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
Puppet::Type.newtype(:anchor) do
  desc <<-'ENDOFDESC'
  A simple resource type intended to be used as an anchor in a composite class.

      class ntp {
        class { 'ntp::package': }
        -> class { 'ntp::config': }
        -> class { 'ntp::service': }

        # These two resources "anchor" the composed classes
        # such that the end user may use "require" and "before"
        # relationships with Class['ntp']
        anchor { 'ntp::begin': }   -> class  { 'ntp::package': }
        class  { 'ntp::service': } -> anchor { 'ntp::end': }
      }

  This resource allows all of the classes in the ntp module to be contained
  within the ntp class from a dependency management point of view.

  This allows the end user of the ntp module to establish require and before
  relationships easily:

      class { 'ntp': } -> class { 'mcollective': }
      class { 'mcollective': } -> class { 'ntp': }

  ENDOFDESC

  newparam :name do
    desc "The name of the anchor resource."
  end

end
