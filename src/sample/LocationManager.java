package ajou.aiot.samples;

import java.util.ArrayList;
import java.util.Random;



public class LocationManager {

    private static LocationManager instance;
	private ILocationChangeHandler areaChangeCallback;
	LocSimulator locationSimulator;
	
	
	class Rect {
		double x1,x2,y1,y2;
		Rect() {}
		public void setRange(double x1, double x2, double y1, double y2) {
			this.x1=x1;
			this.x2=x2;
			this.y1=y1;
			this.y2=y2;
		}		
		
		public boolean contains(double x, double y) {
			if (x>=x1 && x<x2 && y>=y1 && y<y2) {
				return true;
			} 
			return false;
		}
	}
	
	class LocSimulator implements Runnable{		
		Thread myThread=null;
		private ArrayList<Rect> areaList;
		final double MIN_X=126D;
		final double MIN_Y=34D;
		final double MAX_X=130D;
		final double MAX_Y=39D;
		double nowX=-1;
		double nowY=-1;
		private Random rand;
		int areaCode;
		LocSimulator(){
			this.rand = new Random();	        
			initLoc();
		}
		@Override
		public void run() {			
			long locationUpdateInterval=1000;
			while(true){
				try {
					Thread.sleep(locationUpdateInterval);
					drive();					
					int tmpAreaCode=getArea(nowX,nowY);					
					if (tmpAreaCode!=this.areaCode) {
						int oldAreaCode=this.areaCode;
						this.areaCode = tmpAreaCode;						
						areaChangeCallback.onAreaChange(oldAreaCode, this.areaCode);
						locationUpdateInterval=2000+rand.nextInt(10000);//allow some time to stay at one location
					} else {
						locationUpdateInterval=1000;
					}
					areaChangeCallback.onLocationChange(tmpAreaCode, nowX, nowY);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
		}
		
		private void drive() {
			double pX=-1D;
			double pY=-1D;
			if (rand.nextBoolean()) {
				//for X gen polaritiry is positive
				pX=1D;
			} 
			
			if (rand.nextBoolean()) {
				pY=1D;
			}
				
			double tmpX=this.nowX+pX*this.rand.nextDouble()*this.rand.nextInt(2);
			if (tmpX<this.MIN_X) {
				tmpX=this.MIN_X;
			} else if (tmpX>this.MAX_X) {
				tmpX=this.MAX_X-0.0001D;
			}
			
			double tmpY=this.nowY+pY*this.rand.nextDouble()*this.rand.nextInt(3);
			if (tmpY<this.MIN_Y) {
				tmpY=this.MIN_Y;
			} else if (tmpY>this.MAX_Y) {
				tmpY=this.MAX_Y-0.0001D;
			}
			this.nowX=tmpX;
			this.nowY=tmpY;
			
		}
		

		private void initLoc() {
			areaList=new ArrayList<Rect>();
			for (int i=0;i<9;i++) {
				areaList.add(new Rect());
			}
			// x: 126~130, y: 34~39 
			//area1 -> 경도(126~127.3333), 위도 37.3333~39
	        areaList.get(0).setRange(126, 127.3333, 37.3333, 39);
	        //area2 -> 경도(127.3333~ 128.6666), 위도 37.3333~39
	        areaList.get(1).setRange(127.3333, 128.6666, 37.3333, 39);
	        //area3 -> 경도(128.6666~ 130), 위도 37.3333~39
	        areaList.get(2).setRange(128.6666, 130, 37.3333, 39);
	        //area4 -> 경도(126~127.3333), 위도 35.6777~37.3333
	        areaList.get(3).setRange(126, 127.3333, 35.6777, 37.3333);
	        //area5 -> 경도(127.3333~ 128.6666), 위도 35.6777~37.3333
	        areaList.get(4).setRange(127.3333, 128.6666, 35.6777, 37.3333);
	        //area6 -> 경도(128.6666~ 130), 위도 35.6777~37.3333
	        areaList.get(5).setRange(128.6666, 130, 35.6777, 37.3333);
	        //area7 -> 경도(126~127.3333), 위도 34~ 35.6777
	        areaList.get(6).setRange(126, 127.3333, 34, 35.6777);
	        //area8 -> 경도(127.3333~ 128.6666), 위도 34~ 35.6777
	        areaList.get(7).setRange(127.3333, 128.6666, 34, 35.6777);
	        //area9 -> 경도(128.6666~ 130), 위도 34~ 35.6777
	        areaList.get(8).setRange(128.6666, 130, 34, 35.6777);	        
	        this.nowX=rand.nextDouble()*0.99 * (this.MAX_X-this.MIN_X) + this.MIN_X; 
	        this.nowY=rand.nextDouble()*0.99 * (this.MAX_Y-this.MIN_Y) + this.MIN_Y;
	        this.areaCode=getArea(nowX,nowY);
	        	        
		}
		private int getArea(double x, double y) {
			for (int i=0;i<9;i++) {
				if (areaList.get(i).contains(x, y))
					return i;
			}
			System.err.println("Can find areaCode for " + x + ", "+ y);
			System.exit(-1);
			return -1;
		}

		public void start() {
			myThread.start();
		}

		public double getX() {
			return nowX;
		}
		public double getY() {
			return nowY;
		}

		public double getRandY() {
			// TODO Auto-generated method stub
			return rand.nextDouble()*0.99 * (this.MAX_Y-this.MIN_Y) + this.MIN_Y;
		}

		public double getRandX() {
			// TODO Auto-generated method stub
			return nowX=rand.nextDouble()*0.99 * (this.MAX_X-this.MIN_X) + this.MIN_X;
		}
	} 
	
    private LocationManager(){
    	
    	initSimulator();
    }
    
    private void initSimulator() {
    	locationSimulator=new LocSimulator();
    	locationSimulator.myThread=new Thread(locationSimulator);
    	    	
	}

	public static synchronized LocationManager getInstance() {
        if (instance == null) {
            instance = new LocationManager();
        }
        return instance;
    }

	
	public void startService() {
		locationSimulator.start();
	}

	public void addLocationChangeCallback(ILocationChangeHandler iAreaChangeHandler) {
		this.areaChangeCallback=iAreaChangeHandler;
	}

	public int getCurrentArea() {		
		return this.locationSimulator.areaCode;
	}
	
	public double getCurrentLatitude() {
		return this.locationSimulator.nowY;
	}

	public double getCurrentLongitute() {
		return this.locationSimulator.nowX;
	}

	public double getRandomLatitude() {
		return this.locationSimulator.getRandY();
	}

	public double getRandomLongitute() {
		return this.locationSimulator.getRandX();
	}
}
