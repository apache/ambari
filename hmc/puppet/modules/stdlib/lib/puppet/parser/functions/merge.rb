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
module Puppet::Parser::Functions

  newfunction(:merge, :type => :rvalue, :doc => <<-'ENDHEREDOC') do |args|
    Merges two or more hashes together and returns the resulting hash.

    For example:

      $hash1 = {'one' => 1, 'two', => 2}
      $hash1 = {'two' => 2, 'three', => 2}
      $merged_hash = merge($hash1, $hash2)
      # merged_hash =  {'one' => 1, 'two' => 2, 'three' => 2}

    ENDHEREDOC

    if args.length < 2
      raise Puppet::ParseError, ("merge(): wrong number of arguments (#{args.length}; must be at least 2)")
    end
    args.each do |arg|
      unless arg.is_a?(Hash)
        raise Puppet::ParseError, "merge: unexpected argument type #{arg.class}, only expects hash arguments"
      end
    end

    args.inject({}, :merge)

  end

end
