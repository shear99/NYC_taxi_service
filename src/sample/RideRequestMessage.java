package ajou.aiot.samples;

public class RideRequestMessage {
	// POJO
	int TIMESTAMP;
	int USERID;
	Loc CURRENT;
	Loc DESTINATION;
	
	RideRequestMessage(int userID, double initX, double initY, double destX, double destY){
		TIMESTAMP=Epoch.getSec();
		USERID=userID;		
		CURRENT=new Loc (initX,initY);
		DESTINATION=new Loc (destX,destY);
	}
}

