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
provider_class = Puppet::Type.type(:append_line).provider(:ruby)
describe provider_class do
  before :each do
    tmp = Tempfile.new('tmp')
    @tmpfile = tmp.path
    tmp.close!
    @resource = Puppet::Type::Append_line.new(
      {:name => 'foo', :path => @tmpfile, :line => 'foo'}
    )
    @provider = provider_class.new(@resource)
  end
  it 'should detect if the line exists in the file' do
    File.open(@tmpfile, 'w') do |fh|
      fh.write('foo')
    end
    @provider.exists?.should be_true
  end
  it 'should detect if the line does not exist in the file' do
    File.open(@tmpfile, 'w') do |fh|
      fh.write('foo1')
    end
    @provider.exists?.should be_nil
  end
  it 'should append to an existing file when creating' do
    @provider.create
    File.read(@tmpfile).chomp.should == 'foo'
  end
end
