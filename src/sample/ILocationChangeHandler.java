package sample;
public interface ILocationChangeHandler {
	public void onAreaChange(int prev, int current);
	public void onLocationChange(int area, double longitute, double latitude);
}
