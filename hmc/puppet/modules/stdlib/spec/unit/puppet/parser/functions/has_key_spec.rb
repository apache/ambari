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
