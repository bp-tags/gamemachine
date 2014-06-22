module Example
  class PlayerRegister
    include Models

    attr_accessor :params, :errors
    def initialize(params)
      @params = params
      @errors = {}
    end

    def valid?
      errors.size == 0
    end

    def add_error(field,message)
      errors[field] ||= []
      errors[field] << message
    end

    def error
        {'error' => errors}
    end

    def success(message)
      {'success' => message}
    end

    # This is called whenever a player is registered
    # It must return a simple hash with a success or error key
    def register!

      # Create one character at registration time
      password = params['password']
      username = params['username']
      character_name = params['character_name']

      # make sure user/character do not already exist
      if User.find(username,10000)
        add_error('username','must not already exist')
        return error
      end

      if username == character_name
        add_error('username','must be different from character name')
      end

      # Create user
      user = User.new(
        :id => username,
        :password => password,
      )
      if user.valid?
        user.save
      else
        errors.merge!(user.errors)
      end

      # Create character
      stats = Vitals.new(
        :id => username,
        :max_health => 100,
        :health => 100,
        :defense_skill => 10,
        :offense_skill => 10,
        :entity_type => 'player'
      )
      if stats.valid?
        stats.save
      else
        errors.merge!(stats.errors)
      end

      if valid?
        success("Account created!")
      else
        error
      end
    end

  end
end
