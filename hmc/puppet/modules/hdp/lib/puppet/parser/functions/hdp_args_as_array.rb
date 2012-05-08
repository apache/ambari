#to handle differences in how args passed in
module Puppet::Parser::Functions
  newfunction(:hdp_args_as_array, :type => :rvalue) do |args|
    args.kind_of?(Array) ? args : [args]
  end
end
