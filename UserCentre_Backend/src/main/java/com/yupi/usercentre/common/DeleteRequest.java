package com.yupi.usercentre.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用删除请求参数
 *
 * 加上序列化器，方便序列化
 */
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = -1234567890L;

    /**
     * id
     */
    private long id;

}

