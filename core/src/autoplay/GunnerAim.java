package autoplay;
import model.CRes;
import player.CPlayer;
import screen.GameScr;
/** Bounded Gunner search against the client's current terrain. No support for tornado/special bullets. */
final class GunnerAim {
 static int[] solve(CPlayer me,CPlayer target){
  int bestAngle=target.x>=me.x?45:135,bestForce=20;double best=Double.MAX_VALUE;
  int ax=GameScr.windx*80/100,ay=GameScr.windy*80/100;
  for(int angle=10;angle<=170;angle+=2)for(int force=5;force<=30;force++){
   int x=me.x+(20*CRes.cos(angle)>>10),y=me.y-12-(20*CRes.sin(angle)>>10);
   int vx=force*CRes.cos(angle)>>10,vy=-(force*CRes.sin(angle)>>10),tx=0,ty=0;
   boolean end=false;
   for(int frame=0;frame<180&&!end;frame++){
    int nx=x+vx,ny=y+vy,steps=Math.max(1,Math.max(Math.abs(vx),Math.abs(vy)));
    for(int k=1;k<=steps;k++){
     int px=x+(nx-x)*k/steps,py=y+(ny-y)*k/steps;
     double d=Math.hypot(px-target.x,py-(target.y-12));
     if(d<best){best=d;bestAngle=angle;bestForce=force;}
     if(d<9)return new int[]{angle,force};
     if(py>map.MM.mapHeight+100||px < -100||px>map.MM.mapWidth+100||GameScr.mm.isLand(px,py)){end=true;break;}
    }
    x=nx;y=ny;tx+=Math.abs(ax);ty+=Math.abs(ay);
    vx+=(ax>=0?1:-1)*(tx/100);vy+=(ay>=0?1:-1)*(ty/100)+1;tx%=100;ty%=100;
   }
  }
  return new int[]{bestAngle,bestForce};
 }
}
