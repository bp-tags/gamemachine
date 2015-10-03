
package io.gamemachine.messages;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.io.UnsupportedEncodingException;

import io.protostuff.ByteString;
import io.protostuff.GraphIOUtil;
import io.protostuff.Input;
import io.protostuff.Message;
import io.protostuff.Output;
import io.protostuff.ProtobufOutput;

import java.io.ByteArrayOutputStream;
import io.protostuff.JsonIOUtil;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.runtime.RuntimeSchema;

import io.gamemachine.util.LocalLinkedBuffer;


import java.nio.charset.Charset;


import java.lang.reflect.Field;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorSelection;
import akka.pattern.AskableActorSelection;

import akka.util.Timeout;
import java.util.concurrent.TimeUnit;


import io.gamemachine.core.ActorUtil;

import org.javalite.common.Convert;
import org.javalite.activejdbc.Model;
import io.protostuff.Schema;
import io.protostuff.UninitializedMessageException;

import io.gamemachine.core.PersistableMessage;


import io.gamemachine.objectdb.Cache;
import io.gamemachine.core.CacheUpdate;

@SuppressWarnings("unused")
public final class Vitals implements Externalizable, Message<Vitals>, Schema<Vitals>, PersistableMessage{



    public static Schema<Vitals> getSchema()
    {
        return DEFAULT_INSTANCE;
    }

    public static Vitals getDefaultInstance()
    {
        return DEFAULT_INSTANCE;
    }

    static final Vitals DEFAULT_INSTANCE = new Vitals();
    static final String defaultScope = Vitals.class.getSimpleName();

    			public String id;
	    
        			public Integer health;
	    
        			public Integer stamina;
	    
        			public Integer magic;
	    
        			public Long lastCombat;
	    
        			public Integer dead;
	    
        			public Integer armor;
	    
        			public Integer spellResist;
	    
        			public Integer elementalResist;
	    
        			public Integer spellPenetration;
	    
        			public Integer magicRegen;
	    
        			public Integer healthRegen;
	    
        			public Integer staminaRegen;
	    
        			public Integer changed;
	    
        			public String grid;
	    
        			public Integer staminaDrain;
	    
        
	public static VitalsCache cache() {
		return VitalsCache.getInstance();
	}
	
	public static VitalsStore store() {
		return VitalsStore.getInstance();
	}


    public Vitals()
    {
        
    }

	static class CacheRef {
	
		private final CacheUpdate cacheUpdate;
		private final String id;
		
		public CacheRef(CacheUpdate cacheUpdate, String id) {
			this.cacheUpdate = cacheUpdate;
			this.id = id;
		}
		
		public void send() {
			ActorSelection sel = ActorUtil.findLocalDistributed("cacheUpdateHandler", id);
			sel.tell(cacheUpdate,null);
		}
		
