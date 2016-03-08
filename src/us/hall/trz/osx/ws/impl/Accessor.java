package us.hall.trz.osx.ws.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class Accessor {

	public static Object getForcedField(final Object target,final String declaredField) {
		return AccessController.doPrivileged(new PrivilegedAction() {
			Class cl = target.getClass();
			Field pf = null;
			public Object run() {
				/* 
				 * 
				 */
				try {
					pf = cl.getDeclaredField(declaredField);
					pf.setAccessible(true);
					return pf.get(target);
				} catch (NoSuchFieldException e) {
					/* Thrown if  serialPersistentField is not a data
					 * member of the class.
					 */
					e.printStackTrace();
					return null;
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					return null;
				} catch (IllegalArgumentException e) {
					/* Thrown if the field serialPersistentField is not
					 * static.
					 */
					 e.printStackTrace();
					return null;
				} catch (ClassCastException e) {
					/* Thrown if a field serialPersistentField exists
					 * but it is not of type ObjectStreamField.
					 */
					e.printStackTrace();
					return null;
				}
			}
		});
	}
			
	public static Object getForcedObject(final Object target,final Class[] types,final Object[] args) {
		return AccessController.doPrivileged(new PrivilegedAction() {
			Constructor cnst = null;
			public Object run() {
				/* 
				 * 
				 */
				try {
					Class cl = Class.forName(target.getClass().getName());
					cnst = cl.getDeclaredConstructor(types);
					cnst.setAccessible(true);
					return cnst.newInstance(args);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					return null;
				} catch (InstantiationException e) {
					e.printStackTrace();
					return null;
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					return null;
				} catch (IllegalArgumentException e) {
					/* Thrown if the field serialPersistentField is not
					 * static.
					 */
					 e.printStackTrace();
					return null;
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
					return null;
				} catch (InvocationTargetException e) {
					/* Thrown if a field serialPersistentField exists
					 * but it is not of type ObjectStreamField.
					 */
					e.getTargetException().printStackTrace();
					return null;
				}
			}
		});
	}
}
	