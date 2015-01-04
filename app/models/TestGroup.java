package models;

import junit.framework.Test;
import play.db.ebean.Model;

import javax.persistence.*;
import java.util.List;

/**
 * Represents an entity that might contain multiple test cases
 * Created by yerlibilgin on 22/12/14.
 */
@Entity
@Table(name = "TestCaseGroup")
public class TestGroup extends Model {
  @Id
  public Long id;

  @Column(unique = true, nullable = false)
  public String name;

  @ManyToOne
  @Column(nullable = false)
  public User owner;

  @OneToMany(cascade = CascadeType.ALL)
  @Basic(fetch = FetchType.LAZY)
  public List<TestAssertion> testAssertions;

  @Column(nullable = false, length = 50)
  public String shortDescription;

  public String description;

  public static final Finder<Long, TestGroup> find = new Finder<>(
      Long.class, TestGroup.class);

  public static List<TestGroup> findByUser(User user){
    return find.where().eq("owner", user).setOrderBy("id").findList();
  }
  public static TestGroup findByName(String name) {
    return find.where().eq("name", name).findUnique();
  }
}