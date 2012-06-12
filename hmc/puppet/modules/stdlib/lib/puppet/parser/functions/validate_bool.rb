module Puppet::Parser::Functions

  newfunction(:validate_bool, :doc => <<-'ENDHEREDOC') do |args|
    Validate all passed values are true or false.  Abort catalog compilation if the
    value does not pass the check.

    Example:

    These booleans validate

        $iamtrue = true
        validate_bool(true)
        validate_bool(true, true, false, $iamtrue)

    These strings do NOT validate and will abort catalog compilation

        $some_array = [ true ]
        validate_bool("false")
        validate_bool("true")
        validate_bool($some_array)

    * Jeff McCune <jeff@puppetlabs.com>
    * Dan Bode <dan@puppetlabs.com>

    ENDHEREDOC

    unless args.length > 0 then
      raise Puppet::ParseError, ("validate_bool(): wrong number of arguments (#{args.length}; must be > 0)")
    end

    args.each do |arg|
      unless (arg.is_a?(TrueClass) || arg.is_a?(FalseClass))
        raise Puppet::ParseError, ("#{arg.inspect} is not a boolean.  It looks to be a #{arg.class}")
      end
    end

  end

end
