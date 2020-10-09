package wuxiang.jd.service;

import wuxiang.jd.pojo.Item;

import java.util.List;

public interface ItemService {

    /**
     *
     * @param item
     */
    public void save(Item item);

    public List<Item> findAll(Item item);
}
