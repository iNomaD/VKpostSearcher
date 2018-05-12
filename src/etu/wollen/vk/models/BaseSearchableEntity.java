package etu.wollen.vk.models;

import java.util.Date;

public abstract class BaseSearchableEntity {

    protected Date date;

    public BaseSearchableEntity(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
