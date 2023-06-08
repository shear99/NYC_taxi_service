package ajou.aiot.samples;

public class PaymentRequestMessage {
	// POJO
	int TIMESTAMP;
	String RIDEID;
	String COST;	
	int DRIVERID;
	
	PaymentRequestMessage(String rideId, String cost, int driverid){
		TIMESTAMP=Epoch.getSec();
		RIDEID=rideId;		
		COST=cost;
		DRIVERID=driverid;
	}
}

