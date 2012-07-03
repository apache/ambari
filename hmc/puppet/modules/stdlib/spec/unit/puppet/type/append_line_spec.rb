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
require 'tempfile'
describe Puppet::Type.type(:append_line) do
  before :each do
    @append_line = Puppet::Type.type(:append_line).new(:name => 'foo', :line => 'line', :path => '/tmp/path')
  end
  it 'should accept a line and path' do
    @append_line[:line] = 'my_line'
    @append_line[:line].should == 'my_line'
  end
  it 'should accept posix filenames' do
    @append_line[:path] = '/tmp/path'
    @append_line[:path].should == '/tmp/path'
  end
  it 'should not accept unqualified path' do
    expect { @append_line[:path] = 'file' }.should raise_error(Puppet::Error, /File paths must be fully qualified/)
  end
  it 'should require that a line is specified' do
    expect { Puppet::Type.type(:append_line).new(:name => 'foo', :path => '/tmp/file') }.should raise_error(Puppet::Error, /Both line and path are required attributes/)
  end
  it 'should require that a file is specified' do
    expect { Puppet::Type.type(:append_line).new(:name => 'foo', :line => 'path') }.should raise_error(Puppet::Error, /Both line and path are required attributes/)
  end
end
