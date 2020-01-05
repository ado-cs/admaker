package we.lcx.admaker.common.consts;

/**
 * Created by LinChenxiao on 2019/12/12 20:00
 **/
public interface Urls {
    String YUNYING_POSITIONS = "position/list";
    String YUNYING_TEMPLATES = "flight/getFlightTemplate";
    String YUNYING_UNITS = "template/single";
    String YUNYING_FLIGHT_QUERY = "flight/list";
    String YUNYING_CREATE = "position/create";
    String YUNYING_FLIGHT = "adConfig/queryList";
    String YUNYING_STATUS = "adConfig/changAdFlightAccessStatus";

    String MAISUI_PRICE = "campaignPackage/getAdvisedPrice";
    String MAISUI_CREATE = "adform/create";
    String MAISUI_CREATIVE_QUERY = "adform/query/info";
    String MAISUI_AD_LIST = "adform/query/list";
    String MAISUI_OPEN = "adform/update/serve";
    String MAISUI_PAUSE = "adform/update/pause";
    String MAISUI_DELETE = "adform/delete";

    String MAITIAN_RESOURCE = "resource/list";
    String MAITIAN_RESOURCE_NEW = "resource/create";
    String MAITIAN_ITEM_LIST = "resource/single";
    String MAITIAN_ITEM = "resource/item/create";
    String MAITIAN_REVENUE = "resource/revenue/create";
    String MAITIAN_RESERVE = "reserve/create";
    String MAITIAN_DEAL_LIST = "schedule/page";
    String MAITIAN_DEAL = "schedule/create";
    String MAITIAN_QUERY = "schedule/item/getReserveResourceItem";
    String MAITIAN_DEAL_ITEM = "schedule/item/create";
    String MAITIAN_TEMPLATE = "advertise/listTemplate";
    String MAITIAN_CREATE = "advertise/create";
    String MAITIAN_CREATIVE_QUERY = "advertise/itemCreatives";

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
