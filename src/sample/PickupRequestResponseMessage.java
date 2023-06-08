package ajou.aiot.samples;

public class PickupRequestResponseMessage {
	int TIMESTAMP;
	int DRIVERID;
	String RIDEID;
	String RESULT;

	public PickupRequestResponseMessage(String rIDEID, String rESULT, int dRIVERID) {

		TIMESTAMP = Epoch.getSec();
		RIDEID = rIDEID;
		DRIVERID = dRIVERID;
		RESULT = rESULT;		
	}
	
}
