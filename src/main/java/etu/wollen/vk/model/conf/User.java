package etu.wollen.vk.model.conf;

public final class User {
	
	private final long id;
	private final String firstName;
	private final String lastName;

	public User(long id, String firstName, String lastName) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
	}
	
	public long getId() {
		return id;
	}

	@Override
	public String toString() {
		return id + "{"+firstName+" "+lastName+"}";
	}
}
