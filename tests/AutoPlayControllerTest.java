import autoplay.*;
import java.util.*;
public class AutoPlayControllerTest {
 static int checks;
 static class Fake implements AutoPlayController.Port {
  int login,join,ready,start,shots,heals,back,disconnect;boolean host=true;
  public void login(){login++;} public void join(){join++;}public void ready(){ready++;}public void startMatch(){start++;}public void shoot(){shots++;}public void heal(){heals++;}
  public void backToRoom(){back++;}public void disconnect(){disconnect++;}public void event(String t,String d){}public void markHostReady(){}public boolean hostReady(){return host;}
 }
 static void ok(boolean v,String s){if(!v)throw new AssertionError(s);checks++;System.out.println("PASS "+s);}
 static AutoConfig config(String role,boolean heal){Map<String,String> m=new HashMap<>();m.put("AUTO_ROLE",role);m.put("AUTO_HEAL",""+heal);return new AutoConfig(m);}
 static AutoPlayController.Frame frame(){AutoPlayController.Frame f=new AutoPlayController.Frame();f.connected=true;f.loggedIn=true;f.correctOwner=true;f.correctRoom=true;return f;}
 static void joined(AutoPlayController c, AutoPlayController.Frame f){c.enable(0);c.tick(1,f);c.tick(2,f);f.inRoom=true;f.participants=2;c.tick(3,f);}
 public static void main(String[]args){
  Fake p=new Fake();AutoPlayController c=new AutoPlayController(config("A",false),p);AutoPlayController.Frame f=frame();joined(c,f);
  ok(p.login==1&&p.join==1,"one login and join");
  c.tick(4,f);ok(p.start==0,"host waits for readiness");f.partnerReady=true;c.tick(5,f);c.tick(6,f);ok(p.start==1,"one start request");
  f.inRoom=false;f.inGame=true;f.matchSerial=1;c.tick(7,f);f.myTurn=true;f.alive=true;f.turnSerial=1;c.tick(1010,f);c.tick(1020,f);ok(p.shots==1,"no duplicate shot within turn");
  f.myTurn=false;f.turnSerial=2;c.tick(2000,f);ok(p.shots==1,"no shot on opponent turn");f.myTurn=true;f.turnSerial=3;c.tick(3000,f);ok(p.shots==2,"new own turn shoots once");
  f.resultSerial=1;c.tick(4000,f);c.tick(4010,f);ok(c.completed==1,"result counted once");c.tick(7100,f);ok(p.back==0,"waits for server rematch cooldown");c.tick(16100,f);ok(p.back==1,"returns after result");
  long t=17000;
  for(int match=2;match<=3;match++){
   f.inRoom=true;f.inGame=false;f.partnerReady=true;c.tick(t,f);f.inRoom=false;f.inGame=true;f.matchSerial=match;c.tick(t+1,f);
   f.resultSerial=match;c.tick(t+2000,f);c.tick(t+14100,f);t+=15000;
  }
  ok(c.completed==3&&p.disconnect==1,"three confirmed matches trigger relogin");f.loggedIn=false;f.connected=false;c.tick(t+2000,f);ok(p.login==2,"fresh login requested");f.connected=true;f.loggedIn=true;c.tick(t+2001,f);ok(c.state==AutoPlayController.State.DONE,"done only after confirmed relogin");
  p=new Fake();c=new AutoPlayController(config("A",false),p);f=frame();joined(c,f);c.stop();f.partnerReady=true;c.tick(999,f);ok(p.start==0&&c.state==AutoPlayController.State.OFF,"stop cancels pending actions");
  p=new Fake();c=new AutoPlayController(config("A",false),p);f=frame();joined(c,f);f.unexpectedPlayer=true;c.tick(4,f);ok(c.state==AutoPlayController.State.FAILED,"unexpected player rejected");
  p=new Fake();c=new AutoPlayController(config("A",false),p);f=frame();joined(c,f);f.correctRoom=false;c.tick(4,f);ok(c.state==AutoPlayController.State.FAILED,"wrong room rejected");
  p=new Fake();c=new AutoPlayController(config("A",false),p);f=frame();joined(c,f);f.correctOwner=false;c.tick(4,f);ok(c.state==AutoPlayController.State.FAILED,"wrong owner rejected");
  p=new Fake();c=new AutoPlayController(config("B",false),p);p.host=false;f=frame();c.enable(0);c.tick(1,f);c.tick(2,f);ok(p.join==0,"B waits for A room marker");p.host=true;c.tick(3,f);f.inRoom=true;f.participants=2;c.tick(4,f);c.tick(5,f);c.tick(6,f);ok(p.ready==1,"B sends ready once");f.inRoom=false;f.inGame=true;f.matchSerial=1;c.tick(7,f);ok(c.state==AutoPlayController.State.PLAY,"early start event accepted after ready request");
  c.tick(610000,f);ok(c.state==AutoPlayController.State.FAILED&&c.completed==0,"match timeout is failure not success");
  p=new Fake();c=new AutoPlayController(config("A",false),p);f=frame();joined(c,f);f.connected=false;c.tick(4,f);ok(c.state==AutoPlayController.State.FAILED,"disconnect stops automation");
  p=new Fake();c=new AutoPlayController(config("A",true),p);f=frame();joined(c,f);f.partnerReady=true;c.tick(4,f);f.inGame=true;f.matchSerial=1;c.tick(5,f);f.myTurn=true;f.alive=true;f.turnSerial=1;f.canHeal=true;c.tick(2000,f);c.tick(2001,f);ok(p.heals==1&&p.shots==0,"heal waits for acknowledgement");f.itemSerial++;c.tick(2002,f);ok(p.shots==1,"shoot follows item acknowledgement");c.stop();f.turnSerial++;c.tick(3000,f);ok(p.shots==1,"stop during combat prevents new shot");
  p=new Fake();c=new AutoPlayController(config("A",false),p);c.enable(0);f=frame();f.loggedIn=false;c.tick(16000,f);ok(c.state==AutoPlayController.State.FAILED,"login timeout");
  Map<String,String> withBots=new HashMap<>();withBots.put("AUTO_BOT_IDS","-50,-51");
  p=new Fake();c=new AutoPlayController(new AutoConfig(withBots),p);f=frame();joined(c,f);f.partnerReady=true;c.tick(4,f);
  ok(p.start==0,"fixture waits for all invited bots");f.participants=4;c.tick(5,f);ok(p.start==1,"fixture starts only with expected participant count");
  try{withBots.put("AUTO_BOT_IDS","-1");new AutoConfig(withBots);throw new AssertionError("invalid bot ID accepted");}catch(IllegalArgumentException expected){ok(true,"fixture rejects empty-seat sentinel as bot ID");}
  Map<String,String> mapConfig=new HashMap<>();
  ok(new AutoConfig(mapConfig).mapId==-1,"map override disabled by default");
  mapConfig.put("AUTO_MAP_ID","2");ok(new AutoConfig(mapConfig).mapId==2,"explicit PvP map accepted");
  mapConfig.put("AUTO_MAP_ID","30");try{new AutoConfig(mapConfig);throw new AssertionError("boss map accepted");}catch(IllegalArgumentException expected){ok(true,"fixture map limited to PvP maps");}
  System.out.println(checks+" controller checks passed");
 }
}
