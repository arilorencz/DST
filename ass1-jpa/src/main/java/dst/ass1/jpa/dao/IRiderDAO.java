package dst.ass1.jpa.dao;

import dst.ass1.jpa.model.IRider;

import java.util.Date;
import java.util.List;

public interface IRiderDAO extends GenericDAO<IRider> {

    IRider findByEmail(String email);
}
