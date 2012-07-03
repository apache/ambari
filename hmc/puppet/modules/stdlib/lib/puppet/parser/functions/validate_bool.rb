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

  newfunction(:validate_bool, :doc => <<-'ENDHEREDOC') do |args|
    Validate all passed values are true or false.  Abort catalog compilation if the
    value does not pass the check.

    Example:

    These booleans validate

        $iamtrue = true
        validate_bool(true)
        validate_bool(true, true, false, $iamtrue)

    These strings do NOT validate and will abort catalog compilation

        $some_array = [ true ]
        validate_bool("false")
        validate_bool("true")
        validate_bool($some_array)

    * Jeff McCune <jeff@puppetlabs.com>
    * Dan Bode <dan@puppetlabs.com>

    ENDHEREDOC

    unless args.length > 0 then
      raise Puppet::ParseError, ("validate_bool(): wrong number of arguments (#{args.length}; must be > 0)")
    end

    args.each do |arg|
      unless (arg.is_a?(TrueClass) || arg.is_a?(FalseClass))
        raise Puppet::ParseError, ("#{arg.inspect} is not a boolean.  It looks to be a #{arg.class}")
      end
    end

  end

end
