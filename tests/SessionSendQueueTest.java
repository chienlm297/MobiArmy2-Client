import network.Message;
import network.Session_ME;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

public class SessionSendQueueTest {
    static Field field(String name)throws Exception {
        Field f=Session_ME.class.getDeclaredField(name);f.setAccessible(true);return f;
    }
    public static void main(String[]args)throws Exception {
        Session_ME session=new Session_ME();
        Object sender=field("sender").get(session);
        Field queueField=sender.getClass().getDeclaredField("sendingMessage");queueField.setAccessible(true);
        Queue<Message> queue=(Queue<Message>)queueField.get(sender);
        Message a=new Message(1),b=new Message(2);
        session.sendMessage(a);session.sendMessage(b);
        if(queue.poll()!=a || queue.poll()!=b || queue.poll()!=null)throw new AssertionError("FIFO/poll");
        AtomicReference<Throwable> failure=new AtomicReference<>();
        session.connected=true;session.key=new byte[]{7};field("getKeyComplete").setBoolean(session,true);
        field("dos").set(session,new DataOutputStream(new OutputStream(){public void write(int b){}}));
        Thread worker=new Thread((Runnable)sender);session.sendThread=worker;
        worker.setUncaughtExceptionHandler((t,e)->failure.compareAndSet(null,e));worker.start();
        Thread producer=new Thread(()->{try{for(int i=0;i<100000;i++)session.sendMessage(new Message(i));}catch(Throwable e){failure.compareAndSet(null,e);}});
        Thread clearer=new Thread(()->{try{for(int i=0;i<100000;i++)queue.clear();}catch(Throwable e){failure.compareAndSet(null,e);}});
        producer.start();clearer.start();producer.join(10000);clearer.join(10000);
        Thread stale=new Thread((Runnable)sender);stale.setUncaughtExceptionHandler((t,e)->failure.compareAndSet(null,e));stale.start();stale.join(2000);
        boolean staleStopped=!stale.isAlive();
        session.sendThread=null;session.connected=false;worker.interrupt();stale.interrupt();worker.join(2000);stale.join(2000);
        if(producer.isAlive() || clearer.isAlive() || worker.isAlive() || !staleStopped)throw new AssertionError("worker failed to terminate");
        if(failure.get()!=null)throw new AssertionError("concurrent sender failure",failure.get());
        System.out.println("PASS FIFO; 100000 concurrent enqueue/clear operations with real Sender; stale worker exits; interrupted worker exits");
    }
}
