package fr.pederobien.sound.impl;

import java.util.Map;
import java.util.StringJoiner;

import fr.pederobien.sound.interfaces.IEffectParametersHolder;

public class EffectParametersHolder implements IEffectParametersHolder {
	private final String effectName;
	private final EffectParameter[] parameters;

	/**
	 * Creates a holder that contains possible parameters to be updated.
	 * 
	 * @param parameters The list of parameters to update.
	 */
	public EffectParametersHolder(String effectName, Map<String, Class<?>> description) {
		this.effectName = effectName;
		this.parameters = new EffectParameter[description.size()];

		int index = 0;
		for (Map.Entry<String, Class<?>> entry : description.entrySet())
			parameters[index++] = new EffectParameter(entry.getKey(), entry.getValue());
	}

	@Override
	public String getEffectName() {
		return effectName;
	}

	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(",", "{", "}");
		joiner.add("name=" + getEffectName());
		for (EffectParameter parameter : parameters)
			joiner.add(String.format("%s=%s", parameter.getName(), parameter.getValue()));

		return joiner.toString();
	}

	/**
	 * Update the value of each parameter registered in the given map.
	 * 
	 * @param values A map that gather parameter's name / parameter's value.
	 */
	@Override
	public void update(Map<String, Object> values) {
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			EffectParameter parameter = getParameter(entry.getKey());
			if (parameter != null)
				parameter.setValue(entry.getValue());
		}
	}

	@Override
	public void setValue(String name, Object value) {
		EffectParameter parameter = getParameter(name);
		if (parameter != null)
			parameter.setValue(value);
	}

	/**
	 * Get the value of a parameter.
	 * 
	 * @param name The name of the parameter.
	 * @return The value of the parameter, if registered and defined, null otherwise.
	 */
	@Override
	public Object getValue(String name) {
		EffectParameter parameter = getParameter(name);
		return parameter == null ? null : parameter.getValue();
	}

	/**
	 * Get the effect parameter associated to the given name.
	 * 
	 * @param name The name of the parameter to retrieve.
	 * @return The effect parameter if registered, null otherwise.
	 */
	private EffectParameter getParameter(String name) {
		for (EffectParameter parameter : parameters)
			if (parameter.getName().equals(name))
				return parameter;

		return null;
	}

	private class EffectParameter {
		private final String name;
		private final Class<?> clazz;
		private Object value;

		/**
		 * Creates a parameter, it is the association of a name and a value.
		 * 
		 * @param name  The parameter's name.
		 * @param clazz The class of the parameter value.
		 */
		public EffectParameter(String name, Class<?> clazz) {
			this.name = name;
			this.clazz = clazz;

			value = null;
		}

		/**
		 * @return The name of the parameter.
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return The value of the parameter.
		 */
		public Object getValue() {
			return value;
		}

		/**
		 * Set the value of this parameter.
		 * 
		 * @param value The new value of the parameter.
		 */
		public void setValue(Object value) {
			if (value == null)
				return;

			if (!clazz.isInstance(value)) {
				String format = "%s's value datatype should be %s";
				throw new IllegalArgumentException(String.format(format, name, clazz.getSimpleName()));
			}

			this.value = value;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof EffectParameter))
				return false;

			EffectParameter other = (EffectParameter) obj;
			return name.equals(other.getName()) && value.equals(other.getValue());
		}

		@Override
		public String toString() {
			return String.format("%s=%s", getName(), getValue());
		}
	}
}
