require 'puppet'
require 'mocha'
describe Puppet::Parser::Functions.function(:merge) do

  # Pulled from Dan's create_resources function
  # TODO - these let statements should be moved somewhere
  # where they can be resued
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

  describe 'when calling merge from puppet' do
    it "should not compile when no arguments are passed" do
      Puppet[:code] = 'merge()'
      expect { compiler.compile }.should raise_error(Puppet::ParseError, /wrong number of arguments/)
    end
    it "should not compile when 1 argument is passed" do
      Puppet[:code] = "$my_hash={'one' => 1}\nmerge($my_hash)"
      expect { compiler.compile }.should raise_error(Puppet::ParseError, /wrong number of arguments/)
    end
  end
  describe 'when calling merge on the scope instance' do
    it 'should require all parameters are hashes' do
      expect { new_hash = scope.function_merge([{}, '2'])}.should raise_error(Puppet::ParseError, /unexpected argument type String/)

    end
    it 'should be able to merge two hashes' do
      new_hash = scope.function_merge([{'one' => '1', 'two' => '1'}, {'two' => '2', 'three' => '2'}])
      new_hash['one'].should   == '1'
      new_hash['two'].should   == '2'
      new_hash['three'].should == '2'
    end
    it 'should merge multiple hashes' do
      hash = scope.function_merge([{'one' => 1}, {'one' => '2'}, {'one' => '3'}])
      hash['one'].should == '3'
    end
    it 'should accept empty hashes' do
      scope.function_merge([{},{},{}]).should == {}
    end

  end

end
