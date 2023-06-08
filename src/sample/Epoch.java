package ajou.aiot.samples;

public class Epoch {
	public static final int getSec() {
		return (int) (getMsec()/1000);
	}
	public static final long getMsec() {
		return System.currentTimeMillis();
	}
}
