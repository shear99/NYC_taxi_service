package ajou.aiot.samples;

public class Loc {
	double longitude;
	double latitude;
	Loc(double x, double y){
		this.longitude=x;
		this.latitude=y;
	}
	@Override
	public String toString() {
		return "Loc [longitude=" + String.format("%.2f", longitude) + ", latitude=" + String.format("%.2f", latitude) + "]";
	}
	
}
