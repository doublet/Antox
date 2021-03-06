package im.tox.antox.tox;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import im.tox.antox.utils.Constants;
import im.tox.jtoxcore.ToxException;

public class ToxDoService extends IntentService {

    private static final String TAG = "im.tox.antox.tox.ToxDoService";

    private ToxScheduleTaskExecutor toxScheduleTaskExecutor = new ToxScheduleTaskExecutor(1);

    private ToxSingleton toxSingleton = ToxSingleton.getInstance();;

    public ToxDoService() {
        super("ToxDoService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent.getAction().equals(Constants.START_TOX)) {
                toxSingleton.initTox(getApplicationContext());
                toxScheduleTaskExecutor.scheduleAtFixedRate(new DoTox(), 0, 50, TimeUnit.MILLISECONDS);
        } else if (intent.getAction().equals(Constants.STOP_TOX)) {
            if (toxScheduleTaskExecutor != null) {
                toxScheduleTaskExecutor.shutdownNow();
            }
            stopSelf();
        }
    }

    /* Extend the scheduler to have it restart itself on any exceptions */
    private class ToxScheduleTaskExecutor extends ScheduledThreadPoolExecutor {

        public ToxScheduleTaskExecutor(int size) {
            super(1);
        }

        @Override
        public ScheduledFuture scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return super.scheduleAtFixedRate(wrapRunnable(command), initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return super.scheduleWithFixedDelay(wrapRunnable(command), initialDelay, delay, unit);
        }

        private Runnable wrapRunnable(Runnable command) {
            return new LogOnExceptionRunnable(command);
        }

        private class LogOnExceptionRunnable implements Runnable{
            private Runnable theRunnable;
            public LogOnExceptionRunnable(Runnable theRunnable) {
                super();
                this.theRunnable = theRunnable;
            }
            @Override
            public void run() {
                try {
                    theRunnable.run();
                } catch (Exception e) {
                    Log.d(TAG, "Executor has caught an exception");
                    e.printStackTrace();
                    toxScheduleTaskExecutor.scheduleAtFixedRate(new DoTox(), 0, 50, TimeUnit.MILLISECONDS);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private class DoTox implements Runnable {
        @Override
        public void run() {
            /* Praise the sun */
            try {
                toxSingleton.jTox.doTox();
                toxSingleton.isRunning = true;
            } catch (ToxException e) {
                Log.d(TAG, e.getError().toString());
                e.printStackTrace();
            }
        }
    }
}
