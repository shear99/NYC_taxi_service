
package sample;
import java.io.IOException;
import java.util.Properties;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.SolaceProperties.AuthenticationProperties;
import com.solace.messaging.config.SolaceProperties.ServiceProperties;
import com.solace.messaging.config.SolaceProperties.TransportLayerProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.publisher.DirectMessagePublisher;
import com.solace.messaging.receiver.DirectMessageReceiver;
import com.solace.messaging.receiver.MessageReceiver.MessageHandler;
import com.solace.messaging.resources.TopicSubscription;
import com.solace.messaging.resources.Topic;
import com.google.gson.Gson; 
import java.util.Random;



public class PlatformApp {    
	
    private static volatile boolean isShutdown = false;           // are we done yet?
    private static int areaCode=-1;
    public static void main(String... args) throws IOException, InterruptedException {
    	
    	
        if (args.length < 4) {  // Check command line arguments
            System.out.printf("Usage: UserApp <host:port> <message-vpn> <client-username> <password> <userId>%n", "UserID");            
            System.exit(-1);
        }
        Gson gson = new Gson ();
        final Properties properties = new Properties();
        properties.setProperty(TransportLayerProperties.HOST, args[0]);          // host:port
        properties.setProperty(ServiceProperties.VPN_NAME,  args[1]);     // message-vpn
        properties.setProperty(AuthenticationProperties.SCHEME_BASIC_USER_NAME, args[2]);      // client-username
        if (args.length > 3) {
            properties.setProperty(AuthenticationProperties.SCHEME_BASIC_PASSWORD, args[3]);  // client-password
        }
        if (args.length > 4) {
        	areaCode=Integer.parseInt(args[4]);
        }
        
        properties.setProperty(ServiceProperties.RECEIVER_DIRECT_SUBSCRIPTION_REAPPLY, "true");  // subscribe Direct subs after reconnect

        final MessagingService messagingService = MessagingService.builder(ConfigurationProfile.V1)
                .fromProperties(properties).build().connect();  // blocking connect to the broker

        // create and start the publisher 
        final DirectMessagePublisher publisher = messagingService.createDirectMessagePublisherBuilder()
                .onBackPressureWait(1).build().start();
        
        String rideReqTopicSubsction="RideRequest/>";
        String locationUpdateTopicSubsction="LocationUpdate/*/*/>";
        String pickupRequestResponseSubscription="PickupRequestResponse/>";
        String pickupCompleteRequestResponseSubscription="PickupComplete/>";
        String dropoffCompleteRequestResponseSubscription="DropoffComplete/>";
        if (areaCode>-1) {
        	rideReqTopicSubsction="RideRequest/"+areaCode;
        	pickupRequestResponseSubscription="PickupRequestResponse/"+areaCode;
        	pickupCompleteRequestResponseSubscription="PickupComplete/"+areaCode;
        	dropoffCompleteRequestResponseSubscription="DropoffComplete/"+areaCode;
        	locationUpdateTopicSubsction="LocationUpdate/*/*/"+areaCode;
        	System.out.println("Responsible to handle area " + areaCode);
        }
        
        final DirectMessageReceiver rideRequestReceiver = messagingService.createDirectMessageReceiverBuilder()
        		.withSubscriptions(TopicSubscription.of(rideReqTopicSubsction)).build().start();
        
        final DirectMessageReceiver pickupRequestResponseReceiver = messagingService.createDirectMessageReceiverBuilder()
        		.withSubscriptions(TopicSubscription.of(pickupRequestResponseSubscription)).build().start();
        
        final DirectMessageReceiver pickupCompleteReceiver = messagingService.createDirectMessageReceiverBuilder()
        		.withSubscriptions(TopicSubscription.of(pickupCompleteRequestResponseSubscription)).build().start();
        
        final DirectMessageReceiver dropoffCompleteReceiver = messagingService.createDirectMessageReceiverBuilder()
        		.withSubscriptions(TopicSubscription.of(dropoffCompleteRequestResponseSubscription)).build().start();
        
        final DirectMessageReceiver locationUpdateReceiver = messagingService.createDirectMessageReceiverBuilder()
        		.withSubscriptions(TopicSubscription.of(locationUpdateTopicSubsction)).build().start();
        
        
        final MessageHandler rideRequestReceiverHandler = (inboundMessage) -> {
            //System.out.println("DUMP: \n"+inboundMessage.dump());  // just print
        	
            RideRequestMessage rideReq=gson.fromJson(inboundMessage.getPayloadAsString(), RideRequestMessage.class);
            String rideId=getRideId(rideReq.USERID, rideReq.TIMESTAMP);
            PickupRequestMessage pickUpMsg = new PickupRequestMessage(rideId, rideReq.CURRENT, rideReq.DESTINATION);
            
            publisher.publish(gson.toJson(pickUpMsg).getBytes(), Topic.of("PickupRequest/"+inboundMessage.getDestinationName().split("/")[1]));
        };
        
        final MessageHandler pickupRequestResponseReceiverHandler = (inboundMessage) -> {
            System.out.println("DUMP: \n"+inboundMessage.dump());  // just print
        	
            PickupRequestResponseMessage pickupReqResp=gson.fromJson(inboundMessage.getPayloadAsString(), PickupRequestResponseMessage.class);
            RideRequestResponseMessage rideReqResp = new RideRequestResponseMessage(pickupReqResp.RIDEID, 10000/*TBD*/, pickupReqResp.DRIVERID);
            publisher.publish(gson.toJson(rideReqResp).getBytes(), Topic.of("RideRequestResponse/"+getUserIDFromRideId(pickupReqResp.RIDEID)));
        };
        
        final MessageHandler pickupCompleteReceiverHandler = (inboundMessage) -> {
            System.out.println("DUMP: \n"+inboundMessage.dump());  // just print
        	
        	
        };
        
        final MessageHandler dropoffCompleteReceiverHandler = (inboundMessage) -> {
            System.out.println("DUMP: \n"+inboundMessage.dump());  // just print        	
        };
        
        final MessageHandler locationUpdateReceiverHandler = (inboundMessage) -> {
            //System.out.println("DUMP: \n"+inboundMessage.dump());  // just print// too much loggings
        	
        	LocUpdateMessage locUpdateMessage=gson.fromJson(inboundMessage.getPayloadAsString(), LocUpdateMessage.class);
        	
        	System.out.println("Driver "+ locUpdateMessage.DRIVERID + " in " + locUpdateMessage.STATUS + " AT " + locUpdateMessage.LOCATION.toString());
            
        };
                
        locationUpdateReceiver.receiveAsync(locationUpdateReceiverHandler);
        rideRequestReceiver.receiveAsync(rideRequestReceiverHandler);
        pickupRequestResponseReceiver.receiveAsync(pickupRequestResponseReceiverHandler);
        pickupCompleteReceiver.receiveAsync(pickupCompleteReceiverHandler);
        dropoffCompleteReceiver.receiveAsync(dropoffCompleteReceiverHandler);
      
        while (System.in.available() == 0 && !isShutdown) {  // loop now, just use main thread
            try {                            	
            } catch (RuntimeException e) {
                System.out.printf("### Exception caught during producer.send(): %s%n",e);
                isShutdown = true;
            }
        }                
        
        isShutdown = true;
        publisher.terminate(500);
        rideRequestReceiver.terminate(500);
        pickupRequestResponseReceiver.terminate(500);
        pickupCompleteReceiver.terminate(500);
        dropoffCompleteReceiver.terminate(500);
        messagingService.disconnect();
        
    }

    
   
	private static int getUserIDFromRideId(String rideId) {
		return Integer.parseInt(rideId.split("-")[2]);
	}



	static String getRideId(int UID, int epoch) {
		Random rand = new Random();		
	    return "RIDE-"+rand.nextInt(1000)+"-"+UID+"-TIME-"+epoch;

	}

	
	
}
