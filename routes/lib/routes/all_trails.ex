defmodule Routes.AllTrails do

  alias Routes.API

  def all(sender) do
    spawn __MODULE__, :trail_list, []
  end

  defp trail_list() do
    res = API.post(API.url("getAllTrails", alt: :json), []) |> API.json_response
    IO.puts inspect(res)
  end


end
