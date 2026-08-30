package fr.pederobien.sound.interfaces;

import java.util.Map;

public interface IEffectParametersHolder {

	/**
	 * @return The name of the effect associated to this holder.
	 */
	String getEffectName();

	/**
	 * Update the value of each parameter registered in the given map.
	 * 
	 * @param values A map that gather parameter's name / parameter's value.
	 */
	void update(Map<String, Object> values);

	/**
	 * Set the value of a parameter.
	 * 
	 * @param name  The parameter's name.
	 * @param value The parameter's value.
	 */
	void setValue(String name, Object value);

	/**
	 * Get the value of a parameter.
	 * 
	 * @param name The name of the parameter.
	 * @return The value of the parameter, if registered and defined, null otherwise.
	 */
	public Object getValue(String name);
}
