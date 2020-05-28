package model;

import javafx.beans.property.SimpleStringProperty;

public class Person {
    private SimpleStringProperty id;
    private SimpleStringProperty name;
    private SimpleStringProperty finger;
    private SimpleStringProperty password;

    public Person() {
        id = new SimpleStringProperty();
        name = new SimpleStringProperty();
        finger = new SimpleStringProperty();
        password = new SimpleStringProperty();
    }

    public Person(String id, String name, String finger, String password) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.finger = new SimpleStringProperty(finger);
        this.password = new SimpleStringProperty(password);
    }

    public Person(String name, String finger) {
        this.name = new SimpleStringProperty(name);
        this.finger = new SimpleStringProperty(finger);
    }
    
    public Person(String name, String finger, String password) {
        this.name = new SimpleStringProperty(name);
        this.finger = new SimpleStringProperty(finger);
        this.password = new SimpleStringProperty(password);
    }


    public final SimpleStringProperty idProperty() {
        return this.id;
    }

    public final String getId() {
        return this.idProperty().get();
    }

    public final void setId(final String id) {
        this.idProperty().set(id);
    }

    public final SimpleStringProperty nameProperty() {
        return this.name;
    }

    public final String getName() {
        return this.nameProperty().get();
    }

    public final void setName(final String name) {
        this.nameProperty().set(name);
    }

    public final SimpleStringProperty fingerProperty() {
        return this.finger;
    }

    public final String getFinger() {
        return this.fingerProperty().get();
    }

    public final void setFinger(final String finger) {
        this.fingerProperty().set(finger);
    }
    public final SimpleStringProperty passwordProperty() {
        return this.password;
    }

    public final String getPassword() {
        return this.passwordProperty().get();
    }

    public final void setPassword(final String password) {
        this.passwordProperty().set(password);
    }

   }