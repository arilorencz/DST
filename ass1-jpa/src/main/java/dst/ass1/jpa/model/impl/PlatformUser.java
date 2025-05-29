package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.IPlatformUser;
import dst.ass1.jpa.util.Constants;

import javax.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class PlatformUser implements IPlatformUser {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String tel;

    @Column
    private Double avgRating;

    // getters and setters
    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    @Override
    public String getName() { return name; }

    @Override
    public void setName(String name) { this.name = name; }

    @Override
    public String getTel() { return tel; }

    @Override
    public void setTel(String tel) { this.tel = tel; }

    @Override
    public Double getAvgRating() { return avgRating; }

    @Override
    public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }
}
