module Puppet::Parser::Functions
  newfunction(:hdp_is_empty, :type => :rvalue) do |args|
    args = function_hdp_args_as_array(args)
    el = args[0]
    el.nil? or (el.respond_to?(:to_s) and ["undefined","undef",""].include?(el.to_s))
  end
end
