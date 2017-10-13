package org.daisy.streamline.api.tasks;

import java.io.File;
import java.util.List;

import org.daisy.streamline.api.media.AnnotatedFile;

/**
 * Provides an abstract base for expanding tasks. 
 * 
 * @author Joel Håkansson
 *
 */
public abstract class ExpandingTask extends InternalTask { //NOPMD

	/**
	 * Creates a new expanding task with the specified name
	 * @param name the name of the task
	 */
	public ExpandingTask(String name) {
		super(name);
	}

	/**
	 * Resolves the task into other tasks based on the contents of the <code>input</code>.
	 * @param input input file
	 * @return returns a list of internal tasks
	 * @throws InternalTaskException throws InternalTaskException if something goes wrong.
	 */
	public abstract List<InternalTask> resolve(File input) throws InternalTaskException;
	
	/**
	 * Resolves the task into other tasks based on the contents of the <code>input</code>.
	 * @param input annotated input file
	 * @return returns a list of internal tasks
	 * @throws InternalTaskException throws InternalTaskException if something goes wrong.
	 */
	public abstract List<InternalTask> resolve(AnnotatedFile input) throws InternalTaskException;

}