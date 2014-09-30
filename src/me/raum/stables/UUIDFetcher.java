package me.raum.stables;

import com.google.common.collect.ImmutableList;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

public class UUIDFetcher
  implements Callable<Map<String, UUID>>
{
  private static final int MAX_SEARCH = 100;
  private static final String PROFILE_URL = "https://api.mojang.com/profiles/page/";
  private static final String AGENT = "minecraft";
  private final JSONParser jsonParser = new JSONParser();
  private final List<String> names;
  
  public UUIDFetcher(List<String> names)
  {
    this.names = ImmutableList.copyOf(names);
  }
  
  public Map<String, UUID> call()
    throws Exception
  {
    Map<String, UUID> uuidMap = new HashMap();
    String body = buildBody(this.names);
    for (int i = 1; i < 100; i++)
    {
      HttpURLConnection connection = createConnection(i);
      writeBody(connection, body);
      JSONObject jsonObject = (JSONObject)this.jsonParser.parse(new InputStreamReader(connection.getInputStream()));
      JSONArray array = (JSONArray)jsonObject.get("profiles");
      Number count = (Number)jsonObject.get("size");
      if (count.intValue() == 0) {
        break;
      }
      for (Object profile : array)
      {
        JSONObject jsonProfile = (JSONObject)profile;
        String id = (String)jsonProfile.get("id");
        String name = (String)jsonProfile.get("name");
        UUID uuid = UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20, 32));
        uuidMap.put(name, uuid);
      }
    }
    return uuidMap;
  }
  
  private static void writeBody(HttpURLConnection connection, String body)
    throws Exception
  {
    DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
    writer.write(body.getBytes());
    writer.flush();
    writer.close();
  }
  
  private static HttpURLConnection createConnection(int page)
    throws Exception
  {
    URL url = new URL("https://api.mojang.com/profiles/page/" + page);
    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setUseCaches(false);
    connection.setDoInput(true);
    connection.setDoOutput(true);
    return connection;
  }
  
  private static String buildBody(List<String> names)
  {
    List<JSONObject> lookups = new ArrayList();
    for (String name : names)
    {
      JSONObject obj = new JSONObject();
      obj.put("name", name);
      obj.put("agent", "minecraft");
      lookups.add(obj);
    }
    return JSONValue.toJSONString(lookups);
  }
}
