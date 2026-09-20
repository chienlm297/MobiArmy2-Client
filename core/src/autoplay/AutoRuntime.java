package autoplay;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import coreLG.*;
import com.teamobi.mobiarmy2.GameMidlet;
import network.*;
import screen.*;
import player.*;
import model.PlayerInfo;

/** Adapter invoked on MainGame's update thread, after inbound packets are processed. */
public final class AutoRuntime implements AutoPlayController.Port {
 private static AutoRuntime instance;
 private static boolean attempted;
 private final AutoConfig config;
 private final AutoRecorder recorder;
 private final AutoPlayController controller;
 private boolean loggedIn,hostMarked;
 private long matches,turns,results,items,pendingLogin;
 private int board=-1,lastActor=-1;
 private String terminal="";
 private final java.util.List<String> pendingScreenshots=new java.util.ArrayList<>();
 private AutoRuntime()throws Exception{
  config=new AutoConfig(System.getenv());
  if(config.partner.isEmpty())throw new IllegalArgumentException("AUTO_PARTNER must name the other character");
  if(System.getenv("AUTO_USERNAME")==null||System.getenv("AUTO_PASSWORD")==null)throw new IllegalArgumentException("AUTO_USERNAME/AUTO_PASSWORD required");
  if(System.getenv("AUTO_PROFILE_DIR")==null)throw new IllegalArgumentException("AUTO_PROFILE_DIR required for isolated client data");
  recorder=new AutoRecorder(config);controller=new AutoPlayController(config,this);
 }
 public static void tick(){
  if(!attempted&&Boolean.parseBoolean(System.getenv("AUTO_ENABLED"))&&CCanvas.serverListScreen!=null){attempted=true;toggle();}
  if(instance==null)return;
  try{if(instance.pendingLogin>0 && System.currentTimeMillis()>=instance.pendingLogin){instance.pendingLogin=0;instance.performLogin();}instance.controller.tick(System.currentTimeMillis(),instance.frame());instance.writeTerminal();}
  catch(Exception e){e.printStackTrace();instance.controller.fail("Client action error: "+e.getClass().getSimpleName());}
 }
 public static void toggle(){
  try{if(instance==null)instance=new AutoRuntime();if(instance.controller.running())stop();else {instance.hostMarked=false;instance.terminal="";instance.matches=instance.turns=instance.results=instance.items=0;instance.controller.enable(System.currentTimeMillis());}}
  catch(Exception e){e.printStackTrace();CCanvas.startOKDlg("Auto-play: "+e.getMessage());}
 }
 public static void stop(){if(instance!=null){instance.pendingLogin=0;instance.controller.stop();}}
 public static String status(){return instance==null?"":("AUTO "+instance.config.role+" "+instance.controller.state+" "+instance.controller.completed+"/"+instance.config.matches+" | F8 toggle  F9 stop");}
 public static void loginConfirmed(){if(instance!=null){instance.loggedIn=true;instance.event("LOGIN_CONFIRMED",instance.wallet());}}
 public static void error(String reason){if(instance!=null){instance.controller.fail(reason);}}
 public static void room(int value){if(instance!=null){instance.board=value;instance.event("ROOM_METADATA",PrepareScr.currentRoom+"/"+value);}}
 public static void match(){if(instance!=null){instance.matches++;instance.lastActor=-1;instance.event("MATCH_START",""+instance.matches);instance.event("MATCH_MAP",String.valueOf(map.MM.mapID));if(PM.p!=null)for(int i=0;i<PM.p.length;i++)if(PM.p[i]!=null)instance.event("ACTOR",i+":"+PM.p[i].IDDB);}}
 public static void turn(int who){if(instance!=null&&who!=instance.lastActor){instance.lastActor=who;instance.turns++;instance.event("TURN",who+":"+instance.turns);}}
 public static void result(){if(instance!=null&&instance.controller.state==AutoPlayController.State.PLAY){instance.results++;}}
 public static void item(int who){if(instance!=null&&who==GameScr.myIndex)instance.items++;}
 public static void observedMove(int who,int x,int y){if(instance!=null)instance.event("POSITION",who+":"+x+","+y);}
 public static void observedHP(int who,int hp){if(instance!=null)instance.event("HP_RECEIVED",who+":"+hp);}
 public static void observedShot(int who,int type,int angle){if(instance!=null)instance.event("SHOT_RECEIVED",who+":"+type+":"+angle);}
 public static void observedItem(int who,int id){if(instance!=null)instance.event("ITEM_RECEIVED",who+":"+id);}
 public static void broadcast(String text){if(instance!=null)instance.event("BROADCAST",text);}
 private AutoPlayController.Frame frame(){
  AutoPlayController.Frame f=new AutoPlayController.Frame();f.loggedIn=loggedIn;f.connected=Session_ME.gI().connected;
  f.inRoom=CCanvas.curScr==CCanvas.prepareScr && board>=0;f.inGame=CCanvas.curScr==CCanvas.gameScr&&CCanvas.gameScr!=null;
  f.matchSerial=matches;f.turnSerial=turns;f.resultSerial=results;f.itemSerial=items;
  if(CCanvas.prepareScr!=null && TerrainMidlet.myInfo!=null){
   f.correctRoom=PrepareScr.currentRoom==config.room&&board==config.board;
   int owner=CCanvas.prepareScr.autoOwnerID();
   f.correctOwner=config.role.equals("A")?owner==TerrainMidlet.myInfo.IDDB:false;
   boolean botReady=true;int botsPresent=0;
   for(Object o:CCanvas.prepareScr.playerInfos){PlayerInfo p=(PlayerInfo)o;if(p.IDDB==-1)continue;f.participants++;
    if(p.IDDB==TerrainMidlet.myInfo.IDDB)f.ready=p.isReady;
    else if(config.partner.equals(p.name)){f.partnerReady=p.isReady;if(config.role.equals("B"))f.correctOwner=owner==p.IDDB;}
    else if(config.botIds.contains(p.IDDB)){botReady=botReady&&p.isReady;botsPresent++;}
    else f.unexpectedPlayer=true;
   }
   f.partnerReady=f.partnerReady&&botReady&&botsPresent==config.botIds.size()&&(config.mapId<0||PrepareScr.curMap==config.mapId);
  }
  CPlayer me=me();if(me!=null){f.alive=me.hp>0;f.myTurn=PM.curP==GameScr.myIndex;
   if(me.item!=null&&me.maxhp>0&&me.hp*100L<me.maxhp*70L)for(int item:me.item)if(item==0)f.canHeal=true;
  }f.inRoom = f.inRoom && f.participants>0;return f;
 }
 private CPlayer me(){return PM.p==null||GameScr.myIndex<0||GameScr.myIndex>=PM.p.length?null:PM.p[GameScr.myIndex];}
 public void login(){
  loggedIn=false;boolean connected=Session_ME.gI().connected;
  if(connected)Session_ME.gI().close(911);
  pendingLogin=System.currentTimeMillis()+(connected?2000:1);
 }
 private void performLogin(){
  loggedIn=false;board=-1;Session_ME.gI().start=false;GameMidlet.IP=config.host;GameMidlet.PORT=config.port;GameMidlet.server=2;
  CCanvas.loginScr=new LoginScr();CCanvas.loginScr.show();LoginScr.remember=0;
  CCanvas.loginScr.autoLogin(System.getenv("AUTO_USERNAME"),System.getenv("AUTO_PASSWORD"));event("LOGIN_REQUEST","sent");
 }
 public void join(){GameService.gI().joinBoard((byte)config.room,(byte)config.board,System.getenv().getOrDefault("AUTO_ROOM_PASSWORD",""));event("JOIN_REQUEST",config.room+"/"+config.board);}
 private void selectItems(){CCanvas.prepareScr.itemCur=new int[]{0,0,1,1,-1,-1,-1,-1};GameService.gI().changeItem(CCanvas.prepareScr.itemCur);}
 public void ready(){selectItems();GameService.gI().ready(true);event("READY_REQUEST","sent");}
 public void startMatch(){selectItems();GameService.gI().startGame();event("START_REQUEST","sent");}
 public void shoot(){
  CPlayer me=me();if(me==null||me.gun!=0)throw new IllegalStateException("Auto-play currently supports Gunner ID 0 only");
  CPlayer target=null;for(CPlayer p:PM.p)if(p!=null&&p!=me&&p.hp>0&&p.team!=me.team&&(target==null||p.hp<target.hp))target=p;
  if(target==null)throw new IllegalStateException("No living enemy");
  int[] aim=System.getenv().getOrDefault("AUTO_AIM","SEARCH").equals("FIXED")?new int[]{target.x>=me.x?config.angle:180-config.angle,config.force}:GunnerAim.solve(me,target);
  GameService.gI().waitForFIRE((byte)0,(short)me.x,(short)me.y,(short)aim[0],(byte)aim[1],(byte)0,(byte)1);event("SHOT_REQUEST","turn="+turns+" angle="+aim[0]+" force="+aim[1]);
 }
 public void heal(){GameService.gI().useItem((byte)0);event("HEAL_REQUEST","turn="+turns);}
 public void backToRoom(){if(CCanvas.curScr==CCanvas.gameScr&&CCanvas.gameScr!=null)CCanvas.gameScr.doExit();CCanvas.endDlg();}
 public void disconnect(){loggedIn=false;Session_ME.gI().close(910);event("RELOGIN_BEGIN",wallet());}
 public void markHostReady(){if(hostMarked)return;try{Files.write(Paths.get(config.output,"A-room"),(config.room+"/"+config.board).getBytes(StandardCharsets.UTF_8));hostMarked=true;if(config.role.equals("A")&&config.mapId>=0){GameService.gI().mapSelect((byte)config.mapId);event("MAP_REQUEST",String.valueOf(config.mapId));}if(config.role.equals("A"))for(int botId:config.botIds){GameService.gI().inviteFriend(false,botId);event("BOT_INVITE",String.valueOf(botId));}}catch(Exception e){throw new IllegalStateException(e);}}
 public boolean hostReady(){return Files.exists(Paths.get(config.output,"A-room"));}
 private String wallet(){if(TerrainMidlet.myInfo==null)return "unavailable";return "id="+TerrainMidlet.myInfo.IDDB+" xu="+TerrainMidlet.myInfo.xu+" luong="+TerrainMidlet.myInfo.luong;}
 public void event(String type,String detail){recorder.event(type,detail);if(type.equals("FAILED")){AutoPlayController.Frame f=frame();recorder.event("FRAME", "room="+f.inRoom+" correctRoom="+f.correctRoom+" owner="+f.correctOwner+" extra="+f.unexpectedPlayer+" count="+f.participants+" actualOwner="+CCanvas.prepareScr.autoOwnerID()+" "+wallet());}if(type.equals("MATCH_COMPLETE")){recorder.event("WALLET",wallet());screenshot("match-"+controller.completed);}}
 private void screenshot(String name){pendingScreenshots.add(name);}
 public static void afterRender(){if(instance==null)return;for(String name:instance.pendingScreenshots)instance.capture(name);instance.pendingScreenshots.clear();}
 private void capture(String name){try{Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());try{PixmapIO.writePNG(Gdx.files.absolute(Paths.get(config.output,"client-"+config.role+"-"+name+".png").toAbsolutePath().toString()),p,-1,true);}finally{p.dispose();}}catch(Exception e){recorder.event("SCREENSHOT_ERROR",e.getClass().getSimpleName());}}
 private void writeTerminal()throws Exception{String s=controller.state.name();if((s.equals("DONE")||s.equals("FAILED")||s.equals("OFF"))&&!s.equals(terminal)){terminal=s;screenshot(s);Files.write(Paths.get(config.output,"client-"+config.role+".summary"),("status="+s+"\ncompleted="+controller.completed+"\n"+wallet()+"\n").getBytes(StandardCharsets.UTF_8));}}
}
