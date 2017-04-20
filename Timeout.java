import java.util.Timer;
import java.util.TimerTask;

public class Timeout {
	private Timer timer;
	private boolean running;

	public Timeout(int seconds) {
		timer = new Timer();
		running = true;
		//timer.schedule(new RemindTask(this), seconds*1000);

		timer.schedule(new TimerTask() {
	      		@Override
	      		public void run() {
	        		System.out.println("time expired");
	        		running = false;
	      		}	
	    	}, seconds*1000);
	}

	public boolean isRunning() {
		return running;
	}

	public static void main(String[] args) {
		Timeout timeout = new Timeout(2); 
		while (timeout.isRunning());	
		System.out.println("Timeout occured");
	}

}
