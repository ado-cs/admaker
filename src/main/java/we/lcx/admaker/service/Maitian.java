package we.lcx.admaker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import we.lcx.admaker.common.consts.URLs;
import we.lcx.admaker.utils.WordsTool;
import we.lcx.admaker.utils.HttpExecutor;
import we.lcx.admaker.utils.TaskBuilder;

import javax.annotation.PostConstruct;

/**
 * Created by LinChenxiao on 2019/12/19 16:07
 **/
@Slf4j
@Service
public class Maitian {
    @Value("${ad.maitian.url}")
    private String URL;

    @Value("${ad.maitian.cookie}")
    private String COOKIE;

    //以下字段需在麦田提前建立对应条目

    @Value("${ad.maitian.resourceId}")
    private String RESOURCE_ID; //资源id



    @PostConstruct
    public void login() {
        if (WordsTool.valid(HttpExecutor.doRequest(
                TaskBuilder.post(URL + URLs.MAITIAN_LOGIN)
                        .cookie(COOKIE)
                        .build()).getBody(), "false", "success")) {
            log.error("login failed.");
        }
    }

    private void createItem() {

    }

}
