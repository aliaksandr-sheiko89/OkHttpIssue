package example.okhtttp.issue.cancell.okhttpcancellissue;

import com.google.gson.JsonElement;

import io.reactivex.Single;
import retrofit2.http.GET;

public interface TestService {
    @GET("search?q=test")
    Single<JsonElement> performRequest2();
}
