
package sample;
import java.io.IOException;
import java.util.Properties;

import com.solace.messaging.MessagingService;
import com.solace.messaging.PubSubPlusClientException;
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


public class UserApp {    
	
    private static volatile boolean isShutdown = false;           // are we done yet?
    private static boolean isPickupAndMovingToTarget=false;    
    private static String rideId=null;
	private static int userId=0;
	static final int TIMEOUT_FOR_RETRY=5;
	static String currentTaxi=null;
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
        	userId=Integer.parseInt(args[4]);
        }
        properties.setProperty(ServiceProperties.RECEIVER_DIRECT_SUBSCRIPTION_REAPPLY, "true");  // subscribe Direct subs after reconnect

        final MessagingService messagingService = MessagingService.builder(ConfigurationProfile.V1)
                .fromProperties(properties).build().connect();  // blocking connect to the broker

        // create and start the publisher 
        final DirectMessagePublisher publisher = messagingService.createDirectMessagePublisherBuilder()
                .onBackPressureWait(1).build().start();
        
        
        final DirectMessageReceiver rideRequestResponseReceiver = messagingService.createDirectMessageReceiverBuilder()
        		.withSubscriptions(TopicSubscription.of("RideRequestResponse/"+userId)).build().start();
        
        final DirectMessageReceiver paymentRequestReceiver = messagingService.createDirectMessageReceiverBuilder()
        		.withSubscriptions(TopicSubscription.of("PaymentRequest/"+userId)).build().start();
        
                
        final MessageHandler pickupRespHandler = (inboundMessage) -> {
            System.out.println("DUMP: \n"+inboundMessage.dump());  // just print
        	
            if (isPickupAndMovingToTarget) {
            	System.out.println("Don't take the duplicated Taxi call");
            	return;
            }
            isPickupAndMovingToTarget=true;
            
            RideRequestResponseMessage rideReqResp=gson.fromJson(inboundMessage.getPayloadAsString(), RideRequestResponseMessage.class);
            
            System.out.println("rideReqResp received for RIDE-ID: "+ rideReqResp.RIDEID);
            rideId=rideReqResp.RIDEID;
            currentTaxi=rideReqResp.TAXINUMBER;
        };
        
        final MessageHandler paymentReqHandler = (inboundMessage) -> {
            System.out.println("DUMP: \n"+inboundMessage.dump());  // just print
            
            // Only accept the one arrived First
        	
            
            PaymentRequestMessage paymentReq = gson.fromJson(inboundMessage.getPayloadAsString(), PaymentRequestMessage.class);
            if (currentTaxi.equals("T"+paymentReq.DRIVERID)) {
            	isPickupAndMovingToTarget=false;
            	System.out.println("Fee is " + paymentReq.COST + " on Taxi " + currentTaxi);  // just print
            } else {
            	System.out.println("I didn't take it");  // just print
            	return;
            }
            
            
        };
                
        rideRequestResponseReceiver.receiveAsync(pickupRespHandler);
        paymentRequestReceiver.receiveAsync(paymentReqHandler);
      
        int retryCnt=0;
        
        RideRequestMessage reqMsg=getRideRequestMessageString(userId, 
        		LocationManager.getInstance().getCurrentLongitute(), 
        		LocationManager.getInstance().getCurrentLatitude(),
        		LocationManager.getInstance().getRandomLongitute(), 
        		LocationManager.getInstance().getRandomLatitude());
                
        
        while (System.in.available() == 0 && !isShutdown) {  // loop now, just use main thread
        	if (!isPickupAndMovingToTarget) {
        		if (retryCnt%TIMEOUT_FOR_RETRY==0) {
        			if (retryCnt>0) {
        				System.out.println("Retry " + retryCnt/TIMEOUT_FOR_RETRY + " On area "+LocationManager.getInstance().getCurrentArea());
        			}
        			reqMsg.TIMESTAMP=Epoch.getSec();
        			publisher.publish(gson.toJson(reqMsg).getBytes(), Topic.of("RideRequest/"+LocationManager.getInstance().getCurrentArea()));
        		}
        	} else {
        		//let's escape and wait for PaymentRequest
        		break;
        	}
        	retryCnt++;
        	Thread.sleep(1000);
        }
        

        while (isPickupAndMovingToTarget) {  // loop now, just use main thread
        	System.out.println("Wait for payment completed");
        	Thread.sleep(5000);
        }
                
        isShutdown = true;
        publisher.terminate(500);
        rideRequestResponseReceiver.terminate(500);
        paymentRequestReceiver.terminate(500);
        messagingService.disconnect();
        System.out.println("Ride Done " + rideId);
    }

	private static RideRequestMessage getRideRequestMessageString(int userId2, double initX, double initY, double destX, double destY) {		
		return new RideRequestMessage(userId2, initX, initY, destX, destY);				
	}
	
}
