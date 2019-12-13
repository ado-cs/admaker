package we.lcx.admaker.service;

import org.springframework.stereotype.Service;
import we.lcx.admaker.common.AdPackage;
import we.lcx.admaker.common.consts.Cookies;
import we.lcx.admaker.common.enums.BiddingMode;
import we.lcx.admaker.common.consts.Params;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.utils.Helper;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.TaskBuilder;

/**
 * Created by LinChenxiao on 2019/12/12 19:21
 **/
@Service
public class MaiSui {
    public String newPlan(String name) {
        return Helper.parseBody(HttpExecutor.doRequest(
                TaskBuilder.post(URLs.URL + URLs.MAISUI_PLAN)
                        .cookie(Cookies.MAISUI)
                        .param(Params.MAISUI_PLAN)
                        .param("adPlanName", name + Helper.randomSuffix(6)).build()).getBody(), "adPlanId");
    }

    public String getPrice(String planId, String uid, BiddingMode mode) {
        return Helper.parseBody(HttpExecutor.doRequest(
                TaskBuilder.post(URLs.URL + URLs.MAISUI_PRICE)
                        .cookie(Cookies.MAISUI)
                        .param(Params.MAISUI_PRICE)
                        .param("adPlanId", planId)
                        .param("campaignPackageUid", uid)
                        .param("billingMode", mode.getCode()).build()).getBody(), "campaignPackagePrice");
    }

    public String createAd(String planId, String adName, BiddingMode mode, String price, AdPackage pack, int days) {
        return null;
    }
}
