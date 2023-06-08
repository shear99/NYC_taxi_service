package ajou.aiot.samples;

public class PickupRequestMessage {
	int TIMESTAMP;
	String RIDEID;
	Loc CURRENT;
	Loc DESTINATION;

	public PickupRequestMessage(String rIDEID, Loc cURRENT, Loc dESTINATION) {

		TIMESTAMP = Epoch.getSec();
		RIDEID = rIDEID;
		CURRENT = cURRENT;
		DESTINATION = dESTINATION;
	}
	
}
