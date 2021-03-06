module Wavefront
  class Vertex
    attr_reader :position, :uv, :normal, :position_index, :texture_index, :normal_index
    alias :tex :uv

    def initialize p, uv, n, p_index, t_index, n_index
      raise "cannot initialize vertex without a position!" if p.nil?
      @position, @uv, @normal = p, uv, n
      @position_index, @texture_index, @normal_index = p_index, t_index, n_index
    end

    def composite_index
      "p_#{position_index}_n_#{normal_index}_t#{texture_index}"
    end

  end
end