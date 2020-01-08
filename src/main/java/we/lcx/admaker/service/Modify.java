package we.lcx.admaker.service;

import java.util.Collection;

/**
 * Created by LinChenxiao on 2020/01/05 23:04
 **/
public interface Modify {
    boolean update(Collection<Integer> list, boolean flag);
    boolean remove(Collection<Integer> list);
    void refreshAds();
    Collection getAds();
    void clean();
}
