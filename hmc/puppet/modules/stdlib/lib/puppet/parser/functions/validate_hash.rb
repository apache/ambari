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

  newfunction(:validate_hash, :doc => <<-'ENDHEREDOC') do |args|
    Validate all passed values are a Hash data structure
    value does not pass the check.

    Example:

    These values validate

        $my_hash = { 'one' => 'two' }
        validate_hash($my_hash)

    These values do NOT validate

        validate_hash(true)
        validate_hash('some_string')
        $undefined = undef
        validate_hash($undefined)

    * Jeff McCune <jeff@puppetlabs.com>

    ENDHEREDOC

    unless args.length > 0 then
      raise Puppet::ParseError, ("validate_hash(): wrong number of arguments (#{args.length}; must be > 0)")
    end

    args.each do |arg|
      unless arg.is_a?(Hash)
        raise Puppet::ParseError, ("#{arg.inspect} is not a Hash.  It looks to be a #{arg.class}")
      end
    end

  end

end
