defmodule Routes.API do

  use HTTPoison.Base

  @api "https://mapaton-public.appspot.com/_ah/api/dashboardAPI/v1/"

  defp url(path, options \\ []) do
    Path.join([@api, path]) <> query_string(options)
  end

  defp query_string([]), do: ""
  defp query_string(options) do
    s = Enum.map(options, fn {k, v} -> "#{k}=#{v}" end) |> Enum.join("&")
    "?#{s}"
  end

  defp json_response({:ok, %{body: body}}) do
    Poison.Parser.parse!(body)
  end


  defp process_request_body(body) when is_list(body) do
    Enum.reduce(body, %{}, fn {k,v}, m -> Map.put(m, k, v) end)
    |> process_request_body
  end

  defp process_request_body(body = %{}) do
    Poison.encode!(body)
  end

end
