module Puppet::Parser::Functions

  newfunction(:merge, :type => :rvalue, :doc => <<-'ENDHEREDOC') do |args|
    Merges two or more hashes together and returns the resulting hash.

    For example:

      $hash1 = {'one' => 1, 'two', => 2}
      $hash1 = {'two' => 2, 'three', => 2}
      $merged_hash = merge($hash1, $hash2)
      # merged_hash =  {'one' => 1, 'two' => 2, 'three' => 2}

    ENDHEREDOC

    if args.length < 2
      raise Puppet::ParseError, ("merge(): wrong number of arguments (#{args.length}; must be at least 2)")
    end
    args.each do |arg|
      unless arg.is_a?(Hash)
        raise Puppet::ParseError, "merge: unexpected argument type #{arg.class}, only expects hash arguments"
      end
    end

    args.inject({}, :merge)

  end

end
