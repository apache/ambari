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
Puppet::Type.newtype(:append_line) do

  desc <<-EOT
  Type that can append a line to a file if it does not already contain it.

  Example:

  append_line { 'sudo_rule':
    path => '/etc/sudoers',
    line => '%admin ALL=(ALL) ALL',
  }

  EOT

  ensurable do
    defaultto :present
    newvalue(:present) do
      provider.create
    end
  end

  newparam(:name, :namevar => true) do
    desc 'arbitrary name used as identity'
  end

  newparam(:line) do
    desc 'The line to be appended to the path.'
  end

  newparam(:path) do
    desc 'File to possibly append a line to.'
    validate do |value|
      unless (Puppet.features.posix? and value =~ /^\//) or (Puppet.features.microsoft_windows? and (value =~ /^.:\// or value =~ /^\/\/[^\/]+\/[^\/]+/))
        raise(Puppet::Error, "File paths must be fully qualified, not '#{value}'")
      end
    end
  end

  validate do
    unless self[:line] and self[:path]
      raise(Puppet::Error, "Both line and path are required attributes")
    end
  end
end
