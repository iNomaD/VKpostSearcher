package etu.wollen.vk.model.database;

import java.util.Date;

public abstract class BaseSearchableEntity implements PrintableEntity {

    protected Date date;

    BaseSearchableEntity(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
