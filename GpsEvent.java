// GPS Event class
public class GpsEvent {

    public String name;
    public double latitude;
    public double longitude;
    public double altitude;

    //creating a gps event
    public GpsEvent(String name, double latitude, double longitude, double altitude){
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }
    // returns a string representation of the gps event
    public String toString(){
        return this.name+" | lat:"+this.latitude+" lon:"+this.longitude+" alt:"+this.altitude;
    }

} 
