package ajou.aiot.samples;

public class PickupCompleteMessage {
	// POJO
	int TIMESTAMP;
	String RIDEID;
	Loc LOCATION;
	
	PickupCompleteMessage(String rideId, Loc location){
		TIMESTAMP=Epoch.getSec();
		RIDEID=rideId;		
		LOCATION = location;		
	}
}