		public Vitals result(int timeout) {
			ActorSelection sel = ActorUtil.findLocalDistributed("cacheUpdateHandler", id);
			Timeout t = new Timeout(Duration.create(timeout, TimeUnit.MILLISECONDS));
			AskableActorSelection askable = new AskableActorSelection(sel);
			Future<Object> future = askable.ask(cacheUpdate, t);
			try {
				Await.result(future, t.duration());
				return cache().getCache().get(id);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
	}
	
	public static class VitalsCache {

		private static HashMap<String, Field> cachefields = new HashMap<String, Field>();
		private static Cache<String, Vitals> cache = new Cache<String, Vitals>(120, 5000);
		
		private VitalsCache() {
		}
		
		private static class LazyHolder {
			private static final VitalsCache INSTANCE = new VitalsCache();
		}
	
		public static VitalsCache getInstance() {
			return LazyHolder.INSTANCE;
		}
		
	    public void init(int expiration, int size) {
			cache = new Cache<String, Vitals>(expiration, size);
		}
	
		public Cache<String, Vitals> getCache() {
			return cache;
		}
		
		public CacheRef setField(String id, String field, Object value) {
			return updateField(id, field, value, CacheUpdate.SET);
		}
		
		public CacheRef incrementField(String id, String field, Object value) {
			return updateField(id, field, value, CacheUpdate.INCREMENT);
		}
		
		public CacheRef decrementField(String id, String field, Object value) {
			return updateField(id, field, value, CacheUpdate.DECREMENT);
		}
		
		private CacheRef updateField(String id, String field, Object value, int updateType) {
			CacheUpdate cacheUpdate = new CacheUpdate(VitalsCache.class, id, value, field, updateType);
			return new CacheRef(cacheUpdate,id);
		}
		
		public CacheRef set(Vitals message) {
			CacheUpdate cacheUpdate = new CacheUpdate(VitalsCache.class, message);
			return new CacheRef(cacheUpdate,message.id);
		}
	
		public Vitals get(String id, int timeout) {
			Vitals message = cache.get(id);
			if (message == null) {
				message = Vitals.store().get(id, timeout);
			}
			return message;
		}
			
		public static Vitals setFromUpdate(CacheUpdate cacheUpdate) throws IllegalArgumentException, IllegalAccessException  {
			Vitals message = null;
			String field = cacheUpdate.getField();
			if (field == null) {
				message = (Vitals) cacheUpdate.getMessage();
				if (message == null) {
					throw new RuntimeException("Attempt to store empty message in cache");
				}
				cache.set(message.id, message);
				Vitals.store().set(message);
			} else {
				message = Vitals.cache().get(cacheUpdate.getId(), 10);
				if (message == null) {
					throw new RuntimeException("Cannot set field on null message");
				}
				if (!cachefields.containsKey(field)) {
	            	try {
						cachefields.put(field, Vitals.class.getField(field));
					} catch (NoSuchFieldException e) {
						throw new RuntimeException("No such field "+field);
					} catch (SecurityException e) {
						throw new RuntimeException("Security Exception accessing field "+field);
					}
	        	}
				Field f = cachefields.get(field);
				Class<?> klass = f.getType();
				if (cacheUpdate.getUpdateType() == CacheUpdate.SET) {
					f.set(message, klass.cast(cacheUpdate.getFieldValue()));
				} else {
					int updateType = cacheUpdate.getUpdateType();
					Object value = cacheUpdate.getFieldValue();
					if (klass == Integer.TYPE || klass == Integer.class) {
						Integer i;
						if (updateType == CacheUpdate.INCREMENT) {
							i = (Integer)f.get(message) + (Integer) value;
							f.set(message, klass.cast(i));
						} else if (updateType == CacheUpdate.DECREMENT) {
							i = (Integer)f.get(message) - (Integer) value;
							f.set(message, klass.cast(i));
						}
					} else if (klass == Long.TYPE || klass == Long.class) {
						Long i;
						if (updateType == CacheUpdate.INCREMENT) {
							i = (Long)f.get(message) + (Long) value;
							f.set(message, klass.cast(i));
						} else if (updateType == CacheUpdate.DECREMENT) {
							i = (Long)f.get(message) - (Long) value;
							f.set(message, klass.cast(i));
						}
					} else if (klass == Double.TYPE || klass == Double.class) {
						Double i;
						if (updateType == CacheUpdate.INCREMENT) {
							i = (Double)f.get(message) + (Double) value;
							f.set(message, klass.cast(i));
						} else if (updateType == CacheUpdate.DECREMENT) {
							i = (Double)f.get(message) - (Double) value;
							f.set(message, klass.cast(i));
						}
					} else if (klass == Float.TYPE || klass == Float.class) {
						Float i;
						if (updateType == CacheUpdate.INCREMENT) {
							i = (Float)f.get(message) + (Float) value;
							f.set(message, klass.cast(i));
						} else if (updateType == CacheUpdate.DECREMENT) {
							i = (Float)f.get(message) - (Float) value;
							f.set(message, klass.cast(i));
						}
					}
				}
				cache.set(message.id, message);
				Vitals.store().set(message);
			}
			return message;
		}
	
	}
	
	public static class VitalsStore {
	
		private VitalsStore() {
		}
		
		private static class LazyHolder {
			private static final VitalsStore INSTANCE = new VitalsStore();
		}
	
		public static VitalsStore getInstance() {
			return LazyHolder.INSTANCE;
		}
		
		public static String scopeId(String playerId, String id) {
    		return playerId + "##" + id;
    	}
    
	    public static String unscopeId(String id) {
	    	if (id.contains("##")) {
	    		String[] parts = id.split("##");
	        	return parts[1];
	    	} else {
	    		throw new RuntimeException("Expected "+id+" to contain ##");
	    	}
	    }
	    
	    public static String defaultScope() {
	    	return defaultScope;
	    }
		
	    public void set(Vitals message) {
	    	set(defaultScope(),message);
		}
	    
	    public void delete(String id) {
	    	delete(defaultScope(),id);
	    }
	    
	    public Vitals get(String id, int timeout) {
	    	return get(defaultScope(),id,timeout);
	    }
	    
	    public void set(String scope, Vitals message) {
	    	Vitals clone = message.clone();
			clone.id = scopeId(scope,message.id);
			ActorSelection sel = ActorUtil.findDistributed("object_store", clone.id);
			sel.tell(clone, null);
		}
			
		public void delete(String scope, String id) {
			String scopedId = scopeId(scope,id);
			ActorSelection sel = ActorUtil.findDistributed("object_store", scopedId);
			ObjectdbDel del = new ObjectdbDel().setEntityId(scopedId);
			sel.tell(del, null);
		}
			
		public Vitals get(String scope, String id, int timeout) {
			String scopedId = scopeId(scope,id);
			ObjectdbGet get = new ObjectdbGet().setEntityId(scopedId).setKlass("Vitals");
			ActorSelection sel = ActorUtil.findDistributed("object_store", scopedId);
			Timeout t = new Timeout(Duration.create(timeout, TimeUnit.MILLISECONDS));
			AskableActorSelection askable = new AskableActorSelection(sel);
			Vitals message = null;
			Object result;
			Future<Object> future = askable.ask(get,t);
			try {
				result = Await.result(future, t.duration());
				if (result instanceof Vitals) {
					message = (Vitals)result;
				} else if (result instanceof ObjectdbStatus) {
					return null;
				}
			} catch (Exception e) {
				throw new RuntimeException("Operation timed out");
			}
			if (message == null) {
				return null;
			}
			message.id = unscopeId(message.id);
			return message;
		}
		
	}
	

	


	public static void clearModel(Model model) {
    	    	    	    	    	    	model.set("vitals_id",null);
    	    	    	    	    	    	model.set("vitals_health",null);
    	    	    	    	    	    	model.set("vitals_stamina",null);
    	    	    	    	    	    	model.set("vitals_magic",null);
    	    	    	    	    	    	model.set("vitals_last_combat",null);
    	    	    	    	    	    	model.set("vitals_dead",null);
    	    	    	    	    	    	model.set("vitals_armor",null);
    	    	    	    	    	    	model.set("vitals_spell_resist",null);
    	    	    	    	    	    	model.set("vitals_elemental_resist",null);
    	    	    	    	    	    	model.set("vitals_spell_penetration",null);
    	    	    	    	    	    	model.set("vitals_magic_regen",null);
    	    	    	    	    	    	model.set("vitals_health_regen",null);
    	    	    	    	    	    	model.set("vitals_stamina_regen",null);
    	    	    	    	    	    	model.set("vitals_changed",null);
    	    	    	    	    	    	model.set("vitals_grid",null);
    	    	    	    	    	    	model.set("vitals_stamina_drain",null);
    	    }
    
	public void toModel(Model model) {
    	    	    	    	
    	    	    	if (id != null) {
    	       	    	model.setString("vitals_id",id);
    	        		
    	}
    	    	    	    	    	
    	    	    	if (health != null) {
    	       	    	model.setInteger("vitals_health",health);
    	        		
    	}
    	    	    	    	    	
    	    	    	if (stamina != null) {
    	       	    	model.setInteger("vitals_stamina",stamina);
    	        		
    	}
    	    	    	    	    	
    	    	    	if (magic != null) {
    	       	    	model.setInteger("vitals_magic",magic);
    	        		
    	}
    	    	    	    	    	
    	    	    	if (lastCombat != null) {
    	       	    	model.setLong("vitals_last_combat",lastCombat);
    	        		
    	}
    	    	    	    	    	
    	    	    	if (dead != null) {
    	       	    	model.setInteger("vitals_dead",dead);
    	        		
    	}
    	    	    	    	    	
    	    	    	if (armor != null) {
    	       	    	model.setInteger("vitals_armor",armor);
    	        		
    	}
    	    	    	    	    	
    	    	    	if (spellResist != null) {
    	       	    	model.setInteger("vitals_spell_resist",spellResist);
    	        		
    	}
    	    	    	    	    	
    	    	    	if (elementalResist != null) {
    	       	    	model.setInteger("vitals_elemental_resist",elementalResist);
    	        		
    	}
    	    	    	    	    	
    	    	    	if (spellPenetration != null) {
    	       	    	model.setInteger("vitals_spell_penetration",spellPenetration);
    	        		
    	}
    	    	    	    	    	
    	    	    	if (magicRegen != null) {
    	       	    	model.setInteger("vitals_magic_regen",magicRegen);
    	        		
    	}
    	    	    	    	    	
    	    	    	if (healthRegen != null) {
    	       	    	model.setInteger("vitals_health_regen",healthRegen);
    	        		
    	}
    	    	    	    	    	
    	    	    	if (staminaRegen != null) {
    	       	    	model.setInteger("vitals_stamina_regen",staminaRegen);
    	        		
    	}
    	    	    	    	    	
    	    	    	if (changed != null) {
    	       	    	model.setInteger("vitals_changed",changed);
    	        		
    	}
    	    	    	    	    	
    	    	    	if (grid != null) {
    	       	    	model.setString("vitals_grid",grid);
    	        		
    	}
    	    	    	    	    	
    	    	    	if (staminaDrain != null) {
    	       	    	model.setInteger("vitals_stamina_drain",staminaDrain);
    	        		
    	}
    	    	    }
    
	public static Vitals fromModel(Model model) {
		boolean hasFields = false;
    	Vitals message = new Vitals();
    	    	    	    	    	
    	    	    	String idField = model.getString("vitals_id");
    	    	
    	if (idField != null) {
    		message.setId(idField);
    		hasFields = true;
    	}
    	    	    	    	    	    	
    	    	    	Integer healthField = model.getInteger("vitals_health");
    	    	
    	if (healthField != null) {
    		message.setHealth(healthField);
    		hasFields = true;
    	}
    	    	    	    	    	    	
    	    	    	Integer staminaField = model.getInteger("vitals_stamina");
    	    	
    	if (staminaField != null) {
    		message.setStamina(staminaField);
    		hasFields = true;
    	}
    	    	    	    	    	    	
    	    	    	Integer magicField = model.getInteger("vitals_magic");
    	    	
    	if (magicField != null) {
    		message.setMagic(magicField);
    		hasFields = true;
    	}
    	    	    	    	    	    	
    	    	    	Long lastCombatField = model.getLong("vitals_last_combat");
    	    	
    	if (lastCombatField != null) {
    		message.setLastCombat(lastCombatField);
    		hasFields = true;
    	}
    	    	    	    	    	    	
    	    	    	Integer deadField = model.getInteger("vitals_dead");
    	    	
    	if (deadField != null) {
    		message.setDead(deadField);
    		hasFields = true;
    	}
    	    	    	    	    	    	
    	    	    	Integer armorField = model.getInteger("vitals_armor");
    	    	
    	if (armorField != null) {
    		message.setArmor(armorField);
    		hasFields = true;
    	}
    	    	    	    	    	    	
    	    	    	Integer spellResistField = model.getInteger("vitals_spell_resist");
    	    	
    	if (spellResistField != null) {
    		message.setSpellResist(spellResistField);
    		hasFields = true;
    	}
    	    	    	    	    	    	
    	    	    	Integer elementalResistField = model.getInteger("vitals_elemental_resist");
    	    	
    	if (elementalResistField != null) {
    		message.setElementalResist(elementalResistField);
    		hasFields = true;
    	}
    	    	    	    	    	    	
    	    	    	Integer spellPenetrationField = model.getInteger("vitals_spell_penetration");
    	    	
    	if (spellPenetrationField != null) {
    		message.setSpellPenetration(spellPenetrationField);
    		hasFields = true;
    	}
    	    	    	    	    	    	
    	    	    	Integer magicRegenField = model.getInteger("vitals_magic_regen");
    	    	
    	if (magicRegenField != null) {
    		message.setMagicRegen(magicRegenField);
    		hasFields = true;
    	}
    	    	    	    	    	    	
    	    	    	Integer healthRegenField = model.getInteger("vitals_health_regen");
    	    	
    	if (healthRegenField != null) {
    		message.setHealthRegen(healthRegenField);
    		hasFields = true;
    	}
    	    	    	    	    	    	
    	    	    	Integer staminaRegenField = model.getInteger("vitals_stamina_regen");
    	    	
    	if (staminaRegenField != null) {
    		message.setStaminaRegen(staminaRegenField);
    		hasFields = true;
    	}
    	    	    	    	    	    	
    	    	    	Integer changedField = model.getInteger("vitals_changed");
    	    	
    	if (changedField != null) {
    		message.setChanged(changedField);
    		hasFields = true;
    	}
    	    	    	    	    	    	
    	    	    	String gridField = model.getString("vitals_grid");
    	    	
    	if (gridField != null) {
    		message.setGrid(gridField);
    		hasFields = true;
    	}
    	    	    	    	    	    	
    	    	    	Integer staminaDrainField = model.getInteger("vitals_stamina_drain");
    	    	
    	if (staminaDrainField != null) {
    		message.setStaminaDrain(staminaDrainField);
    		hasFields = true;
    	}
    	    	    	if (hasFields) {
    		return message;
    	} else {
    		return null;
    	}
    }


	    
    public Boolean hasId()  {
        return id == null ? false : true;
    }
        
		public String getId() {
		return id;
	}
	
	public Vitals setId(String id) {
		this.id = id;
		return this;	}
	
		    
    public Boolean hasHealth()  {
        return health == null ? false : true;
    }
        
		public Integer getHealth() {
		return health;
	}
	
	public Vitals setHealth(Integer health) {
		this.health = health;
		return this;	}
	
		    
    public Boolean hasStamina()  {
        return stamina == null ? false : true;
    }
        
		public Integer getStamina() {
		return stamina;
	}
	
	public Vitals setStamina(Integer stamina) {
		this.stamina = stamina;
		return this;	}
	
		    
    public Boolean hasMagic()  {
        return magic == null ? false : true;
    }
        
		public Integer getMagic() {
		return magic;
	}
	
	public Vitals setMagic(Integer magic) {
		this.magic = magic;
		return this;	}
	
		    
    public Boolean hasLastCombat()  {
        return lastCombat == null ? false : true;
    }
        
		public Long getLastCombat() {
		return lastCombat;
	}
	
	public Vitals setLastCombat(Long lastCombat) {
		this.lastCombat = lastCombat;
		return this;	}
	
		    
    public Boolean hasDead()  {
        return dead == null ? false : true;
    }
        
		public Integer getDead() {
		return dead;
	}
	
	public Vitals setDead(Integer dead) {
		this.dead = dead;
		return this;	}
	
		    
    public Boolean hasArmor()  {
        return armor == null ? false : true;
    }
        
		public Integer getArmor() {
		return armor;
	}
	
	public Vitals setArmor(Integer armor) {
		this.armor = armor;
		return this;	}
	
		    
    public Boolean hasSpellResist()  {
        return spellResist == null ? false : true;
    }
        
		public Integer getSpellResist() {
		return spellResist;
	}
	
	public Vitals setSpellResist(Integer spellResist) {
		this.spellResist = spellResist;
		return this;	}
	
		    
    public Boolean hasElementalResist()  {
        return elementalResist == null ? false : true;
    }
        
		public Integer getElementalResist() {
		return elementalResist;
	}
	
	public Vitals setElementalResist(Integer elementalResist) {
		this.elementalResist = elementalResist;
		return this;	}
	
		    
    public Boolean hasSpellPenetration()  {
        return spellPenetration == null ? false : true;
    }
        
		public Integer getSpellPenetration() {
		return spellPenetration;
	}
	
	public Vitals setSpellPenetration(Integer spellPenetration) {
		this.spellPenetration = spellPenetration;
		return this;	}
	
		    
    public Boolean hasMagicRegen()  {
        return magicRegen == null ? false : true;
    }
        
		public Integer getMagicRegen() {
		return magicRegen;
	}
	
	public Vitals setMagicRegen(Integer magicRegen) {
		this.magicRegen = magicRegen;
		return this;	}
	
		    
    public Boolean hasHealthRegen()  {
        return healthRegen == null ? false : true;
    }
        
		public Integer getHealthRegen() {
		return healthRegen;
	}
	
	public Vitals setHealthRegen(Integer healthRegen) {
		this.healthRegen = healthRegen;
		return this;	}
	
		    
    public Boolean hasStaminaRegen()  {
        return staminaRegen == null ? false : true;
    }
        
		public Integer getStaminaRegen() {
		return staminaRegen;
	}
	
	public Vitals setStaminaRegen(Integer staminaRegen) {
		this.staminaRegen = staminaRegen;
		return this;	}
	
		    
    public Boolean hasChanged()  {
        return changed == null ? false : true;
    }
        
		public Integer getChanged() {
		return changed;
	}
	
	public Vitals setChanged(Integer changed) {
		this.changed = changed;
		return this;	}
	
		    
    public Boolean hasGrid()  {
        return grid == null ? false : true;
    }
        
		public String getGrid() {
		return grid;
	}
	
	public Vitals setGrid(String grid) {
		this.grid = grid;
		return this;	}
	
		    
    public Boolean hasStaminaDrain()  {
        return staminaDrain == null ? false : true;
    }
        
		public Integer getStaminaDrain() {
		return staminaDrain;
	}
	
	public Vitals setStaminaDrain(Integer staminaDrain) {
		this.staminaDrain = staminaDrain;
		return this;	}
	
	
  
    // java serialization

    public void readExternal(ObjectInput in) throws IOException
    {
        GraphIOUtil.mergeDelimitedFrom(in, this, this);
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        GraphIOUtil.writeDelimitedTo(out, this, this);
    }

    // message method

    public Schema<Vitals> cachedSchema()
    {
        return DEFAULT_INSTANCE;
    }

    // schema methods

    public Vitals newMessage()
    {
        return new Vitals();
    }

    public Class<Vitals> typeClass()
    {
        return Vitals.class;
    }

    public String messageName()
    {
        return Vitals.class.getSimpleName();
    }

    public String messageFullName()
    {
        return Vitals.class.getName();
    }

    public boolean isInitialized(Vitals message)
    {
        return true;
    }

    public void mergeFrom(Input input, Vitals message) throws IOException
    {
        for(int number = input.readFieldNumber(this);; number = input.readFieldNumber(this))
        {
            switch(number)
            {
                case 0:
                    return;
                            	case 1:
            	                	                	message.id = input.readString();
                	break;
                	                	
                            	            	case 2:
            	                	                	message.health = input.readInt32();
                	break;
                	                	
                            	            	case 3:
            	                	                	message.stamina = input.readInt32();
                	break;
                	                	
                            	            	case 4:
            	                	                	message.magic = input.readInt32();
                	break;
                	                	
                            	            	case 5:
            	                	                	message.lastCombat = input.readInt64();
                	break;
                	                	
                            	            	case 6:
            	                	                	message.dead = input.readInt32();
                	break;
                	                	
                            	            	case 7:
            	                	                	message.armor = input.readInt32();
                	break;
                	                	
                            	            	case 8:
            	                	                	message.spellResist = input.readInt32();
                	break;
                	                	
                            	            	case 9:
            	                	                	message.elementalResist = input.readInt32();
                	break;
                	                	
                            	            	case 10:
            	                	                	message.spellPenetration = input.readInt32();
                	break;
                	                	
                            	            	case 11:
            	                	                	message.magicRegen = input.readInt32();
                	break;
                	                	
                            	            	case 12:
            	                	                	message.healthRegen = input.readInt32();
                	break;
                	                	
                            	            	case 13:
            	                	                	message.staminaRegen = input.readInt32();
                	break;
                	                	
                            	            	case 14:
            	                	                	message.changed = input.readInt32();
                	break;
                	                	
                            	            	case 15:
            	                	                	message.grid = input.readString();
                	break;
                	                	
                            	            	case 16:
            	                	                	message.staminaDrain = input.readInt32();
                	break;
                	                	
                            	            
                default:
                    input.handleUnknownField(number, this);
            }   
        }
    }


    public void writeTo(Output output, Vitals message) throws IOException
    {
    	    	
    	    	if(message.id == null)
            throw new UninitializedMessageException(message);
    	    	
    	    	    	if(message.id != null)
            output.writeString(1, message.id, false);
    	    	
    	            	
    	    	
    	    	    	if(message.health != null)
            output.writeInt32(2, message.health, false);
    	    	
    	            	
    	    	
    	    	    	if(message.stamina != null)
            output.writeInt32(3, message.stamina, false);
    	    	
    	            	
    	    	
    	    	    	if(message.magic != null)
            output.writeInt32(4, message.magic, false);
    	    	
    	            	
    	    	
    	    	    	if(message.lastCombat != null)
            output.writeInt64(5, message.lastCombat, false);
    	    	
    	            	
    	    	
    	    	    	if(message.dead != null)
            output.writeInt32(6, message.dead, false);
    	    	
    	            	
    	    	
    	    	    	if(message.armor != null)
            output.writeInt32(7, message.armor, false);
    	    	
    	            	
    	    	
    	    	    	if(message.spellResist != null)
            output.writeInt32(8, message.spellResist, false);
    	    	
    	            	
    	    	
    	    	    	if(message.elementalResist != null)
            output.writeInt32(9, message.elementalResist, false);
    	    	
    	            	
    	    	
    	    	    	if(message.spellPenetration != null)
            output.writeInt32(10, message.spellPenetration, false);
    	    	
    	            	
    	    	
    	    	    	if(message.magicRegen != null)
            output.writeInt32(11, message.magicRegen, false);
    	    	
    	            	
    	    	
    	    	    	if(message.healthRegen != null)
            output.writeInt32(12, message.healthRegen, false);
    	    	
    	            	
    	    	
    	    	    	if(message.staminaRegen != null)
            output.writeInt32(13, message.staminaRegen, false);
    	    	
    	            	
    	    	
    	    	    	if(message.changed != null)
            output.writeInt32(14, message.changed, false);
    	    	
    	            	
    	    	
    	    	    	if(message.grid != null)
            output.writeString(15, message.grid, false);
    	    	
    	            	
    	    	
    	    	    	if(message.staminaDrain != null)
            output.writeInt32(16, message.staminaDrain, false);
    	    	
    	            	
    }

	public void dumpObject()
    {
    	System.out.println("START Vitals");
    	    	if(this.id != null) {
    		System.out.println("id="+this.id);
    	}
    	    	if(this.health != null) {
    		System.out.println("health="+this.health);
    	}
    	    	if(this.stamina != null) {
    		System.out.println("stamina="+this.stamina);
    	}
    	    	if(this.magic != null) {
    		System.out.println("magic="+this.magic);
    	}
    	    	if(this.lastCombat != null) {
    		System.out.println("lastCombat="+this.lastCombat);
    	}
    	    	if(this.dead != null) {
    		System.out.println("dead="+this.dead);
    	}
    	    	if(this.armor != null) {
    		System.out.println("armor="+this.armor);
    	}
    	    	if(this.spellResist != null) {
    		System.out.println("spellResist="+this.spellResist);
    	}
    	    	if(this.elementalResist != null) {
    		System.out.println("elementalResist="+this.elementalResist);
    	}
    	    	if(this.spellPenetration != null) {
    		System.out.println("spellPenetration="+this.spellPenetration);
    	}
    	    	if(this.magicRegen != null) {
    		System.out.println("magicRegen="+this.magicRegen);
    	}
    	    	if(this.healthRegen != null) {
    		System.out.println("healthRegen="+this.healthRegen);
    	}
    	    	if(this.staminaRegen != null) {
    		System.out.println("staminaRegen="+this.staminaRegen);
    	}
    	    	if(this.changed != null) {
    		System.out.println("changed="+this.changed);
    	}
    	    	if(this.grid != null) {
    		System.out.println("grid="+this.grid);
    	}
    	    	if(this.staminaDrain != null) {
    		System.out.println("staminaDrain="+this.staminaDrain);
    	}
    	    	System.out.println("END Vitals");
    }
    
    public String getFieldName(int number)
    {
        switch(number)
        {
        	        	case 1: return "id";
        	        	case 2: return "health";
        	        	case 3: return "stamina";
        	        	case 4: return "magic";
        	        	case 5: return "lastCombat";
        	        	case 6: return "dead";
        	        	case 7: return "armor";
        	        	case 8: return "spellResist";
        	        	case 9: return "elementalResist";
        	        	case 10: return "spellPenetration";
        	        	case 11: return "magicRegen";
        	        	case 12: return "healthRegen";
        	        	case 13: return "staminaRegen";
        	        	case 14: return "changed";
        	        	case 15: return "grid";
        	        	case 16: return "staminaDrain";
        	            default: return null;
        }
    }

    public int getFieldNumber(String name)
    {
        final Integer number = __fieldMap.get(name);
        return number == null ? 0 : number.intValue();
    }

    private static final java.util.HashMap<String,Integer> __fieldMap = new java.util.HashMap<String,Integer>();
    static
    {
    	    	__fieldMap.put("id", 1);
    	    	__fieldMap.put("health", 2);
    	    	__fieldMap.put("stamina", 3);
    	    	__fieldMap.put("magic", 4);
    	    	__fieldMap.put("lastCombat", 5);
    	    	__fieldMap.put("dead", 6);
    	    	__fieldMap.put("armor", 7);
    	    	__fieldMap.put("spellResist", 8);
    	    	__fieldMap.put("elementalResist", 9);
    	    	__fieldMap.put("spellPenetration", 10);
    	    	__fieldMap.put("magicRegen", 11);
    	    	__fieldMap.put("healthRegen", 12);
    	    	__fieldMap.put("staminaRegen", 13);
    	    	__fieldMap.put("changed", 14);
    	    	__fieldMap.put("grid", 15);
    	    	__fieldMap.put("staminaDrain", 16);
    	    }
   
   public static List<String> getFields() {
	ArrayList<String> fieldNames = new ArrayList<String>();
	String fieldName = null;
	Integer i = 1;
	
    while(true) { 
		fieldName = Vitals.getSchema().getFieldName(i);
		if (fieldName == null) {
			break;
		}
		fieldNames.add(fieldName);
		i++;
	}
	return fieldNames;
}

public static Vitals parseFrom(byte[] bytes) {
	Vitals message = new Vitals();
	ProtobufIOUtil.mergeFrom(bytes, message, Vitals.getSchema());
	return message;
}

public static Vitals parseFromJson(String json) throws IOException {
	byte[] bytes = json.getBytes(Charset.forName("UTF-8"));
	Vitals message = new Vitals();
	JsonIOUtil.mergeFrom(bytes, message, Vitals.getSchema(), false);
	return message;
}

public Vitals clone() {
	byte[] bytes = this.toByteArray();
	Vitals vitals = Vitals.parseFrom(bytes);
	return vitals;
}
	
public byte[] toByteArray() {
	return toProtobuf();
	//return toJson();
}

public String toJson() {
	boolean numeric = false;
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	try {
		JsonIOUtil.writeTo(out, this, Vitals.getSchema(), numeric);
	} catch (IOException e) {
		e.printStackTrace();
		throw new RuntimeException("Json encoding failed");
	}
	String json = new String(out.toByteArray(), Charset.forName("UTF-8"));
	return json;
}

public byte[] toPrefixedByteArray() {
	LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
  Schema<Vitals> schema = Vitals.getSchema();
    
	final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ProtobufOutput output = new ProtobufOutput(buffer);
    try
    {
    	schema.writeTo(output, this);
        final int size = output.getSize();
        ProtobufOutput.writeRawVarInt32Bytes(out, size);
        final int msgSize = LinkedBuffer.writeTo(out, buffer);
        assert size == msgSize;
        
        buffer.clear();
        return out.toByteArray();
    }
    catch (IOException e)
    {
        throw new RuntimeException("Serializing to a byte array threw an IOException " + 
                "(should never happen).", e);
    }
 
}

public byte[] toProtobuf() {
	LinkedBuffer buffer = LocalLinkedBuffer.get();
	byte[] bytes = null;

	try {
		bytes = ProtobufIOUtil.toByteArray(this, Vitals.getSchema(), buffer);
		buffer.clear();
	} catch (Exception e) {
		buffer.clear();
		e.printStackTrace();
		throw new RuntimeException("Protobuf encoding failed "+e.getMessage());
	}
	return bytes;
}

public ByteBuf toByteBuf() {
	ByteBuf bb = Unpooled.buffer(512, 2048);
	LinkedBuffer buffer = LinkedBuffer.use(bb.array());

	try {
		ProtobufIOUtil.writeTo(buffer, this, Vitals.getSchema());
	} catch (Exception e) {
		e.printStackTrace();
		throw new RuntimeException("Protobuf encoding failed "+e.getMessage());
	}
	return bb;
}

}