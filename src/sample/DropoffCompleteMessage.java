package ajou.aiot.samples;

public class DropoffCompleteMessage {
	// POJO
	int TIMESTAMP;
	String RIDEID;
	Loc LOCATION;
	int DRIVERID;
	
	DropoffCompleteMessage(String rideId, Loc location, int driverid){
		TIMESTAMP=Epoch.getSec();
		RIDEID=rideId;		
		LOCATION = location;
		DRIVERID=driverid;
	}
}

