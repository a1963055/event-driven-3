import nz.sodium.*;

public class Example {

    public static void main(String[] args){

        // Initialise the GPS Service
        GpsService serv = new GpsService();
        Stream<GpsEvent>[] streams = serv.getEventStreams();
        for(Stream<GpsEvent> s : streams){
            s.listen((GpsEvent ev) -> System.out.println(ev));
        }
    }

} 
