package entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Post {

    @Id
    @GeneratedValue
    private Integer id;
    private String title;
    private String photo;
    private String geo;

    public Post() {}

    public String getGeo() { return geo; }

    public void setGeo(String geo) { this.geo = geo; }

    public String getPhoto() { return photo; }

    public void setPhoto(String photo) { this.photo = photo; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public Integer getId() { return id; }

    public void setId(Integer id) { this.id = id; }
}
