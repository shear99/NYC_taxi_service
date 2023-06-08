
package ajou.aiot.samples;

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


public class DriverApp {    
	
    private static volatile boolean isShutdown = false;           // are we done yet?
    private static boolean isPickupAndMovingToTarget=false;
	private static int driverId=0;
	
    /** Simple application for doing pub/sub. */
    public static void main(String... args) throws IOException, InterruptedException {
    	
    	
        if (args.length < 4) {  // Check command line arguments
            System.out.printf("Usage: DriverApp <host:port> <message-vpn> <client-username> <password> <DriverID>%n", "DriverApp");            
            System.exit(-1);
        }
        
        Gson gson = new Gson();
        final Properties properties = new Properties();
        properties.setProperty(TransportLayerProperties.HOST, args[0]);          // host:port
        properties.setProperty(ServiceProperties.VPN_NAME,  args[1]);     // message-vpn
        properties.setProperty(AuthenticationProperties.SCHEME_BASIC_USER_NAME, args[2]);      // client-username
        if (args.length > 3) {
            properties.setProperty(AuthenticationProperties.SCHEME_BASIC_PASSWORD, args[3]);  // client-password
        }
        if (args.length > 4) {
        	driverId=Integer.parseInt(args[4]);
        }
        properties.setProperty(ServiceProperties.RECEIVER_DIRECT_SUBSCRIPTION_REAPPLY, "true");  // subscribe Direct subs after reconnect

        final MessagingService messagingService = MessagingService.builder(ConfigurationProfile.V1)
                .fromProperties(properties).build().connect();  // blocking connect to the broker

        // create and start the publisher 
        final DirectMessagePublisher publisher = messagingService.createDirectMessagePublisherBuilder()
                .onBackPressureWait(1).build().start();
        

        
        LocationManager.getInstance().startService();
        final DirectMessageReceiver pickupRequestReceiver = messagingService.createDirectMessageReceiverBuilder()
        		.withSubscriptions(TopicSubscription.of("PickupRequest/"+LocationManager.getInstance().getCurrentArea())).build().start();
        
        LocationManager.getInstance().addLocationChangeCallback(new ILocationChangeHandler() {
			@Override
			public void onAreaChange(int prev, int current) {
				try {
					pickupRequestReceiver.addSubscription(TopicSubscription.of("PickupRequest/"+current));
					pickupRequestReceiver.removeSubscription(TopicSubscription.of("PickupRequest/"+prev));
					System.out.println("AreaChanged from " + prev + " to "+ current );
				} catch (PubSubPlusClientException | InterruptedException e) {
					isShutdown=true;
					e.printStackTrace();
				}							
			}

			@Override
			public void onLocationChange(int area, double longitute, double latitude) {
				String status=(isPickupAndMovingToTarget?"BUSY":"IDLE");
				String topicString = "LocationUpdate/"+driverId+"/"
							+ status+"/"+area;
		        //System.out.printf(">> Calling send() on %s%n",topicString);
				publisher.publish(getLocationUpdateMessageString(driverId,status, longitute, latitude), Topic.of(topicString));				
			}
        	
			private byte[] getLocationUpdateMessageString(int driverId, String status, double longitute, double latitude) {
				LocUpdateMessage msg = new LocUpdateMessage(driverId, status,  longitute, latitude);				
				String jsonInString = gson.toJson(msg);
				return jsonInString.getBytes();
			}
        });
        
        final MessageHandler pickupReqHandler = (inboundMessage) -> {
            System.out.println("DUMP: \n"+inboundMessage.dump());  // just print
        	
            if (!isPickupAndMovingToTarget) {
            	System.out.println("I am busy now");	
            }
            
            PickupRequestMessage pickUpMsg = gson.fromJson(inboundMessage.getPayloadAsString(), PickupRequestMessage.class);
            
            PickupRequestResponseMessage pickupRespMsg  = new PickupRequestResponseMessage(pickUpMsg.RIDEID, "SUCCESS", driverId);
            publisher.publish(gson.toJson(pickupRespMsg).getBytes(), Topic.of("PickupRequestResponse/"+inboundMessage.getDestinationName().split("/")[1]));
                    	
            isPickupAndMovingToTarget=true;
        	
            try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            PickupCompleteMessage pickupCompleteMsg  = new PickupCompleteMessage(pickUpMsg.RIDEID, pickUpMsg.CURRENT);
            publisher.publish(gson.toJson(pickupCompleteMsg).getBytes(), Topic.of("PickupComplete/"+inboundMessage.getDestinationName().split("/")[1]));
            
            try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            DropoffCompleteMessage dropoffCompleteMsg  = new DropoffCompleteMessage(pickUpMsg.RIDEID, pickUpMsg.DESTINATION, driverId);
            publisher.publish(gson.toJson(dropoffCompleteMsg).getBytes(), Topic.of("DropoffComplete/"+inboundMessage.getDestinationName().split("/")[1]));
            isPickupAndMovingToTarget=false;
        };
                
        
                
        pickupRequestReceiver.receiveAsync(pickupReqHandler);
        
      
        while (System.in.available() == 0 && !isShutdown) {  // loop now, just use main thread
            try {
                            	
            } catch (RuntimeException e) {
                System.out.printf("### Exception caught during producer.send(): %s%n",e);
                isShutdown = true;
            }
        }
        isShutdown = true;
        publisher.terminate(500);
        pickupRequestReceiver.terminate(500);
        messagingService.disconnect();
        System.out.println("Main thread quitting.");
    }

    

	
}
