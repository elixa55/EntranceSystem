package model;

import javafx.beans.property.SimpleStringProperty;

public class Person {
	
    /** members of bean
     * 
     */
    private SimpleStringProperty id;
    private SimpleStringProperty firstname;
    private SimpleStringProperty lastname;
    private SimpleStringProperty finger;
    private SimpleStringProperty occupation;
    private SimpleStringProperty password;

    /** constructors region
     * 
     */
    public Person() {
        id = new SimpleStringProperty();
        firstname = new SimpleStringProperty();
        lastname = new SimpleStringProperty();
        finger = new SimpleStringProperty();
        occupation = new SimpleStringProperty();
        password = new SimpleStringProperty();
    }

    public Person(String id, String firstname, String lastname, String finger, String occupation, String password) {
        this.id = new SimpleStringProperty(id);
        this.firstname = new SimpleStringProperty(firstname);
        this.lastname = new SimpleStringProperty(lastname);
        this.finger = new SimpleStringProperty(finger);
        this.occupation = new SimpleStringProperty(occupation);
        this.password = new SimpleStringProperty(password);
    }

    public Person(String lastname, String finger) {
        this.lastname = new SimpleStringProperty(lastname);
        this.finger = new SimpleStringProperty(finger);
    }
    
    public Person(String lastname, String finger, String password) {
        this.lastname = new SimpleStringProperty(lastname);
        this.finger = new SimpleStringProperty(finger);
        this.password = new SimpleStringProperty(password);
    }


    /** javafx getters and setters of members
     * @return
     */
    public final SimpleStringProperty idProperty() {
        return this.id;
    }

    public final String getId() {
        return this.idProperty().get();
    }

    public final void setId(final String id) {
        this.idProperty().set(id);
    }

    public final SimpleStringProperty lastnameProperty() {
        return this.lastname;
    }

    public final String getLastname() {
        return this.lastnameProperty().get();
    }

    public final void setLastname(final String lastname) {
        this.lastnameProperty().set(lastname);
    }
    
    public final SimpleStringProperty firstnameProperty() {
        return this.firstname;
    }

    public final String getFirstname() {
        return this.firstnameProperty().get();
    }

    public final void setFirstname(final String firstname) {
        this.firstnameProperty().set(firstname);
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
    
    public final SimpleStringProperty occupationProperty() {
        return this.occupation;
    }

    public final String getOccupation() {
        return this.occupationProperty().get();
    }

    public final void setOccupation(final String occupation) {
        this.occupationProperty().set(occupation);
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