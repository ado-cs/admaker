package we.lcx.admaker.common;

import lombok.Data;
import org.springframework.http.HttpMethod;
import java.util.Map;

/**
 * Created by LinChenxiao on 2019/12/12 19:02
 **/
@Data
public class Task {
    private String url;
    private HttpMethod method = HttpMethod.POST;
    private String cookie;
    private Map<String, Object> params;
}
