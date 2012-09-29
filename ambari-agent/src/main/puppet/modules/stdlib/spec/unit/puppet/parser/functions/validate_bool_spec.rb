require 'puppet'

# We don't need this for the basic tests we're doing
# require 'spec_helper'

# Dan mentioned that Nick recommended the function method call
# to return the string value for the test description.
# this will not even try the test if the function cannot be
# loaded.
describe Puppet::Parser::Functions.function(:validate_bool) do

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

  describe 'when calling validate_bool from puppet' do

    %w{ true false }.each do |the_string|

      it "should not compile when #{the_string} is a string" do
        Puppet[:code] = "validate_bool('#{the_string}')"
        get_scope
        expect { @scope.compiler.compile }.should raise_error(Puppet::ParseError, /is not a boolean/)
      end

      it "should compile when #{the_string} is a bare word" do
        Puppet[:code] = "validate_bool(#{the_string})"
        get_scope
        @scope.compiler.compile
      end

    end

    it "should not compile when an arbitrary string is passed" do
      Puppet[:code] = 'validate_bool("jeff and dan are awesome")'
      get_scope
      expect { @scope.compiler.compile }.should raise_error(Puppet::ParseError, /is not a boolean/)
    end

    it "should not compile when no arguments are passed" do
      Puppet[:code] = 'validate_bool()'
      get_scope
      expect { @scope.compiler.compile }.should raise_error(Puppet::ParseError, /wrong number of arguments/)
    end

    it "should compile when multiple boolean arguments are passed" do
      Puppet[:code] = <<-'ENDofPUPPETcode'
        $foo = true
        $bar = false
        validate_bool($foo, $bar, true, false)
      ENDofPUPPETcode
      get_scope
      @scope.compiler.compile
    end

    it "should compile when multiple boolean arguments are passed" do
      Puppet[:code] = <<-'ENDofPUPPETcode'
        $foo = true
        $bar = false
        validate_bool($foo, $bar, true, false, 'jeff')
      ENDofPUPPETcode
      get_scope
      expect { @scope.compiler.compile }.should raise_error(Puppet::ParseError, /is not a boolean/)
    end

  end

end

