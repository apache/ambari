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

  newfunction(:getvar, :type => :rvalue, :doc => <<-'ENDHEREDOC') do |args|
    Lookup a variable in a remote namespace.

    For example:

      $foo = getvar('site::data::foo')

    This is useful if the namespace itself is stored in a string:

      $bar = getvar("${datalocation}::bar")
    ENDHEREDOC

    unless args.length == 1
      raise Puppet::ParseError, ("getvar(): wrong number of arguments (#{args.length}; must be 1)")
    end

    self.lookupvar("#{args[0]}")

  end

end
