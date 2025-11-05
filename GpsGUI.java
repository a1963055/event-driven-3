import nz.sodium.*;
import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.List;

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

class TimedEvent {
    GpsEvent event;
    long timestamp;
    
    TimedEvent(GpsEvent event, long timestamp) {
        this.event = event;
        this.timestamp = timestamp;
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
        
        Cell<Integer>[] distances = new Cell[10];
        for(int i = 0; i < 10; i++) {
            Stream<TimedEvent> timedStream = streams[i].map(ev -> new TimedEvent(ev, System.currentTimeMillis()));
            distances[i] = timedStream.accum(new ArrayList<TimedEvent>(), (evList, te) -> {
                List<TimedEvent> newList = new ArrayList<>(evList);
                newList.add(te);
                long cutoff = System.currentTimeMillis() - 300000;
                newList.removeIf(t -> t.timestamp < cutoff);
                return newList;
            }).map(evList -> {
                if(evList.size() < 2) return 0;
                double total = 0.0;
                for(int j = 1; j < evList.size(); j++) {
                    total += distance3D(evList.get(j-1).event, evList.get(j).event);
                }
                return (int)Math.ceil(total);
            });
        }
        
        JPanel distancePanel = new JPanel(new GridLayout(10, 2, 5, 5));
        distancePanel.setBorder(BorderFactory.createTitledBorder("Distance (last 5 min, meters)"));
        JLabel[] distanceLabels = new JLabel[10];
        
        for(int i = 0; i < 10; i++) {
            distanceLabels[i] = new JLabel("0 m");
            distancePanel.add(new JLabel("Tracker" + i + ":"));
            distancePanel.add(distanceLabels[i]);
            
            final int idx = i;
            Operational.updates(distances[i]).listen(dist -> {
                SwingUtilities.invokeLater(() -> {
                    distanceLabels[idx].setText(dist + " m");
                });
            });
        }
        
        mainPanel.add(trackerPanel);
        mainPanel.add(distancePanel);
        mainPanel.add(eventDisplay);
        mainPanel.add(controlPanel);
        mainPanel.add(filteredDisplay);
        frame.add(mainPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}

