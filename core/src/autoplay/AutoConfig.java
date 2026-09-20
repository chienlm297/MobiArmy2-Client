package autoplay;
import java.util.Map;
public final class AutoConfig {
 public final String role, partner, host, output;
 public final java.util.Set<Integer> botIds;
 public final int participants;
 public final int port, room, board, matches, angle, force, mapId;
 public final long lobbyDelay, shotDelay;
 public final long stepTimeout, partnerTimeout, matchTimeout, totalTimeout;
 public final boolean enabled, heal, relogin;
 public AutoConfig(Map<String,String> e) {
  java.util.Set<Integer> ids=new java.util.HashSet<>();
  for(String value:e.getOrDefault("AUTO_BOT_IDS",e.getOrDefault("AUTO_BOT_ID", "")).split(","))if(!value.trim().isEmpty()) {
   int id=Integer.parseInt(value.trim());if(id>=-1)throw new IllegalArgumentException("Bot IDs must be below -1");ids.add(id);
  }
  if(ids.size()>6)throw new IllegalArgumentException("At most six fixture bots");
  botIds=java.util.Collections.unmodifiableSet(ids);participants=2+ids.size();
  enabled=Boolean.parseBoolean(e.getOrDefault("AUTO_ENABLED","false"));
  role=e.getOrDefault("AUTO_ROLE","A"); if(!role.equals("A")&&!role.equals("B"))throw new IllegalArgumentException("AUTO_ROLE must be A/B");
  partner=e.getOrDefault("AUTO_PARTNER",""); host=e.getOrDefault("AUTO_HOST","127.0.0.1");
  output=e.getOrDefault("AUTO_RUN_DIR","test-results/manual");
  port=number(e,"AUTO_PORT",8122,1,65535); room=number(e,"AUTO_ROOM",0,0,127); board=number(e,"AUTO_BOARD",0,0,127);
  mapId=number(e,"AUTO_MAP_ID",-1,-1,29);
  matches=number(e,"AUTO_MATCHES",3,1,100); angle=number(e,"AUTO_ANGLE",45,0,180);force=number(e,"AUTO_FORCE",20,1,30);
  lobbyDelay=number(e,"AUTO_LOBBY_DELAY_MS",0,0,30000);shotDelay=number(e,"AUTO_SHOT_DELAY_MS",1000,0,10000);
  stepTimeout=number(e,"AUTO_STEP_TIMEOUT_SECONDS",15,5,120)*1000L;partnerTimeout=60000;matchTimeout=number(e,"AUTO_MATCH_TIMEOUT_SECONDS",600,30,3600)*1000L;
  totalTimeout=number(e,"AUTO_TOTAL_TIMEOUT_SECONDS",1800,60,86400)*1000L;
  heal=Boolean.parseBoolean(e.getOrDefault("AUTO_HEAL","false")); relogin=Boolean.parseBoolean(e.getOrDefault("AUTO_RELOGIN","true"));
 }
 private static int number(Map<String,String> e,String key,int def,int min,int max){int v=Integer.parseInt(e.getOrDefault(key,""+def));if(v<min||v>max)throw new IllegalArgumentException(key+" out of range");return v;}
}
