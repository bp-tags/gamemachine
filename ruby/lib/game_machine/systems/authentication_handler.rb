module GameMachine
  module Systems
    class AuthenticationHandler < Actor

      def post_init
        @authenticated_players = {}
      end

      def authenticated?(player)
        (player && @authenticated_players.fetch(player.id,nil)) ? true : false
      end

      def authenticate(player)
        player.authtoken == Settings.authtoken
      end

      def register_player(client_message)
        player_register = PlayerRegister.new.
          set_client_connection(client_message.client_connection).
          set_player_id(client_message.player.id).
          set_observer(get_self.path.name)
        PlayerRegistry.find.ask(player_register,100)
      end

      def handler
        EntityDispatcher.find
      end

      def on_receive(message)
        if message.is_a?(Disconnected)
          GameMachine.logger.debug "AuthenticationHandler Disconnected #{message.player_id}"
          @authenticated_players.delete(message.player_id)
        else
          player = message.player
          player.authenticated = false
          if authenticated?(player)
            player.authenticated = true
            handler.tell(message)
          elsif authenticate(player)
            player.authenticated = true
            @authenticated_players[player.id] = true
            register_player(message)
            handler.tell(message)
          else
            error_message = Helpers::GameMessage.new(player.id)
          end
        end
      end

    end
  end
end
