package we.lcx.admaker.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import we.lcx.admaker.common.AdPackage;
import we.lcx.admaker.common.AdUnit;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.Settings;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.common.enums.ShowType;
import we.lcx.admaker.utils.WordsTool;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.TaskBuilder;
import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Created by LinChenxiao on 2019/12/13 09:54
 **/
@Slf4j
@Service
public class Basic {
    @Value("${ad.maisui.url}")
    private String URL;

    @Value("${ad.maisui.cookie}")
    private String COOKIE;

    private Map<String, AdPackage> packages = new HashMap<>();

    @PostConstruct
    public void login() {
        if (WordsTool.valid(HttpExecutor.doRequest(
                TaskBuilder.get(URL + URLs.MAISUI_LOGIN)
                        .cookie(COOKIE)
                        .build()).getBody(), "false", "success")) {
            log.error("登录麦穗失败.");
            return;
        }
        List<AdPackage> list = getPackagesList();
        if (CollectionUtils.isEmpty(list)) {
            log.error("获取广告版位失败.");
            return;
        }
        Map<String, AdPackage> map = new HashMap<>();
        for (AdPackage v : list) map.put(v.getId(), v);
        synchronized (this) {
            packages = map;
        }

    }

    private List<AdPackage> getPackagesList() {
        String var1 = HttpExecutor.doRequest(
                TaskBuilder.post(URL + URLs.YUNYING_URL + URLs.MAISUI_PACKAGES)
                        .cookie(COOKIE)
                        .param(Params.MAISUI_PACKAGES)
                        .build()).getBody();
        Map var2 = JSON.parseObject(var1, Map.class);
        if (WordsTool.valid(var2, "false", "success")) {
            log.error("login expired. body={}", var1);
            return null;
        }
        List var3 = WordsTool.getList(var2, "result", "list");
        if (var3 == null) return null;
        List<AdPackage> var4 = new ArrayList<>();
        for (Object var5 : var3) {
            if (var5 instanceof Map) {
                Map var6 = (Map) var5;
                if (WordsTool.valid(var6, "201", "status", "code")) {
                    AdPackage var7 = new AdPackage();
                    var7.setId(WordsTool.getString(var6, "uid"));
                    var7.setName(WordsTool.getString(var6, "name"));
                    List var8 = WordsTool.getList(var6, "templateList");
                    if (CollectionUtils.isEmpty(var8)) continue;
                    Object var9 = var8.get(0);
                    if (!(var9 instanceof Map)) continue;
                    Map var10 = (Map) var9;
                    var7.setRefId(WordsTool.getString(var10, "uid"));
                    var7.setMainType(ShowType.of(WordsTool.getString(var10, "mainShowType", "code")));
                    var7.setShowType(WordsTool.getString(var10, "showType"));
                    List var11 = WordsTool.getList(var10, "locationTypeJsonList");
                    if (CollectionUtils.isEmpty(var11)) continue;
                    List<AdUnit> var12 = new ArrayList<>();
                    for (Object var13 : var11) {
                        if (!(var13 instanceof Map)) continue;
                        Map var14 = (Map) var13;
                        if (!WordsTool.valid(var14, "1", "need")) continue;
                        AdUnit var15 = new AdUnit();
                        var15.setId(WordsTool.getString(var14, "uid"));
                        var15.setOrder(WordsTool.getString(var14, "orderId"));
                        if (WordsTool.valid(var14, "TEXT", "type")) {
                            var15.setType(ShowType.TEXT);
                            var15.setLimit(WordsTool.getString(var14, "length"));
                            var15.setContent(WordsTool.repeat(var15.getLimit()));
                        } else {
                            var15.setContent(Settings.DEFAULT_URL);
                            var15.setAppendix(Settings.DEFAULT_MD5);
                            var15.setType(ShowType.PICTURE);
                            var15.setLimit(WordsTool.getString(var14, "size"));
                        }
                        var12.add(var15);
                    }
                    var7.setUnits(var12);
                    var4.add(var7);
                }
            }
        }
        return var4;
    }

    public Collection<AdPackage> getPackages() {
        return packages.values();
    }

    public AdPackage getPackage(String id) {
        return packages.get(id);
    }
}

