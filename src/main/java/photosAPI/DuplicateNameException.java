package photosAPI;

public class DuplicateNameException extends Exception {
	public DuplicateNameException() {
		this("There is more than one file with that name");
	}

	public DuplicateNameException(String message) {
		super(message);
	}
}
