package wuxiang.jd.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import wuxiang.jd.pojo.Item;

public interface ItemDao extends JpaRepository<Item,Long> {

}
