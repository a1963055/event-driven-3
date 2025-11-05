import nz.sodium.*;
import javax.swing.*;
import java.awt.*;

public class GpsGUI {
    public static void main(String[] args) {
        GpsService serv = new GpsService();
        Stream<GpsEvent>[] streams = serv.getEventStreams();
        
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

