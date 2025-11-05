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
        trackerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Tracker Displays"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
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
        eventDisplay.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Event Display"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        final Timer[] clearTimer = new Timer[]{new Timer()};
        
        for(int i = 0; i < streams.length; i++) {
            streams[i].listen(ev -> {
                SwingUtilities.invokeLater(() -> {
                    String display = ev.name + "," + ev.latitude + "," + ev.longitude + "," + ev.altitude;
                    eventDisplay.setText(display);
                    clearTimer[0].cancel();
                    clearTimer[0] = new Timer();
                    clearTimer[0].schedule(new TimerTask() {
                        public void run() {
                            SwingUtilities.invokeLater(() -> eventDisplay.setText(""));
                        }
                    }, 3000);
                });
            });
        }
        
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Range Filter Controls"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        JTextField latInput = new JTextField(10);
        latInput.setText("40");
        JTextField lonInput = new JTextField(10);
        lonInput.setText("116");
        JTextField rangeInput = new JTextField(10);
        rangeInput.setText("1.0");
        JButton setButton = new JButton("Set Range");
        JLabel latLabel = new JLabel("Lat: 40.0");
        JLabel lonLabel = new JLabel("Lon: 116.0");
        JLabel rangeLabel = new JLabel("Range: 1.0°");
        
        CellSink<Double> latCell = new CellSink<Double>(40.0);
        CellSink<Double> lonCell = new CellSink<Double>(116.0);
        CellSink<Double> rangeCell = new CellSink<Double>(1.0);
        
        setButton.addActionListener(e -> {
            try {
                double lat = Double.parseDouble(latInput.getText());
                double lon = Double.parseDouble(lonInput.getText());
                double range = Double.parseDouble(rangeInput.getText());
                latCell.send(lat);
                lonCell.send(lon);
                rangeCell.send(range);
                latLabel.setText("Lat: " + lat);
                lonLabel.setText("Lon: " + lon);
                rangeLabel.setText("Range: " + range + "°");
            } catch (Exception ex) {}
        });
        
        Stream<GpsEvent> filtered = combined.snapshot(latCell, lonCell, rangeCell, (ev, latVal, lonVal, rangeVal) -> {
            double latDiff = Math.abs(ev.latitude - latVal);
            double lonDiff = Math.abs(ev.longitude - lonVal);
            boolean inRange = (latDiff < rangeVal && lonDiff < rangeVal);
            if (!inRange) {
                return null;
            }
            return ev;
        }).filter(ev -> ev != null);
        
        controlPanel.add(new JLabel("Latitude:"));
        controlPanel.add(latInput);
        controlPanel.add(new JLabel("Longitude:"));
        controlPanel.add(lonInput);
        controlPanel.add(new JLabel("Range (°):"));
        controlPanel.add(rangeInput);
        controlPanel.add(setButton);
        controlPanel.add(latLabel);
        controlPanel.add(lonLabel);
        controlPanel.add(rangeLabel);
        
        JLabel filteredDisplay = new JLabel("No events in range");
        filteredDisplay.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Filtered Events (within range)"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        filteredDisplay.setFont(new Font(filteredDisplay.getFont().getName(), Font.BOLD, 12));
        filteredDisplay.setForeground(Color.BLUE);
        
        final Timer[] clearFilterTimer = new Timer[]{new Timer()};
        filtered.listen(ev -> {
            SwingUtilities.invokeLater(() -> {
                String display = ev.name + "," + ev.latitude + "," + ev.longitude + "," + ev.altitude;
                filteredDisplay.setText(display);
                filteredDisplay.setForeground(Color.GREEN);
                clearFilterTimer[0].cancel();
                clearFilterTimer[0] = new Timer();
                clearFilterTimer[0].schedule(new TimerTask() {
                    public void run() {
                        SwingUtilities.invokeLater(() -> {
                            filteredDisplay.setText("No events in range");
                            filteredDisplay.setForeground(Color.BLUE);
                        });
                    }
                }, 5000);
            });
        });
        
        Cell<Integer>[] distances = new Cell[10];
        for(int i = 0; i < 10; i++) {
            Stream<TimedEvent> timedStream = streams[i].map(ev -> new TimedEvent(ev, System.currentTimeMillis()));
            Cell<Double> latC = latCell;
            Cell<Double> lonC = lonCell;
            Cell<Double> rangeC = rangeCell;
            distances[i] = timedStream.snapshot(latCell, lonCell, rangeCell, (te, latVal, lonVal, rangeVal) -> {
                double latDiff = Math.abs(te.event.latitude - latVal);
                double lonDiff = Math.abs(te.event.longitude - lonVal);
                return (latDiff < rangeVal && lonDiff < rangeVal) ? te : null;
            }).filter(te -> te != null).accum(new ArrayList<TimedEvent>(), (te, evList) -> {
                ArrayList<TimedEvent> newList = new ArrayList<TimedEvent>(evList);
                newList.add(te);
                long cutoff = System.currentTimeMillis() - 300000;
                newList.removeIf(t -> t.timestamp < cutoff);
                return newList;
            }).lift(latC, lonC, rangeC, (evList, latVal, lonVal, rangeVal) -> {
                if(evList.size() < 2) return 0;
                ArrayList<TimedEvent> inRange = new ArrayList<TimedEvent>();
                for(TimedEvent te : evList) {
                    double latDiff = Math.abs(te.event.latitude - latVal);
                    double lonDiff = Math.abs(te.event.longitude - lonVal);
                    if(latDiff < rangeVal && lonDiff < rangeVal) {
                        inRange.add(te);
                    }
                }
                if(inRange.size() < 2) return 0;
                double total = 0.0;
                for(int j = 1; j < inRange.size(); j++) {
                    total += distance3D(inRange.get(j-1).event, inRange.get(j).event);
                }
                return (int)Math.ceil(total);
            });
        }
        
        JPanel distancePanel = new JPanel(new GridLayout(10, 2, 5, 5));
        distancePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Distance (last 5 min, meters)"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JLabel[] distanceLabels = new JLabel[10];
        
        for(int i = 0; i < 10; i++) {
            distanceLabels[i] = new JLabel("0 m");
            distancePanel.add(new JLabel("Tracker" + i + ":"));
            distancePanel.add(distanceLabels[i]);
            
            final int idx = i;
            // Display initial value
            SwingUtilities.invokeLater(() -> {
                distanceLabels[idx].setText(distances[idx].sample() + " m");
            });
            // Listen for updates
            Operational.updates(distances[i]).listen(dist -> {
                SwingUtilities.invokeLater(() -> {
                    distanceLabels[idx].setText(dist + " m");
                });
            });
        }
        
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(trackerPanel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(distancePanel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(eventDisplay);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(controlPanel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(filteredDisplay);
        mainPanel.add(Box.createVerticalStrut(5));
        
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}
