package eu.heliovo.monitoring.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;

public class ObjectCopyUtils {

	/**
	 * Copies all objects of the source collection and adds them to the target
	 * collection.
	 * 
	 * @param <I>
	 *            collection element type
	 * @param source
	 *            source collection
	 * @param target
	 *            target collection
	 * @return target collection
	 */
	public static <I> Collection<I> copyCollection(final Collection<I> source, final Collection<I> target) {
		for (final I object : source) {
			target.add(ObjectCopyUtils.copy(object));
		}
		return target;
	}

	/**
	 * Copies an object which must implement Serializable.
	 * 
	 * @param <I>
	 *            object type
	 * @param copyObject
	 *            object to copy
	 * @return copy
	 */
	public static <I> I copy(final I copyObject) {
		try {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
			final ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(copyObject);
			final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			final ObjectInputStream ois = new ObjectInputStream(bais);
			final I deepCopy = (I) ois.readObject();
			return deepCopy;
		} catch (final ClassCastException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
}
