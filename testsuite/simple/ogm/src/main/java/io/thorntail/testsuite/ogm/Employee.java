package io.thorntail.testsuite.ogm;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Arun Gupta
 */
@Entity
@NamedQueries({
	@NamedQuery(name = "Employee.findAll", query = "SELECT e FROM Employee e")
})
@XmlRootElement
public class Employee implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private int id;

	@Column(length = 40)
	private String name;

	public Employee() {
	}

	public Employee(String name) {
		this.name = name;
	}

	public Employee(int id, String name){
		this(name);
		setId(id);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name + " " + id;
	}

	@Override
	public boolean equals(Object obj) {
		if (null == obj)
			return false;
		if (!(obj instanceof Employee))
			return false;
		Employee that = (Employee) obj;
		if (that.name.equals(this.name) && that.id == this.id)
			return true;
		else
			return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.name);
	}

}