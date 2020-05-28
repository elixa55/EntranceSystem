package model;

import javafx.collections.ObservableList;

public interface PersonDao {
    final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    final String URL = "jdbc:mysql://localhost/finger";
        final String USERNAME ="root";
        final String PASSWORD = "";
    void createTable();
    ObservableList<Person> get();
    void add(Person p);
    void remove(Person p);
    void update(Person p);
}
