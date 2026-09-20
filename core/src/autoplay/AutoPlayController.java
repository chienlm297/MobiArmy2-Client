package autoplay;

/** All calls occur on the client's update thread. A pending command is never retried blindly. */
public final class AutoPlayController {
 public enum State { OFF, LOGIN, JOIN, PARTNER, START, PLAY, RESULT, RELOGIN_WAIT, RELOGIN, DONE, FAILED }
 public interface Port {
  void login(); void join(); void ready(); void startMatch(); void shoot(); void heal();
  void backToRoom(); void disconnect(); void event(String type,String detail); void markHostReady(); boolean hostReady();
 }
 public static final class Frame {
  public boolean loggedIn, connected, inRoom, inGame, ready, partnerReady, myTurn, alive, canHeal;
  public boolean correctRoom=true, correctOwner=true, unexpectedPlayer;
  public int participants; public long matchSerial, turnSerial, resultSerial, itemSerial;
 }
 public State state=State.OFF;
 public int completed;
 private final AutoConfig config; private final Port port;
 private long started, entered, matchStarted, lastShot=-1, activeMatch, lastResult, lastHeal=-1, itemBefore, healAt;
 private boolean readySent, healing;
 public AutoPlayController(AutoConfig c,Port p){config=c;port=p;}
 public boolean running(){return state!=State.OFF&&state!=State.DONE&&state!=State.FAILED;}
 public void enable(long now){if(running())return;completed=0;readySent=false;started=now;lastShot=-1;activeMatch=0;lastResult=0;lastHeal=-1;healing=false;transition(State.LOGIN,now);port.login();}
 public void stop(){state=State.OFF;healing=false;port.event("STOP","Manual stop; pending actions cancelled");}
 public void fail(String message){if(!running())return;state=State.FAILED;healing=false;port.event("FAILED",message);}
 private void transition(State s,long now){state=s;entered=now;port.event("STATE",s.name());}
 public void tick(long now,Frame f){
  if(!running())return;
  if(now-started>config.totalTimeout){fail("Total timeout");return;}
  if(state!=State.LOGIN&&state!=State.RELOGIN&&state!=State.RELOGIN_WAIT&&!f.connected){fail("Disconnected");return;}
  long limit=(state==State.PARTNER||state==State.JOIN)?config.partnerTimeout:config.stepTimeout;
  if(state!=State.PLAY&&state!=State.RESULT&&state!=State.RELOGIN_WAIT&&now-entered>limit){fail("Timeout: "+state);return;}
  switch(state){
   case LOGIN:
    if(f.loggedIn){lastResult=f.resultSerial;transition(State.JOIN,now);}
    break;
   case JOIN:
    if(!readySent){if(config.role.equals("B")&&!port.hostReady())break;port.join();readySent=true;}
    if(f.inRoom){readySent=false;transition(State.PARTNER,now);}
    break;
   case PARTNER:
    if(readySent&&f.inGame&&f.matchSerial>activeMatch){activeMatch=f.matchSerial;matchStarted=now;lastShot=-1;lastHeal=-1;healing=false;transition(State.PLAY,now);break;}
    if(!f.inRoom||!f.correctRoom||!f.correctOwner||f.unexpectedPlayer){fail("Wrong room/owner or unexpected participant");break;}
    if(config.role.equals("A"))port.markHostReady();
    if(f.participants!=config.participants)break;
    if(config.role.equals("B")){if(!readySent&&!f.ready){port.ready();readySent=true;} if(f.ready)transition(State.START,now);}
    else if(f.partnerReady && now-entered>=config.lobbyDelay){port.startMatch();transition(State.START,now);}
    break;
   case START:
    if(f.inGame&&f.matchSerial>activeMatch){activeMatch=f.matchSerial;lastShot=-1;lastHeal=-1;healing=false;matchStarted=now;transition(State.PLAY,now);}
    break;
   case PLAY:
    if(f.resultSerial>lastResult){lastResult=f.resultSerial;completed++;port.event("MATCH_COMPLETE",completed+" server-confirmed");transition(State.RESULT,now);break;}
    if(now-matchStarted>config.matchTimeout){fail("Match timeout (not counted as completed)");break;}
    if(!f.myTurn||!f.alive){healing=false;break;}
    if(lastShot==f.turnSerial)break;
    if(healing){if(f.itemSerial>itemBefore){healing=false;}else {if(now-healAt>config.stepTimeout)fail("Item acknowledgement timeout");break;}}
    if(config.heal&&f.canHeal&&lastHeal!=f.turnSerial){lastHeal=f.turnSerial;itemBefore=f.itemSerial;healing=true;healAt=now;port.heal();break;}
    if(now-entered<config.shotDelay)break;
    lastShot=f.turnSerial;port.shoot();break;
   case RESULT:
    if(now-entered<12000)break;
    port.backToRoom();readySent=false;
    if(completed>=config.matches){if(config.relogin){port.disconnect();transition(State.RELOGIN_WAIT,now);}else transition(State.DONE,now);}
    else transition(State.PARTNER,now);
    break;
   case RELOGIN_WAIT:
    if(now-entered>=2000&&!f.connected){transition(State.RELOGIN,now);port.login();}
    else if(now-entered>config.stepTimeout)fail("Disconnect not confirmed");
    break;
   case RELOGIN:
    if(f.loggedIn){port.event("RELOGIN_CONFIRMED","Fresh server login received");transition(State.DONE,now);}
    break;
   default: break;
  }
 }
}
