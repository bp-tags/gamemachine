package com.game_machine.core;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import GameMachine.Messages.ClientManagerEvent;
import GameMachine.Messages.Entity;
import GameMachine.Messages.Neighbors;
import GameMachine.Messages.Player;
import GameMachine.Messages.TrackExtra;
import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class EntityTracking extends UntypedActor {

	LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	public static ConcurrentHashMap<String, TrackExtra> extra = new ConcurrentHashMap<String, TrackExtra>();
	public static String name = "fastpath_entity_tracking";
	
	private Grid aoeGrid;
	private Grid grid;
	private ActorSelection messageGateway;
	
	public EntityTracking() {
		messageGateway = ActorUtil.getSelectionByName(MessageGateway.name);
		grid = Grid.find("default");
		aoeGrid = Grid.find("aoe");
		Commands.clientManagerRegister(name);
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		
		if (message instanceof Entity) {
			Entity entity = (Entity)message;
			
			if (entity.hasGetNeighbors()) {
				SendNeighbors(entity);
			}
			
			if (entity.hasTrackEntity()) {
				setEntityLocation(entity);
			}
			
		} else if (message instanceof ClientManagerEvent) {
			ClientManagerEvent event = (ClientManagerEvent)message;
			if (event.event.equals("disconnected")) {
				removePlayerData(event);
			}
		} else {
			unhandled(message);
		}

	}
	
	private void removePlayerData(ClientManagerEvent event) {
		grid.remove(event.player_id);
		aoeGrid.remove(event.player_id);
		extra.remove(event.player_id);
	}
	
	private void SendNeighbors(Entity entity) {
		Float x = entity.getNeighbors.vector3.x;
		Float y = entity.getNeighbors.vector3.y;
		if (x == null) {
			x = 0f;
		}
		if (y == null) {
			y = 0f;
		}
		
		ArrayList<GridValue> searchResults = grid.neighbors(x, y, entity.getNeighbors.neighborType);
		ArrayList<Entity> neighbors = neighborsAsEntity(searchResults);
		
		if (neighbors.isEmpty()) {
			return;
		}
		
		if (entity.hasPlayer()) {
			SendNeighbors(neighbors, entity.player);
		}
	}
	
	private void SendNeighbors(ArrayList<Entity> neighbors, Player player) {
		int count = 0;
		Neighbors slice = new Neighbors();
		
		for (Entity entity : neighbors) {
			slice.addEntity(entity);
			count++;
			if (count >= 20) {
				SendToGateway(player,slice);
				count = 0;
				slice = new Neighbors();
			}
		}
		if (slice.getEntityCount() >= 1) {
			SendToGateway(player,slice);
		}
		
	}
	
	private void SendToGateway(Player player,Neighbors slice) {
		Entity playerMessage = new Entity();
		playerMessage.setNeighbors(slice);
		playerMessage.setPlayer(player);
		playerMessage.setId(player.id);
		messageGateway.tell(playerMessage, getSelf());
	}
	
	private ArrayList<Entity> neighborsAsEntity(ArrayList<GridValue> searchResults) {
		ArrayList<Entity> neighbors = new ArrayList<Entity>();
		for (GridValue gridvalue : searchResults) {
			Entity result = new Entity();
			result.setId(gridvalue.id);
			GameMachine.Messages.Vector3 vector = new GameMachine.Messages.Vector3();
			vector.setX(gridvalue.x);
			vector.setY(gridvalue.y);
			vector.setZ(gridvalue.z);
			result.setVector3(vector);
			
			if (extra.containsKey(gridvalue.id)) {
				result.setTrackExtra(extra.get(gridvalue.id));
			}
			neighbors.add(result);
		}
		return neighbors;
	}
	
	private void setEntityLocation(Entity entity) {
		
		GameMachine.Messages.Vector3 vector = entity.vector3;
		
		// So either protostuff or protobuf-net has a bug where 0 floats come through as null
		// This is *really* annoying must track down
		if (vector.x == null) {
			vector.x = 0f;
		}
		if (vector.y == null) {
			vector.y = 0f;
		}
		if (vector.z == null) {
			vector.z = 0f;
		}
		
		grid.set(entity.id, vector.x, vector.y, vector.z, entity.entityType);
		aoeGrid.set(entity.id, vector.x, vector.y, vector.z, entity.entityType);
		if (entity.trackEntity.hasTrackExtra()) {
			extra.put(entity.id, entity.trackEntity.trackExtra);
		}
	}

}