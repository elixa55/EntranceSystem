package model;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class PersonDaoImpl implements PersonDao {

    /**
     * 
     */
    Connection conn = null;
    Statement createStatement = null;
    DatabaseMetaData dbmd = null;

    public void createTable() {
        try {
            conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (SQLException e) {
           e.getMessage();
        }
        if (conn != null) {
            try {
                createStatement = conn.createStatement();
            } catch (SQLException e) {
                e.getMessage();
            }
        }
        try {
            dbmd = conn.getMetaData();
        } catch (SQLException e) {
            e.getMessage();
        }
        try {
            ResultSet rs = dbmd.getTables(null, null, "%", null);
            if (!rs.next()) {
                createStatement.execute("CREATE TABLE IF NOT EXISTS persons ( `id` INT NOT NULL AUTO_INCREMENT , `firstname` VARCHAR(20) NOT NULL , `lastname` VARCHAR(20) NOT NULL ,`finger` VARCHAR(6000) NOT NULL, `occupation` VARCHAR(20) NOT NULL , `password` VARCHAR(10) NOT NULL, PRIMARY KEY (`id`)) ENGINE = InnoDB;");
            }
        } catch (SQLException e) {
           e.getMessage();
        }
    }

    /** create a new record
     * @param rs
     * @return
     */
    private Person createPerson(ResultSet rs) {
        Person p = null;
        try {
            p = new Person();
            p.setId(rs.getString("id"));
            p.setFirstname(rs.getString("firstname"));
            p.setLastname(rs.getString("lastname"));
            p.setFinger(rs.getString("finger"));
            p.setOccupation(rs.getString("occupation"));
            p.setPassword(rs.getString("password"));
        } catch (SQLException e) {
            e.getMessage();
        }
        return p;
    }

    @Override
    public ObservableList<Person> get() {
        String sql = "SELECT * FROM persons";
        ObservableList<Person> list = FXCollections.observableArrayList();
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            createStatement = conn.createStatement();
            ResultSet rs = createStatement.executeQuery(sql);
            while (rs.next()) {
                Person p = createPerson(rs);
                list.add(p);
            }
            rs.close();
            conn.close();
        } catch (ClassNotFoundException | SQLException e) {
           e.getMessage();
        }
        return list;
    }

    @Override
    public void add(Person p) {
        String sql = "INSERT INTO persons (firstname, lastname, finger, occupation, password) VALUES (?,?,?,?,?)";
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, p.getFirstname());
            preparedStatement.setString(2, p.getLastname());
            preparedStatement.setString(3, p.getFinger());
            preparedStatement.setString(4, p.getOccupation());
            preparedStatement.setString(5,  p.getPassword());
            preparedStatement.execute();
            conn.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.getMessage();
        }
    }

    @Override
    public void remove(Person p) {
        String sql = "DELETE FROM persons WHERE id = ?";
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setInt(1, Integer.parseInt(p.getId()));
            preparedStatement.execute();
            conn.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.getMessage();
        }
    }

    @Override
    public void update(Person p) {
        String sql = "UPDATE persons SET firstname = ?, lastname = ?, finger = ?, occupation = ?, password = ? WHERE id = ?";
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, p.getFirstname());
            preparedStatement.setString(2, p.getLastname());
            preparedStatement.setString(3, p.getFinger());
            preparedStatement.setString(4, p.getOccupation());
            preparedStatement.setString(5, p.getPassword());
            preparedStatement.setString(6, p.getId());
            preparedStatement.execute();
            conn.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.getMessage();
        }

    }

}
