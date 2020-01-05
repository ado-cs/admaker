package we.lcx.admaker.service;

import we.lcx.admaker.common.enums.DealMode;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by LinChenxiao on 2020/01/05 23:04
 **/
public interface Modify {
    void update(Collection list, boolean flag);
    void remove(Collection list);
    void refreshAds();
    List getAds(String flightName, DealMode deal, Integer fee);
    Map getAds();
}
