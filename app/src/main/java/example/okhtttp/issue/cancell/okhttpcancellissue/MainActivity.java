package example.okhtttp.issue.cancell.okhttpcancellissue;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private TestService testService;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CompositeDisposable mainDisposables = new CompositeDisposable();
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // If comment ProviderInstaller.installIfNeeded(getApplicationContext()); the can not be reproduced
        try {
            ProviderInstaller.installIfNeeded(getApplicationContext());
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.google.by/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(createApiBuilder().build())
                .build();

        testService = retrofit.create(TestService.class);
        random = new Random();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseResources();
    }

    public void retry(View view) {
        releaseResources();

        mainDisposables.add(
                Completable.fromAction(new Action() {
                    @Override
                    public void run() throws Exception {
                        System.out.println("MainActivity.run");
                        disposables.clear();

                        disposables.add(testService.performRequest2()
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeWith(new TestObserver()));
                        disposables.add(testService.performRequest2()
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeWith(new TestObserver()));
                    }
                })
                        .delay(random.nextInt(200) + 70, TimeUnit.MILLISECONDS)
                        .repeat(20)
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableCompletableObserver() {
                            @Override
                            public void onComplete() {
                                System.out.println("complete!");
                            }

                            @Override
                            public void onError(Throwable e) {
                                System.err.println("e = [" + e + "]");
                            }
                        }));
    }

    private void releaseResources() {
        disposables.clear();
        mainDisposables.clear();
    }

    private static OkHttpClient.Builder createApiBuilder() {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
        logger.setLevel(HttpLoggingInterceptor.Level.BODY);
        httpClient.addInterceptor(logger);
        httpClient.connectTimeout(15, TimeUnit.SECONDS);
        httpClient.readTimeout(15, TimeUnit.SECONDS);

        return httpClient;
    }

    private static class TestObserver extends DisposableSingleObserver<JsonElement> {
        @Override
        public void onSuccess(JsonElement s) {
            System.out.println("TestObserver.onSuccess");
        }

        @Override
        public void onError(Throwable e) {
            System.err.println("e = [" + e + "]");
        }
    }
}
