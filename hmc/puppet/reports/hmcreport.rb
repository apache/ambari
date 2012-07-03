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
require 'puppet'

Puppet::Reports.register_report(:hmcreport) do
  desc <<-DESC
    Send summary report information to the report directory." 
  DESC

  def process
    client = self.configuration_version
    summary = self.to_yaml
    dir = File.join(Puppet[:reportdir], client)
    if ! FileTest.exists?(dir)
      FileUtils.mkdir_p(dir)
      FileUtils.chmod_R(0750, dir)
    end
    file = self.host
    destination = File.join(dir, file)
    begin
      File.open(destination, "w", 0640) do |f|
        f.print summary
      end
    rescue => detail
      puts detail.backtrace if Puppet[:trace]
      Puppet.warning "Could not write report for #{self.host} at #{destination}: #{detail}"
    end

    # Only testing cares about the return value
    file

  end 
end
