import nz.sodium.*;
import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

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
        
        Stream<GpsEvent> combined = streams[0];
        for(int i = 1; i < streams.length; i++) {
            combined = combined.merge(streams[i], (a, b) -> a);
        }
        
        JFrame frame = new JFrame("GPS Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        JPanel trackerPanel = new JPanel(new GridLayout(10, 4, 5, 5));
        trackerPanel.setBorder(BorderFactory.createTitledBorder("Tracker Displays"));
        
        JLabel[] trackerLabels = new JLabel[10];
        JLabel[] latLabels = new JLabel[10];
        JLabel[] lonLabels = new JLabel[10];
        
        for(int i = 0; i < 10; i++) {
            trackerLabels[i] = new JLabel("Tracker" + i);
            latLabels[i] = new JLabel("0.0");
            lonLabels[i] = new JLabel("0.0");
            
            trackerPanel.add(trackerLabels[i]);
            trackerPanel.add(latLabels[i]);
            trackerPanel.add(lonLabels[i]);
            trackerPanel.add(new JLabel(""));
            
            final int idx = i;
            simplified[i].listen(sg -> {
                SwingUtilities.invokeLater(() -> {
                    trackerLabels[idx].setText(sg.name);
                    latLabels[idx].setText(String.format("%.8f", sg.latitude));
                    lonLabels[idx].setText(String.format("%.8f", sg.longitude));
                });
            });
        }
        
        JLabel eventDisplay = new JLabel("");
        eventDisplay.setBorder(BorderFactory.createTitledBorder("Event Display"));
        Timer clearTimer = new Timer();
        
        for(int i = 0; i < streams.length; i++) {
            streams[i].listen(ev -> {
                SwingUtilities.invokeLater(() -> {
                    String display = ev.name + "," + ev.latitude + "," + ev.longitude + "," + ev.altitude;
                    eventDisplay.setText(display);
                    clearTimer.cancel();
                    clearTimer = new Timer();
                    clearTimer.schedule(new TimerTask() {
                        public void run() {
                            SwingUtilities.invokeLater(() -> eventDisplay.setText(""));
                        }
                    }, 3000);
                });
            });
        }
        
        mainPanel.add(trackerPanel);
        mainPanel.add(eventDisplay);
        frame.add(mainPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}

