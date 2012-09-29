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
