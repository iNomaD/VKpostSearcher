package etu.wollen.vk.models;

import java.util.Comparator;

public class DateComparator implements Comparator<BaseSearchableEntity> {

    @Override
    public int compare(BaseSearchableEntity w1, BaseSearchableEntity w2) {
        return w1.getDate().compareTo(w2.getDate());
    }
}
