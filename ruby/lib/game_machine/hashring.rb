module GameMachine
  class Hashring

    RING_SIZE = 160
    REPLICAS = 160

    attr_reader :buckets

    def self.create_actor_ring(name)
      buckets = (0..RING_SIZE).map {|r| "#{name}#{r}"}
      new(buckets)
    end

    def initialize(buckets)
      @buckets = buckets
      @ring = ConsistentHashing::Ring.new(@buckets,REPLICAS)
    end

    def remove_bucket(bucket)
      if @buckets.include?(bucket)
        @buckets.delete_if {|b| b == bucket}
        @ring.delete(bucket)
      end
    end

    def add_bucket(bucket)
      unless @buckets.include?(bucket)
        @ring << bucket
        @buckets << bucket
      end
    end

    def bucket_for(value)
      @ring.node_for(value)
    end

  end
end
