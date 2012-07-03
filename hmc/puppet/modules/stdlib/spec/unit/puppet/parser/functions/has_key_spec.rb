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
require 'mocha'
describe Puppet::Parser::Functions.function(:has_key) do

  # Pulled from Dan's create_resources function
  # TODO - this should be moved to spec_helper since the
  # logic is likely to be applied to multiple rspec files.
  let(:compiler) {
    topscope = Puppet::Parser::Scope.new
    # This is necessary so we don't try to use the compiler to discover our parent.
    topscope.parent = nil
    my_scope = Puppet::Parser::Scope.new
    my_scope.compiler = Puppet::Parser::Compiler.new(Puppet::Node.new("floppy", :environment => 'production'))
    my_scope.parent = topscope
    compiler = my_scope.compiler
  }
  let(:scope) {
    scope = Puppet::Parser::Scope.new
    scope.stubs(:environment).returns(Puppet::Node::Environment.new('production'))
    scope
  }

  describe 'when calling has_key from puppet' do
    it "should not compile when no arguments are passed" do
      Puppet[:code] = 'has_key()'
      expect { compiler.compile }.should raise_error(Puppet::ParseError, /wrong number of arguments/)
    end
    it "should not compile when 1 argument is passed" do
      Puppet[:code] = "has_key('foo')"
      expect { compiler.compile }.should raise_error(Puppet::ParseError, /wrong number of arguments/)
    end
    it "should require the first value to be a Hash" do
      Puppet[:code] = "has_key('foo', 'bar')"
      expect { compiler.compile }.should raise_error(Puppet::ParseError, /expects the first argument to be a hash/)
    end
  end
  describe 'when calling the function has_key from a scope instance' do
    it 'should detect existing keys' do
      scope.function_has_key([{'one' => 1}, 'one']).should be_true
    end
    it 'should detect existing keys' do
      scope.function_has_key([{'one' => 1}, 'two']).should be_false
    end
  end

end
