module Puppet::Parser::Functions

  newfunction(:validate_hash, :doc => <<-'ENDHEREDOC') do |args|
    Validate all passed values are a Hash data structure
    value does not pass the check.

    Example:

    These values validate

        $my_hash = { 'one' => 'two' }
        validate_hash($my_hash)

    These values do NOT validate

        validate_hash(true)
        validate_hash('some_string')
        $undefined = undef
        validate_hash($undefined)

    * Jeff McCune <jeff@puppetlabs.com>

    ENDHEREDOC

    unless args.length > 0 then
      raise Puppet::ParseError, ("validate_hash(): wrong number of arguments (#{args.length}; must be > 0)")
    end

    args.each do |arg|
      unless arg.is_a?(Hash)
        raise Puppet::ParseError, ("#{arg.inspect} is not a Hash.  It looks to be a #{arg.class}")
      end
    end

  end

end
