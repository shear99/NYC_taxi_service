package ajou.aiot.samples;

public class LocUpdateMessage {
	// POJO
	int TIMESTAMP;
	int DRIVERID;
	Loc LOCATION;
	String STATUS;
	
	public LocUpdateMessage(int driverID, String status, double x, double y){
		TIMESTAMP=Epoch.getSec();
		DRIVERID=driverID;
		STATUS=status;
		LOCATION=new Loc (x,y);
	}
	
}

