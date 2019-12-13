package we.lcx.admaker.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import we.lcx.admaker.common.AdPackage;
import we.lcx.admaker.common.AdUnit;
import we.lcx.admaker.common.consts.Cookies;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.common.enums.ShowType;
import we.lcx.admaker.utils.DataKeeper;
import we.lcx.admaker.utils.Helper;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.TaskBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by LinChenxiao on 2019/12/13 09:54
 **/
@Slf4j
@Service
public class BaseInfo {

    @SuppressWarnings("unchecked")
    public List<AdPackage> getPackages() {
        Object var0 = DataKeeper.getCache("packages");
        if (var0 != null) return (List) var0;
        String var1 = HttpExecutor.doRequest(
                TaskBuilder.post(URLs.URL + URLs.MAISUI_PACKAGES)
                        .cookie(Cookies.MAISUI)
                        .param(Params.MAISUI_PACKAGES)
                        .build()).getBody();
        Map var2 = JSON.parseObject(var1, Map.class);
        if (Helper.valid(var2, "false", "success")) {
            log.error("login expired. body={}", var1);
            return null;
        }
        List var3 = Helper.getList(var2, "result", "list");
        if (var3 == null) return null;
        List<AdPackage> var4 = new ArrayList<>();
        for (Object var5 : var3) {
            if (var5 instanceof Map) {
                Map var6 = (Map) var5;
                if (Helper.valid(var6, "201", "status", "code")) {
                    AdPackage var7 = new AdPackage();
                    var7.setId(Helper.getString(var6, "uid"));
                    var7.setName(Helper.getString(var6, "name"));
                    List var8 = Helper.getList(var6, "templateList");
                    if (CollectionUtils.isEmpty(var8)) continue;
                    Object var9 = var8.get(0);
                    if (!(var9 instanceof Map)) continue;
                    Map var10 = (Map) var9;
                    var7.setRefId(Helper.getString(var10, "uid"));
                    var7.setType(ShowType.of(Helper.getString(var10, "mainShowType", "code")));
                    List var11 = Helper.getList(var10, "locationTypeJsonList");
                    if (CollectionUtils.isEmpty(var11)) continue;
                    List<AdUnit> var12 = new ArrayList<>();
                    for (Object var13 : var11) {
                        if (!(var13 instanceof Map)) continue;
                        Map var14 = (Map) var13;
                        if (!Helper.valid(var14, "1", "need")) continue;
                        AdUnit var15 = new AdUnit();
                        var15.setId(Helper.getString(var14, "uid"));
                        var15.setOrder(Helper.getString(var14, "orderId"));
                        if (Helper.valid(var14, "TEXT", "type")) {
                            var15.setType(ShowType.TEXT);
                            var15.setLimit(Helper.getString(var14, "length"));
                        } else {
                            var15.setType(ShowType.PICTURE);
                            var15.setLimit(Helper.getString(var14, "size"));
                        }
                        var12.add(var15);
                    }
                    var7.setUnits(var12);
                    var4.add(var7);
                }
            }
        }
        DataKeeper.putCache("packages", var4, 600 * 1000);
        return var4;
    }

    public boolean newPackage(String id, List<String> info) {

        return true;
    }
}

