/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * 
 *                                               Histórico de Revisões
 * --------------------------------------------------------------------------------------------------------------------
 * Autor                                      Data       ID Tarefa                       Comentários
 * --------------------------------------|------------|--------------|-------------------------------------------------
 * 04343650413 (pcc)                       06/09/2016                  Versão inicial.
 * 
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
package com.github.pcmnac.prilaku;

import java.util.HashMap;
import java.util.Map;

/**
 * @author pcmnac@gmail.com
 * 
 */
public class Pku {

    private class Pair {
        Class<?> behavior;
        Class<?> domainClass;

        /**
         * @param behavior
         * @param domainClass
         */
        public Pair(Class<?> behavior, Class<?> domainClass) {
            super();
            this.behavior = behavior;
            this.domainClass = domainClass;
        }

        /**
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((behavior == null) ? 0 : behavior.hashCode());
            result = prime * result + ((domainClass == null) ? 0 : domainClass.hashCode());
            return result;
        }

        /**
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Pair other = (Pair) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (behavior == null) {
                if (other.behavior != null)
                    return false;
            } else if (!behavior.equals(other.behavior))
                return false;
            if (domainClass == null) {
                if (other.domainClass != null)
                    return false;
            } else if (!domainClass.equals(other.domainClass))
                return false;
            return true;
        }

        private Pku getOuterType() {
            return Pku.this;
        }

    }

    private static Map<Class<?>, Map<Class<?>, Class<?>>> map = new HashMap<Class<?>, Map<Class<?>, Class<?>>>();

    private static Map<Pair, Object> cache = new HashMap<Pair, Object>();

    public Object get(Object domainObject, Class<?> behavior) {

        Object behaviorImpl = cache.get(new Pair(behavior, domainObject.getClass()));

        if (behavior == null) {
            try {

                Class<?> behaviorClass = map.get(behavior).get(domainObject).getClass();
                behaviorImpl = behaviorClass.newInstance();

            } catch (Exception e) {
                throw new RuntimeException("Error creating behavior instance", e);
            }
        }

        return behaviorImpl;

    }

    public void register(Class<?> domainClass, Class<?> behavior, Class<?> implementationClass) {

        Map<Class<?>, Class<?>> implementations = map.get(behavior);

        if (implementations == null) {
            implementations = new HashMap<Class<?>, Class<?>>();
            map.put(behavior, implementations);
        }

        implementations.put(domainClass, implementationClass);

    }

}
