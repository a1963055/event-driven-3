import org.junit.Test;
import static org.junit.Assert.*;

public class GpsGUI_Test {
    
    @Test
    public void testFeetToMeters() {
        double feet = 100.0;
        double meters = GpsGUI.feetToMeters(feet);
        assertEquals(30.48, meters, 0.01);
    }
    
    @Test
    public void testDistance3D() {
        GpsEvent a = new GpsEvent("Test", 0.0, 0.0, 0.0);
        GpsEvent b = new GpsEvent("Test", 0.001, 0.001, 100.0);
        double dist = GpsGUI.distance3D(a, b);
        assertTrue(dist > 0);
    }
}

