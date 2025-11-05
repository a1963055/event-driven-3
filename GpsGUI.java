import nz.sodium.*;
import javax.swing.*;
import java.awt.*;

class SimplifiedGps {
    String name;
    double latitude;
    double longitude;
    
    SimplifiedGps(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}

public class GpsGUI {
    public static void main(String[] args) {
        GpsService serv = new GpsService();
        Stream<GpsEvent>[] streams = serv.getEventStreams();
        
        Stream<SimplifiedGps>[] simplified = new Stream[streams.length];
        for(int i = 0; i < streams.length; i++) {
            simplified[i] = streams[i].map(ev -> new SimplifiedGps(ev.name, ev.latitude, ev.longitude));
        }
        
        JFrame frame = new JFrame("GPS Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        frame.add(mainPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}

