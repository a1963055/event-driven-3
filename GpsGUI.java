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
    static double feetToMeters(double feet) {
        return feet * 0.3048;
    }
    
    static double distance3D(GpsEvent a, GpsEvent b) {
        double latDiff = (b.latitude - a.latitude) * 111000.0;
        double lonDiff = (b.longitude - a.longitude) * 111000.0 * Math.cos(Math.toRadians(a.latitude));
        double altDiff = feetToMeters(b.altitude - a.altitude);
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff + altDiff * altDiff);
    }
    
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
        
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Range Filter Controls"));
        
        JTextField latInput = new JTextField(10);
        JTextField lonInput = new JTextField(10);
        JButton setButton = new JButton("Set Range");
        JLabel latLabel = new JLabel("Lat: 0.0");
        JLabel lonLabel = new JLabel("Lon: 0.0");
        
        CellSink<Double> latCell = new CellSink<Double>(0.0);
        CellSink<Double> lonCell = new CellSink<Double>(0.0);
        
        setButton.addActionListener(e -> {
            try {
                double lat = Double.parseDouble(latInput.getText());
                double lon = Double.parseDouble(lonInput.getText());
                latCell.send(lat);
                lonCell.send(lon);
                latLabel.setText("Lat: " + lat);
                lonLabel.setText("Lon: " + lon);
            } catch (Exception ex) {}
        });
        
        Stream<GpsEvent> filtered = combined.snapshot(latCell, lonCell, (ev, lat, lon) -> {
            double latDiff = Math.abs(ev.latitude - lat);
            double lonDiff = Math.abs(ev.longitude - lon);
            return (latDiff < 0.1 && lonDiff < 0.1) ? ev : null;
        }).filter(ev -> ev != null);
        
        controlPanel.add(new JLabel("Latitude:"));
        controlPanel.add(latInput);
        controlPanel.add(new JLabel("Longitude:"));
        controlPanel.add(lonInput);
        controlPanel.add(setButton);
        controlPanel.add(latLabel);
        controlPanel.add(lonLabel);
        
        JLabel filteredDisplay = new JLabel("");
        filteredDisplay.setBorder(BorderFactory.createTitledBorder("Filtered Events"));
        
        filtered.listen(ev -> {
            SwingUtilities.invokeLater(() -> {
                String display = ev.name + "," + ev.latitude + "," + ev.longitude + "," + ev.altitude;
                filteredDisplay.setText(display);
            });
        });
        
        mainPanel.add(trackerPanel);
        mainPanel.add(eventDisplay);
        mainPanel.add(controlPanel);
        mainPanel.add(filteredDisplay);
        frame.add(mainPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}

