package us.hall.trz.osx;

import java.io.IOException;

/**
 * Exception for any read only type access
 * 
 * @author mjh
 *
 */
public class ReadOnlyException extends IOException {

	static final long serialVersionUID = -5133087319872937530L;
	
    /**
     * Constructs an instance of this class.
     */
    public ReadOnlyException() {
    }
    
    public ReadOnlyException(String message) {
    	super(message);
    }
}
