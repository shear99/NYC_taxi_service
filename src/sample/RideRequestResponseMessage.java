package ajou.aiot.samples;

public class RideRequestResponseMessage {
	// POJO
	int TIMESTAMP;
	String RIDEID;
	int ETA;// sec
	String TAXINUMBER;
	
	RideRequestResponseMessage(String rideId, int eta, int dRIVERID){
		TIMESTAMP=Epoch.getSec();
		RIDEID=rideId;		
		ETA=eta;
		TAXINUMBER="T"+dRIVERID;
	}
}

