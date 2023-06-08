
package ajou.aiot.samples;

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



public class PaymentApp {    
	
    private static volatile boolean isShutdown = false;           // are we done yet?
    private static int areaCode=-1;
    public static void main(String... args) throws IOException, InterruptedException {
    	
    	
        if (args.length < 4) {  // Check command line arguments
            System.out.printf("Usage: PaymentApp <host:port> <message-vpn> <client-username> <password> <userId>%n");            
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
        
        String dropoffCompleteRequestResponseSubscription="DropoffComplete/>";
        if (areaCode>-1) {
        	dropoffCompleteRequestResponseSubscription="DropoffComplete/"+areaCode;
        	System.out.println("Responsible to handle area " + areaCode);
        }
        
        
        final DirectMessageReceiver dropoffCompleteReceiver = messagingService.createDirectMessageReceiverBuilder()
        		.withSubscriptions(TopicSubscription.of(dropoffCompleteRequestResponseSubscription)).build().start();
        
                
        
        final MessageHandler dropoffCompleteReceiverHandler = (inboundMessage) -> {
            System.out.println("DUMP: \n"+inboundMessage.dump());  // just print
            DropoffCompleteMessage pickupReqResp=gson.fromJson(inboundMessage.getPayloadAsString(), DropoffCompleteMessage.class);
            PaymentRequestMessage paymentReq = new PaymentRequestMessage(pickupReqResp.RIDEID, 10000+"KRW"/*TBD*/, pickupReqResp.DRIVERID);
            publisher.publish(gson.toJson(paymentReq).getBytes(), Topic.of("PaymentRequest/"+getUserIDFromRideId(pickupReqResp.RIDEID)));
            
        };
        
        final MessageHandler locationUpdateReceiverHandler = (inboundMessage) -> {
            //System.out.println("DUMP: \n"+inboundMessage.dump());  // just print// too much loggings
        	
        	LocUpdateMessage locUpdateMessage=gson.fromJson(inboundMessage.getPayloadAsString(), LocUpdateMessage.class);
        	
        	System.out.println("Driver "+ locUpdateMessage.DRIVERID + " in " + locUpdateMessage.STATUS + " AT " + locUpdateMessage.LOCATION.toString());
            
        };
                
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
