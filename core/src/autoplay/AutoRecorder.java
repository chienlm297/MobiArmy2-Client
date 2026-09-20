package autoplay;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
public final class AutoRecorder {
 private final Path dir,log;
 public AutoRecorder(AutoConfig c)throws IOException{dir=Paths.get(c.output);Files.createDirectories(dir);log=dir.resolve("client-"+c.role+".jsonl");}
 public synchronized void event(String type,String detail){
  String line="{\"time\":"+System.currentTimeMillis()+",\"type\":\""+escape(type)+"\",\"detail\":\""+escape(detail)+"\"}\n";
  try{Files.write(log,line.getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException e){throw new IllegalStateException("Cannot write autoplay evidence",e);}
 }
 private static String escape(String s){StringBuilder b=new StringBuilder();for(char c:s.toCharArray()){if(c=='"'||c=='\\')b.append('\\').append(c);else if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}return b.toString();}
}
