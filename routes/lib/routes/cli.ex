defmodule Routes.CLI do

  alias Routes.AllTrails

  def main([]) do
    IO.puts "JAJA"
    _pid = spawn(AllTrails, :all, [self()])
    receive do
      {:trails, trails} ->
        IO.puts inspect({:trails, trails})
    end
  end


end
