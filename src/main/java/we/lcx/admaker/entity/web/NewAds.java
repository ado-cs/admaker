package we.lcx.admaker.entity.web;

import lombok.Data;

/**
 * Created by Lin Chenxiao on 2019-12-22
 **/
@Data
public class NewAds {
    private Integer flight;
    private Integer type;
    private Integer amount;
    private Integer deal;
    private Integer fee;
    private Integer flow;
    private String name;
    private String begin;
    private String end;
}
