package we.lcx.admaker.common.consts;

/**
 * Created by LinChenxiao on 2019/12/12 20:00
 **/
public interface URLs {
    String YUNYING_LIST = "flight/list";
    String YUNYING_QUERY = "flight/getFlightTemplate";
    String YUNYING_CREATE = "position/create";
    String YUNYING_FLIGHT = "adConfig/queryList";
    String YUNYING_STATUS = "adConfig/changAdFlightAccessStatus";

    String MAISUI_PLAN = "adPlan/create";
    String MAISUI_PRICE = "campaignPackage/getAdvisedPrice";
    String MAISUI_CREATE = "adform/create";
    String MAISUI_PACKAGES = "campaignPackage/query/list";

    String MAITIAN_RESOURCE = "resource/list";
    String MAITIAN_RESOURCE_NEW = "resource/create";
    String MAITIAN_ITEM = "resource/item/create";
    String MAITIAN_REVENUE = "resource/revenue/create";
    String MAITIAN_RESERVE = "reserve/create";
    String MAITIAN_DEAL = "schedule/create";
    String MAITIAN_QUERY = "schedule/item/getReserveResourceItem";
    String MAITIAN_DEAL_ITEM = "schedule/item/create";
    String MAITIAN_TEMPLATE = "advertise/listTemplate";
    String MAITIAN_CREATE = "advertise/create";

    String MAITIAN_PAGE = "schedule/page";
    String MAITIAN_DETAIL = "schedule/detail";
    String MAITIAN_ITEM_CLOSE = "schedule/item/trafficSwitch/update";
    String MAITIAN_AD_LIST = "advertise/list";
    String MAITIAN_AD_CLOSE = "advertise/status";
    String MAITIAN_RESERVATION_DELETE = "reserve/delete";
    String MAITIAN_ITEM_DELETE = "schedule/item/delete";
    String MAITIAN_AD_DELETE = "advertise/delete";

    String COMMON_APPROVE = "autoAudit/callback?inner=inner";

}
