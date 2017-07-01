package uclan.ac.uk.weatherapp.broadcast.receiver;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;


public class CheckConnectivity extends BroadcastReceiver {
    private final PublishSubject<Boolean> onNetworkStateChange = PublishSubject.create();

    @Override
    public void onReceive(Context context, Intent arg1) {
        final boolean isNotConnected = arg1.getBooleanExtra(
                ConnectivityManager.EXTRA_NO_CONNECTIVITY
                , false);

        onNetworkStateChange.onNext(isNotConnected);
    }

    public Observable<Boolean> getNetworkStateChange() {
        return onNetworkStateChange.hide();
    }
}
