module Puppet::Parser::Functions
  newfunction(:get_map_keys, :type => :rvalue) do |args|
    map = args[0]
    keys = Array.new
    map.keys.each {|key| keys << key}
    return keys
  end
end
