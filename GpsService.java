import java.io.*;
import java.util.*;
import nz.sodium.*;

public class GpsService {

    private LinkedList<Stream<GpsEvent>> streams;

    public GpsService(){
        streams = new LinkedList<Stream<GpsEvent>>();
        this.start();
    }

    @SuppressWarnings("unchecked")
     public Stream<GpsEvent>[] getEventStreams(){
        return ((Stream<GpsEvent>[])this.streams.toArray(new Stream[0]));
    }

    @SuppressWarnings("unchecked")
     private void start(){

        LinkedList<Double[]>[] data;
        LinkedList<Timer> timers = new LinkedList<Timer>();
        Timer t;
        GpsInput ev;

        // Read the data file
        try {   
            FileInputStream fileIn = new FileInputStream("gps.dat");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            data = (LinkedList<Double[]>[]) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
            return;
        } catch (ClassNotFoundException c) {
            System.out.println("Data not found");
            c.printStackTrace();
            return;
        }

        // Setting up and beginning playback of records for each GPS tracker
        for(int i=0; i<data.length; i++){    
            t = new Timer();
            timers.add(t);
            StreamSink<GpsEvent> s = new StreamSink<GpsEvent>();
            ev = new GpsInput(i,data[i],t,s);
            t.schedule(ev,1000);
            this.streams.add((Stream<GpsEvent>) s);
        }
    }

    private class GpsInput extends TimerTask {

        public int id = 0;
        public LinkedList<Double[]> data;
        public Timer timer;
        public StreamSink<GpsEvent> stream;

        public GpsInput(int id, LinkedList<Double[]> data, Timer timer, StreamSink<GpsEvent> stream){
            this.id = id;
            this.data = data;
            this.timer = timer;
            this.stream = stream;
        }

        public void run() {
            Double[] event = data.poll();
            data.add(event);
            Double[] next = data.peek();

            stream.send(new GpsEvent("Tracker"+id,event[0].doubleValue(),event[1].doubleValue(),event[2].doubleValue()));
            timer.schedule(new GpsInput(id,data,timer,stream),next[3].longValue()*1000);
        }
    }

}