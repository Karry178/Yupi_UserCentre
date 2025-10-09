package com.yupi.usercentre.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页请求参数
 *
 * 加上序列化器，方便序列化
 */
@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = -1234567890L;

    /**
     * 默认页面大小
     */
    protected int pageSize = 10;

    /**
     * 默认当前页号
     */
    protected int pageNum = 1;
}
