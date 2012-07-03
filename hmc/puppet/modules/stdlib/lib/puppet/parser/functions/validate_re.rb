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

  newfunction(:validate_re, :doc => <<-'ENDHEREDOC') do |args|
    Perform simple validation of a string against a regular expression.  The second
    argument of the function should be a string regular expression (without the //'s)
    or an array of regular expressions.  If none of the regular expressions in the array
    match the string passed in, then an exception will be raised.

    Example:

    These strings validate against the regular expressions

        validate_re('one', '^one$')
        validate_re('one', [ '^one', '^two' ])

    These strings do NOT validate

        validate_re('one', [ '^two', '^three' ])

    Jeff McCune <jeff@puppetlabs.com>

    ENDHEREDOC
    if args.length != 2 then
      raise Puppet::ParseError, ("validate_re(): wrong number of arguments (#{args.length}; must be 2)")
    end

    msg = "validate_re(): #{args[0].inspect} does not match #{args[1].inspect}"

    raise Puppet::ParseError, (msg) unless args[1].any? do |re_str|
      args[0] =~ Regexp.compile(re_str)
    end

  end

end
