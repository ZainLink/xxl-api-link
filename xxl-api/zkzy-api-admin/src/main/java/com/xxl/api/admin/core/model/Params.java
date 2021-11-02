package com.xxl.api.admin.core.model;

import java.io.Serializable;

/**
 * Created by Thinkpad-W530 on 2021/10/29.
 */
public class Params implements Serializable {

    private String notNull;

    private String type;

    private String name;

    private String desc;


    public String getNotNull() {
        return notNull;
    }

    public void setNotNull(String notNull) {
        this.notNull = notNull;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
