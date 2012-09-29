require 'puppet'

# We don't need this for the basic tests we're doing
# require 'spec_helper'

# Dan mentioned that Nick recommended the function method call
# to return the string value for the test description.
# this will not even try the test if the function cannot be
# loaded.
describe Puppet::Parser::Functions.function(:validate_hash) do

  # Pulled from Dan's create_resources function
  def get_scope
    @topscope = Puppet::Parser::Scope.new
    # This is necessary so we don't try to use the compiler to discover our parent.
    @topscope.parent = nil
    @scope = Puppet::Parser::Scope.new
    @scope.compiler = Puppet::Parser::Compiler.new(Puppet::Node.new("floppy", :environment => 'production'))
    @scope.parent = @topscope
    @compiler = @scope.compiler
  end

  describe 'when calling validate_hash from puppet' do

    %w{ true false }.each do |the_string|

      it "should not compile when #{the_string} is a string" do
        Puppet[:code] = "validate_hash('#{the_string}')"
        get_scope
        expect { @scope.compiler.compile }.should raise_error(Puppet::ParseError, /is not a Hash/)
      end

      it "should not compile when #{the_string} is a bare word" do
        Puppet[:code] = "validate_hash(#{the_string})"
        get_scope
        expect { @scope.compiler.compile }.should raise_error(Puppet::ParseError, /is not a Hash/)
      end

    end

    it "should compile when multiple hash arguments are passed" do
      Puppet[:code] = <<-'ENDofPUPPETcode'
        $foo = {}
        $bar = { 'one' => 'two' }
        validate_hash($foo, $bar)
      ENDofPUPPETcode
      get_scope
      @scope.compiler.compile
    end

    it "should not compile when an undef variable is passed" do
      Puppet[:code] = <<-'ENDofPUPPETcode'
        $foo = undef
        validate_hash($foo)
      ENDofPUPPETcode
      get_scope
      expect { @scope.compiler.compile }.should raise_error(Puppet::ParseError, /is not a Hash/)
    end

  end

end

