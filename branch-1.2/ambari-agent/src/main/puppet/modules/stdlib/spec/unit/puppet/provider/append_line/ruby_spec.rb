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
